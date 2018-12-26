package net.sothatsit.audiostream;

import net.sothatsit.audiostream.client.Client;
import net.sothatsit.audiostream.client.ClientSettings;
import net.sothatsit.audiostream.gui.AudioStreamGUI;
import net.sothatsit.audiostream.gui.ClientGUI;
import net.sothatsit.audiostream.server.Server;
import net.sothatsit.audiostream.server.ServerSettings;

import javax.sound.sampled.AudioFormat;

/**
 * The entry point to the AudioStream application.
 *
 * @author Paddy Lamont
 */
public class Main {

    public static final AudioFormat DEFAULT_FORMAT = new AudioFormat(48000, 16, 2, true, false);
    public static final String DEFAULT_CLIENT_MIXER = "Built-in Output";
    private static final String DEFAULT_SERVER_MIXER = "Soundflower (2ch)";
    public static final int DEFAULT_BUFFER_SIZE = 1024 * 12;
    public static final double DEFAULT_REPORT_INTERVAL_SECS = 0.5;

    public static void displayGeneralUsage() {
        System.err.println("Usage:");
        System.err.println("  java -jar AudioStream.jar <client / server> [options]");
        System.exit(1);
    }

    public static void displayClientUsage() {
        System.err.println("Usage:");
        System.err.println("  java -jar AudioStream.jar client <address:port>");
        System.exit(1);
    }

    public static void displayServerUsage() {
        System.err.println("Usage:");
        System.err.println("  java -jar AudioStream.jar server <port>");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            commandLineMain(args);
            return;
        }

        AudioUtils.displayMixerInfo();
        new AudioStreamGUI().show();
    }

    public static void commandLineMain(String[] args) throws Exception {
        if (args.length == 0) {
            displayGeneralUsage();
            return;
        }

        boolean isServer = args[0].equalsIgnoreCase("server");
        boolean isClient = args[0].equalsIgnoreCase("client");

        if (!isServer && !isClient) {
            displayGeneralUsage();
            return;
        }

        if (isClient) {
            if (args.length != 2) {
                displayClientUsage();
                return;
            }

            String[] split = args[1].split(":");
            String address = split[0];
            int port;

            try {
                port = Integer.valueOf(split[1]);
            } catch (NumberFormatException e) {
                displayClientUsage();
                return;
            }

            ClientSettings settings = new ClientSettings(
                    DEFAULT_FORMAT,
                    AudioUtils.getMixer(DEFAULT_CLIENT_MIXER),
                    DEFAULT_BUFFER_SIZE,
                    DEFAULT_REPORT_INTERVAL_SECS,
                    address, port
            );

            new Client(settings).run();
        } else {
            if (args.length != 2) {
                displayServerUsage();
                return;
            }

            int port;
            try {
                port = Integer.valueOf(args[1]);
            } catch (NumberFormatException e) {
                displayServerUsage();
                return;
            }

            ServerSettings settings = new ServerSettings(
                    DEFAULT_FORMAT,
                    AudioUtils.getMixer(DEFAULT_SERVER_MIXER),
                    DEFAULT_BUFFER_SIZE,
                    DEFAULT_REPORT_INTERVAL_SECS,
                    port
            );

            new Server(settings).run();
        }
    }
}
