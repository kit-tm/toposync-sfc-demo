package wam;

import java.util.concurrent.ThreadLocalRandom;

public class MoleGenerator {
    private static final int AMOUNT_OF_MOLES = 2;
    private static final int ROWS = 3;
    private static final int COLS = 3;
    private final ThreadLocalRandom rand;

    public MoleGenerator() {
        this.rand = ThreadLocalRandom.current();
    }

    public GridPosition[] generateRandomMoles() {
        GridPosition[] moles = new GridPosition[AMOUNT_OF_MOLES];
        for (int i = 0; i < AMOUNT_OF_MOLES; i++) {
            moles[i] = generateRandomMole();
        }
        return moles;
    }

    private GridPosition generateRandomMole() {
        int row = rand.nextInt(ROWS + 1);
        int col = rand.nextInt(COLS + 1);
        return new GridPosition(row, col);
    }
}
