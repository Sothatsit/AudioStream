package net.sothatsit.audiostream.client;

import net.sothatsit.audiostream.util.StreamMonitor;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.Socket;

/**
 * Settings used for a Client.
 *
 * @author Paddy Lamont
 */
public class ClientSettings {

    public final AudioFormat format;
    public final Mixer.Info mixer;
    public final int bufferSize;
    public final double reportIntervalSecs;

    public final String ip;
    public final int port;

    public ClientSettings(AudioFormat format,
                          Mixer.Info mixer,
                          int bufferSize,
                          double reportIntervalSecs,
                          String ip,
                          int port) {

        this.format = format;
        this.mixer = mixer;
        this.bufferSize = bufferSize;
        this.reportIntervalSecs = reportIntervalSecs;

        this.ip = ip;
        this.port = port;
    }

    public SourceDataLine getSourceDataLine() throws LineUnavailableException {
        return AudioSystem.getSourceDataLine(format, mixer);
    }

    public Socket connectToSocket() throws IOException {
        return new Socket(ip, port);
    }

    public StreamMonitor createStreamMonitor() {
        return new StreamMonitor(reportIntervalSecs, format.getSampleSizeInBits() / 8);
    }

    public String getAddress() {
        return ip + ":" + port;
    }
}
