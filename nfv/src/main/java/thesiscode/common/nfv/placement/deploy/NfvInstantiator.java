package thesiscode.common.nfv.placement.deploy;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.traffic.NprNfvTypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NfvInstantiator {
    private Logger log = LoggerFactory.getLogger(getClass());


    public ConnectPoint instantiate(NprNfvTypes.Type vnfType, Device switchToAttach, boolean hwAccelerated) {
        URL url = null;
        HttpURLConnection con = null;
        String instantiatedAtPort = null;
        try {
            String urlString = "http://localhost:9000/" + switchToAttach.id().uri().getSchemeSpecificPart() + "/" +
                    vnfType.toString();

            if (hwAccelerated) {
                urlString += "_accelerated";
            }
            url = new URL(urlString);
            log.info("sending request to {}", url);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("PUT");
            instantiatedAtPort = new BufferedReader(new InputStreamReader(con.getInputStream())).lines()
                                                                                                .collect(Collectors.joining());
            log.info("response code: {}", con.getResponseCode());
            log.info("instated at port: {}", instantiatedAtPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
                /*
                stub
                 */
        ConnectPoint cp = new ConnectPoint(switchToAttach.id(), PortNumber.portNumber(Integer.parseInt(instantiatedAtPort)));
        log.info("VNF {} instantiated at cp: {}", vnfType, cp);
        return cp;
    }

    public Map<NprNfvTypes.Type, Set<ConnectPoint>> instantiate(Map<NprNfvTypes.Type, Set<Device>> toInstantiate) {
        Map<NprNfvTypes.Type, Set<ConnectPoint>> cps = new HashMap<>();

        for (NprNfvTypes.Type type : toInstantiate.keySet()) {
            log.info("instantiating VNF {} at {}", type.toString(), toInstantiate.get(type));

            Set<ConnectPoint> cpSet = cps.computeIfAbsent(type, k -> new HashSet<>());

            for (Device dev : toInstantiate.get(type)) {

                URL url = null;
                HttpURLConnection con = null;
                String instantiatedAtPort = null;
                try {
                    url = new URL(
                            "http://localhost:9000/" + dev.id().uri().getSchemeSpecificPart() + "/" + type.toString());
                    log.info("sending request to {}", url);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("PUT");
                    instantiatedAtPort = new BufferedReader(new InputStreamReader(con.getInputStream())).lines()
                                                                                                        .collect(Collectors
                                                                                                                         .joining());
                    log.info("response code: {}", con.getResponseCode());
                    log.info("instated at port: {}", instantiatedAtPort);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /*
                stub
                 */
                ConnectPoint cp = new ConnectPoint(dev.id(), PortNumber.portNumber(Integer.parseInt(instantiatedAtPort)));
                log.info("VNF {} instantiated at cp: {}", type, cp);
                cpSet.add(cp);
            }

        }

        log.info("cps: {}", cps);
        return cps;
    }

}
