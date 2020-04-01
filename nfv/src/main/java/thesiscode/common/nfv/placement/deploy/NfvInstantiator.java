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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Collectors;

public class NfvInstantiator {
    private static final String URL_FORMAT = "http://localhost:9000/%s/%s";
    private final Logger log = LoggerFactory.getLogger(getClass());

    public void remove(NprNfvTypes.Type vnfType, Device switchToAttach, boolean hwAccelerated) throws InstantiationException {
        int respCode = 0;
        try {
            URL url = requestUrl(vnfType, switchToAttach, hwAccelerated);
            log.info("sending request to {}", url);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("DELETE");
            respCode = con.getResponseCode();
            log.info("response code: {}", respCode);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (respCode == HttpURLConnection.HTTP_OK) {
            log.info("VNF removed");
        } else {
            throw new InstantiationException("Could not remove VNF");
        }
    }

    private URL requestUrl(NprNfvTypes.Type vnfType, Device switchToAttach, boolean hwAccelerated) throws MalformedURLException {
        final String dpid = switchToAttach.id().uri().getSchemeSpecificPart();
        String urlString = String.format(URL_FORMAT, dpid, vnfType.toString());
        if (hwAccelerated) {
            urlString += "_accelerated";
        }
        return new URL(urlString);
    }


    public ConnectPoint instantiate(NprNfvTypes.Type vnfType, Device switchToAttach, boolean hwAccelerated) throws InstantiationException {
        String instantiatedAtPort = null;
        int respCode = 0;
        try {
            URL url = requestUrl(vnfType, switchToAttach, hwAccelerated);
            log.info("sending request to {}", url);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("PUT");
            instantiatedAtPort = new BufferedReader(new InputStreamReader(con.getInputStream())).lines()
                                                                                                .collect(Collectors.joining());
            respCode = con.getResponseCode();
            log.info("response code: {}", respCode);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (respCode == HttpURLConnection.HTTP_OK) {
            log.info("instated at port: {}", instantiatedAtPort);
            ConnectPoint cp = new ConnectPoint(switchToAttach.id(),
                    PortNumber.portNumber(Integer.parseInt(instantiatedAtPort)));
            log.info("VNF {} instantiated at cp: {}", vnfType, cp);
            return cp;
        } else {
            throw new InstantiationException("Could not instantiate VNF.");
        }
    }

}
