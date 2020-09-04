package net.sothatsit.audiostream.view;

import net.sothatsit.audiostream.communication.RemoteServer;
import net.sothatsit.audiostream.model.AudioStreamModel;
import net.sothatsit.property.Property;
import net.sothatsit.property.awt.*;

import javax.swing.*;
import java.awt.*;

/**
 * A GUI for configuring a Server.
 *
 * @author Paddy Lamont
 */
public class ControlServerPanel extends PropertyPanel {

    private final AudioStreamModel model;
    private final Property<String> status;

    private final BasicListModel<RemoteServer> availableServers;

    public ControlServerPanel(AudioStreamModel model) {
        this.model = model;

        Property<Boolean> isServerRunning = model.running;
        Property<Boolean> inSetupMode = Property.not(isServerRunning);

        Property<String> portString = Property.create("portString", "");
        Property<Integer> port = portString.map("port", ControlServerPanel::parsePort);
        Property<Boolean> isPortValid = port.isNotNull("isPortValid");
        model.controlServerPort.set(port);

        this.status = Property.ternary("status", isServerRunning, "Running", "Offline");

        setLayout(new GridBagLayout());

        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.WEST)
                .fill(GridBagConstraints.HORIZONTAL)
                .insets(5, 5, 5, 5)
                .weightX(1);

        { // Control server configuration
            add(new PropertySeparator("AudioStream"), constraints.build(4));
            constraints.nextRow();

            { // Port
                PropertyLabel portLabel = new PropertyLabel("Port");
                PropertyTextField portField = new PropertyTextField(portString);

                portLabel.setForeground(Property.ternary("portLabel_fg", isPortValid, Color.BLACK, Color.RED));
                portField.setEnabled(inSetupMode);

                add(portLabel, constraints.weightX(0).build());
                add(portField, constraints.padX(50).build());
                constraints.nextRow();
            }

            { // Connection
                PropertyLabel statusLabel = new PropertyLabel(status);
                statusLabel.setForeground(Property.ternary("status_fg", isServerRunning, Color.DARK_GRAY, Color.GRAY));

                add("Status", constraints.weightX(0).build());
                add(statusLabel, constraints.build(3));
                constraints.nextRow();

                PropertyButton startButton = new PropertyButton("Start", model::start);
                PropertyButton stopButton = new PropertyButton("Stop", model::stop);

                startButton.setPreferredSize(new Dimension(150, 30));
                stopButton.setPreferredSize(new Dimension(150, 30));

                startButton.setEnabled(inSetupMode);
                stopButton.setEnabled(isServerRunning);

                add(GuiUtils.buildCenteredPanel(startButton, stopButton), constraints.build(4));
                constraints.nextRow();
            }
        }

        { // Peers
            add(new PropertySeparator("Available Servers"), constraints.build(4));
            constraints.nextRow();

            // TODO : Would be cool to be able to select a row and have the details of
            //        that remote server shown to the right of the list of servers
            availableServers = new BasicListModel<>();

            JList<RemoteServer> availableServersList = new JList<>();
            availableServersList.setSelectionModel(new NoSelectionModel());
            availableServersList.setVisibleRowCount(8);
            availableServersList.setCellRenderer(new RemoteAudioServerListRenderer(model.encryption));
            availableServersList.setModel(availableServers);

            JScrollPane availableScrollPane = new JScrollPane(
                    availableServersList,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            );
            availableScrollPane.setMinimumSize(new Dimension(30, 150));

            add(availableScrollPane, constraints.weight(1.0, 1.0).fill(GridBagConstraints.BOTH).build(4));
            constraints.nextRow();
        }

        new Timer(500, event -> availableServers.replaceAll(model.remoteServerIndex.getServers())).start();
    }

    /**
     * @return {@param portString} parsed as a short and then converted to an Integer, or null if invalid.
     */
    private static Integer parsePort(String portString) {
        try {
            return (int) Short.parseShort(portString);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Disables all selection in JList.
     */
    private static class NoSelectionModel extends DefaultListSelectionModel {

        @Override
        public void setAnchorSelectionIndex(final int anchorIndex) {}

        @Override
        public void setLeadAnchorNotificationEnabled(final boolean flag) {}

        @Override
        public void setLeadSelectionIndex(final int leadIndex) {}

        @Override
        public void setSelectionInterval(final int index0, final int index1) { }
    }
}
