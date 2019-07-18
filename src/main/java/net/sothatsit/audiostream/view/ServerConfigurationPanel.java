package net.sothatsit.audiostream.view;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.audio.AudioType;
import net.sothatsit.audiostream.audio.AudioUtils;
import net.sothatsit.audiostream.encryption.Encryption;
import net.sothatsit.audiostream.model.AudioStreamModel;
import net.sothatsit.property.Property;
import net.sothatsit.audiostream.communication.audio.AudioServer;
import net.sothatsit.audiostream.communication.audio.AudioServerSettings;
import net.sothatsit.function.Either;
import net.sothatsit.property.awt.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * A GUI for configuring a Server.
 *
 * @author Paddy Lamont
 */
public class ServerConfigurationPanel extends PropertyPanel {

    private final AudioStreamModel model;
    private final AudioServer server;
    private final Property<String> status;

    public ServerConfigurationPanel(AudioStreamModel model) {
        this.model = model;
        this.server = model.audioServer;

        Property<Boolean> isServerRunning = server.isRunning();
        Property<Boolean> inSetupMode = Property.not(isServerRunning);

        AudioProperties audioProperties = new AudioProperties();
        Property<String> portString = Property.create("portString", "");
        Property<Integer> port = portString.map("port", ServerConfigurationPanel::parsePort);
        Property<Boolean> isPortValid = port.isNotNull("isPortValid");
        model.controlServerPort.set(port);

        Property<Either<AudioServerSettings, String>> serverSettings = Property.map(
                "serverSettings",
                audioProperties.mixer, audioProperties.audioFormat, audioProperties.bufferSize, model.encryption,
                ServerConfigurationPanel::constructServerSettings
        );
        model.audioServerSettings.set(Either.getLeftOrNull(serverSettings));
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

                PropertyButton startButton = new PropertyButton("Start", () -> {
                    server.start();
                    try {
                        model.start();
                    } catch (IOException exception) {
                        GuiUtils.reportError(exception);
                    }
                });
                PropertyButton stopButton = new PropertyButton("Stop", () -> {
                    server.stop();
                    try {
                        model.stop();
                    } catch (IOException exception) {
                        GuiUtils.reportError(exception);
                    }
                });

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

    // TODO : This can be removed with more use of properties...
    public void update() {
        if (server.getIsRunning()) {
            status.set("Running: " + server.getConnectionCount() + " clients");
            return;
        }

        Exception exception = server.takeThreadException();
        if (exception != null) {
            GuiUtils.reportError(exception);
        }

        status.set("Not running");
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
    private static Either<AudioServerSettings, String> constructServerSettings(Mixer.Info mixer,
                                                                               Either<AudioFormat, String> audioFormatEither,
                                                                               Integer bufferSize,
                                                                               Encryption encryption) {
        if (mixer == null)
            return Either.right("Please select a mixer");
        if (audioFormatEither.isRight())
            return audioFormatEither.right();

        AudioFormat format = audioFormatEither.getLeft();
        if (!AudioUtils.isAudioFormatSupported(AudioType.INPUT, mixer, format))
            return Either.right("Unsupported audio format");

        int bufferSizeBytes = (bufferSize != null ? bufferSize : AudioStream.DEFAULT_BUFFER_SIZE);
        double reportIntervalSecs = AudioStream.DEFAULT_REPORT_INTERVAL_SECS;

        // TODO : This seems like a wasteful way to find a free port
        int port;
        try (ServerSocket portSocket = new ServerSocket(0)) {
            port = portSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        AudioServerSettings settings = new AudioServerSettings(
                format,
                mixer,
                bufferSizeBytes,
                reportIntervalSecs,
                port,
                encryption
        );
        return Either.left(settings);
    }
}
