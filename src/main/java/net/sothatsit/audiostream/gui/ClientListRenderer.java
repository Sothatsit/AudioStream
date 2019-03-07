package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioStreamIcons;
import net.sothatsit.audiostream.client.Client;
import net.sothatsit.audiostream.client.ClientState;

import javax.swing.*;
import java.awt.*;

/**
 * A ListCellRenderer for rendering a JList of Clients.
 *
 * @author Paddy Lamont
 */
public class ClientListRenderer implements ListCellRenderer<Client> {

    private Icon getStatusIcon(Client client) {
        ClientState clientState = client.getState();
        switch (clientState) {
            case CONNECTING:
                return AudioStreamIcons.SERVER_STATUS_CONNECTING_ICON;

            case CONNECTED:
                return AudioStreamIcons.SERVER_STATUS_RUNNING_ICON;

            case ERRORED:
            case STOPPED:
                return AudioStreamIcons.SERVER_STATUS_ERRORED_ICON;

            default:
                throw new IllegalStateException("Unknown ClientState " + clientState);
        }
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Client> list,
                                                  Client client,
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
