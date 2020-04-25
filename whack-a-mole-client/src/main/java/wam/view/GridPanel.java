package wam.view;

import wam.Responder;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.IOException;
import java.util.Collection;

public class GridPanel extends JPanel {
    private static final Color INACTIVE = Color.LIGHT_GRAY;
    private static final Color ACTIVE = Color.RED;
    private static final Color CLICKED_CORRECT = Color.GREEN;

    private Cell[][] cells;
    private long round;
    private Responder responder;

    private Collection<GridPosition> currentMoles;

    public GridPanel(int rows, int cols, int cellSize, Responder responder) {
        this.responder = responder;
        cells = new Cell[rows][cols];

        CellMouseListener mouseListener = new CellMouseListener(this);

        Dimension prefSize = new Dimension(cellSize, cellSize);
        setLayout(new GridLayout(rows, cols));
        for (int row = 0; row < cells.length; row++) {
            for (int col = 0; col < cells[row].length; col++) {
                Cell cell = new Cell(new GridPosition(row, col));
                cell.setOpaque(true);
                cell.setBackground(INACTIVE);
                cell.setBorder(new LineBorder(Color.BLACK));
                cell.setPreferredSize(prefSize);
                cell.addMouseListener(mouseListener);
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
            SwingUtilities.invokeLater(() -> cells[mole.row][mole.col].setBackground(INACTIVE));
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
            SwingUtilities.invokeLater(() -> cells[mole.row][mole.col].setBackground(ACTIVE));
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    public void cellClicked(Cell cell) {
        if (cell.getBackground() == ACTIVE) {
            SwingUtilities.invokeLater(() -> cell.setBackground(CLICKED_CORRECT));
        }

        try {
            responder.respond(round, cell.pos);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
