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

    private final byte[] delayBuffer;
    private int delayBufferFilled;

    public AudioWriter(Mixer.Info mixer,
                       AudioFormat format,
                       int bufferDelayBytes,
                       int bufferBytes) throws LineUnavailableException {

        this.format = format;
        this.line =  AudioSystem.getSourceDataLine(format, mixer);
        this.delayBuffer = new byte[bufferDelayBytes];

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

    public void write(byte[] bytes) {
        write(bytes, 0, bytes.length);
    }

    public void write(byte[] bytes, int offset, int length) {
        // Try write the audio to the audio output.
        int newDelayBufferFilled = delayBufferFilled + length;
        if (newDelayBufferFilled > delayBuffer.length) {
            // Calculate where to write the audio from.
            int bytesToWrite = newDelayBufferFilled - delayBuffer.length;
            int bytesToWriteFromDelayBuffer = Math.min(delayBufferFilled, bytesToWrite);
            int bytesToWriteFromBytes = bytesToWrite - bytesToWriteFromDelayBuffer;

            // Write from the delay buffer.
            if (bytesToWriteFromDelayBuffer > 0) {
                line.write(delayBuffer, 0, bytesToWriteFromDelayBuffer);
                delayBufferFilled -= bytesToWriteFromDelayBuffer;
                System.arraycopy(delayBuffer, bytesToWriteFromDelayBuffer, delayBuffer, 0, delayBufferFilled);
            }

            // Write from bytes.
            if (bytesToWriteFromBytes > 0) {
                line.write(bytes, offset, bytesToWriteFromBytes);
                offset += bytesToWriteFromBytes;
                length -= bytesToWriteFromBytes;
            }
        }

        // Write the remaining bytes to the delay buffer.
        System.arraycopy(bytes, offset, delayBuffer, delayBufferFilled, length);
        delayBufferFilled += length;
    }

    private static int roundToMultipleOfAll(int number, int... factors) {
        for (int factor : factors) {
            number /= factor;
            number *= factor;
        }
        return number;
    }
}
