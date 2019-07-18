package net.sothatsit.audiostream.audio;

import javax.sound.sampled.*;

/**
 * A class that allows the writing of audio to the OS.
 *
 * @author Paddy Lamont
 */
public class AudioWriter {

    private final AudioFormat format;
    private final SourceDataLine line;
    private final int bufferBytes;

    public AudioWriter(Mixer.Info mixer,
                       AudioFormat format,
                       int bufferBytes) throws LineUnavailableException {

        this.format = format;
        this.line =  AudioSystem.getSourceDataLine(format, mixer);

        // The buffer size must be a multiple of the sample size and the frame size
        this.bufferBytes = roundToMultipleOfAll(
                bufferBytes, format.getSampleSizeInBits() / 8, format.getFrameSize()
        );
    }

    public void start() throws LineUnavailableException {
        line.open(format, bufferBytes);
        line.start();
    }

    public void stop() {
        line.stop();
        line.close();
    }

    public void write(byte[] bytes, int offset, int length) {
        line.write(bytes, offset, length);
    }

    public void write(byte[] bytes) {
        line.write(bytes, 0, bytes.length);
    }

    private static int roundToMultipleOfAll(int number, int... factors) {
        for (int factor : factors) {
            number /= factor;
            number *= factor;
        }
        return number;
    }
}
