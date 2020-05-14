package toposync.demo.view;

import javax.swing.*;

public class StatusPane extends JPanel {
    private JLabel statPre;
    private JTextArea status;

    public StatusPane() {
        initStatus();
    }

    private void initStatus() {
        statPre = new JLabel("Status:");
        status = new JTextArea("No tree installed.");

        add(statPre);
        add(status);
    }

    public void reset() {
        status.setText("No tree installed.");
    }

    public void setStatus(String statusMessage, boolean showBlinkingCaret) {
        status.setText(statusMessage);
        status.getCaret().setVisible(showBlinkingCaret);
    }

}
