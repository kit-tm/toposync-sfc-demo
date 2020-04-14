package wam.view;

import javax.swing.*;
import java.util.Collection;

public class ClientWindow extends JFrame {
    private static final int ROWS = 4;
    private static final int COLS = 4;
    private static final int CELL_SIZE = 100;
    private GridPanel gridPane;
    private long round = 0;

    public ClientWindow(String ip) {
        super(String.format("Whack-A-Mole Client(%s)", ip));
        SwingUtilities.invokeLater(this::createAndShowGui);
    }

    public void showMoles(long round, Collection<GridPosition> moles) {
        this.round = round;
        gridPane.wipeMoles();
        gridPane.showMoles(round, moles);
    }

    private void createAndShowGui() {
        gridPane = new GridPanel(ROWS, COLS, CELL_SIZE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().add(gridPane);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
