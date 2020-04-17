package main.view;

import main.ProgressMonitor;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class ProgressWindow extends JFrame implements ProgressMonitor {
    private static final String LOADER_FILE = "spinner_32.gif";
    private static final String CHECKMARK_FILE = "checkmark_32.png";
    private static final float FONT_SIZE = 34.0f;
    private Container contPane;
    private ImageIcon checkmark;
    private ImageIcon loading;
    private JLabel calcSol;
    private JLabel uninstall;
    private JLabel placeVNF;
    private JLabel flowRules;
    private boolean oldSolutionExists;


    public ProgressWindow() {
        super("Progress");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        contPane = getContentPane();

        initLayout();
        loadIcons();
    }

    private void initLayout() {
        contPane.setLayout(new BoxLayout(contPane, BoxLayout.Y_AXIS));
    }

    private void loadIcons() {
        loading = createImageIcon(LOADER_FILE);
        checkmark = createImageIcon(CHECKMARK_FILE);
    }

    @Override
    public void init(boolean oldSolutionExists, String ilpType) {
        contPane.removeAll();

        this.oldSolutionExists = oldSolutionExists;

        calcSol = new JLabel("Calculating " + ilpType + " solution.", loading, JLabel.CENTER);
        setFontSize(calcSol);
        add(calcSol);

        if (oldSolutionExists) {
            uninstall = new JLabel("Uninstalling old solution.", JLabel.CENTER);
            setFontSize(uninstall);
            add(uninstall);
        }

        placeVNF = new JLabel("Placing transcoder VNF.", JLabel.CENTER);
        setFontSize(placeVNF);
        add(placeVNF);

        flowRules = new JLabel("Installing flow rules.", JLabel.CENTER);
        setFontSize(flowRules);
        add(flowRules);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public void solutionCalculated() {
        calcSol.setIcon(checkmark);
        if (oldSolutionExists) {
            setLoading(uninstall);
        } else {
            setLoading(placeVNF);
        }
        pack();
    }

    @Override
    public void oldSolutionUninstalled() {
        done(uninstall);
        setLoading(placeVNF);
    }

    @Override
    public void vnfPlaced() {
        done(placeVNF);
        setLoading(flowRules);
    }

    @Override
    public void flowsInstalled() {
        done(flowRules);
        setVisible(false);
    }

    private void setLoading(JLabel label) {
        label.setIcon(loading);
    }

    private void done(JLabel label) {
        label.setIcon(checkmark);
    }

    private void setFontSize(JLabel label) {
        label.setFont(label.getFont().deriveFont(FONT_SIZE));
    }


    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    private ImageIcon createImageIcon(String path) {
        URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }


    // for testing purposes
    /*
    public static void main(String[] args) throws InterruptedException {
        ProgressMonitor pm = new ProgressWindow();
        pm.init(true, "Shortest-Path-SFC");
        Thread.sleep(2000);
        pm.solutionCalculated();
        Thread.sleep(2000);
        pm.oldSolutionUninstalled();
        Thread.sleep(2000);
        pm.vnfPlaced();
        Thread.sleep(2000);
        pm.flowsInstalled();
    }*/
}