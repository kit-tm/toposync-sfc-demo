package org.onosproject.statistics;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component(immediate = true)
public class LinkStatistics implements LinkListener {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService pktService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int FLOW_TABLE_FOR_STATISTIC = 1;


    protected static final Byte[] IP_PROTOCOLS = {IPv4.PROTOCOL_IGMP, IPv4.PROTOCOL_UDP, IPv4.PROTOCOL_TCP,
            IPv4.PROTOCOL_ICMP};
    protected static final Short[] ETHER_TYPES = {Ethernet.TYPE_ARP, Ethernet.TYPE_LLDP};

    protected static Set<Link> links = new HashSet<>();
    private Set<Byte> ipProtos = new HashSet<>();
    private Set<Short> etherTypes = new HashSet<>();
    protected static Map<Link, Map<Byte, Long>> ipStats = new HashMap<>();
    protected static Map<Link, Map<Short, Long>> etherStats = new HashMap<>();

    private InternalPacketProcessor packetProcessor;
    private ApplicationId appId;
    private InternalDeviceListener deviceListener;

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent deviceEvent) {
            if (deviceEvent.type() == DeviceEvent.Type.DEVICE_ADDED) {
                installDefaultFlows(appId, deviceEvent.subject().id());
            }
        }
    }

    private void installDefaultFlows(ApplicationId appId, DeviceId deviceId) {
        for (Byte ipProto : IP_PROTOCOLS) {
            flowRuleService.applyFlowRules(DefaultFlowRule
                                                   .builder()
                                                   .forDevice(deviceId)
                                                   .forTable(FLOW_TABLE_FOR_STATISTIC)
                                                   .makePermanent()
                                                   .withSelector(DefaultTrafficSelector
                                                                         .builder()
                                                                         .matchEthType(Ethernet.TYPE_IPV4)
                                                                         .matchIPProtocol(ipProto)
                                                                         .build())
                                                   .withTreatment(DefaultTrafficTreatment
                                                                          .builder()
                                                                          .setOutput(PortNumber.CONTROLLER)
                                                                          .build())
                                                   .fromApp(appId)
                                                   .withPriority(FlowRule.MAX_PRIORITY)
                                                   .build());
        }

        for (Short etherType : ETHER_TYPES) {
            flowRuleService.applyFlowRules(DefaultFlowRule
                                                   .builder()
                                                   .forDevice(deviceId)
                                                   .forTable(FLOW_TABLE_FOR_STATISTIC)
                                                   .makePermanent()
                                                   .withSelector(DefaultTrafficSelector
                                                                         .builder()
                                                                         .matchEthType(etherType)
                                                                         .build())
                                                   .withTreatment(DefaultTrafficTreatment
                                                                          .builder()
                                                                          .setOutput(PortNumber.CONTROLLER)
                                                                          .build())
                                                   .fromApp(appId)
                                                   .withPriority(FlowRule.MAX_PRIORITY)
                                                   .build());
        }

    }

    @Deactivate
    protected void activate() {
        Collections.addAll(ipProtos, IP_PROTOCOLS);
        Collections.addAll(etherTypes, ETHER_TYPES);

        log.info("links of linkservice: {}", linkService.getLinks());
        linkService.getLinks().forEach(l -> links.add(l));
        for (Link l : links) {
            initMapsForLink(l);
        }
        linkService.addListener(this);

        appId = coreService.registerApplication("org.onosproject.statistics");

        packetProcessor = new InternalPacketProcessor(linkService);
        pktService.addProcessor(packetProcessor, PacketProcessor.director(0));

        for (Device device : deviceService.getDevices()) {
            installDefaultFlows(appId, device.id());
        }

        deviceListener = new InternalDeviceListener();
        deviceService.addListener(deviceListener);

        log.info("started");
    }


    @Deactivate
    protected void deactivate() {
        pktService.removeProcessor(packetProcessor);
        deviceService.removeListener(deviceListener);
        log.info("bye!");
    }


    @Override
    public void event(LinkEvent linkEvent) {
        log.debug("linkevent!");
        if (linkEvent.type() == LinkEvent.Type.LINK_ADDED || linkEvent.type() == LinkEvent.Type.LINK_UPDATED) {
            Link changedLink = linkEvent.subject();
            links.add(changedLink);
            initMapsForLink(changedLink);
            log.debug("new ether stats: {}", etherStatsToString(etherStats));
            log.debug("new ip stats: {}", ipStatsToString(ipStats));
        } else if (linkEvent.type() == LinkEvent.Type.LINK_REMOVED) {
            // TODO
        }
    }

    private void initMapsForLink(Link l) {
        initIpMapForLink(l);
        initEtherMapForLink(l);
    }

    private void initIpMapForLink(Link l) {
        Map<Byte, Long> ipMap = new HashMap<>();
        for (Byte ipProt : ipProtos) {
            ipMap.put(ipProt, 0l);
        }
        ipStats.put(l, ipMap);
    }

    private void initEtherMapForLink(Link l) {
        Map<Short, Long> etherMap = new HashMap<>();
        for (Short etherType : etherTypes) {
            etherMap.put(etherType, 0l);
        }
        etherStats.put(l, etherMap);
    }

    private class InternalPacketProcessor implements PacketProcessor {
        private LinkService ls;
        private int count = 0;


        public InternalPacketProcessor(LinkService ls) {
            this.ls = ls;
        }

        @Override
        public void process(PacketContext packetContext) {

            log.debug("packet number {}", count++);

            InboundPacket in = packetContext.inPacket();
            Ethernet eth = in.parsed();

            ConnectPoint source = in.receivedFrom();
            log.debug("linkService: {}", linkService);
            log.debug("connectpointsource: {}", source);
            Set<Link> ingressLinks = linkService.getIngressLinks(source);
            log.debug("ingressLinks: {}", ingressLinks);
            // TODO if ingressLinks is empty the packet was on edge link, handle this too??

            short packetEtherType = eth.getEtherType();
            log.debug("packetethertype: {}", "0x" + Integer.toHexString(packetEtherType & 0xffff));
            if (etherTypes.contains(packetEtherType)) {
                for (Link link : ingressLinks) {
                    Map<Short, Long> etherMapForLink = etherStats.get(link);
                    //log.info("etherstats: {}", etherStats);
                    log.debug("ethermapforlink: {}", etherMapForLink);
                    long newValue = etherMapForLink.get(packetEtherType) + 1;
                    etherMapForLink.put(packetEtherType, newValue);
                }
                log.debug("new ether stats: {}", etherStatsToString(etherStats));
                log.debug("new ether stats: {}", etherStats);
            }

            if (packetEtherType == Ethernet.TYPE_IPV4) {
                IPv4 ip = (IPv4) eth.getPayload();
                byte packetIpProtocol = ip.getProtocol();
                log.debug("packetipprotocol: {}", "0x" + Integer.toHexString(packetIpProtocol & 0xffff));
                if (ipProtos.contains(packetIpProtocol)) {
                    for (Link link : ingressLinks) {
                        Map<Byte, Long> ipMapForLink = ipStats.get(link);
                        long newValue = ipMapForLink.get(packetIpProtocol) + 1;
                        ipMapForLink.put(packetIpProtocol, newValue);
                    }
                }
                log.debug("new ip stats: {}", ipStatsToString(ipStats));
            }
        }

    }

    private String etherStatsToString(Map<Link, Map<Short, Long>> stats) {
        StringBuilder builder = new StringBuilder();

        for (Link l : stats.keySet()) {
            builder
                    .append("\n")
                    .append(connectPointToString(l.src()))
                    .append(" -> ")
                    .append(connectPointToString(l.dst()))
                    .append(":[");
            Map<Short, Long> map = stats.get(l);
            for (short etherType : map.keySet()) {
                builder
                        .append("0x")
                        .append(Integer.toHexString(etherType & 0xffff))
                        .append("=")
                        .append(map.get(etherType))
                        .append(",");
            }
            builder.append("]");
        }

        return builder.toString();
    }

    private String ipStatsToString(Map<Link, Map<Byte, Long>> stats) {
        StringBuilder builder = new StringBuilder();

        for (Link l : stats.keySet()) {
            builder
                    .append("\n")
                    .append(connectPointToString(l.src()))
                    .append(" -> ")
                    .append(connectPointToString(l.dst()))
                    .append(":[");
            Map<Byte, Long> map = stats.get(l);
            for (byte ipProto : map.keySet()) {
                builder
                        .append("0x")
                        .append(Integer.toHexString(ipProto & 0xffff))
                        .append("=")
                        .append(map.get(ipProto))
                        .append(",");
            }
            builder.append("]");
        }

        return builder.toString();
    }

    private String connectPointToString(ConnectPoint cp) {
        StringBuilder builder = new StringBuilder();
        String cpString = cp.toString();
        builder.append("s");
        for (char c : cpString.toCharArray()) {
            if (c != 'o' && c != 'f' && c != '0' && c != ':') {
                builder.append(c);
            }
        }
        return builder.toString();
    }

}
