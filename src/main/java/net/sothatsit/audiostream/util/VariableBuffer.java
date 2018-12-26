package net.sothatsit.audiostream.util;

import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A variably sized buffer for holding streaming data.
 *
 * This class is NOT thread safe and should
 * always be used from synchronized blocks.
 *
 * @author Paddy Lamont
 */
public class VariableBuffer {

    private final Lock lock;
    private final Condition bytesRead;

    private byte[] buffer;
    private int read;

    /**
     * Construct a buffer with an initial
     * capacity of initialCapacity bytes.
     */
    public VariableBuffer(int initialCapacity) {
        this.buffer = new byte[initialCapacity];
        this.read = 0;

        this.lock = new ReentrantLock();
        this.bytesRead = lock.newCondition();
    }

    public int size() {
        lock.lock();
        try {
            return read;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Push data into this buffer.
     */
    public void push(byte[] bytes, int from, int length) {
        lock.lock();
        try {
            int newCapacity = buffer.length;
            while (newCapacity - read < length) {
                newCapacity = buffer.length * 2;
            }

            if (newCapacity > buffer.length) {
                this.buffer = Arrays.copyOf(buffer, newCapacity);
            }

            System.arraycopy(bytes, from, buffer, read, length);

            this.read += length;
            bytesRead.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Pop exactly length bytes from this buffer and place them into outBuffer.
     */
    public void pop(byte[] outBuffer, int from, int length) {
        lock.lock();
        try {
            while (read < length) {
                try {
                    bytesRead.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Exception while waiting for bytes", e);
                }
            }

            System.arraycopy(buffer, 0, outBuffer, from, length);
            System.arraycopy(buffer, length, buffer, 0, read - length);

            this.read -= length;
        } finally {
            lock.unlock();
        }
    }
}
