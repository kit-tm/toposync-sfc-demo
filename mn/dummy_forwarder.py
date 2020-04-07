# adapted from https://github.com/soarpenguin/python-scripts/blob/master/sniffer.py
import binascii
import socket, sys
import ipaddress
import argparse
from struct import *
from scapy.all import *
import time

#Convert a string of 6 characters of ethernet address into a dash separated hex string
def eth_addr (a) :
    b = "%.2x:%.2x:%.2x:%.2x:%.2x:%.2x" % (ord(a[0]) , ord(a[1]) , ord(a[2]), ord(a[3]), ord(a[4]) , ord(a[5]))
    return b

parser = argparse.ArgumentParser()
parser.add_argument('--v', help="verbose", action="store_true")
args = parser.parse_args()


s = socket.socket( socket.AF_PACKET , socket.SOCK_RAW , socket.ntohs(0x0003))

eno1 = conf.L2socket(iface='eno1')
s8 = conf.L2socket(iface='dummy-eth1')
s9 = conf.L2socket(iface='dummy-eth2')

time_since_arp = {'10.0.0.10' : 0, 
                  '10.0.0.11' : 0,
                  '10.0.0.1'  : 0,
                  '0.0.0.0'   : 0}

def forward_arp(pkt, ip, socket, ts):
    if time_since_arp[ip] == 0 or ((ts - time_since_arp[ip]) > 10):
        if args.v:
            print('forwarding ARP')
        socket.send(pkt)
        time_since_arp[ip] = ts

while True:
    packet = s.recvfrom(10000)
    packet = packet[0]

    eth_length = 14
    eth_header = packet[:eth_length]
    eth = unpack('!6s6sH' , eth_header)
    eth_type = eth[2]
    eth_dst = eth_addr(packet[0:6])
    eth_src = eth_addr(packet[6:12])

    #if(args.v):
        #print('Ethernet[Dst: %s, Src: %s, EtherType: %s]' % (eth_dst, eth_src, hex(eth_type)))
        

    if eth_type == 0x0800: # IPv4
        ip_header = packet[eth_length:20+eth_length]
        ip_data = packet[20+eth_length:]
        iph = unpack('!BBHHHBBH4s4s' , ip_header)
        version_ihl = iph[0]
        version = version_ihl >> 4
        ihl = version_ihl & 0xF
        iph_length = ihl * 4
        ttl = iph[5]

        sentByDummy = (ttl == 62)
        if sentByDummy:
            continue

        protocol = iph[6]
        s_addr = socket.inet_ntoa(iph[8])
        d_addr = socket.inet_ntoa(iph[9])

        #if args.v:
            #print('IP[Proto: %s, Src: %s, Dst: %s, ttl: %s]' % (hex(protocol), s_addr, d_addr, ttl))
        
        if ttl == 42: # sent from mininet -> forward to hardware
            if args.v:
                print('forwarding to eno1')
            pkt = Ether(dst=eth_dst)/IP(dst=d_addr, src=s_addr, ttl=62, proto=protocol)/Raw(load=ip_data)
            eno1.send(pkt)
        elif s_addr == '10.0.0.10':
            if args.v:
                print('forwarding to s9')
            pkt = Ether(dst=eth_dst)/IP(dst=d_addr, src=s_addr, ttl=62, proto=protocol)/Raw(load=ip_data)
            s9.send(pkt)
        elif s_addr == '10.0.0.11':
            if args.v:
                print('forwarding to s8')
            pkt = Ether(dst=eth_dst)/IP(dst=d_addr, src=s_addr, ttl=62, proto=protocol)/Raw(load=ip_data)
            s8.send(pkt) 


    elif eth_type == 0x0806: # ARP
        arp_pkt = packet[eth_length:28+eth_length]

        arp = unpack('2s2s1s1s2s6s4s6s4s', arp_pkt)

        src_mac = eth_addr(arp_pkt[8:14])
        src_ip = socket.inet_ntoa(arp[6])
        dst_mac = eth_addr(arp_pkt[18:24])
        dst_ip = socket.inet_ntoa(arp[8])

        if args.v:
            print('ARP[SrcMAC:%s, SrcIP:%s, DstMAC:%s, DstIP:%s' % (src_mac, src_ip, dst_mac, dst_ip))

        out_sock = None
        if src_ip == '10.0.0.10':
            out_sock = s9
        elif src_ip == '10.0.0.11':
            out_sock = s8
        elif src_ip == '10.0.0.1' or src_ip == '0.0.0.0':
            out_sock = eno1
        
        current_ts = time.time()

        forward_arp(packet, src_ip, out_sock, current_ts)