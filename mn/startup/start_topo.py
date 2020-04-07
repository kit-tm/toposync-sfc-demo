import sys 
sys.path.append("/home/felix/Desktop/toposync")

import re
import time
import threading
import argparse
import subprocess

from mininet.net import Containernet
from mininet.link import TCLink, Intf
from mininet.node import RemoteController, Docker, Node, Host
from mininet.cli import CLI
from mininet.log import setLogLevel, info, error
from mn.cn_rest import start_rest
import mn.topo.Topos
from mininet.util import quietRun

INTERFACE = 'eno1'

def checkIntf( intf ):
    "Make sure intf exists and is not configured."
    config = quietRun( 'ifconfig %s 2>/dev/null' % intf, shell=True )
    if not config:
        error( 'Error:', intf, 'does not exist!\n' )
        exit( 1 )
    ips = re.findall( r'\d+\.\d+\.\d+\.\d+', config )
    if ips:
        error( 'Error:', intf, 'has an IP address,'
               'and is probably in use!\n' )
        exit( 1 )

def printAndExecute(host, cmdString):
    print("%s: %s" % (host.name, cmdString))
    host.cmd(cmdString)

def startARP(host, srcIP, srcMAC, dstIP, iface):
    cmdString = 'python /home/felix/Desktop/toposync/mn/ARP_client.py \"%s\" %s %s %s' % (srcIP, dstIP, srcMAC, iface)
    printAndExecute(host, cmdString)

def startPing(host):
    cmdString = 'python /home/felix/Desktop/toposync/mn/ping/ping.py &'
    printAndExecute(host, cmdString)


def main():
    parser = argparse.ArgumentParser(description='Starts a containernet topology and a REST server for VNF instantiation from controller.')
    parser.add_argument('topology', metavar='T', type=str, nargs=1, help="the topology to start")
    parser.add_argument('--hw', action='store_true')

    args = parser.parse_args()

    topo = args.topology[0]

    if topo == "tetra":
        topo = mn.topo.Topos.TetraTopo(hw=args.hw)
    elif topo == "paper":
        topo = mn.topo.Topos.PaperTopo(hw=args.hw)
    else:
        print("[ERROR] Invalid topology: %s" % topo)


    setLogLevel('info')

    net = Containernet(controller=RemoteController, topo=topo, build=True, autoSetMacs=True, link=TCLink)

    if args.hw:
        print('**Adding dummy host (hardware setup).')
        net.addHost('dummy')
        _intf = Intf(INTERFACE, node=net.getNodeByName('dummy'))
        client_switches = list(map(net.getNodeByName, topo.getClientSwitches()))
        for sw in client_switches:
            net.addLink(sw, net.getNodeByName('dummy'))

    net.start()

    print("**Starting containernet REST Server.")
    thr = threading.Thread(target=start_rest, args=(net,)) # comma behind net is on purpose
    thr.daemon = True
    thr.start()

    # wait for connection with controller
    time.sleep(3)

    hosts = net.hosts

    # send arp from reqHost to every other host -> required by ONOS HostService to resolve hosts (i.e. map MAC<->IP)
    if args.hw:
        print('**Starting to ARP from server (hardware setup).')
        server = net.getNodeByName('shost')
        startARP(server, server.IP(), server.MAC(), '10.0.0.20', server.intf())
    else:
        print('**Starting to ARP for ONOS host discovery.')
        reqHost = hosts[0]
        for host in hosts:
            if(host is not reqHost):
                startARP(reqHost, reqHost.IP(), reqHost.MAC(), host.IP(), reqHost.intf())

    if args.hw:
        dummy = net.getNodeByName('dummy')
        print('*Starting dummy_forwarder on dummy.')
        printAndExecute(dummy, 'ip link set lo up')  # avoid bind error due to missing loopback intf
        printAndExecute(dummy, 'python ../dummy_forwarder.py &')
        time.sleep(3)
        print('**Pinging clients from dummy.')
        printAndExecute(dummy, 'ping -I eno1 -c 1 10.0.0.11')
        printAndExecute(dummy, 'ping -I eno1 -c 1 10.0.0.10')
 

    # start to ping from server to clients
    print('**Starting to ping from server to clients.')
    shost = net.getNodeByName('shost')
    startPing(shost)

    # start to plot ping results
    print('**Starting to plot ping results.')
    p = subprocess.Popen(['python','/home/felix/Desktop/toposync/mn/ping/plotter.py', '&'])

    CLI(net)

    p.kill()
    net.stop()

if __name__ == '__main__':
    main()

