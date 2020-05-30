package wam.view;

import javax.swing.*;

public class Cell extends JLabel {
    public GridPosition pos;

    public Cell(GridPosition pos) {
        super();
        this.pos = pos;
        this.setVerticalAlignment(SwingConstants.CENTER);
        this.setHorizontalAlignment(SwingConstants.CENTER);
    }

}
