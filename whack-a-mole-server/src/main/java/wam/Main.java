package wam;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    private static MoleGenerator gen;
    private static MolesXMLEncoder enc;
    private static ServerCommunication communication;
    private static ScheduledExecutorService executor;
    private static long round = 0;

    public static void main(String[] args) throws IOException, ParserConfigurationException {
        long period = parseArgs(args);

        gen = new MoleGenerator();
        enc = new MolesXMLEncoder();
        communication = new ServerCommunication();

        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            try {
                generateAndSend();
            } catch (ParserConfigurationException | TransformerException | IOException e) {
                e.printStackTrace();
            }
        }, 0, period, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(Main::cleanup));

        communication.startReceiveLoop();
    }

    private static long parseArgs(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected exactly one argument, the sending period (in ms)");
        }

        return Long.parseLong(args[0]);
    }

    private static void generateAndSend() throws TransformerException, ParserConfigurationException, IOException {
        GridPosition[] moles = gen.generateRandomMoles();
        String molesXML = enc.toXML(round++, moles);
        communication.send(molesXML);
    }

    private static void cleanup() {
        executor.shutdownNow();
        communication.close();
        System.out.println("Cleaned up!");
    }

}
