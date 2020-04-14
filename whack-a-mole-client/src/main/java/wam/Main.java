package wam;

import wam.view.ClientWindow;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws IOException {
        checkArgs(args);
        InetAddress ip = ip(args[0]);
        Receiver receiver = new Receiver(ip);
        Runtime.getRuntime().addShutdownHook(new Thread(receiver::close));
        ClientWindow cw = new ClientWindow(ip.toString());
        receiver.startReceiveLoop(cw);
    }

    private static void checkArgs(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected exactly one argument, the bind IP.");
        }
    }

    private static InetAddress ip(String arg) {
        try {
            return InetAddress.getByName(arg);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Not a valid IP.");
        }
    }
}
