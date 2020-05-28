package main.view;

import main.ProgressMonitor;

import javax.swing.*;

public class DeleteProgressWindow extends ProgressWindow {

    @Override
    public void init(boolean oldSolutionExists, String ilpType) {
        contPane.removeAll();

        uninstall = new JLabel("Uninstalling tree.", JLabel.CENTER);
        setFontSize(uninstall);
        add(uninstall);

        setLocationRelativeTo(null);
        setLoading(uninstall);
        pack();
        setVisible(true);
    }

    @Override
    public void solutionCalculated(long durationMs) {
        // intentionally blank
    }

    @Override
    public void oldSolutionUninstalled(long durationMs) {
        done(uninstall, durationMs);
    }

    @Override
    public void vnfPlaced(long durationMs) {
        // intentionally blank
    }

    @Override
    public void flowsInstalled(long durationMs) {
        // intentionally blank
    }


    public static void main(String[] args) throws InterruptedException {
        ProgressMonitor pm = new DeleteProgressWindow();
        pm.init(true, "");
        Thread.sleep(2000);
        pm.oldSolutionUninstalled(700);
    }
}
