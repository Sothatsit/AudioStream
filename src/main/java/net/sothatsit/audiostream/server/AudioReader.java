package net.sothatsit.audiostream.server;

import net.sothatsit.audiostream.gui.util.GuiUtils;
import net.sothatsit.audiostream.util.VariableBuffer;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class that reads a stream of audio from the OS.
 *
 * @author Paddy Lamont
 */
public class AudioReader implements Runnable {

    private final List<VariableBuffer> outBuffers;
    private final AudioFormat format;
    private final TargetDataLine line;
    private final byte[] buffer;

    private AtomicBoolean running;
    private Thread thread;

    public AudioReader(Mixer.Info mixer, AudioFormat format, int bufferSamples) throws LineUnavailableException {
        this.outBuffers = new ArrayList<>();
        this.format = format;
        this.line = AudioSystem.getTargetDataLine(format, mixer);

        int bufferSize = bufferSamples * format.getSampleSizeInBits() / 8;
        this.buffer = new byte[bufferSize];
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
        if (thread != null)
            throw new IllegalStateException("Thread already running");

        running = new AtomicBoolean(true);
        thread = new Thread(this, "AudioReader");
        thread.start();
    }

    public void stop() {
        if (thread == null)
            throw new IllegalStateException("Thread is not running");

        running.set(false);
    }

    public void stopForcefully() {
        if (thread == null)
            throw new IllegalStateException("Thread is not running");

        thread.stop();
        running = null;
    }

    @Override
    public void run() {
        try {
            readAudio();
        } catch (Exception exception) {
            GuiUtils.reportErrorFatal(exception);
        }
    }

    public void readAudio() throws LineUnavailableException {
        try {
            line.open(format, buffer.length);
            line.start();

            while (running.get()) {
                int read = line.read(buffer, 0, buffer.length);
                if (read < 0)
                    throw new RuntimeException("Invalid read");

                if (read == 0)
                    continue;

                synchronized (outBuffers) {
                    for (VariableBuffer outBuffer : outBuffers) {
                        outBuffer.push(buffer, 0, read);
                    }
                }
            }
        } finally {
            line.stop();
            line.close();
        }
    }
}
