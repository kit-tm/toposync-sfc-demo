import sys 
sys.path.append("/home/felix/Desktop/toposync")

import time
import threading

from mininet.net import Containernet
from mininet.link import TCLink
from mininet.node import RemoteController, Docker
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from mn.topo import Topos

def startARP(host, srcIP, srcMAC, dstIP, iface):
    cmdString = 'python /home/felix/Desktop/toposync/mn/ARP_client.py \"%s\" %s %s %s' % (srcIP, dstIP, srcMAC, iface)
    printAndExecute(host, cmdString)

def printAndExecute(host, cmdString):
    print("%s: %s" % (host.name, cmdString))
    host.cmd(cmdString)

def main():
    setLogLevel('info')

    topo = Topos.TetraTopo()
    topo.addServer(topo.tbs[0])
    topo.addClients([topo.tbs[5], topo.tbs[10]])

    net = Containernet(controller=RemoteController, topo=topo, build=False, autoSetMacs=True, link=TCLink)
    net.start()

    # wait for connection with controller
    time.sleep(5)

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

