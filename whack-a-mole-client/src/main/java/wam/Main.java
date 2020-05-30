package wam;

import wam.view.ClientWindow;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws IOException {
        checkArgs(args);
        InetAddress ip = ip(args[0]);
        boolean isHardwareClient = false;
        if (args.length == 2) {
            isHardwareClient = hardwareClient(args[1]);
        }


        Communication communication = new Communication(ip);
        Runtime.getRuntime().addShutdownHook(new Thread(communication::close));

        ClientWindow cw = new ClientWindow(ip.toString(), isHardwareClient);
        communication.startReceiveLoop(cw);
    }

    private static void checkArgs(String[] args) {
        if (args.length == 1) {
            return;
        } else if (args.length == 2 && args[1].equals("--hw")) {
            return;
        } else {
            throw new IllegalArgumentException("Expected exactly one argument, the bind IP. A second argument --hw " + "may be specified.");
        }
    }

    private static InetAddress ip(String arg) {
        try {
            return InetAddress.getByName(arg);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Not a valid IP.");
        }
    }

    private static boolean hardwareClient(String arg) {
        return arg.equals("--hw");
    }
}
