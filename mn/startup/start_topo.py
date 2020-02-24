import sys 
sys.path.append("/home/felix/Desktop/toposync")

import time
import threading

import argparse

from mininet.net import Containernet
from mininet.link import TCLink
from mininet.node import RemoteController, Docker
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from mn.cn_rest import start_rest
import mn.topo.Topos

def printAndExecute(host, cmdString):
    print("%s: %s" % (host.name, cmdString))
    host.cmd(cmdString)

def startARP(host, srcIP, srcMAC, dstIP, iface):
    cmdString = 'python /home/felix/Desktop/ba/code/mn/hosts/cli/ARP/ARP_client.py \"%s\" %s %s %s' % (srcIP, dstIP, srcMAC, iface)
    printAndExecute(host, cmdString)

def main():
    parser = argparse.ArgumentParser(description='Starts a containernet topology and a REST server for VNF instantiation from controller.')
    parser.add_argument('topology', metavar='T', type=str, nargs=1, help="the topology to start")

    args = parser.parse_args()

    topo = args.topology[0]

    if topo == "tetra":
        topo = mn.topo.Topos.TetraTopo()
        topo.addServer(topo.tbs[0])
        topo.addClients([topo.tbs[5], topo.tbs[10]])
    elif topo == "paper":
        topo = mn.topo.Topos.PaperTopo()
    else:
        print("[ERROR] Invalid topology: %s" % topo)


    setLogLevel('info')



    net = Containernet(controller=RemoteController, topo=topo, build=False, autoSetMacs=True, link=TCLink)
    net.start()

    print("**Starting containernet REST Server.")
    thr = threading.Thread(target=start_rest, args=(net,)) # comma behind net is on purpose
    thr.daemon = True
    thr.start()

    # wait for connection with controller
    time.sleep(3)

    hosts = net.hosts

    # send arp from reqHost to every other host -> required by ONOS HostService to resolve hosts (i.e. map MAC<->IP)
    reqHost = hosts[0]
    for host in hosts:
        if(host is not reqHost):
            startARP(reqHost, reqHost.IP(), reqHost.MAC(), host.IP(), reqHost.intf())

    CLI(net)

    net.stop()

if __name__ == '__main__':
    main()
