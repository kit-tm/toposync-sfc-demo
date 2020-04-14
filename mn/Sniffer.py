# adapted from https://github.com/soarpenguin/python-scripts/blob/master/sniffer.py
import binascii
import socket, sys
import ipaddress
import argparse
import time
import csv
from struct import *
from scapy.all import *
import threading
import xml.etree.ElementTree as ET

#Convert a string of 6 characters of ethernet address into a dash separated hex string
def eth_addr (a) :
    b = "%.2x:%.2x:%.2x:%.2x:%.2x:%.2x" % (ord(a[0]) , ord(a[1]) , ord(a[2]), ord(a[3]), ord(a[4]) , ord(a[5]))
    return b

parser = argparse.ArgumentParser()
parser.add_argument("iface", help="layer 2 interface to redirect multicast packets to", type=str)
parser.add_argument("delay", help="delay which should be added by this VNF", type=int)
parser.add_argument("name", help="VNF name to concatenate to UDP payload", type=str)
parser.add_argument('--v', help="verbose", action="store_true")
args = parser.parse_args()

s = socket.socket( socket.AF_PACKET , socket.SOCK_RAW , socket.ntohs(0x0003))

scapy_sock = conf.L2socket(iface=args.iface)

def debug_send(pkt):
    if args.v:
        print ("sending now!")
    scapy_sock.send(pkt)

def ttl_mac(vnf_name):
    ether_dst = None
    ttl = None
    if args.name == "TRANSCODER" or args.name == "TRANSCODER_accelerated":
        ether_dst = "22:22:22:22:22:22"
        ttl = 42
    elif args.name == "INTRUSION_DETECTION":
        ether_dst = "33:33:33:33:33:33"
        ttl = 43
    return (ttl, ether_dst)

def forward_delayed(pkt, delay, reception_time):
    #print("from receive to before send: %s" % (int(round(time.time() * 1000)) - reception_time))
    before_send = int(round(time.time() * 1000))
    if before_send - reception_time >= args.delay: # if delay by unpacking the packet already exceeds the delay, instantly send the packet back
        #print("sending packet right away")
        scapy_sock.send(pkt)
    else:
        #print("scheduling sending for in %s seconds." % str((args.delay - (before_send - reception_time)) / 1000.0))
        t = threading.Timer((args.delay - (before_send - reception_time)) / 1000.0, debug_send, args=[pkt])
        t.start()

def transcode_to_csv(xml_data):
    csv_string = ""
    if args.v:
        print('need to transcode %s' % xml_data)
    root = ET.fromstring(xml_data)
    for idx, child in enumerate(root):
        if child.tag == 'mole':
            row, col = (child.attrib['row'], child.attrib['col'])
            if args.v:
                print('row: %s' % row)
                print('col: %s' % col)
                print('...')
            csv_string += (row + ',' + col)
        elif child.tag == 'round':
            roundCnt = child.attrib['count']
            if args.v:
                print('round_count: %s' % roundCnt)
            csv_string += roundCnt

        if idx != len(root)-1:
            csv_string += '\n'

    
    if args.v:
        print('transcoded:\n\"%s\"' % csv_string)

    return csv_string



while True:
    packet = s.recvfrom(10000)

    # time of reception
    reception_time = int(round(time.time() * 1000))

    packet = packet[0]

    eth_length = 14

    eth_header = packet[:eth_length]
    eth = unpack('!6s6sH' , eth_header)
    eth_type = eth[2]
    eth_dst = eth_addr(packet[0:6])
    eth_src = eth_addr(packet[6:12])
    if(args.v):
        print('Ethernet[Dst: %s, Src: %s, EtherType: %s]' % (eth_dst, eth_src, hex(eth_type)))

    if eth_type == 0x0800: # IPv4
        ip_header = packet[eth_length:20+eth_length]

        ip_data = packet[20+eth_length:]

        iph = unpack('!BBHHHBBH4s4s' , ip_header)

        version_ihl = iph[0]
        version = version_ihl >> 4
        ihl = version_ihl & 0xF

        iph_length = ihl * 4

        ttl = iph[5]
        protocol = iph[6]
        s_addr = socket.inet_ntoa(iph[8])
        d_addr = socket.inet_ntoa(iph[9])

        if args.v:
            print('IP[Proto: %s, Src: %s, Dst: %s, ttl: %s]' % (hex(protocol), s_addr, d_addr, ttl))

        # ttl 42 are packets sent Transcoder, ttl 43 sent by intrusion detection -> ignore packets sent by "us" -> avoid processing packets several times if looped back to the VNF
        if( ((args.name == "TRANSCODER" or args.name == "TRANSCODER_accelerated") and ttl == 42) or  args.name == "INTRUSION_DETECTION" and ttl == 43): 
            continue
            

        ip_dst = ipaddress.IPv4Address(iph[9])

        if str(ip_dst) == "10.0.0.1": # ping from receiver back to source
            #print("directly forwarding reverse ping")
            ttl,_ = ttl_mac(args.name)
            pkt = Ether(dst=eth_dst)/IP(dst=d_addr, src=s_addr, ttl=ttl, proto=protocol)/Raw(load=ip_data)
            scapy_sock.send(pkt)

        ## UDP
        if protocol == 17:
            u = iph_length + eth_length
            udph_length = 8
            udp_header = packet[u:u+8]

            #now unpack them :)
            udph = unpack('!HHHH' , udp_header)

            source_port = udph[0]
            dest_port = udph[1]
            length = udph[2]
            checksum = udph[3]

            h_size = eth_length + iph_length + udph_length
            data_size = len(packet) - h_size

            #get data from the packet
            data = packet[h_size:]
            if args.v:
                print("UDP[SrcPort: %s, DstPort: %s, Len: %s, Checksum: %s, Data: %s" % (source_port, dest_port, length, hex(checksum), binascii.hexlify(data)))
            
            if dest_port == 9090:
                if args.v:
                    print('whack-a-mole packet!')

                ttl, mac_dst = ttl_mac(args.name)

                transcoded_data = transcode_to_csv(str(data))

                pkt = Ether(dst=mac_dst)/IP(dst=d_addr, src=s_addr, ttl=ttl, proto=protocol)/UDP(sport=source_port,dport=dest_port)/Raw(load=transcoded_data)

                forward_delayed(pkt, args.delay, reception_time)
                

        elif ip_dst.is_multicast: # just delay everything else sent towards the group
            ttl, mac_dst = ttl_mac(args.name)
            pkt = Ether(dst=mac_dst)/IP(dst=d_addr, src=s_addr, ttl=ttl, proto=protocol)/Raw(load=ip_data)
            forward_delayed(pkt, args.delay, reception_time)

        # protocol other than UDP
        else :
            if args.v:
                print('unhandled IP protocol: ' + str(protocol))
