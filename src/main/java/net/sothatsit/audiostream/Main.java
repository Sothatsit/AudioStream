package net.sothatsit.audiostream;

/**
 * The entry point to the AudioStream application.
 *
 * @author Paddy Lamont
 */
public class Main {

    public static void displayUsage() {
        System.err.println("Usage:");
        System.err.println("  java -jar AudioStream.jar");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length != 0) {
            displayUsage();
            return;
        }

        new AudioStream().start();
    }
}
