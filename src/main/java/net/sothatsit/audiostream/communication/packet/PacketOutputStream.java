package net.sothatsit.audiostream.communication.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wraps an OutputStream to write whole packets.
 *
 * @author Paddy Lamont
 */
public class PacketOutputStream implements AutoCloseable {

    private final byte[] lengthBuffer = new byte[4];
    private final OutputStream outputStream;

    public PacketOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public OutputStream getDelegate() {
        return outputStream;
    }

    @Override
    public void close() throws Exception {
        outputStream.close();
    }

    public void writePacket(byte[] packet) throws IOException {
        writePacket(packet, 0, packet.length);
    }

    public void writePacket(byte[] packet, int offset, int length) throws IOException {
        if (offset + length > packet.length)
            throw new IndexOutOfBoundsException("length + offset > packet");

        // Write the packet length
        synchronized (lengthBuffer) {
            writeIntToBytes(lengthBuffer, 0, length);
            outputStream.write(lengthBuffer, 0, 4);
        }

        // Write the packet itself
        outputStream.write(packet, offset, length);
    }

    private static void writeIntToBytes(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >> 24);
        bytes[offset + 1] = (byte) (value >> 16);
        bytes[offset + 2] = (byte) (value >> 8);
        bytes[offset + 3] = (byte) value;
    }
}
