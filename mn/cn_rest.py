import BaseHTTPServer

class CnRESTRequestHandler( BaseHTTPServer.BaseHTTPRequestHandler ):
    cn_net = None
    container = "own_test_container:ubuntu1804"

    def _get_switch_by_dpid(self, dpid):
        switch_by_dpid = None
        print("looking for dpid: %s" % dpid)
        for switch in self.cn_net.switches:
            if(switch.dpid == dpid):
                switch_by_dpid = switch
        print("found switch by dpid: %s" % switch_by_dpid)
        return switch_by_dpid

    def _vnf_host_name(self, vnf_type):
        return "v%s" % str(vnf_type).lower()[0:2]

    def do_DELETE(self):
        print("cn_net: %s" % self.cn_net)
        print("self.path: %s" % self.path)
        splitted_path = self.path[1:].split('/') #[1:] because path starts with / -> .split('/') would have size 3
        self.remove(splitted_path[0],splitted_path[1])

    def remove(self, dpid, vnf_type):
        switch_by_dpid = self._get_switch_by_dpid(dpid)
        host_name = self._vnf_host_name(vnf_type)
        print("removing %s@%s" %(host_name, switch_by_dpid))

        print("removing link")
        self.cn_net.removeLink(node1=str(switch_by_dpid), node2=str(host_name))

        print("removing docker host")
        self.cn_net.removeDocker(host_name)

        print("finished removing")

        self.send_response(200)
        self.end_headers()
        self.wfile.close()


    def do_PUT(self):
        print("cn_net: %s" % self.cn_net)
        print("self.path: %s" % self.path)
        splitted_path = self.path[1:].split('/') #[1:] because path starts with / -> .split('/') would have size 3
        self.instantiate(splitted_path[0], splitted_path[1])

    def instantiate(self, dpid, vnf_type):
        print("instantiating %s at %s" % (vnf_type, dpid))

        # create and add docker host
        volumes = ["/home:/home"] # code sync
        host_name = self._vnf_host_name(vnf_type)
        vnf_docker_host = self.cn_net.addDocker(host_name, dimage=self.container, volumes=volumes)
        print("created host: %s" % vnf_docker_host)

        # get switch by dpid
        switch_by_dpid = self._get_switch_by_dpid(dpid)

        # create link between host and switch
        link = self.cn_net.addLink(vnf_docker_host, switch_by_dpid)
        print("connected host and switch via link %s" % link)

        # start sniffer on docker host
        cmd_string = ""
        processing_delay = None
        if vnf_type.endswith("_accelerated"):
            processing_delay = 100
        else:
            processing_delay = 400

        cmd_string = "python /home/felix/Desktop/toposync/mn/Sniffer.py %s %s %s &" % ((host_name + '-eth0'), processing_delay, vnf_type)
        print("starting sniffer: %s: %s" % (host_name, cmd_string))
        vnf_docker_host.cmd(cmd_string)

        vnf_connect_port_str = str(link.intf2)
        vnf_connect_port = vnf_connect_port_str[(vnf_connect_port_str.find('eth') + 3):]
 
        self.send_response(200)
        self.end_headers()
        self.wfile.write(vnf_connect_port)
        self.wfile.close()




class WrappedHTTPServer:
    def __init__(self, net):
        CnRESTRequestHandler.cn_net = net
        myServer = BaseHTTPServer.HTTPServer(("127.0.0.1", 9000), CnRESTRequestHandler)
        myServer.serve_forever()


def start_rest(net):
    WrappedHTTPServer(net)

