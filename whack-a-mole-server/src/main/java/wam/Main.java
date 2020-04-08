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

    public static void main(String[] args) throws IOException {
        gen = new MoleGenerator();
        enc = new MolesXMLEncoder();
        sender = new Sender();

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            try {
                generateAndSend();
            } catch (ParserConfigurationException | TransformerException | IOException e) {
                e.printStackTrace();
            }
        }, 0, 2, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow));
    }

    private static void generateAndSend() throws TransformerException, ParserConfigurationException, IOException {
        GridPosition[] moles = gen.generateRandomMoles();
        String molesXML = enc.toXML(moles);
        sender.send(molesXML);
    }

}
