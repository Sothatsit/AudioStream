package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.gui.util.*;
import net.sothatsit.audiostream.property.Property;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.BiConsumer;

/**
 * A window for viewing the details of a connected client.
 *
 * @author Paddy Lamont
 */
public class ServerInputDialog extends PropertyDialog {

    private static final Dimension DIALOG_SIZE = new Dimension(400, 130);

    private final BiConsumer<InetAddress, Integer> applyCallback;

    private final Property<String> addressString;
    private final Property<String> portString;
    private final Property<InetAddress> address;
    private final Property<Integer> port;

    private final Property<Boolean> isAddressValid;
    private final Property<Boolean> isPortValid;
    private final Property<Boolean> isValid;

    public ServerInputDialog(JFrame parent, BiConsumer<InetAddress, Integer> applyCallback) {
        super(parent, "Add New Server");

        this.applyCallback = applyCallback;

        this.addressString = Property.createNonNull("addressString", "");
        this.portString = Property.createNonNull("portString", "");
        this.address = addressString.map("address", ServerInputDialog::parseAddress);
        this.port = portString.map("port", ServerInputDialog::parsePort);

        this.isAddressValid = address.isNotNull("isAddressValid");
        this.isPortValid = port.isNotNull("isPortValid");
        this.isValid = Property.and(isAddressValid, isPortValid);

        JDialog dialog = getComponent();
        dialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new GridBagLayout());
        dialog.setPreferredSize(DIALOG_SIZE);
        dialog.setAlwaysOnTop(true);

        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.NORTHWEST)
                .fill(GridBagConstraints.BOTH)
                .insets(5, 5, 5, 5);

        { // Input Fields
            PropertyLabel addressLabel = GuiUtils.createLabel("Address");
            PropertyTextField addressField = GuiUtils.createTextField(addressString);

            PropertyLabel portLabel = GuiUtils.createLabel("Port");
            PropertyTextField portField = GuiUtils.createTextField(portString);

            addressLabel.setForeground(Property.ifCond("addressForeground", isAddressValid, Color.BLACK, Color.RED));
            portLabel.setForeground(Property.ifCond("portForeground", isPortValid, Color.BLACK, Color.RED));

            add(addressLabel, constraints.build());
            add(addressField, constraints.weightX(1.0).build());
            constraints.nextRow();

            add(portLabel, constraints.build());
            add(portField, constraints.weightX(1.0).build());
            constraints.nextRow();
        }

        { // Action Buttons
            PropertyButton addButton = new PropertyButton("Add Server", this::addServer);
            PropertyButton cancelButton = new PropertyButton("Cancel Server", this::dispose);

            addButton.setEnabled(isValid);

            add(GuiUtils.buildCenteredPanel(addButton, cancelButton), constraints.build(2));
            constraints.nextRow();
        }

        { // Empty space
            add(new JPanel(), constraints.weightY(1.0).build());
            constraints.nextRow();
        }
    }

    private void addServer() {
        dispose();

        InetAddress address = this.address.get();
        Integer port = this.port.get();

        if (address == null || port == null)
            return;

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

    /**
     * @return {@param addressString} converted to an InetAddress, or null if invalid
     */
    private static InetAddress parseAddress(String addressString) {
        if (addressString.isEmpty())
            return null;

        try {
            return InetAddress.getByName(addressString);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * @return {@param portString} converted to an Integer, or null if invalid
     */
    private static Integer parsePort(String portString) {
        try {
            return Integer.valueOf(portString);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
