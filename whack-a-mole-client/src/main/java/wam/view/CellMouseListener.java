package wam.view;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CellMouseListener extends MouseAdapter {
    private GridPanel gridPane;

    public CellMouseListener(GridPanel gridPane) {
        this.gridPane = gridPane;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            gridPane.cellClicked((JLabel) e.getSource());
        }
    }
}
