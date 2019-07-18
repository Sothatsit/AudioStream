package net.sothatsit.audiostream.audio;

import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.util.VariableBuffer;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that reads a stream of audio from the OS.
 *
 * @author Paddy Lamont
 */
public class AudioReader {

    private final AudioFormat format;
    private final TargetDataLine line;
    private final byte[] buffer;

    private final List<VariableBuffer> outBuffers;
    private final LoopedThread readThread;

    public AudioReader(Mixer.Info mixer,
                       AudioFormat format,
                       int bufferSamples) throws LineUnavailableException {

        this.format = format;
        this.line = AudioSystem.getTargetDataLine(format, mixer);

        // The bufferSize must be a multiple of the sample size and the frame size
        int bufferSize = bufferSamples * format.getSampleSizeInBits() / 8;
        bufferSize = (bufferSize / format.getFrameSize()) * format.getFrameSize();
        this.buffer = new byte[bufferSize];

        this.outBuffers = new ArrayList<>();
        this.readThread = new LoopedThread("readThread", this::readAudio);
    }

    public void addOutBuffer(VariableBuffer outBuffer) {
        synchronized (outBuffers) {
            outBuffers.add(outBuffer);
        }
    }

    public void removeOutBuffer(VariableBuffer outBuffer) {
        synchronized (outBuffers) {
            outBuffers.remove(outBuffer);
        }
    }

    public void start() throws LineUnavailableException {
        line.open(format, buffer.length);
        line.start();
        readThread.start();
    }

    public void stop() {
        readThread.stop();
        line.stop();
        line.close();
    }

    private synchronized void readAudio() {
        int read = line.read(buffer, 0, buffer.length);
        if (read < 0)
            throw new RuntimeException("Invalid audio read, " + read);

        if (read == 0)
            return;

        synchronized (outBuffers) {
            for (VariableBuffer outBuffer : outBuffers) {
                outBuffer.push(buffer, 0, read);
            }
        }
    }
}
