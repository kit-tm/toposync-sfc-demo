package wam;

import wam.view.ClientWindow;
import wam.view.GridPosition;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class Communication {
    private static final int PORT = 9090;
    private DatagramSocket sock;

    public Communication(InetAddress bindAddr) throws SocketException {
        this.sock = new DatagramSocket(PORT, bindAddr);
    }

    public void startReceiveLoop(ClientWindow window) {
        Thread t = new Thread(() -> receiveLoop(window));
        t.setUncaughtExceptionHandler((e, th) -> th.printStackTrace());
        t.start();
    }

    private void receiveLoop(ClientWindow window) {
        while (true) {
            byte[] buf = new byte[1000];
            DatagramPacket pkt = new DatagramPacket(buf, 1000);
            try {
                sock.receive(pkt);
            } catch (IOException e) {
                throw new IllegalStateException(e.getCause());
            }

            String str = new String(pkt.getData(), StandardCharsets.UTF_8);

            Set<GridPosition> moles = new HashSet<>();

            long round = 0;
            String[] lines = str.trim().split("\n");


            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (i == 0) {
                    round = Long.parseLong(line);
                } else {
                    String[] rowCol = line.trim().split(",");
                    int row = Integer.parseInt(rowCol[0]);
                    int col = Integer.parseInt(rowCol[1]);
                    moles.add(new GridPosition(row, col));
                }
            }

            window.showMoles(round, moles);
        }
    }

    public void close() {
        sock.close();
    }
}
