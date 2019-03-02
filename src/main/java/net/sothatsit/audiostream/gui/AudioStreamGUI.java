package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioStream;

import javax.swing.*;

/**
 * The graphical user interface for controlling the AudioStream application.
 *
 * @author Paddy Lamont
 */
public class AudioStreamGUI {

    private final JFrame frame;

    public AudioStreamGUI() {
        this.frame = new JFrame(AudioStream.TITLE);
        this.frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        JTabbedPane pane = new JTabbedPane();

        pane.add("Client", new ClientGUI());
        pane.add("Server", new ServerGUI());

        frame.add(pane);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public JFrame getFrame() {
        return frame;
    }

    public void show() {
        if (!frame.isVisible()) {
            frame.setVisible(true);
        } else {
            frame.toFront();
            frame.requestFocus();
        }
    }
}
