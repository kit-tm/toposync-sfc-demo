package wam.view;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Collection;

public class GridPanel extends JPanel {
    private static final Color INACTIVE = Color.LIGHT_GRAY;
    private static final Color ACTIVE = Color.RED;
    private JLabel[][] labels;
    private long round;

    public GridPanel(int rows, int cols, int cellSize) {
        labels = new JLabel[rows][cols];

        CellMouseListener mouseListener = new CellMouseListener(this);

        Dimension prefSize = new Dimension(cellSize, cellSize);
        setLayout(new GridLayout(rows, cols));
        for (int row = 0; row < labels.length; row++) {
            for (int col = 0; col < labels[row].length; col++) {
                JLabel label = new JLabel();
                label.setOpaque(true);
                label.setBackground(INACTIVE);
                label.setBorder(new LineBorder(Color.BLACK));
                label.setPreferredSize(prefSize);
                label.addMouseListener(mouseListener);
                add(label);
                labels[row][col] = label;
            }
        }
    }

    public void wipeMoles() {
        SwingUtilities.invokeLater(() -> {
            for (int row = 0; row < labels.length; row++) {
                for (int col = 0; col < labels[row].length; col++) {
                    labels[row][col].setBackground(INACTIVE);
                }
            }
            repaint();
        });
    }

    public void showMoles(long round, Collection<GridPosition> moles) {
        this.round = round;
        SwingUtilities.invokeLater(() -> {
            for (GridPosition mole : moles) {
                labels[mole.row][mole.col].setBackground(ACTIVE);
                repaint();
            }
            repaint();
        });
    }

    public void cellClicked(JLabel label) {
        if (label.getBackground() == INACTIVE) {
            System.out.println("Clicked inactive cell!"); // TODO
        } else if (label.getBackground() == ACTIVE) {
            System.out.println("Correct!"); // TODO
        }
    }


}
