package wam.view;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Collection;

public class GridPanel extends JPanel {
    private static final Color INACTIVE = Color.LIGHT_GRAY;
    private static final Color ACTIVE = Color.RED;
    private static final float LARGE_FONT_SIZE = 30.0f; // used in HW setup (fullscreen whack-a-mole)

    private Cell[][] cells;
    private long round;

    private Collection<GridPosition> currentMoles;

    private String clientName;

    public GridPanel(int rows, int cols, int cellSize, String clientName, boolean useLargeFont) {
        this.clientName = clientName;

        cells = new Cell[rows][cols];

        Dimension prefSize = new Dimension(cellSize, cellSize);
        setLayout(new GridLayout(rows, cols));
        for (int row = 0; row < cells.length; row++) {
            for (int col = 0; col < cells[row].length; col++) {
                Cell cell = new Cell(new GridPosition(row, col));
                cell.setOpaque(true);
                cell.setBackground(INACTIVE);
                cell.setForeground(Color.BLACK);
                cell.setBorder(new LineBorder(Color.BLACK));
                cell.setPreferredSize(prefSize);

                if (useLargeFont) {
                    cell.setFont(cell.getFont().deriveFont(LARGE_FONT_SIZE));
                }

                add(cell);
                cells[row][col] = cell;
            }
        }
    }

    public void wipeMoles() {
        if (currentMoles == null) {
            return;
        }

        for (GridPosition mole : currentMoles) {
            Cell cell = cells[mole.row][mole.col];
            cell.setBackground(INACTIVE);
            cell.setText(null);
        }

        currentMoles = null;
    }

    public void setRound(long round) {
        this.round = round;
    }

    public void showMoles(Collection<GridPosition> moles) {
        currentMoles = moles;
        for (GridPosition mole : moles) {
            SwingUtilities.invokeLater(() -> {
                Cell moleCell = cells[mole.row][mole.col];
                moleCell.setBackground(ACTIVE);
                moleCell.setText(round + " (" + clientName + ")");
            });
        }
        SwingUtilities.invokeLater(this::repaint);
    }
}
