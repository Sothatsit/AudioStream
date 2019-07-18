package net.sothatsit.audiostream.communication.audio;

import net.sothatsit.audiostream.encryption.Encryption;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;

/**
 * Settings used for a Server.
 *
 * @author Paddy Lamont
 */
public class AudioServerSettings {

    public final AudioFormat format;
    public final Mixer.Info mixer;
    public final int bufferSize;
    public final double reportIntervalSecs;
    public final int port;
    public final Encryption encryption;

    public AudioServerSettings(AudioFormat format,
                               Mixer.Info mixer,
                               int bufferSize,
                               double reportIntervalSecs,
                               int port,
                               Encryption encryption) {

        this.format = format;
        this.mixer = mixer;
        this.bufferSize = bufferSize;
        this.reportIntervalSecs = reportIntervalSecs;
        this.port = port;
        this.encryption = encryption;
    }
}
