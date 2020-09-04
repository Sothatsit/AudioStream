package net.sothatsit.audiostream;

import net.sothatsit.audiostream.model.AudioStreamModel;
import net.sothatsit.audiostream.util.RemovableListener;
import net.sothatsit.audiostream.view.AudioStreamWindow;
import net.sothatsit.audiostream.view.AudioStreamTrayIcon;

import java.awt.*;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Manages the whole AudioStream application.
 *
 * @author Paddy Lamont
 */
public class AudioStream {

    public static String NAME = "AudioStream";
    public static String VERSION = "v1.2";
    public static String TITLE = NAME + " " + VERSION;
    public static Dimension DEFAULT_GUI_SIZE = new Dimension(640, 480);

    public static InetAddress MULTICAST_ADDRESS;
    public static int MULTICAST_PORT = 5647;
    public static InetSocketAddress MULTICAST_SOCKET_ADDRESS;
    public static String AUDIOSTREAM_PREFIX = "AudioStream";
    static {
        try {
            MULTICAST_ADDRESS = InetAddress.getByName("235.236.234.237");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        MULTICAST_SOCKET_ADDRESS = new InetSocketAddress(MULTICAST_ADDRESS, MULTICAST_PORT);
    }

    public static final int DEFAULT_BUFFER_DELAY_MS = 100;
    public static final int MAX_BUFFER_DELAY_MS = 10_000;
    public static final double DEFAULT_REPORT_INTERVAL_SECS = 0.5;

    private final AudioStreamModel model;
    private final AudioStreamWindow gui;
    private final AudioStreamTrayIcon trayIcon;

    private boolean running = false;
    private RemovableListener dockListener;

    public AudioStream() {
        if (Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().setIconImage(AudioStreamIcons.AUDIO_STREAM_DOCK_ICON);
        }

        this.model = new AudioStreamModel();
        this.gui = new AudioStreamWindow(model);
        this.trayIcon = new AudioStreamTrayIcon(model);

        MenuItem openItem = new MenuItem("Open Window");
        openItem.addActionListener(e -> gui.show());

        MenuItem quitItem = new MenuItem("Quit AudioStream");
        quitItem.addActionListener(e -> System.exit(0));

        trayIcon.addPopupMenuItem(openItem);
        trayIcon.addPopupMenuItem(quitItem);
    }

    public synchronized void start() {
        if (running)
            throw new IllegalStateException("Already running");

        running = true;

        trayIcon.addToSystemTray();
        trayIcon.scheduleThread();

        gui.show();

        dockListener = RemovableListener.createAppReopenedListener(gui::show);
    }

    public synchronized void stop() {
        if (!running)
            throw new IllegalStateException("Not running");

        running = false;

        model.stop();

        trayIcon.removeFromSystemTray();
        trayIcon.stopThread();

        gui.close();

        if (dockListener != null) {
            dockListener.remove();
            dockListener = null;
        }
    }
}
