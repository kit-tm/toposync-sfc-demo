from mininet.topo import Topo
from mininet.log import info

# 0x1 and 0x2 are PoPs

def create_dpid_from_id(msb, id):
    "create DPID with given MSB and ID (LSBs). Zero Pad in between to fill 16 character"
    id = str(id)

    dpid = msb + id.zfill(15)

    return dpid

def addLinkAndDebugPrint(topo, node1, node2):
    if node1.endswith("host") or node2.endswith("host"):
        topo.addLink(node1, node2)
        print('added link %s <-> %s' % (node1, node2))
    else:
        topo.addLink(node1, node2, **{'delay': '50ms'})
        print('added link %s <-> %s (artificial delay)' % (node1, node2))
    

class PaperTopo( Topo ):
        def __init__(self):
            Topo.__init__(self)
            self._nodes = [None] # None because we want indices to start at 1 (consistent numbering with full paper figure)
            self.addNodes()
            self.addInfrastructureLinks()
            self.addHosts()

        def addHosts(self):
            client1 = self.addHost('c1host', ip='10.0.0.10')
            addLinkAndDebugPrint(self, client1, self._nodes[9])

            client2 = self.addHost('c2host', ip='10.0.0.11')
            addLinkAndDebugPrint(self, client2, self._nodes[8])

            server = self.addHost('shost', ip='10.0.0.1')
            addLinkAndDebugPrint(self, server, self._nodes[1])

        def addNodes(self):
            for i in range(1, 10):
                dpid = create_dpid_from_id(str(1), str(i))
                added = self.addSwitch('s%s' % i, dpid=dpid)
                self._nodes.append(added)

        def addInfrastructureLinks(self):
            # top row
            addLinkAndDebugPrint(self, self._nodes[1], self._nodes[2])
            addLinkAndDebugPrint(self, self._nodes[2], self._nodes[3])
            addLinkAndDebugPrint(self, self._nodes[3], self._nodes[4])

            # lower row
            addLinkAndDebugPrint(self, self._nodes[5], self._nodes[6])
            addLinkAndDebugPrint(self, self._nodes[6], self._nodes[7])
            addLinkAndDebugPrint(self, self._nodes[7], self._nodes[8])

            # connect upper row and lower row
            addLinkAndDebugPrint(self, self._nodes[2], self._nodes[5])
            addLinkAndDebugPrint(self, self._nodes[3], self._nodes[6])
            addLinkAndDebugPrint(self, self._nodes[4], self._nodes[7])

            # s9
            addLinkAndDebugPrint(self, self._nodes[9], self._nodes[5])
        
            

# DXTTs, DXTs and TBSs are switches, each TBS is connected to exactly one host
# DXTTs DPID: 0x0 + id
# DXTs DPID:  0x1 + id
# TBS DPID:   0x2 + id
class TetraTopo( Topo ):

    def __init__(self):
        Topo.__init__(self)
        self.dxtt = []
        self.dxt = []
        self.tbs = []
        self.tbsHosts = []
        self.dwsHosts = []
        self.addNodes()
        self.addLinks()
        

    def addNodes(self):
        self.addDxtt()
        self.addDxt()
        self.addTbs()

    def addDxtt(self):
        # add 4 DXTTs
        for i in range(4):
            self.dxtt.append(self.addSwitch('dxtt%s' % (i + 1), dpid=str(i+1)))

    def addDxt(self):
        # add 10 DXTs
        for i in range(10):
            id = i + 1 # ID (LSBs of DPID)

            dpid = create_dpid_from_id("1", id) # DPID
             
            # finally add DXT switch with DPID
            self.dxt.append(self.addSwitch('dxt%s' % id, dpid=dpid))

    def addTbs(self):
        # add TBSs
        for i in range(24):
            id = i + 1 # ID

            dpid = create_dpid_from_id("2", id) # DPID

            self.tbs.append(self.addSwitch('tbs%s' % id, dpid=dpid))

    def addServer(self, node):
        print("adding server..")
        server = self.addHost('shost', ip='10.0.0.1')
        addLinkAndDebugPrint(self, server, node)

    def addClients(self, nodes):
        print("adding clients..")
        for idx, node in enumerate(nodes):
            print("idx %d, node %s" %(idx, node))
            client = self.addHost('c%ihost' % idx, ip='10.0.0.%i' % (idx+10))
            addLinkAndDebugPrint(self, client, node)
        

    def addLinks(self):
        self.addDxttInterconnectLinks()
        self.addDxtToDxttLinks()
        self.addTbsToDxtLinks()

    def addDxttInterconnectLinks(self):
        # interconnect DXTTs (full mesh)
        for i in range(4):
            for j in range(i+1, 4):
                addLinkAndDebugPrint(self, self.dxtt[i], self.dxtt[j])

    def addDxtToDxttLinks(self):
        # connect DXTs with DXTTs (each DXT connected to 2 DXTTs)
        addLinkAndDebugPrint(self, self.dxt[0], self.dxtt[0])
        addLinkAndDebugPrint(self, self.dxt[0], self.dxtt[1])
        addLinkAndDebugPrint(self, self.dxt[1], self.dxtt[0])
        addLinkAndDebugPrint(self, self.dxt[1], self.dxtt[1])
        addLinkAndDebugPrint(self, self.dxt[2], self.dxtt[1])
        addLinkAndDebugPrint(self, self.dxt[2], self.dxtt[3])
        addLinkAndDebugPrint(self, self.dxt[3], self.dxtt[1])
        addLinkAndDebugPrint(self, self.dxt[3], self.dxtt[3])
        addLinkAndDebugPrint(self, self.dxt[4], self.dxtt[1])
        addLinkAndDebugPrint(self, self.dxt[4], self.dxtt[3])
        addLinkAndDebugPrint(self, self.dxt[5], self.dxtt[2])
        addLinkAndDebugPrint(self, self.dxt[5], self.dxtt[3])
        addLinkAndDebugPrint(self, self.dxt[6], self.dxtt[2])
        addLinkAndDebugPrint(self, self.dxt[6], self.dxtt[3])
        addLinkAndDebugPrint(self, self.dxt[7], self.dxtt[0])
        addLinkAndDebugPrint(self, self.dxt[7], self.dxtt[2])
        addLinkAndDebugPrint(self, self.dxt[8], self.dxtt[2])
        addLinkAndDebugPrint(self, self.dxt[8], self.dxtt[0])
        addLinkAndDebugPrint(self, self.dxt[9], self.dxtt[0])
        addLinkAndDebugPrint(self, self.dxt[9], self.dxtt[2])

    def addTbsToDxtLinks(self):
        # connect TBSs with DXTs (ring or star)
        addLinkAndDebugPrint(self, self.tbs[0], self.dxt[0])
        addLinkAndDebugPrint(self, self.tbs[1], self.dxt[0])

        addLinkAndDebugPrint(self, self.tbs[2], self.dxt[1])
        addLinkAndDebugPrint(self, self.tbs[3], self.dxt[1])
        addLinkAndDebugPrint(self, self.tbs[4], self.dxt[1])

        addLinkAndDebugPrint(self, self.tbs[5], self.dxt[2])
        addLinkAndDebugPrint(self, self.tbs[6], self.dxt[2])
        addLinkAndDebugPrint(self, self.tbs[6], self.tbs[5])

        addLinkAndDebugPrint(self, self.tbs[7], self.dxt[3])
        addLinkAndDebugPrint(self, self.tbs[8], self.dxt[3])

        addLinkAndDebugPrint(self, self.tbs[9], self.dxt[4])
        addLinkAndDebugPrint(self, self.tbs[10], self.dxt[4])

        addLinkAndDebugPrint(self, self.tbs[12], self.dxt[5])
        addLinkAndDebugPrint(self, self.tbs[11], self.dxt[5])
        addLinkAndDebugPrint(self, self.tbs[12], self.tbs[11])

        addLinkAndDebugPrint(self, self.tbs[13], self.dxt[6])
        addLinkAndDebugPrint(self, self.tbs[15], self.dxt[6])
        addLinkAndDebugPrint(self, self.tbs[15], self.tbs[14])
        addLinkAndDebugPrint(self, self.tbs[14], self.tbs[13])

        addLinkAndDebugPrint(self, self.tbs[16], self.dxt[7])
        addLinkAndDebugPrint(self, self.tbs[17], self.dxt[7])
        addLinkAndDebugPrint(self, self.tbs[17], self.tbs[16])

        addLinkAndDebugPrint(self, self.tbs[18], self.dxt[8])
        addLinkAndDebugPrint(self, self.tbs[19], self.dxt[8])
        addLinkAndDebugPrint(self, self.tbs[20], self.dxt[8])

        addLinkAndDebugPrint(self, self.tbs[21], self.dxt[9])
        addLinkAndDebugPrint(self, self.tbs[23], self.dxt[9])
        addLinkAndDebugPrint(self, self.tbs[21], self.tbs[22])
        addLinkAndDebugPrint(self, self.tbs[22], self.tbs[23])


