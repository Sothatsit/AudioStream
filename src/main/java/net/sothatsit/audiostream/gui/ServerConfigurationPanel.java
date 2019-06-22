package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.audio.AudioType;
import net.sothatsit.audiostream.audio.AudioUtils;
import net.sothatsit.audiostream.encryption.Encryption;
import net.sothatsit.property.Attribute;
import net.sothatsit.property.Property;
import net.sothatsit.audiostream.server.Server;
import net.sothatsit.audiostream.server.ServerSettings;
import net.sothatsit.property.Either;
import net.sothatsit.property.gui.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
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

    private final Property<Either<ServerSettings, String>> serverSettings;

    private final Attribute<Encryption> encryption;
    private final Property<Server> server;
    private final Property<String> status;

    public ServerConfigurationPanel() {
        this.encryption = Attribute.createNullable("encryption", null);
        this.server = Property.create("server");

        Property<Boolean> inSetupMode = server.isNull("inSetupMode");
        Property<Boolean> isServerRunning = Property.flatMap(
                "isServerRunning", server,
                server -> (server == null ? null : server.getIsRunning())
        ).nonNull(false);

        AudioProperties audioProperties = new AudioProperties();
        Property<String> portString = Property.create("portString", "");
        Property<Integer> port = portString.map("port", ServerConfigurationPanel::parsePort);
        Property<Boolean> isPortValid = port.isNotNull("isPortValid");

        this.serverSettings = Property.map(
                "serverSettings", audioProperties.mixer, audioProperties.audioFormat, audioProperties.bufferSize, port,
                ServerConfigurationPanel::constructServerSettings
        );
        Property<Boolean> hasValidServerSettings = Either.isLeft("hasValidServerSettings", serverSettings);

        this.status = Property.create("status");

        setLayout(new GridBagLayout());

        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.WEST)
                .fill(GridBagConstraints.HORIZONTAL)
                .insets(5, 5, 5, 5)
                .weightX(1);

        { // Audio
            add(new PropertySeparator("Audio"), constraints.build(4));
            constraints.nextRow();

            AudioPropertiesPanel audioPropertiesPanel = new AudioPropertiesPanel(
                    AudioType.INPUT,
                    audioProperties
            );
            audioPropertiesPanel.setEnabled(inSetupMode);

            add(audioPropertiesPanel, constraints.build(4));
            constraints.nextRow();
        }

        { // Connection
            add(new PropertySeparator("Connection"), constraints.build(4));
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

    public Property<Encryption> getEncryption() {
        return encryption.readOnly();
    }

    public void setEncryption(Property<Encryption> encryption) {
        this.encryption.set(encryption);
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

    /**
     * Starts a broadcasting server.
     */
    public void start() {
        Server server = this.server.get();
        if (server != null)
            return;

        Either<ServerSettings, String> settingsEither = serverSettings.get();
        if (settingsEither.isRight())
            return;

        server = new Server(settingsEither.getLeft());
        server.setEncryption(encryption);
        server.start();

        this.server.set(server);
    }

    /**
     * Stops the running server.
     */
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
     * @return {@param portString} converted to an Integer, or null if invalid.
     */
    private static Integer parsePort(String portString) {
        try {
            return Integer.valueOf(portString);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * @return An Either containing a ServerSettings object, or an error String.
     */
    private static Either<ServerSettings, String> constructServerSettings(Mixer.Info mixer,
                                                                          Either<AudioFormat, String> audioFormatEither,
                                                                          Integer bufferSize,
                                                                          Integer port) {
        if (mixer == null)
            return Either.right("Please select a mixer");
        if (audioFormatEither.isRight())
            return audioFormatEither.right();
        if (port == null)
            return Either.right("Please select a valid port");

        AudioFormat format = audioFormatEither.getLeft();
        if (!AudioUtils.isAudioFormatSupported(AudioType.INPUT, mixer, format))
            return Either.right("Unsupported audio format");

        int bufferSizeBytes = (bufferSize != null ? bufferSize : AudioStream.DEFAULT_BUFFER_SIZE);
        double reportIntervalSecs = AudioStream.DEFAULT_REPORT_INTERVAL_SECS;

        ServerSettings settings = new ServerSettings(
                format,
                mixer,
                bufferSizeBytes,
                reportIntervalSecs,
                port
        );
        return Either.left(settings);
    }
}
