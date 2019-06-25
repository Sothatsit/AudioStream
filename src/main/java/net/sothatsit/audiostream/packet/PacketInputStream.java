package net.sothatsit.audiostream.packet;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;

/**
 * Wraps an InputStream to read whole packets.
 *
 * @author Paddy Lamont
 */
public class PacketInputStream implements AutoCloseable {

    private static final int MAX_BUFFER_BYTES = 64 * 1024;

    private final InputStream inputStream;
    private final byte[] buffer = new byte[MAX_BUFFER_BYTES];
    private int read = 0;

    public PacketInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    private byte[] consume(int bytes) throws IOException {
        // Make sure we have at least the necessary number of bytes read
        read(bytes);

        // TODO : Re-using a byte array here would be much more efficient
        byte[] data = new byte[bytes];

        System.arraycopy(buffer, 0, data, 0, bytes);
        System.arraycopy(buffer, bytes, buffer, 0, read - bytes);

        read -= bytes;

        return data;
    }

    private void read(int bytes) throws IOException {
        if (bytes > buffer.length)
            throw new BufferOverflowException();

        while (this.read < bytes) {
            int read = inputStream.read(buffer, this.read, buffer.length - this.read);

            if (read < 0)
                throw new IllegalStateException("Unexpected stream end");

            this.read += read;
        }
    }

    public byte[] readPacket() throws IOException {
        // Read the packet length
        int length = readIntFromBytes(consume(4));

        // Read the packet
        return consume(length);
    }

    private static int readIntFromBytes(byte[] bytes) {
        return readIntFromBytes(bytes, 0);
    }

    private static int readIntFromBytes(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
                ((bytes[offset + 1] & 0xFF) << 16) |
                ((bytes[offset + 2] & 0xFF) << 8) |
                (bytes[offset + 3] & 0xFF);
    }
}
