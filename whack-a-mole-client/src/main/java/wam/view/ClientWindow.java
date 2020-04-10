package wam.view;

import javax.swing.*;
import java.util.Collection;

public class ClientWindow extends JFrame {
    private static final int ROWS = 4;
    private static final int COLS = 4;
    private static final int CELL_SIZE = 100;
    private GridPanel gridPane;

    public ClientWindow(String ip) {
        super(String.format("Whack-A-Mole Client(%s)", ip));
        SwingUtilities.invokeLater(this::createAndShowGui);
    }

    public void showMoles(Collection<GridPosition> moles) {
        gridPane.wipeMoles();
        gridPane.showMoles(moles);
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
