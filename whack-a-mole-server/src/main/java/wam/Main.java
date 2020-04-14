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
    private static Sender sender;
    private static ScheduledExecutorService executor;
    private static long round = 0;

    public static void main(String[] args) throws IOException {
        gen = new MoleGenerator();
        enc = new MolesXMLEncoder();
        sender = new Sender();

        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            try {
                generateAndSend();
            } catch (ParserConfigurationException | TransformerException | IOException e) {
                e.printStackTrace();
            }
        }, 0, 2, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(Main::cleanup));

        System.out.println("Started!");
    }

    private static void generateAndSend() throws TransformerException, ParserConfigurationException, IOException {
        GridPosition[] moles = gen.generateRandomMoles();
        String molesXML = enc.toXML(round++, moles);
        sender.send(molesXML);
    }

    private static void cleanup() {
        executor.shutdownNow();
        sender.close();
        System.out.println("Cleaned up!");
    }

}
