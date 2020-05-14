package toposync.demo.model.delay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DelayManager {
    private static final String[] QUERY_LINK_CMD = new String[]{"/bin/sh", "-c", "tc qdisc | grep delay"};
    private static final String CHANGE_DELAY_TEMPLATE = "tc qdisc change dev %s root netem delay %sms";

    public void changeDelay(int newDelayInMs) throws IOException {
        for (String interfaceName : queryInterfaceNames()) {
            String changeCommand = String.format(CHANGE_DELAY_TEMPLATE, interfaceName, newDelayInMs);
            Runtime.getRuntime().exec(changeCommand);
        }
    }

    private Collection<String> queryInterfaceNames() throws IOException {
        List<String> interfaces = new ArrayList<>();

        queryTc(line -> {
            String interfaceName = line.split(" ")[4];
            interfaces.add(interfaceName);
        });

        return interfaces;
    }

    public int queryCurrentDelay() throws IOException {
        AtomicInteger currentDelay = new AtomicInteger(-1);

        queryTc(line -> {
            String delayWithMsSuffix = line.split(" ")[11]; // e.g. 100.00ms
            String delayInMs = delayWithMsSuffix.substring(0, delayWithMsSuffix.length() - 4); // e.g. 100
            currentDelay.set(Integer.parseInt(delayInMs));
        });

        return currentDelay.get();
    }

    private void queryTc(Consumer<String> lineFunction) throws IOException {
        Process pr = Runtime.getRuntime().exec(QUERY_LINK_CMD);
        BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            lineFunction.accept(line);
        }
    }

}
