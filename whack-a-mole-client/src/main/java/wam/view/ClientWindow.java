package wam.view;

import wam.Responder;

import javax.swing.*;
import java.util.Collection;

public class ClientWindow extends JFrame {
    private static final int ROWS = 4;
    private static final int COLS = 4;
    private static final int CELL_SIZE = 100;
    private GridPanel gridPane;

    public ClientWindow(String ip, Responder responder) {
        super(String.format("Whack-A-Mole Client(%s)", ip));
        SwingUtilities.invokeLater(() -> createAndShowGui(responder));
    }

    public void showMoles(long round, Collection<GridPosition> moles) {
        if (gridPane != null) {
            gridPane.setRound(round);
            gridPane.wipeMoles();
            gridPane.showMoles(moles);
        }
    }

    private void createAndShowGui(Responder responder) {
        gridPane = new GridPanel(ROWS, COLS, CELL_SIZE, responder);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().add(gridPane);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
