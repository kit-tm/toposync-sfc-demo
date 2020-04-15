package wam;

import wam.view.GridPosition;

public class ResponseCSVEncoder {

    public String toCSV(long round, GridPosition clickedCell) {
        return round + "\n" + clickedCell.row + "," + clickedCell.col;
    }

}
