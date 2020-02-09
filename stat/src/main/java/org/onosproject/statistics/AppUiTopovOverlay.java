/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.statistics;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.PropertyPanel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Our topology overlay.
 */
public class AppUiTopovOverlay extends UiTopoOverlay {

    // NOTE: this must match the ID defined in sampleTopov.js
    private static final String OVERLAY_ID = "meowster-overlay";


    public AppUiTopovOverlay() {
        super(OVERLAY_ID);
    }


    @Override
    public void modifyInfraLinkDetails(PropertyPanel pp, ConnectPoint cpA, ConnectPoint cpB) {
        pp.addSeparator();

        Map<Link, Map<Byte, Long>> ipStats = LinkStatistics.ipStats;
        Map<Link, Map<Short, Long>> etherStats = LinkStatistics.etherStats;

        Set<Link> correspondingLinks = findCorrespondingLinks(cpA, cpB);
        log.debug("corresponding links: {}", correspondingLinks);

        for (Byte ipProto : LinkStatistics.IP_PROTOCOLS) {
            int protoCount = 0;
            for (Link l : correspondingLinks) {
                Map<Byte, Long> ipForLink = ipStats.get(l);
                log.debug("ipForLink {}", ipForLink);
                log.debug("ipForLink.get(l): {}", ipForLink.get(ipProto));
                protoCount += ipForLink.get(ipProto);
            }
            String label;
            if (ipProto == IPv4.PROTOCOL_ICMP) {
                label = "ICMP";
            } else if (ipProto == IPv4.PROTOCOL_IGMP) {
                label = "IGMP";
            } else if (ipProto == IPv4.PROTOCOL_TCP) {
                label = "TCP";
            } else if (ipProto == IPv4.PROTOCOL_UDP) {
                label = "UDP";
            } else {
                label = ipProto.toString();
            }

            pp.addProp(ipProto.toString(), label, protoCount);
        }


        for (Short etherType : LinkStatistics.ETHER_TYPES) {
            int etherTypeCount = 0;
            for (Link l : correspondingLinks) {
                Map<Short, Long> etherTypeForLink = etherStats.get(l);
                log.debug("etherTypeForLink {}", etherTypeForLink);
                log.debug("etherForLink.get(l): {}", etherTypeForLink.get(etherType));
                etherTypeCount += etherTypeForLink.get(etherType);
            }
            String label;
            if (etherType == Ethernet.TYPE_ARP) {
                label = "ARP";
            } else if (etherType == Ethernet.TYPE_LLDP) {
                label = "LLDP";
            } else {
                label = etherType.toString();
            }
            pp.addProp(etherType.toString(), label, etherTypeCount);
        }
    }

    private Set<Link> findCorrespondingLinks(ConnectPoint cpA, ConnectPoint cpB) {
        Set<Link> correspondingLinks = new HashSet<>();
        Set<Link> allLinks = LinkStatistics.links;
        for (Link link : allLinks) {
            log.debug("looking at {}", link);
            ConnectPoint src = link.src();
            ConnectPoint dst = link.dst();

            if ((cpA.equals(src) && cpB.equals(dst)) || cpA.equals(dst) && cpB.equals(src)) {
                correspondingLinks.add(link);
            }
        }
        return correspondingLinks;
    }


}
