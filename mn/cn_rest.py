import BaseHTTPServer


class CnRESTRequestHandler( BaseHTTPServer.BaseHTTPRequestHandler ):
    cn_net = None
    type_container_dict = {"TRANSCODER":"own_test_container:ubuntu1804",
                            "INTRUSION_DETECTION": "own_test_container:ubuntu1804",
                            "TRANSCODER_accelerated":"own_test_container:ubuntu1804",
                            "INTRUSION_DETECTION_accelerated": "own_test_container:ubuntu1804",}

    def do_PUT(self):
        print("cn_net: %s" % self.cn_net)
        print("self.path: %s" % self.path)
        splitted_path = self.path[1:].split('/') #[1:] because path starts with / -> .split('/') would have size 3
        self._instantiate(splitted_path[0], splitted_path[1])

    def _instantiate(self, dpid, vnf_type):
        print("instantiating %s at %s" % (vnf_type, dpid))

        # create and add docker host
        dimage = self.type_container_dict[vnf_type]
        volumes = ["/home:/home", # code sync
                   "/etc/localtime:/etc/localtime"] # timezone to host timezone
        host_name = "v%s%s" % (str(vnf_type).lower()[0:2], str(dpid)[1:(len(str(dpid)) - 1)].lstrip('0'))
        vnf_docker_host = self.cn_net.addDocker(host_name, dimage=dimage, volumes=volumes)
        print("created host: %s" % vnf_docker_host)

        # get switch by dpid
        switch_by_dpid = None
        print("looking for dpid: %s" % dpid)
        for switch in self.cn_net.switches:
            print("switch.dpid: %s" % switch.dpid)
            if(switch.dpid == dpid):
                switch_by_dpid = switch
        print("found switch by dpid: %s" % switch_by_dpid)

        # create link between host and switch
        link = self.cn_net.addLink(vnf_docker_host, switch_by_dpid)
        print("connected host and switch via link %s" % link)

        # start sniffer on docker host
        cmd_string = ""
        if vnf_type.endswith("_accelerated"):
            cmd_string = "python /home/felix/Desktop/toposync/mn/Sniffer.py %s %s %s &" % ((host_name + '-eth0'), 100, vnf_type)
            print("starting sniffer: %s: %s" % (host_name, cmd_string))
        else:
            cmd_string = "python /home/felix/Desktop/toposync/mn/Sniffer.py %s %s %s &" % ((host_name + '-eth0'), 400, vnf_type)
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

