package net.sothatsit.audiostream.view;

import net.sothatsit.audiostream.AudioStreamIcons;
import net.sothatsit.audiostream.communication.RemoteServer;
import net.sothatsit.audiostream.encryption.Encryption;
import net.sothatsit.audiostream.encryption.EncryptionVerification;
import net.sothatsit.audiostream.model.RemoteServerDetails;
import net.sothatsit.property.Property;
import net.sothatsit.property.awt.BasicListModel;
import net.sothatsit.property.awt.PropertyLabel;

import javax.swing.*;
import java.awt.*;

/**
 * A ListCellRenderer for rendering a JList of RemoteAudioServers.
 *
 * @author Paddy Lamont
 */
public class RemoteAudioServerListRenderer implements ListCellRenderer<RemoteServer> {

    private final Property<Encryption> encryption;

    public RemoteAudioServerListRenderer(Property<Encryption> encryption) {
        this.encryption = encryption;
    }

    private Icon getStatusIcon(EncryptionVerification encryptionVerification, Encryption encryption) {
        if (encryptionVerification == null)
            return AudioStreamIcons.SERVER_STATUS_ERRORED_ICON;

        if (!encryptionVerification.isEncrypted)
            return AudioStreamIcons.UNENCRYPTED_ICON;

        if (encryptionVerification.matchesEncryption(encryption))
            return AudioStreamIcons.ENCRYPTED_ICON;

        return AudioStreamIcons.UNKNOWN_ENCRYPTION_ICON;
    }

    private Icon getStatusIcon(RemoteServer server) {
        RemoteServerDetails serverDetails = server.getDetails().get();
        if (serverDetails == null)
            return null;

        return getStatusIcon(serverDetails.encryptionVerification, encryption.get());
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends RemoteServer> list,
                                                  RemoteServer server,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {


        JLabel label = new JLabel(server.getAddressString(), JLabel.LEFT);
        JLabel icon = new JLabel(getStatusIcon(server));
        icon.setHorizontalAlignment(JLabel.RIGHT);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(label, BorderLayout.WEST);
        panel.add(icon, BorderLayout.EAST);

        panel.setOpaque(true);
        if (isSelected) {
            panel.setBackground(new Color(210, 210, 255));
        } else {
            panel.setBackground(new Color(230, 230, 230));
        }

        return panel;
    }
}
