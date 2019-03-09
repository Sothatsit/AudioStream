package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.util.Exceptions;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.BiConsumer;

/**
 * A window for viewing the details of a connected client.
 *
 * @author Paddy Lamont
 */
public class ServerInputDialog {

    private static final Dimension DIALOG_SIZE = new Dimension(400, 130);

    private final BiConsumer<InetAddress, Integer> applyCallback;
    private final JDialog dialog;

    private final JLabel addressLabel;
    private final JLabel portLabel;
    private final JTextField addressField;
    private final JTextField portField;

    public ServerInputDialog(JFrame parent, BiConsumer<InetAddress, Integer> applyCallback) {
        this.applyCallback = applyCallback;
        this.dialog = new JDialog(parent, "Add New Server");

        dialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new GridBagLayout());
        dialog.setPreferredSize(DIALOG_SIZE);
        dialog.setAlwaysOnTop(true);

        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.NORTHWEST)
                .fill(GridBagConstraints.BOTH)
                .insets(5, 5, 5, 5);

        Action addAction = new AbstractAction("Add Server") {
            @Override
            public void actionPerformed(ActionEvent e) {
                addServer();
            }
        };
        Action cancelAction = new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };

        { // Input Fields
            addressLabel = new JLabel("Address");
            addressField = new JTextField();
            dialog.add(addressLabel, constraints.build());
            dialog.add(addressField, constraints.weightX(1.0).build());
            constraints.nextRow();

            portLabel = new JLabel("Port");
            portField = new JTextField();
            dialog.add(portLabel, constraints.build());
            dialog.add(portField, constraints.weightX(1.0).build());
            constraints.nextRow();

            ChangeListener modifiedListener = event -> {
                boolean valid = true;

                try {
                    parseAddress(addressField.getText());
                    addressLabel.setForeground(Color.BLACK);
                } catch (UnknownHostException e) {
                    addressLabel.setForeground(Color.RED);
                    valid = false;
                }

                try {
                    Integer.parseInt(portField.getText());
                    portLabel.setForeground(Color.BLACK);
                } catch (NumberFormatException e) {
                    portLabel.setForeground(Color.RED);
                    valid = false;
                }

                addAction.setEnabled(valid);
            };
            modifiedListener.stateChanged(null);

            GuiUtils.addTextChangeListener(addressField, modifiedListener);
            GuiUtils.addTextChangeListener(portField, modifiedListener);
        }

        { // Action Buttons
            dialog.add(GuiUtils.buildCenteredPanel(addAction, cancelAction), constraints.build(2));
            constraints.nextRow();
        }

        { // Empty space
            dialog.add(new JPanel(), constraints.weightY(1.0).build());
            constraints.nextRow();
        }
    }

    private void addServer() {
        dispose();

        InetAddress address;
        int port;

        try {
            address = parseAddress(addressField.getText());
        } catch (UnknownHostException e) {
            throw new Exceptions.ValidationException("Invalid address");
        }

        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            throw new Exceptions.ValidationException("Invalid port");
        }

        applyCallback.accept(address, port);
    }

    public void show() {
        if (!dialog.isVisible()) {
            dialog.pack();
            dialog.setVisible(true);
        }

        dialog.toFront();
        dialog.requestFocus();
    }

    public void dispose() {
        dialog.setVisible(false);
        dialog.dispose();
    }

    private static InetAddress parseAddress(String addressString) throws UnknownHostException {
        if (addressString.isEmpty())
            throw new UnknownHostException("address is empty");

        return InetAddress.getByName(addressString);
    }
}
