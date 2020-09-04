package net.sothatsit.audiostream.communication.packet;

import net.sothatsit.audiostream.communication.io.UnexpectedStreamEndException;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;

/**
 * Wraps a Channel to read and write whole packets.
 *
 * Acts in a blocking way, and therefore should not be used for NIO non-blocking reads/writes ( TODO : Support this ).
 *
 * @author Paddy Lamont
 */
public class PacketChannel implements AutoCloseable {

    private static final int MAX_BUFFER_BYTES = 64 * 1024;

    private final ByteChannel channel;
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;

    public PacketChannel(ByteChannel channel) {
        this.channel = channel;
        this.readBuffer = ByteBuffer.allocate(MAX_BUFFER_BYTES).order(ByteOrder.BIG_ENDIAN);
        this.writeBuffer = ByteBuffer.allocate(MAX_BUFFER_BYTES).order(ByteOrder.BIG_ENDIAN);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    /**
     * Read {@param bytes} bytes and return them as an array.
     */
    private byte[] readAsArray(int bytes) throws IOException {
        synchronized (readBuffer) {
            read(bytes);

            byte[] buffer = new byte[bytes];
            readBuffer.get(buffer);

            return buffer;
        }
    }

    /**
     * Read an int from the channel.
     */
    private int readInt() throws IOException {
        synchronized (readBuffer) {
            read(4);

            return readBuffer.getInt();
        }
    }

    /**
     * Read {@param bytes} bytes and place them into readBuffer.
     */
    private void read(int bytes) throws IOException {
        synchronized (readBuffer) {
            if (bytes > readBuffer.capacity())
                throw new BufferOverflowException();
            if (bytes <= 0)
                throw new IllegalArgumentException("bytes must be a positive integer, not " + bytes);

            // Setup buffer to read the bytes into its beginning
            readBuffer.position(0).limit(bytes);

            // Read into the buffer
            while (readBuffer.position() < bytes) {
                int read = channel.read(readBuffer);
                if (read < 0)
                    throw new UnexpectedStreamEndException();
            }

            // Reset the buffer to the beginning
            readBuffer.limit(bytes).position(0);
        }
    }

    /**
     * Read a packet as a byte array from the channel.
     */
    public byte[] readPacket() throws IOException {
        synchronized (readBuffer) {
            // Read the packet length
            int length = readInt();

            // Read the packet
            return readAsArray(length);
        }
    }

    /**
     * Write the whole of the writeBuffer to the channel.
     */
    private void writeBufferToChannel() throws IOException {
        synchronized (writeBuffer) {
            // Prepare the writeBuffer for writing
            writeBuffer.limit(writeBuffer.position()).position(0);

            // Write it to the channel
            while (writeBuffer.hasRemaining()) {
                channel.write(writeBuffer);
            }
        }
    }

    /**
     * Write the integer {@param number} to the channel.
     */
    private void writeInt(int number) throws IOException {
        synchronized (writeBuffer) {
            // Reset the buffer to the beginning
            writeBuffer.limit(writeBuffer.capacity()).position(0);

            // Write the number
            writeBuffer.putInt(number);
            writeBufferToChannel();
        }
    }

    /**
     * Write the bytes in range [{@param offset}, {@param offset} + {@param length}) in {@param bytes} to the channel.
     */
    private void writeBytes(byte[] bytes, int offset, int length) throws IOException {
        synchronized (writeBuffer) {
            // Reset the buffer to the beginning
            writeBuffer.limit(writeBuffer.capacity()).position(0);

            // Write the bytes
            writeBuffer.put(bytes, offset, length);
            writeBufferToChannel();
        }
    }

    /**
     * Write the bytes of {@param packet} to the channel.
     */
    public void writePacket(byte[] packet) throws IOException {
        writePacket(packet, 0, packet.length);
    }

    /**
     * Write the bytes [offset, offset + length) of {@param packet} to the channel.
     */
    public void writePacket(byte[] packet, int offset, int length) throws IOException {
        if (offset + length > packet.length)
            throw new IndexOutOfBoundsException("length + offset > packet");

        synchronized (writeBuffer) {
            writeInt(length);
            writeBytes(packet, offset, length);
        }
    }
}
