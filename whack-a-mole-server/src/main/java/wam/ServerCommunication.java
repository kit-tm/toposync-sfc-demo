package wam;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ServerCommunication {
    private static final String SERVER_ADDR_STR = "10.0.0.1";
    private static final String GROUP_ADDR_STR = "224.2.3.4";
    private static final int PORT = 9090;
    private DatagramSocket sock;
    private final InetAddress GROUP_ADDR;

    private DocumentBuilder builder;

    public ServerCommunication() throws UnknownHostException, SocketException, ParserConfigurationException {
        this.sock = new DatagramSocket(PORT, InetAddress.getByName(SERVER_ADDR_STR));
        GROUP_ADDR = InetAddress.getByName(GROUP_ADDR_STR);

        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    public void send(String xml) throws IOException {
        byte[] buf = xml.getBytes(StandardCharsets.UTF_8);
        DatagramPacket pkt = new DatagramPacket(buf, 0, buf.length, GROUP_ADDR, PORT);
        sock.send(pkt);
    }

    public void startReceiveLoop() {
        /*Thread th = new Thread(this::receiveLoop);
        th.setUncaughtExceptionHandler((e, t) -> {
            System.err.println("Exception in receive loop thread");
            System.err.println(Arrays.toString(t.getStackTrace()));
        });
        th.start();*/

        receiveLoop();
    }

    private void receiveLoop() {
        while (true) {
            System.out.println(String.format("Starting to listen on %s!", sock.getLocalSocketAddress()));
            byte[] buf = new byte[1000];
            DatagramPacket pkt = new DatagramPacket(buf, buf.length);
            try {
                System.out.println("Starting to receive!");
                sock.receive(pkt);
                System.out.println("Received a packet! " + pkt);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String str = new String(pkt.getData(), StandardCharsets.UTF_8);
            System.out.println("packet content: " + str);

            Document doc = null;

            try {
                doc = builder.parse(new InputSource(new StringReader(str)));
            } catch (SAXException | IOException e) {
                e.printStackTrace();
                continue;
            }

            NodeList nodes = doc.getChildNodes();

            long round = -1;
            GridPosition clickedCell = null;

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equals("response")) {
                    NodeList responseChilds = node.getChildNodes();
                    for (int j = 0; i < responseChilds.getLength(); j++) {
                        node = responseChilds.item(j);
                        final String nodeName = node.getNodeName();
                        if (nodeName.equals("round")) {
                            String roundStr = node.getTextContent();
                            round = Long.parseLong(roundStr);
                            System.out.println("Round: " + roundStr);
                        } else if (nodeName.equals("clickedCell")) {
                            int row = -1;
                            int col = -1;
                            NodeList clickedCellChilds = node.getChildNodes();
                            for (int k = 0; k < clickedCellChilds.getLength(); k++) {
                                node = clickedCellChilds.item(k);
                                if (node.getNodeName().equals("row")) {
                                    row = Integer.parseInt(node.getTextContent());
                                } else if (node.getNodeName().equals("col")) {
                                    col = Integer.parseInt(node.getTextContent());
                                } else {
                                    throw new IllegalStateException("Unexpected node name under clickedCell: " + node.getNodeName());
                                }
                                clickedCell = new GridPosition(row, col);

                                System.out.println(String.format("%s clicked cell (row=%s,col=%s) in round %s.",
                                        pkt.getAddress(), clickedCell.row, clickedCell.col, round));
                            }
                        }
                    }
                } else {
                    throw new IllegalStateException("Unexpected node name under response: " + node.getNodeName());
                }
            }

            System.out.println("finished parsing!");
        }
    }

    public void close() {
        sock.close();
    }

}
