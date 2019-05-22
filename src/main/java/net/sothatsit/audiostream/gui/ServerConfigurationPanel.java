package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.gui.util.*;
import net.sothatsit.audiostream.property.Property;
import net.sothatsit.audiostream.server.Server;
import net.sothatsit.audiostream.server.ServerSettings;
import net.sothatsit.audiostream.util.Either;

import javax.sound.sampled.AudioFormat;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.Socket;

/**
 * A GUI for configuring a Server.
 *
 * @author Paddy Lamont
 */
public class ServerConfigurationPanel extends PropertyPanel {

    private final AudioProperties audioProperties;
    private final Property<String> portString;
    private final Property<Integer> port;
    private final Property<Boolean> isPortValid;
    private final Property<Either<ServerSettings, String>> serverSettings;
    private final Property<Boolean> hasValidServerSettings;

    private final Property<Server> server;
    private final Property<Boolean> inSetupMode;
    private final Property<Boolean> isServerRunning;

    private final Property<String> status;

    public ServerConfigurationPanel() {
        this.server = Property.create("server");
        this.inSetupMode = server.isNull("inSetupMode");
        this.isServerRunning = Property.flatMap(
                "isServerRunning", server,
                server -> (server == null ? null : server.getIsRunning())
        ).nonNull(false);

        this.audioProperties = new AudioProperties();
        this.portString = Property.create("portString", "");
        this.port = portString.map("port", ServerConfigurationPanel::parsePort);
        this.isPortValid = port.isNotNull("isPortValid");

        this.serverSettings = Property.map(
                "serverSettings",
                audioProperties.mixer,
                audioProperties.audioFormat,
                port,
                (mixer, audioFormatEither, port) -> {
                    if (audioFormatEither.isRight())
                        return audioFormatEither.right();
                    if (mixer == null)
                        return Either.right("Please select a mixer");
                    if (port == null)
                        return Either.right("Please select a valid port");

                    AudioFormat format = audioFormatEither.getLeft();
                    int bufferSize = AudioStream.DEFAULT_BUFFER_SIZE;
                    double reportIntervalSecs = AudioStream.DEFAULT_REPORT_INTERVAL_SECS;

                    ServerSettings settings = new ServerSettings(
                            format,
                            mixer,
                            bufferSize,
                            reportIntervalSecs,
                            port
                    );
                    return Either.left(settings);
                }
        );
        this.hasValidServerSettings = Either.isLeft("hasValidServerSettings", serverSettings);

        this.status = Property.create("status");

        setLayout(new GridBagLayout());

        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.WEST)
                .fill(GridBagConstraints.HORIZONTAL)
                .insets(5, 5, 5, 5)
                .weightX(1);

        { // Audio
            add(GuiUtils.createSeparator("Audio"), constraints.build(4));
            constraints.nextRow();

            AudioPropertiesPanel audioPropertiesPanel = new AudioPropertiesPanel(
                    AudioPropertiesPanel.AudioType.INPUT,
                    audioProperties
            );
            audioPropertiesPanel.setEnabled(inSetupMode);

            add(audioPropertiesPanel, constraints.build(4));
            constraints.nextRow();
        }

        { // Connection
            add(GuiUtils.createSeparator("Connection"), constraints.build(4));
            constraints.nextRow();

            { // Port
                PropertyLabel portLabel = GuiUtils.createLabel("Port");
                PropertyTextField portField = GuiUtils.createTextField(portString);

                portLabel.setForeground(Property.ifCond("portLabelForeground", isPortValid, Color.BLACK, Color.RED));
                portField.setEnabled(inSetupMode);

                add(portLabel, constraints.weightX(0).build());
                add(portField, constraints.padX(50).build());
                constraints.nextRow();
            }

            { // Connection
                PropertyLabel statusLabel = GuiUtils.createLabel(status);
                statusLabel.setForeground(Property.ifCond("status_fg", isServerRunning, Color.DARK_GRAY, Color.GRAY));

                add("Status", constraints.weightX(0).build());
                add(GuiUtils.createLabel(status), constraints.build(3));
                constraints.nextRow();

                PropertyButton startButton = new PropertyButton("Start", this::start);
                PropertyButton stopButton = new PropertyButton("Stop", this::stop);

                startButton.setPreferredSize(new Dimension(150, 30));
                stopButton.setPreferredSize(new Dimension(150, 30));

                startButton.setEnabled(Property.and(inSetupMode, hasValidServerSettings));
                stopButton.setEnabled(isServerRunning);

                add(GuiUtils.buildCenteredPanel(startButton, stopButton), constraints.build(4));
                constraints.nextRow();
            }
        }

        { // Empty space
            add(new JPanel(), constraints.weightY(1.0).build(0));
            constraints.nextRow();
        }

        // Start update loop
        new Timer(100, event -> update()).start();
    }

    public Property<Server> getServer() {
        return server;
    }

    // TODO : This can be removed with more use of properties...
    public void update() {
        Server server = this.server.get();
        if (server != null && !server.isRunning()) {
            Exception exception = server.takeThreadException();
            if (exception != null) {
                GuiUtils.reportError(exception);
            }

            this.server.set(null);
            server = null;
        }

        if (server == null) {
            status.set("Not running");
            return;
        }

        Socket[] connected = server.getConnectedSockets();
        status.set("Running: " + connected.length + " clients");
    }

    public void start() {
        Server server = this.server.get();
        if (server != null)
            return;

        Either<ServerSettings, String> settingsEither = serverSettings.get();
        if (settingsEither.isRight())
            return;

        server = new Server(settingsEither.getLeft());
        server.start();

        this.server.set(server);
    }

    public void stop() {
        Server server = this.server.get();
        if (server == null)
            return;

        while (true) {
            try {
                server.stopGracefully();
                break;
            } catch (InterruptedException | IOException exception) {
                exception.printStackTrace();
            }
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
