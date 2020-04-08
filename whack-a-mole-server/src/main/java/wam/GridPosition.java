package wam;

public class GridPosition {
    int row;
    int col;

    GridPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public String toString() {
        return "{row=" + row + ", col=" + col + "}";
    }
}
