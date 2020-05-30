package wam.view;

import javax.swing.*;
import java.util.Collection;

public class ClientWindow extends JFrame {
    private static final int ROWS = 3;
    private static final int COLS = 3;
    private static final int CELL_SIZE = 100;
    private GridPanel gridPane;
    private String ip;
    private final boolean useLargeFont;

    public ClientWindow(String ip, boolean useLargeFont) {
        super(String.format("Client%s (%s)", ip.equals("/10.0.0.10") ? 1 : 2, ip.substring(1)));
        this.ip = ip;
        this.useLargeFont = useLargeFont;
        SwingUtilities.invokeLater(this::createAndShowGui);
    }

    public void showMoles(long round, Collection<GridPosition> moles) {
        if (gridPane != null) {
            gridPane.setRound(round);
            gridPane.wipeMoles();
            gridPane.showMoles(moles);
        }
    }

    private void createAndShowGui() {
        String clientName = (ip.equals("/10.0.0.10")) ? "Client1" : "Client2";
        gridPane = new GridPanel(ROWS, COLS, CELL_SIZE, clientName, useLargeFont);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().add(gridPane);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
