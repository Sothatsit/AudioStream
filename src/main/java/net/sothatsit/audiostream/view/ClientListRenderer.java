package net.sothatsit.audiostream.view;

import net.sothatsit.audiostream.AudioStreamIcons;
import net.sothatsit.audiostream.communication.audio.AudioClient;
import net.sothatsit.audiostream.util.ServiceState;

import javax.swing.*;
import java.awt.*;

/**
 * A ListCellRenderer for rendering a JList of Clients.
 *
 * @author Paddy Lamont
 */
public class ClientListRenderer implements ListCellRenderer<AudioClient> {

    private Icon getStatusIcon(AudioClient client) {
        ServiceState state = client.getState().get();
        switch (state.getType()) {
            case STARTING:
                return AudioStreamIcons.SERVER_STATUS_CONNECTING_ICON;

            case RUNNING:
                return AudioStreamIcons.SERVER_STATUS_RUNNING_ICON;

            case STOPPING:
            case STOPPED:
                return AudioStreamIcons.SERVER_STATUS_ERRORED_ICON;

            default:
                throw new IllegalStateException("Unknown client state " + state.getType());
        }
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends AudioClient> list,
                                                  AudioClient client,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {

        JLabel label = new JLabel(client.getServer().getAddressString(), JLabel.LEFT);
        JLabel icon = new JLabel(getStatusIcon(client), JLabel.RIGHT);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(label, BorderLayout.WEST);
        panel.add(icon, BorderLayout.EAST);

        if (isSelected) {
            panel.setBackground(new Color(220, 220, 255));
        } else {
            panel.setBackground(new Color(240, 240, 240));
        }

        return panel;
    }
}
