import argparse
from scapy.all import *

parser = argparse.ArgumentParser()
parser.add_argument("sourceIP", type=str)
parser.add_argument("destinationIP", type=str)
parser.add_argument("sourceMAC", type=str)
parser.add_argument('interface', help='layer2 interface', type=str)
args = parser.parse_args()

arp = Ether()/ARP(hwsrc=args.sourceMAC, psrc=args.sourceIP, pdst=args.destinationIP, hwdst="00:00:00:00:00:00")
sendp(arp, iface=args.interface)