package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.client.RemoteAudioServer;

import javax.swing.*;
import java.awt.*;

/**
 * A ListCellRenderer for rendering a JList of RemoteAudioServers.
 *
 * @author Paddy Lamont
 */
public class RemoteAudioServerListRenderer implements ListCellRenderer<RemoteAudioServer> {

    @Override
    public Component getListCellRendererComponent(JList<? extends RemoteAudioServer> list,
                                                  RemoteAudioServer value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {

        JLabel label = new JLabel(value.getAddressString(), JLabel.LEFT);

        label.setOpaque(true);
        if (isSelected) {
            label.setBackground(new Color(220, 220, 255));
        } else {
            label.setBackground(new Color(240, 240, 240));
        }

        return label;
    }
}
