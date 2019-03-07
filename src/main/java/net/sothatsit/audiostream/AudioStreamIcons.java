package net.sothatsit.audiostream;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * A class for storing and constructing icons to be used by AudioStream.
 *
 * @author Paddy Lamont
 */
public class AudioStreamIcons {

    public static final BufferedImage AUDIO_STREAM_DOCK_ICON;
    public static final ImageIcon SERVER_STATUS_ERRORED_ICON;
    public static final ImageIcon SERVER_STATUS_CONNECTING_ICON;
    public static final ImageIcon SERVER_STATUS_RUNNING_ICON;

    static {
        AUDIO_STREAM_DOCK_ICON = readResourceImage("/icon.png");

        int serverStatusIconSize = 12;
        SERVER_STATUS_ERRORED_ICON = createServerStatusIcon(Color.RED, serverStatusIconSize);
        SERVER_STATUS_CONNECTING_ICON = createServerStatusIcon(Color.ORANGE, serverStatusIconSize);
        SERVER_STATUS_RUNNING_ICON = createServerStatusIcon(Color.GREEN, serverStatusIconSize);
    }

    private AudioStreamIcons() {
        // Not intended to be constructed
    }

    private static BufferedImage readResourceImage(String file) {
        try {
            return ImageIO.read(AudioStreamIcons.class.getResourceAsStream(file));
        } catch (IOException e) {
            throw new RuntimeException("Exception while reading image resource "+ file, e);
        }
    }

    private static ImageIcon createServerStatusIcon(Color color, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setColor(Color.BLACK);
        graphics.fillArc(0, 0, size, size, 0, 360);

        int inset = 1;
        graphics.setColor(color);
        graphics.fillArc(inset, inset, size - 2 * inset, size - 2 * inset, 0, 360);

        return new ImageIcon(image);
    }
}
