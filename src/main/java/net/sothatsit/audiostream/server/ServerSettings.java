package net.sothatsit.audiostream.server;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;

/**
 * Settings used for a Server.
 *
 * @author Paddy Lamont
 */
public class ServerSettings {

    public final AudioFormat format;
    public final Mixer.Info mixer;
    public final int bufferSize;
    public final double reportIntervalSecs;

    public final int port;

    public ServerSettings(AudioFormat format,
                          Mixer.Info mixer,
                          int bufferSize,
                          double reportIntervalSecs,
                          int port) {

        this.format = format;
        this.mixer = mixer;
        this.bufferSize = bufferSize;
        this.reportIntervalSecs = reportIntervalSecs;

        this.port = port;
    }
}
