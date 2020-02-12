import sys 
sys.path.append("/home/felix/Desktop/ba/code")

import time
import threading

from mininet.net import Containernet
from mininet.link import TCLink
from mininet.node import RemoteController, Docker
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from mn.cn_rest import start_rest
import mn.topo.Topos

def main():
    setLogLevel('info')

    topo = mn.topo.Topos.TetraTopo()
    topo.addServer(topo.tbs[0])
    topo.addClients(topo.tbs[5], topo.tbs[10])

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

