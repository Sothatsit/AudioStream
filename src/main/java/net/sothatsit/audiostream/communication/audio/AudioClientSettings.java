package net.sothatsit.audiostream.communication.audio;

import net.sothatsit.audiostream.StreamMonitor;
import net.sothatsit.audiostream.encryption.Encryption;

import javax.sound.sampled.*;
import java.util.Objects;

/**
 * Settings used for a Client.
 *
 * @author Paddy Lamont
 */
public class AudioClientSettings {

    public final Mixer.Info mixer;
    public final int bufferSizeMS;
    public final int bufferDelayMS;
    public final double reportIntervalSecs;
    public final Encryption encryption;

    public AudioClientSettings(Mixer.Info mixer,
                               int bufferSizeMS,
                               int bufferDelayMS,
                               double reportIntervalSecs,
                               Encryption encryption) {

        this.mixer = mixer;
        this.bufferSizeMS = bufferSizeMS;
        this.bufferDelayMS = bufferDelayMS;
        this.reportIntervalSecs = reportIntervalSecs;
        this.encryption = encryption;
    }

    public StreamMonitor createStreamMonitor(AudioFormat audioFormat) {
        int bytesPerSample = audioFormat.getSampleSizeInBits() / 8;

        return new StreamMonitor(reportIntervalSecs, bytesPerSample);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass()))
            return false;

        AudioClientSettings other = (AudioClientSettings) obj;

        return Objects.equals(mixer, other.mixer)
                && bufferSizeMS == other.bufferSizeMS
                && bufferDelayMS == other.bufferDelayMS
                && reportIntervalSecs == other.reportIntervalSecs
                && Objects.equals(encryption, other.encryption);
    }
}
