package wam.view;

public class GridPosition {
    public int row;
    public int col;

    public GridPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public String toString() {
        return "{row=" + row + ", col=" + col + "}";
    }
}
