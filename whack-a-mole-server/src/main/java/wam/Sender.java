package wam;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Sender {
    private static final String GROUP_ADDR_STR = "224.2.3.4";
    private static final int PORT = 9090;
    private DatagramSocket sock;
    private final InetAddress GROUP_ADDR;

    public Sender() throws UnknownHostException, SocketException {
        this.sock = new DatagramSocket(9090, InetAddress.getByName("10.0.0.1"));
        GROUP_ADDR = InetAddress.getByName(GROUP_ADDR_STR);
    }

    public void send(String xml) throws IOException {
        byte[] buf = xml.getBytes(StandardCharsets.UTF_8);
        DatagramPacket pkt = new DatagramPacket(buf, 0, buf.length, GROUP_ADDR, PORT);
        sock.send(pkt);
    }

    public void close() {
        sock.close();
    }

}
