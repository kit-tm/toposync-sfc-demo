package main.view;

import javax.swing.*;

public class DeleteProgressWindow extends ProgressWindow {

    @Override
    public void init(boolean oldSolutionExists, String ilpType) {
        contPane.removeAll();

        uninstall = new JLabel("Uninstalling old tree.", JLabel.CENTER);
        setFontSize(uninstall);
        add(uninstall);

        setLocationRelativeTo(null);
        setLoading(uninstall);
        pack();
        setVisible(true);
    }

    @Override
    public void solutionCalculated() {
        //
    }

    @Override
    public void oldSolutionUninstalled() {
        done(uninstall);
        setVisible(false);
    }

    @Override
    public void vnfPlaced() {
        //
    }

    @Override
    public void flowsInstalled() {
        //
    }
}
