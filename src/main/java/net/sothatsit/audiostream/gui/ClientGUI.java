package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.Main;
import net.sothatsit.audiostream.client.Client;
import net.sothatsit.audiostream.client.ClientSettings;
import net.sothatsit.audiostream.util.Exceptions.ValidationException;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A GUI for configuring a Client.
 *
 * @author Paddy Lamont
 */
public class ClientGUI extends JPanel {

    private final List<JComponent> settingComponents;

    private final AudioOptionsGUI audioOptions;
    private final GuiUtils.TextFieldAndLabel addressField;
    private final GuiUtils.TextFieldAndLabel portField;
    private final JLabel statusLabel;
    private final JButton connectButton;
    private final JButton disconnectButton;

    private Client client;

    public ClientGUI() {
        this.settingComponents = new ArrayList<>();
        this.client = null;

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
            add(separator.separator, constraints.build(3));
            constraints.nextRow();

            audioOptions = new AudioOptionsGUI(AudioOptionsGUI.AudioType.OUTPUT);
            add(audioOptions, constraints.build(4));
            settingComponents.add(audioOptions);
            constraints.nextRow();
        }

        { // Connection
            separator = GuiUtils.createSeparator("Connection");
            add(separator.label, constraints.build());
            add(separator.separator, constraints.build(3));
            constraints.nextRow();

            { // Address and port

                addressField = GuiUtils.createTextFieldAndLabel("Address", "localhost", ClientGUI::isValidAddress);
                portField = GuiUtils.createTextFieldAndLabel("Port", 6789, ClientGUI::isValidPort);

                add(addressField.label, constraints.build());
                add(addressField.field, constraints.weightX(1.0).build());
                settingComponents.add(addressField.field);
                add(portField.label, constraints.build());
                add(portField.field, constraints.padX(50).build());
                settingComponents.add(portField.field);
                constraints.nextRow();
            }

            { // Connection
                statusLabel = new JLabel();
                add(new JLabel("Status"), constraints.build());
                add(statusLabel, constraints.build(3));
                constraints.nextRow();

                connectButton = new JButton();
                disconnectButton = new JButton();

                connectButton.setPreferredSize(new Dimension(150, 30));
                disconnectButton.setPreferredSize(new Dimension(150, 30));

                connectButton.addActionListener(event -> {
                    connectButton.setEnabled(false);
                    connectButton.setText("Connecting...");
                    connect();
                });
                disconnectButton.addActionListener(event -> {
                    disconnectButton.setEnabled(false);
                    disconnectButton.setText("Disconnecting...");
                    disconnect();
                });

                add(GuiUtils.buildCenteredPanel(connectButton, disconnectButton), constraints.build(4));
                constraints.nextRow();
            }
        }

        { // Empty space
            add(new JPanel(), constraints.weightY(1.0).build());
            constraints.nextRow();
        }

        // Start update loop
        new Timer(100, event -> update()).start();
    }

    public void update() {
        if (client != null && !client.isAlive()) {
            Exception exception = client.takeThreadException();
            if (exception != null) {
                GuiUtils.reportError(exception);
            }

            client = null;
        }

        if (client == null) {
            GuiUtils.enableAll(settingComponents);

            statusLabel.setText("Disconnected");
            statusLabel.setForeground(Color.GRAY);

            connectButton.setText("Connect");
            connectButton.setEnabled(true);

            disconnectButton.setText("Disconnected");
            disconnectButton.setEnabled(false);
            return;
        }

        GuiUtils.disableAll(settingComponents);

        disconnectButton.setText("Disconnect");
        disconnectButton.setEnabled(true);

        connectButton.setEnabled(false);
        statusLabel.setForeground(Color.DARK_GRAY);
        if (client.isConnected()) {
            String clientStatus = client.getStatus();

            if (clientStatus.isEmpty()) {
                statusLabel.setText("Connected");
            } else {
                statusLabel.setText("Connected: " + clientStatus);
            }

            connectButton.setText("Connected");
        } else {
            statusLabel.setText("Connecting...");
            connectButton.setText("Connecting...");
        }
    }

    public void connect() {
        if (client != null)
            throw new ValidationException("Already connected");

        client = new Client(createSettings());
        client.start();
    }

    public void disconnect() {
        if (client == null)
            throw new ValidationException("Already disconnected");

        while (true) {
            try {
                client.stopGracefully();
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        client = null;
    }

    public ClientSettings createSettings() throws ValidationException {
        Mixer.Info mixer = audioOptions.getSelectedMixer();
        AudioFormat format = audioOptions.getSelectedAudioFormat();
        if (mixer == null)
            throw new ValidationException("Please select an Audio Output");
        if (format == null)
            throw new ValidationException("Please select an Audio Format");

        int bufferSize = Main.DEFAULT_BUFFER_SIZE;
        double reportIntervalSecs = Main.DEFAULT_REPORT_INTERVAL_SECS;
        String address = this.addressField.getText();

        int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid port " + portField.getText());
        }

        return new ClientSettings(format, mixer, bufferSize, reportIntervalSecs, address, port);
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
