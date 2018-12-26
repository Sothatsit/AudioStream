package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.Main;
import net.sothatsit.audiostream.server.Server;
import net.sothatsit.audiostream.server.ServerSettings;
import net.sothatsit.audiostream.util.Exceptions;
import net.sothatsit.audiostream.util.Exceptions.ValidationException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * A GUI for configuring a Server.
 *
 * @author Paddy Lamont
 */
public class ServerGUI extends JPanel {

    private final List<JComponent> settingInputs;

    private final GuiUtils.TextFieldAndLabel portField;
    private final AudioOptionsGUI audioOptions;

    private final JLabel statusLabel;
    private final JButton startButton;
    private final JButton stopButton;

    private Server server;

    public ServerGUI() {
        this.settingInputs = new ArrayList<>();
        this.server = null;

        setPreferredSize(AudioStream.GUI_SIZE);
        setLayout(new GridBagLayout());

        GuiUtils.SeparatorAndLabel separator;
        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.WEST)
                .fill(GridBagConstraints.HORIZONTAL)
                .insets(5, 5, 5, 5);

        { // Audio
            separator = GuiUtils.createSeparator("Audio");
            add(separator.label, constraints.build());
            add(separator.separator, constraints.weightX(1.0).build(1));
            constraints.nextRow();

            audioOptions = new AudioOptionsGUI(AudioOptionsGUI.AudioType.INPUT);
            add(audioOptions, constraints.build(4));
            settingInputs.add(audioOptions);
            constraints.nextRow();
        }

        { // Connection
            separator = GuiUtils.createSeparator("Connection");
            add(separator.label, constraints.build());
            add(separator.separator, constraints.weightX(1.0).build());
            constraints.nextRow();

            { // Port
                portField = GuiUtils.createTextFieldAndLabel("Port", 6789, ServerGUI::isValidPort);

                add(portField.label, constraints.build());
                add(portField.field, constraints.padX(50).build());
                settingInputs.add(portField.field);
                constraints.nextRow();
            }

            { // Connection
                statusLabel = new JLabel();
                add(new JLabel("Status"), constraints.build());
                add(statusLabel, constraints.build(3));
                constraints.nextRow();

                startButton = new JButton();
                stopButton = new JButton();

                startButton.setPreferredSize(new Dimension(150, 30));
                stopButton.setPreferredSize(new Dimension(150, 30));

                startButton.addActionListener(event -> {
                    startButton.setEnabled(false);
                    startButton.setText("Starting...");
                    start();
                });
                stopButton.addActionListener(event -> {
                    stopButton.setEnabled(false);
                    stopButton.setText("Stopping...");
                    stop();
                });

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

    public void update() {
        if (server != null && !server.isRunning()) {
            Exception exception = server.takeThreadException();
            if (exception != null) {
                GuiUtils.reportError(exception);
            }

            server = null;
        }

        if (server == null) {
            GuiUtils.enableAll(settingInputs);

            statusLabel.setText("Not running");
            statusLabel.setForeground(Color.GRAY);

            startButton.setText("Start");
            startButton.setEnabled(true);

            stopButton.setText("Stopped");
            stopButton.setEnabled(false);
            return;
        }

        GuiUtils.disableAll(settingInputs);

        Socket[] connected = server.getConnectedSockets();
        statusLabel.setForeground(Color.DARK_GRAY);
        statusLabel.setText("Running: " + connected.length + " clients");

        startButton.setText("Started");
        startButton.setEnabled(false);

        stopButton.setText("Stop");
        stopButton.setEnabled(true);
    }

    public void start() {
        if (server != null)
            throw new ValidationException("Server has already been started");

        server = new Server(createSettings());
        server.start();
    }

    public void stop() {
        if (server == null)
            throw new ValidationException("Server is not running");

        while (true) {
            try {
                server.stopGracefully();
                break;
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }

        server = null;
    }

    public ServerSettings createSettings() throws ValidationException {
        Mixer.Info mixer = audioOptions.getSelectedMixer();
        AudioFormat format = audioOptions.getSelectedAudioFormat();
        if (mixer == null)
            throw new ValidationException("Please select an Audio Input");
        if (format == null)
            throw new ValidationException("Please select an Audio Format");

        int bufferSize = Main.DEFAULT_BUFFER_SIZE;
        double reportIntervalSecs = Main.DEFAULT_REPORT_INTERVAL_SECS;

        int port;
        try {
            port = Integer.parseInt(this.portField.getText());
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid portField " + this.portField.getText());
        }

        return new ServerSettings(format, mixer, bufferSize, reportIntervalSecs, port);
    }

    public static boolean isValidPort(String portString) {
        try {
            int port = Integer.parseInt(portString);
            return port > 0 && port <= Short.MAX_VALUE;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
