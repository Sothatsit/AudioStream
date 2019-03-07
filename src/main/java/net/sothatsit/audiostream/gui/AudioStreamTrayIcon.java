package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioStream;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * A class to handle the system tray icon of AudioStream.
 *
 * @author Paddy Lamont
 */
public class AudioStreamTrayIcon {

    private static final Random random = new Random();
    private static final int NUMS = 30;
    private static final int WIDTH = 128;
    private static final int HEIGHT = 128;
    private static final int BARS = 3;

    private final AudioStream audioStream;
    private final PopupMenu popupMenu;
    private final TrayIcon trayIcon;

    private final List<Double> numbers;

    private Thread thread;
    private boolean running = true;

    public AudioStreamTrayIcon(AudioStream audioStream) {
        this.audioStream = audioStream;
        this.numbers = new ArrayList<>();

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

    private void populateNumbersList() {
        double nextNumber = 0.1;
        if (audioStream.isClientRunning() || audioStream.isServerRunning()) {
            nextNumber = random.nextDouble();
        }

        numbers.add(0, nextNumber);
        while (numbers.size() > NUMS) {
            numbers.remove(numbers.size() - 1);
        }
    }

    private double[] calculateBars() {
        int barIndexSize = NUMS / BARS;

        double[] bars = new double[BARS];
        int[] counts = new int[BARS];
        for (int index = 0; index < numbers.size(); ++index) {
            int bar = index / barIndexSize;
            if (bar >= BARS)
                break;

            bars[bar] += numbers.get(index);
            counts[bar] += 1;
        }

        for (int index = 0; index < BARS; ++index) {
            int count = counts[index];
            if (count == 0)
                continue;

            bars[index] /= count;
        }

        return bars;
    }

    private BufferedImage generateIcon() {
        populateNumbersList();

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double spacing = (double) WIDTH / BARS;
        double barWidth = 0.8 * spacing;
        double barHeight = 0.5 * HEIGHT;
        double padding = (spacing - barWidth) / 2.0;

        double[] bars = calculateBars();
        for (int index = 0; index < BARS; ++index) {
            double height = bars[index] * barHeight;
            double x = (BARS - index - 1) * spacing + padding;
            double y = HEIGHT - height;

            graphics.setColor(Color.LIGHT_GRAY);
            graphics.fillRect((int) x, (int) y, (int) barWidth, (int) height);
        }

        float x = 5;
        float y = 75;
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 80));

        graphics.setColor(audioStream.isClientRunning() ? Color.BLACK : Color.GRAY);
        graphics.drawString("A", x, y);
        x += 64;

        graphics.setColor(audioStream.isServerRunning() ? Color.BLACK : Color.GRAY);
        graphics.drawString("S",  x, y);

        return image;
    }

    public void update() {
        trayIcon.setImage(generateIcon());
    }

    public void runUpdateLoop() {
        while (running) {
            update();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void scheduleThread() {
        if (thread != null && thread.isAlive())
            throw new IllegalStateException("Thread already scheduled");

        running = true;
        thread = new Thread(this::runUpdateLoop);
        thread.setDaemon(true);
        thread.start();
    }

    public void stopThread() {
        if (thread == null || !thread.isAlive())
            return;

        running = false;
        thread.stop();
        thread = null;
    }
}
