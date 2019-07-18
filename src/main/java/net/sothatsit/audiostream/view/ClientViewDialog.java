package net.sothatsit.audiostream.view;

import net.sothatsit.audiostream.audio.AudioUtils;
import net.sothatsit.audiostream.communication.audio.AudioClient;
import net.sothatsit.audiostream.util.ServiceState;
import net.sothatsit.property.Property;
import net.sothatsit.property.awt.GBCBuilder;
import net.sothatsit.property.awt.PropertyLabel;

import javax.sound.sampled.AudioFormat;
import javax.swing.*;
import java.awt.*;

/**
 * A window for viewing the details of a connected audio.
 *
 * @author Paddy Lamont
 */
public class ClientViewDialog {

    private static final Dimension DIALOG_SIZE = new Dimension(400, 100);

    private final AudioClient client;
    private final JDialog dialog;

    private final PropertyLabel statusLabel;

    public ClientViewDialog(JFrame parent, AudioClient client) {
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
            statusLabel = new PropertyLabel(client.getState().map("status", ServiceState::getStatusMessage));
            statusLabel.setForeground(Color.DARK_GRAY);
            dialog.add(new JLabel("Client Status"), constraints.build());
            dialog.add(statusLabel.getComponent(), constraints.weightX(1.0).build());
            constraints.nextRow();
        }

        { // Audio Format
            JLabel formatTitleLabel = new JLabel("Audio Format");
            formatTitleLabel.setVerticalAlignment(JLabel.TOP);
            formatTitleLabel.setVerticalTextPosition(JLabel.TOP);


            Property<AudioFormat> audioFormat = client.getServerDetails().map("format", details -> {
                if (details == null)
                    return null;
                if (details.audioServerDetails == null)
                    return null;
                return details.audioServerDetails.format;
            });

            PropertyLabel formatLine1 = new PropertyLabel(
                    audioFormat.map("formatLine1", ClientViewDialog::convertAudioFormatLine1)
            );
            PropertyLabel formatLine2 = new PropertyLabel(
                    audioFormat.map("formatLine2", ClientViewDialog::convertAudioFormatLine2)
            );
            formatLine1.setForeground(Color.DARK_GRAY);
            formatLine2.setForeground(Color.DARK_GRAY);

            dialog.add(formatTitleLabel, constraints.build());
            formatLine1.addTo(dialog, constraints.weightX(1.0).build());
            constraints.nextRow();
            dialog.add(new JPanel(), constraints.build());
            formatLine2.addTo(dialog, constraints.weightX(1.0).build());
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
        ServiceState state = client.getState().get();
        switch (state.getType()) {
            case STOPPING:
            case STOPPED:
                statusLabel.setForeground(state.hasError() ? Color.RED : Color.GRAY);
                break;

            case STARTING:
            case RUNNING:
                statusLabel.setForeground(Color.DARK_GRAY);
                break;

            default:
                throw new IllegalStateException("Unknown client state " + state.getType());
        }
    }

    public void dispose() {
        dialog.setVisible(false);
        dialog.dispose();
    }

    public static String convertAudioFormatLine1(AudioFormat format) {
        if (format == null)
            return "Unknown";

        return format.getFrameRate() + " Hz " + AudioUtils.convertChannelsToHumanString(format.getChannels());
    }

    public static String convertAudioFormatLine2(AudioFormat format) {
        if (format == null)
            return "";

        return format.getSampleSizeInBits() + "-bit "
                + AudioUtils.toHumanString(format.getEncoding()).toLowerCase()
                + " " + (format.isBigEndian() ? "big-endian" : "little-endian");
    }
}
