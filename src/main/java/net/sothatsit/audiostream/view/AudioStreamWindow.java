package net.sothatsit.audiostream.view;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.model.AudioStreamModel;
import net.sothatsit.audiostream.util.Apple;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The window used to control the AudioStream application.
 *
 * @author Paddy Lamont
 */
public class AudioStreamWindow {

    private final JFrame frame;

    public AudioStreamWindow(AudioStreamModel model) {
        this.frame = new JFrame(AudioStream.TITLE);

        frame.setPreferredSize(AudioStream.DEFAULT_GUI_SIZE);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCloseClick();
            }
        });

        EncryptionPanel encryptionPanel = new EncryptionPanel(model);
        ClientConfigurationPanel clientPanel = new ClientConfigurationPanel(frame, model);
        ServerConfigurationPanel serverPanel = new ServerConfigurationPanel(model);

        JTabbedPane pane = new JTabbedPane();

        pane.add("Audio Receiving", clientPanel.getComponent());
        pane.add("Audio Broadcasting", serverPanel.getComponent());
        pane.add("Encryption", encryptionPanel.getComponent());

        frame.add(pane);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    private void onCloseClick() {
        int chosenOption = JOptionPane.showConfirmDialog(
                frame,
                "Would you like AudioStream to remain running in the background?",
                "Leave AudioStream running?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (chosenOption == JOptionPane.YES_OPTION) {
            frame.setVisible(false);
        } else {
            System.exit(0);
        }
    }

    /**
     * Sets this window to be visible and brings it into focus.
     */
    public void show() {
        if (!frame.isVisible()) {
            frame.setVisible(true);
        }

        if (Apple.isAppleScriptAvailable()) {
            Apple.invokeAppleScript("tell me to activate");
        }

        frame.toFront();
        frame.requestFocus();
    }

    public void close() {
        frame.setVisible(false);
    }
}
