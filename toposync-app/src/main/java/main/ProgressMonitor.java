package main;

public interface ProgressMonitor {
    void init(boolean oldSolutionExists, String ilpType);

    void solutionCalculated(long durationMs);

    void oldSolutionUninstalled(long durationMs);

    void vnfPlaced(long durationMs);

    void flowsInstalled(long durationMs);
}
