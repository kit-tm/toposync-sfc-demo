package wam.view;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Collection;

public class GridPanel extends JPanel {
    private static final Color INACTIVE = Color.LIGHT_GRAY;
    private static final Color ACTIVE = Color.RED;

    private Cell[][] cells;
    private long round;

    private Collection<GridPosition> currentMoles;

    private String clientName;

    public GridPanel(int rows, int cols, int cellSize, String clientName) {
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
            SwingUtilities.invokeLater(() -> {
                cell.setBackground(INACTIVE);
                cell.setText(null);
            });
        }
        SwingUtilities.invokeLater(this::repaint);

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
