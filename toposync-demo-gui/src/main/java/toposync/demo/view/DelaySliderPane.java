package toposync.demo.view;

import toposync.demo.controller.Controller;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class DelaySliderPane extends JPanel implements ChangeListener {
    private static int MIN_DELAY = 50;
    private static int MAX_DELAY = 500;
    private static int INIT_DELAY = 50;
    private int currentDelay = INIT_DELAY;


    private JSlider slider;
    private Controller controller;

    public DelaySliderPane() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        JLabel label = new JLabel("Link delay (ms)");
        label.setAlignmentX(CENTER_ALIGNMENT);
        add(label);

        slider = new JSlider(JSlider.HORIZONTAL, MIN_DELAY, MAX_DELAY, INIT_DELAY);
        slider.setMajorTickSpacing(50);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);
        slider.setMaximumSize(new Dimension(400, 100));
        slider.addChangeListener(this);
        add(slider);
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void setDelay(int ms) {
        if (ms == -1) { // querying delay was not successfull (e.g. topo not set up yet)
            slider.setEnabled(false);
        } else {
            slider.setEnabled(true);
            slider.setValue(ms);
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
        if (!source.getValueIsAdjusting()) {
            int ms = source.getValue();

            if (ms != currentDelay) {
                delayChange(ms);
            }
        }
    }

    private void delayChange(int newDelay) {
        this.currentDelay = newDelay;
        controller.setLinkDelay(newDelay, this);
    }
}
