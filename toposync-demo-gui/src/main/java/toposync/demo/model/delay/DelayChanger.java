package toposync.demo.model.delay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DelayChanger {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String[] QUERY_LINK_CMD = new String[]{"/bin/sh", "-c", "tc qdisc | grep delay"};
    private static final String CHANGE_DELAY_TEMPLATE = "tc qdisc change dev %s root netem delay %sms";

    public void changeDelay(int newDelayInMs) throws IOException {
        Process pr = Runtime.getRuntime().exec(QUERY_LINK_CMD);
        BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String line = null;
        while ((line = reader.readLine()) != null) {
            String interfaceName = line.split(" ")[4];
            String changeCommand = String.format(CHANGE_DELAY_TEMPLATE, interfaceName, newDelayInMs);
            log.debug(changeCommand);
            Runtime.getRuntime().exec(changeCommand);
        }
    }
}
