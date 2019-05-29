package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioUtils;
import net.sothatsit.audiostream.client.Client;
import net.sothatsit.audiostream.client.ClientState;
import net.sothatsit.audiostream.client.ClientStatus;
import net.sothatsit.property.gui.GBCBuilder;
import net.sothatsit.property.gui.PropertyLabel;

import javax.sound.sampled.AudioFormat;
import javax.swing.*;
import java.awt.*;

/**
 * A window for viewing the details of a connected client.
 *
 * @author Paddy Lamont
 */
public class ClientViewDialog {

    private static final Dimension DIALOG_SIZE = new Dimension(400, 100);

    private final Client client;
    private final JDialog dialog;

    private final PropertyLabel statusLabel;

    public ClientViewDialog(JFrame parent, Client client) {
        this.client = client;
        this.dialog = new JDialog(parent, "Client " + client.getServer().getAddressString());

        dialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new GridBagLayout());
        dialog.setPreferredSize(DIALOG_SIZE);

        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.NORTHWEST)
                .fill(GridBagConstraints.BOTH)
                .insets(5, 5, 5, 5);

        { // Status
            statusLabel = new PropertyLabel(client.getStatus().map("status", ClientStatus::getMessage));
            statusLabel.setForeground(Color.DARK_GRAY);
            dialog.add(new JLabel("Client Status"), constraints.build());
            dialog.add(statusLabel.getComponent(), constraints.weightX(1.0).build());
            constraints.nextRow();
        }

        { // Audio Format
            AudioFormat format = client.getSettings().format;

            JLabel formatTitleLabel = new JLabel("Audio Format");
            formatTitleLabel.setVerticalAlignment(JLabel.TOP);
            formatTitleLabel.setVerticalTextPosition(JLabel.TOP);

            JLabel formatBasicDetailsLabel = new JLabel(
                    format.getFrameRate() + " Hz "
                            + AudioUtils.convertChannelsToHumanString(format.getChannels())
            );
            JLabel formatExtraDetailsLabel = new JLabel(
                    format.getSampleSizeInBits() + "-bit "
                            + AudioUtils.toHumanString(format.getEncoding()).toLowerCase()
                            + " " + (format.isBigEndian() ? "big-endian" : "little-endian")
            );
            formatBasicDetailsLabel.setForeground(Color.DARK_GRAY);
            formatExtraDetailsLabel.setForeground(Color.DARK_GRAY);

            dialog.add(formatTitleLabel, constraints.build());
            dialog.add(formatBasicDetailsLabel, constraints.weightX(1.0).build());
            constraints.nextRow();
            dialog.add(new JPanel(), constraints.build());
            dialog.add(formatExtraDetailsLabel, constraints.weightX(1.0).build());
            constraints.nextRow();
        }

        { // Empty space
            dialog.add(new JPanel(), constraints.weightY(1.0).build());
            constraints.nextRow();
        }
    }

    public void show() {
        update();

        if (!dialog.isVisible()) {
            dialog.pack();
            dialog.setVisible(true);
        }

        dialog.toFront();
        dialog.requestFocus();
    }

    public void update() {
        ClientState clientState = client.getState();
        switch (clientState) {
            case STOPPED:
                statusLabel.setForeground(Color.GRAY);
                break;

            case CONNECTED:
            case CONNECTING:
                statusLabel.setForeground(Color.DARK_GRAY);
                break;

            case ERRORED:
                statusLabel.setForeground(Color.RED);
                break;

            default:
                throw new IllegalStateException("Unknown ClientState " + clientState);
        }
    }

    public void dispose() {
        dialog.setVisible(false);
        dialog.dispose();
    }
}
