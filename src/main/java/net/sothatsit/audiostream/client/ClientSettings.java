package net.sothatsit.audiostream.client;

import net.sothatsit.audiostream.StreamMonitor;

import javax.sound.sampled.*;

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

    public ClientSettings(AudioFormat format,
                          Mixer.Info mixer,
                          int bufferSize,
                          double reportIntervalSecs) {

        this.format = format;
        this.mixer = mixer;
        this.bufferSize = bufferSize;
        this.reportIntervalSecs = reportIntervalSecs;
    }

    public SourceDataLine getSourceDataLine() throws LineUnavailableException {
        return AudioSystem.getSourceDataLine(format, mixer);
    }

    public StreamMonitor createStreamMonitor() {
        return new StreamMonitor(reportIntervalSecs, format.getSampleSizeInBits() / 8);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass()))
            return false;

        ClientSettings other = (ClientSettings) obj;

        return format.matches(other.format)
                && mixer.equals(other.mixer)
                && bufferSize == other.bufferSize
                && reportIntervalSecs == other.reportIntervalSecs;
    }
}
