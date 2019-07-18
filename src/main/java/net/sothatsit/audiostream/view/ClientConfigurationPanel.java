package net.sothatsit.audiostream.view;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.audio.AudioType;
import net.sothatsit.audiostream.communication.RemoteServer;
import net.sothatsit.audiostream.communication.RemoteServerIndex;
import net.sothatsit.audiostream.communication.audio.AudioClient;
import net.sothatsit.audiostream.communication.audio.AudioClientManager;
import net.sothatsit.audiostream.communication.audio.AudioClientSettings;
import net.sothatsit.audiostream.model.AudioStreamModel;
import net.sothatsit.property.Property;
import net.sothatsit.function.Either;
import net.sothatsit.property.awt.*;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.List;

/**
 * A GUI for configuring a Client.
 *
 * @author Paddy Lamont
 */
public class ClientConfigurationPanel extends PropertyPanel {

    // TODO : A lot of this could be updated to use properties instead of periodic updating

    private final JFrame parentFrame;
    private final AudioStreamModel model;
    private final RemoteServerIndex remoteServerIndex;
    private final AudioClientManager clientManager;

    private final AudioProperties audioProperties;
    private final Property<Either<AudioClientSettings, String>> clientSettings;

    private final JList<RemoteServer> availableServersList;
    private final JList<AudioClient> connectedServersList;
    private final BasicListModel<RemoteServer> availableServers;
    private final BasicListModel<AudioClient> connectedServers;
    private final Action connectAction;
    private final Action addAction;
    private final Action removeAction;
    private final Action disconnectAction;
    private final Action viewAction;

    private final Map<AudioClient, ClientViewDialog> clientViewWindows;

    public ClientConfigurationPanel(JFrame parentFrame, AudioStreamModel model) {
        this.parentFrame = parentFrame;
        this.model = model;
        this.remoteServerIndex = model.remoteServerIndex;
        this.clientManager = model.audioClientManager;

        this.audioProperties = new AudioProperties();
        this.clientSettings = Property.map(
                "clientSettings", audioProperties.mixer, audioProperties.bufferSize, model.encryption,
                (mixer, bufferBytes, encryption) -> {
                    if (mixer == null)
                        return Either.right("Please select a mixer");

                    AudioClientSettings settings = new AudioClientSettings(
                            mixer,
                            (bufferBytes != null ? bufferBytes : AudioStream.DEFAULT_BUFFER_SIZE),
                            AudioStream.DEFAULT_REPORT_INTERVAL_SECS,
                            encryption
                    );
                    return Either.left(settings);
                }
        );
        model.audioClientSettings.set(Either.getLeftOrNull(clientSettings));

        this.clientViewWindows = new HashMap<>();

        setLayout(new GridBagLayout());

        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.WEST)
                .fill(GridBagConstraints.BOTH)
                .insets(5, 5, 5, 5)
                .weightX(1);

        { // Audio
            add(new PropertySeparator("Audio"), constraints.build(4));
            constraints.nextRow();

            AudioPropertiesPanel audioPropertiesPanel = new AudioPropertiesPanel(
                    AudioType.OUTPUT, audioProperties, false
            );

            add(audioPropertiesPanel, constraints.build(4));
            constraints.nextRow();
        }

        { // Connections
            add(new PropertySeparator("Connections"), constraints.build(4));
            constraints.nextRow();

            { // Server Lists
                JPanel panel = new JPanel();
                panel.setLayout(new GridBagLayout());

                GBCBuilder parentConstraints = constraints;
                constraints = new GBCBuilder()
                        .anchor(GridBagConstraints.WEST)
                        .fill(GridBagConstraints.BOTH)
                        .insets(5, 5, 5, 5);

                availableServers = new BasicListModel<>();
                availableServersList = new JList<>();
                availableServersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                availableServersList.setVisibleRowCount(8);
                availableServersList.setCellRenderer(new RemoteAudioServerListRenderer(model.encryption));
                availableServersList.setModel(availableServers);



                connectedServers = new BasicListModel<>();
                connectedServersList = new JList<>();
                connectedServersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                connectedServersList.setVisibleRowCount(8);
                connectedServersList.setCellRenderer(new ClientListRenderer());
                connectedServersList.setModel(connectedServers);

                connectAction = new AbstractAction("Connect") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        connect();
                    }
                };
                addAction = new AbstractAction("Add") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        addServer();
                    }
                };
                removeAction = new AbstractAction("Remove") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        removeServer();
                    }
                };
                disconnectAction = new AbstractAction("Disconnect") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        disconnect();
                    }
                };
                viewAction = new AbstractAction("View Details") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        viewClient();
                    }
                };

                // Only allow a selection in one of the two server lists
                availableServersList.addListSelectionListener(e -> {
                    if (e.getValueIsAdjusting()) {
                        connectedServersList.clearSelection();
                    }
                    update();
                });
                connectedServersList.addListSelectionListener(e -> {
                    if (e.getValueIsAdjusting()) {
                        availableServersList.clearSelection();
                    }
                    update();
                });

                JScrollPane availableScrollPane = new JScrollPane(
                        availableServersList,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                );
                JScrollPane connectedScrollPane = new JScrollPane(
                        connectedServersList,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                );
                availableScrollPane.setMinimumSize(new Dimension(30, 150));
                connectedScrollPane.setMinimumSize(new Dimension(30, 150));

                JLabel availableServersLabel = new JLabel("Available Servers", SwingConstants.CENTER);
                JLabel connectedServersLabel = new JLabel("Connected Servers", SwingConstants.CENTER);
                availableServersLabel.setFont(availableServersLabel.getFont().deriveFont(Font.BOLD));
                connectedServersLabel.setFont(connectedServersLabel.getFont().deriveFont(Font.BOLD));
                availableServersLabel.setAlignmentX(0.5f);
                connectedServersLabel.setAlignmentX(0.5f);

                panel.add(GuiUtils.buildVerticalPanel(
                        new JComponent[]{
                                availableServersLabel,
                                availableScrollPane,
                                GuiUtils.buildCenteredPanel(connectAction, addAction, removeAction)
                        },
                        new float[]{
                                0.0f,
                                1.0f,
                                0.0f
                        }
                ), constraints.weight(1.0, 1.0).build());

                panel.add(new JLabel("->"), constraints.weight(0, 0).build());

                panel.add(GuiUtils.buildVerticalPanel(
                        new JComponent[]{
                                connectedServersLabel,
                                connectedScrollPane,
                                GuiUtils.buildCenteredPanel(disconnectAction, viewAction)
                        },
                        new float[]{
                                0.0f,
                                1.0f,
                                0.0f
                        }
                ), constraints.weight(1.0, 1.0).build());
                constraints.nextRow();

                // Restore parent constraints
                constraints = parentConstraints;
                add(panel, constraints.weight(1.0, 1.0).build(4));
                constraints.nextRow();
            }
        }

        // Start update loop
        new Timer(100, event -> update()).start();
    }

    public List<AudioClient> getClients() {
        return clientManager.getClients();
    }

    private void connect() {
        RemoteServer server = availableServersList.getSelectedValue();
        if (server == null)
            return;

        clientManager.connect(server);
    }

    private void addServer() {
        new ServerInputDialog(parentFrame, this::addServer).show();
    }

    private void addServer(InetAddress address, int port) {
        try {
            InetSocketAddress socketAddress = new InetSocketAddress(address, port);
            remoteServerIndex.addManualServer(socketAddress);
        } catch (IOException exception) {
            GuiUtils.reportError(exception);
        }
    }

    private void removeServer() {
        RemoteServer server = availableServersList.getSelectedValue();
        if (server == null || !remoteServerIndex.isManuallyAddedServer(server))
            return;

        int chosenOption = JOptionPane.showConfirmDialog(
                parentFrame,
                "Are you sure you wish to remove the server " + server.getAddressString() + "?",
                "Remove server " + server.getAddressString() + "?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (chosenOption != JOptionPane.YES_OPTION)
            return;

        remoteServerIndex.removeManualServer(server);
        update();
    }

    private void disconnect() {
        AudioClient client = connectedServersList.getSelectedValue();
        if (client == null)
            return;

        clientManager.disconnect(client);
    }

    private void viewClient() {
        AudioClient client = connectedServersList.getSelectedValue();
        if (client == null)
            return;

        ClientViewDialog view = clientViewWindows.get(client);
        if (view != null) {
            view.show();
            return;
        }

        view = new ClientViewDialog(parentFrame, client);
        view.show();

        clientViewWindows.put(client, view);
    }

    private void updateClientViewWindows() {
        Set<AudioClient> invalidClients = new HashSet<>(clientViewWindows.keySet());
        invalidClients.removeAll(clientManager.getClients());

        for (AudioClient client : invalidClients) {
            ClientViewDialog view = clientViewWindows.remove(client);

            view.dispose();
        }

        clientViewWindows.values().forEach(ClientViewDialog::update);
    }

    public void update() {
        updateClientViewWindows();

        boolean availableSelected = !availableServersList.isSelectionEmpty();
        boolean connectedSelected = !connectedServersList.isSelectionEmpty();

        connectAction.setEnabled(availableSelected);
        disconnectAction.setEnabled(connectedSelected);
        viewAction.setEnabled(connectedSelected);

        if (availableSelected) {
            RemoteServer selectedServer = availableServersList.getSelectedValue();
            removeAction.setEnabled(remoteServerIndex.isManuallyAddedServer(selectedServer));
        } else {
            removeAction.setEnabled(false);
        }

        List<RemoteServer> disconnectedServers = new ArrayList<>(remoteServerIndex.getServers());
        disconnectedServers.removeAll(clientManager.getServers());

        availableServers.replaceAll(disconnectedServers);
        connectedServers.replaceAll(clientManager.getClients());

        // TODO : This is inefficient, and could take a while to update
        //        Should be using Property listeners
        availableServers.fireContentsChanged();
        connectedServers.fireContentsChanged();
    }

    public static boolean isValidAddress(String address) {
        try {
            InetAddress.getByName(address);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public static boolean isValidPort(String portString) {
        try {
            int port = Integer.parseInt(portString);
            return port > 0 && port <= Short.MAX_VALUE;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String audioFormatToString(AudioFormat format) {
        return format.toString();
    }
}
