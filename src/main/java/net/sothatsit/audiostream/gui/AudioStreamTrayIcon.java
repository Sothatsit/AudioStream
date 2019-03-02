package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioStream;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * A class to handle the system tray icon of AudioStream.
 *
 * @author Paddy Lamont
 */
public class AudioStreamTrayIcon {

    private static final Random random = new Random();
    private static final java.util.List<Double> numbers = new ArrayList<>();
    private static final int NUMS = 30;
    private static final int WIDTH = 128;
    private static final int HEIGHT = 128;

    private final PopupMenu popupMenu;
    private final TrayIcon trayIcon;

    private Thread thread;

    public AudioStreamTrayIcon() {
        this.popupMenu = new PopupMenu();
        this.trayIcon = new TrayIcon(generateIcon(), AudioStream.TITLE, popupMenu);
    }

    public void addPopupMenuItem(MenuItem menuItem) {
        popupMenu.add(menuItem);
    }

    public void addToSystemTray() {
        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeFromSystemTray() {
        SystemTray.getSystemTray().remove(trayIcon);
    }

    public BufferedImage generateIcon() {
        numbers.add(random.nextDouble());
        while (numbers.size() > NUMS) {
            numbers.remove(0);
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) image.getGraphics();

        double a = 0;
        double b = 0;
        double c = 0;
        for (int i=0; i < numbers.size(); ++i) {
            if(i < NUMS / 3) {
                a += numbers.get(i);
            } else if(i < NUMS * 2 / 3) {
                b += numbers.get(i);
            } else {
                c += numbers.get(i);
            }
        }

        a /= NUMS / 3;
        b /= NUMS / 3;
        c /= NUMS / 3;

        graphics.setColor(Color.BLACK);

        int spacing = WIDTH / 3;
        int barWidth = WIDTH / 4;
        int padding = (spacing - barWidth) / 2;

        int aHeight = (int) (HEIGHT * a);
        graphics.fillRect(padding, HEIGHT - aHeight, barWidth, aHeight);

        int bHeight = (int) (HEIGHT * b);
        graphics.fillRect(spacing + padding, HEIGHT - bHeight, barWidth, bHeight);

        int cHeight = (int) (HEIGHT * c);
        graphics.fillRect(2 * spacing + padding, HEIGHT - cHeight, barWidth, cHeight);

        return image;
    }

    public void update() {
        trayIcon.setImage(generateIcon());
    }

    public void scheduleThread() {
        if (thread != null && thread.isAlive())
            throw new RuntimeException("Thread already scheduled");

        thread = new Thread(() -> {
            while(true) {
                update();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void stopThread() {
        if (thread == null || !thread.isAlive())
            return;

        thread.stop();
        thread = null;
    }
}
