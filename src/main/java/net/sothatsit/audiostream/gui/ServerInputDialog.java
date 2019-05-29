package net.sothatsit.audiostream.gui;

import net.sothatsit.property.Property;
import net.sothatsit.property.gui.*;

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

    private final Property<InetAddress> address;
    private final Property<Integer> port;

    public ServerInputDialog(JFrame parent, BiConsumer<InetAddress, Integer> applyCallback) {
        super(parent, "Add New Server");

        this.applyCallback = applyCallback;

        Property<String> addressString = Property.createNonNull("addressString", "");
        Property<String> portString = Property.createNonNull("portString", "");
        this.address = addressString.map("address", ServerInputDialog::parseAddress);
        this.port = portString.map("port", ServerInputDialog::parsePort);

        Property<Boolean> isAddressValid = address.isNotNull("isAddressValid");
        Property<Boolean> isPortValid = port.isNotNull("isPortValid");
        Property<Boolean> isValidServer = Property.and(isAddressValid, isPortValid);

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
            PropertyLabel addressLabel = new PropertyLabel("Address");
            PropertyTextField addressField = new PropertyTextField(addressString);

            PropertyLabel portLabel = new PropertyLabel("Port");
            PropertyTextField portField = new PropertyTextField(portString);

            addressLabel.setForeground(Property.ifCond("addressLabel_fg", isAddressValid, Color.BLACK, Color.RED));
            portLabel.setForeground(Property.ifCond("portLabel_fg", isPortValid, Color.BLACK, Color.RED));

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

            addButton.setEnabled(isValidServer);

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
