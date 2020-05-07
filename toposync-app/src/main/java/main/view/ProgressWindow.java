package main.view;

import main.ProgressMonitor;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class ProgressWindow extends JFrame implements ProgressMonitor {
    protected static final String LOADER_FILE = "spinner_32.gif";
    protected static final String CHECKMARK_FILE = "checkmark_32.png";
    protected static final float FONT_SIZE = 34.0f;
    protected Container contPane;
    protected ImageIcon checkmark;
    protected ImageIcon loading;
    protected JLabel calcSol;
    protected JLabel uninstall;
    protected JLabel placeVNF;
    protected JLabel flowRules;
    protected boolean oldSolutionExists;


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

        calcSol = new JLabel("Calculating " + ilpType + " tree.", loading, JLabel.CENTER);
        setFontSize(calcSol);
        add(calcSol);

        if (oldSolutionExists) {
            uninstall = new JLabel("Uninstalling old tree.", JLabel.CENTER);
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

    void setLoading(JLabel label) {
        label.setIcon(loading);
    }

    void done(JLabel label) {
        label.setIcon(checkmark);
    }

    void setFontSize(JLabel label) {
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