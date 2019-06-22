package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.client.RemoteAudioServerIndex;
import net.sothatsit.audiostream.client.Client;
import net.sothatsit.audiostream.client.ClientState;
import net.sothatsit.audiostream.encryption.Encryption;
import net.sothatsit.audiostream.server.Server;
import net.sothatsit.audiostream.util.Apple;
import net.sothatsit.property.Property;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * The window used to control the AudioStream application.
 *
 * @author Paddy Lamont
 */
public class AudioStreamWindow {

    private final JFrame frame;

    private final ClientConfigurationPanel clientPanel;
    private final ServerConfigurationPanel serverPanel;
    private final EncryptionPanel encryptionPanel;

    public AudioStreamWindow(RemoteAudioServerIndex remoteServerIndex) {
        this.frame = new JFrame(AudioStream.TITLE);

        frame.setPreferredSize(AudioStream.GUI_SIZE);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCloseClick();
            }
        });

        this.encryptionPanel = new EncryptionPanel();
        this.clientPanel = new ClientConfigurationPanel(frame, remoteServerIndex);
        this.serverPanel = new ServerConfigurationPanel();

        Property<Encryption> encryption = getEncryption();
        clientPanel.setEncryption(encryption);
        serverPanel.setEncryption(encryption);

        JTabbedPane pane = new JTabbedPane();

        pane.add("Audio Receiving", clientPanel.getComponent());
        pane.add("Audio Broadcasting", serverPanel.getComponent());
        pane.add("Encryption", encryptionPanel.getComponent());

        frame.add(pane);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public Server getServer() {
        return serverPanel.getServer().get();
    }

    public boolean isServerRunning() {
        Server server = getServer();
        return server != null && server.isRunning();
    }

    public List<Client> getClients() {
        return clientPanel.getClients();
    }

    public boolean isClientRunning() {
        for (Client client : getClients()) {
            if (client.getState() == ClientState.CONNECTED)
                return true;
        }
        return false;
    }

    public Property<Encryption> getEncryption() {
        return encryptionPanel.getEncryption();
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
}
