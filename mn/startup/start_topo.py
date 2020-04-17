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

def startPing(shost):
    print('**Starting to ping from server to clients.')
    cmdString = 'python /home/felix/Desktop/toposync/mn/ping/ping.py &'
    printAndExecute(shost, cmdString)

    print('**Starting to plot ping results.')
    plotter_proc = subprocess.Popen(['python','/home/felix/Desktop/toposync/mn/ping/plotter.py', '&'])

    return plotter_proc

def startWhackAMole(shost, clients):
    SEND_PERIOD_MS = 500
    print('**Starting whack-a-mole server.')
    printAndExecute(shost, 'java -jar ../../whack-a-mole-server/target/whack-a-mole-server-1.0-SNAPSHOT.jar %s &' % SEND_PERIOD_MS)

    if clients is not None:
        print('**Starting whack-a-mole clients.')
        for client in clients:
            printAndExecute(client, 'java -jar ../../whack-a-mole-client/target/whack-a-mole-client-1.0-SNAPSHOT.jar %s &' % client.IP())

def topoInstance(topo, hw):
    inst = None
    if topo == "tetra":
        inst = mn.topo.Topos.TetraTopo(hw=hw)
    elif topo == "paper":
        inst = mn.topo.Topos.PaperTopo(hw=hw)
    else:
        print("[ERROR] Invalid topology: %s" % topo)

    return inst

def addDummyHost(net, topo):
    print('**Adding dummy host (hardware setup).')
    net.addHost('dummy')
    _intf = Intf(INTERFACE, node=net.getNodeByName('dummy'))
    client_switches = list(map(net.getNodeByName, topo.getClientSwitches()))
    for sw in client_switches:
        net.addLink(sw, net.getNodeByName('dummy'))

def setUpDummy(net):
    dummy = net.getNodeByName('dummy')
    print('*Starting dummy_forwarder on dummy.')
    printAndExecute(dummy, 'ip link set lo up')  # avoid bind error due to missing loopback intf
    printAndExecute(dummy, 'python ../dummy_forwarder.py &')
    time.sleep(3)
    print('**Pinging clients from dummy.')
    printAndExecute(dummy, 'ping -I eno1 -c 1 10.0.0.11')
    printAndExecute(dummy, 'ping -I eno1 -c 1 10.0.0.10')

def startREST(net):
    print("**Starting containernet REST Server.")
    thr = threading.Thread(target=start_rest, args=(net,)) # comma behind net is on purpose
    thr.daemon = True
    thr.start()

def arpHw(net):
    print('**Starting to ARP from server (hardware setup).')
    server = net.getNodeByName('shost')
    startARP(server, server.IP(), server.MAC(), '10.0.0.20', server.intf())

def arpEmulated(net):
    print('**Starting to ARP (emulated setup).')
    hosts = net.hosts
    reqHost = hosts[0]
    for host in hosts:
        if(host is not reqHost):
            startARP(reqHost, reqHost.IP(), reqHost.MAC(), host.IP(), reqHost.intf())



def main():
    parser = argparse.ArgumentParser(description='Starts a containernet topology and a REST server for VNF instantiation from controller.')
    parser.add_argument('topology', metavar='T', type=str, nargs=1, help="the topology to start")
    parser.add_argument('--hw', action='store_true')
    args = parser.parse_args()

    topo = topoInstance(args.topology[0], args.hw)

    setLogLevel('info')

    net = Containernet(controller=RemoteController, topo=topo, build=True, autoSetMacs=True, link=TCLink)

    if args.hw:
        addDummyHost(net, topo)

    net.start()

    # for VNF mgmt
    startREST(net)

    # wait for connection with controller
    time.sleep(3)

    # ARP
    if args.hw:
        arpHw(net)
    else:
        arpEmulated(net)

    # dummy
    if args.hw:
        setUpDummy(net)
 
    # plot
    shost = net.getNodeByName('shost')
    plotter_proc = startPing(shost)

    # whack a mole
    if args.hw:
        startWhackAMole(shost, None)
    else:
        clients = [net.getNodeByName('c1host'), net.getNodeByName('c2host')]
        startWhackAMole(shost, clients)

    # standby
    CLI(net)

    # cleanup
    plotter_proc.kill()
    net.stop()

if __name__ == '__main__':
    main()

