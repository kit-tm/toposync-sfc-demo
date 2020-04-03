package main;

public interface ProgressMonitor {
    void init(boolean oldSolutionExists, String ilpType);

    void solutionCalculated();

    void oldSolutionUninstalled();

    void vnfPlaced();

    void flowsInstalled();
}
