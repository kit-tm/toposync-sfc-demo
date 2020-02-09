package util;

import java.io.File;
import java.io.IOException;

public class PlotStarter {
    public static void startPythonPlot(String path) throws IOException {
        String[] command = {"/usr/bin/python2.7", "plot_solution_edges.py", path};
        ProcessBuilder builder = new ProcessBuilder(command);
        builder = builder.directory(new File("/home/felix/Desktop/ba/code/eval"));
        builder.start();
    }
}

