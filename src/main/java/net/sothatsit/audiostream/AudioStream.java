package net.sothatsit.audiostream;

import net.sothatsit.audiostream.client.Client;
import net.sothatsit.audiostream.gui.AudioStreamGUI;
import net.sothatsit.audiostream.gui.AudioStreamTrayIcon;
import net.sothatsit.audiostream.server.Server;
import net.sothatsit.audiostream.util.Apple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Stores information about the program.
 *
 * @author Paddy Lamont
 */
public class AudioStream {

    public static String NAME = "AudioStream";
    public static String VERSION = "v1.0.0";
    public static String TITLE = NAME + " " + VERSION;

    public static Dimension GUI_SIZE = new Dimension(600, 400);

    public static InetAddress MULTICAST_ADDRESS;
    public static int MULTICAST_PORT = 5647;
    public static String AUDIOSTREAM_PREFIX = "AudioStream";
    static {
        try {
            MULTICAST_ADDRESS = InetAddress.getByName("235.236.234.237");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The number of milliseconds to wait with no communication
     * before purging a potential server from the servers list.
     */
    public static final long SERVER_NO_RESPONSE_PURGE_MS = 15 * 1000;

    public static final int DEFAULT_BUFFER_SIZE = 1024 * 12;
    public static final double DEFAULT_REPORT_INTERVAL_SECS = 0.5;


    private final RemoteAudioServerIndex serverIndex;
    private final AudioStreamGUI gui;
    private final AudioStreamTrayIcon trayIcon;

    public AudioStream() {
        if (Apple.isDockIconAvailable()) {
            Apple.setDockIcon(AudioStreamIcons.AUDIO_STREAM_DOCK_ICON);
        }

        this.serverIndex = new RemoteAudioServerIndex(this);
        this.gui = new AudioStreamGUI(serverIndex);
        this.trayIcon = new AudioStreamTrayIcon(this);

        MenuItem openItem = new MenuItem("Open Window");
        openItem.addActionListener(e -> gui.show());

        MenuItem quitItem = new MenuItem("Quit AudioStream");
        quitItem.addActionListener(e -> System.exit(0));

        trayIcon.addPopupMenuItem(openItem);
        trayIcon.addPopupMenuItem(quitItem);
    }

    public Server getServer() {
        return gui.getServer();
    }

    public boolean isServerRunning() {
        return gui.isServerRunning();
    }

    public List<Client> getClients() {
        return gui.getClients();
    }

    public boolean isClientRunning() {
        return gui.isClientRunning();
    }

    public void start() throws IOException {
        serverIndex.start();

        trayIcon.addToSystemTray();
        trayIcon.scheduleThread();

        gui.show();

        if (Apple.isDockListenerAvailable()) {
            Apple.addDockListener(gui::show);
        }
    }
}
