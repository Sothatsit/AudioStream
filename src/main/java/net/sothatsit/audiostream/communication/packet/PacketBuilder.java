package net.sothatsit.audiostream.communication.packet;

import net.sothatsit.audiostream.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * A class used to construct packets for setup communication between audio and server.
 *
 * @author Paddy Lamont
 */
public class PacketBuilder {

    private final ByteArrayOutputStream byteStream;
    private final ObjectOutputStream stream;

    private PacketBuilder() throws IOException {
        this.byteStream = new ByteArrayOutputStream();
        this.stream = new ObjectOutputStream(byteStream);
    }

    private PacketBuilder writePrefix() throws IOException {
        stream.writeUTF(AudioStream.AUDIOSTREAM_PREFIX);
        return this;
    }

    public PacketBuilder writeType(PacketType type) throws IOException {
        stream.writeInt(type.ordinal());
        return this;
    }

    public PacketBuilder writeAddress(InetAddress address) throws IOException {
        stream.writeObject(address);
        return this;
    }

    public PacketBuilder writeSocketAddress(InetSocketAddress address) throws IOException {
        writeAddress(address.getAddress());
        return writeInt(address.getPort());
    }

    public PacketBuilder writeAudioFormat(AudioFormat format) throws IOException {
        stream.writeUTF(format.getEncoding().toString());
        stream.writeFloat(format.getSampleRate());
        stream.writeInt(format.getSampleSizeInBits());
        stream.writeInt(format.getChannels());
        stream.writeInt(format.getFrameSize());
        stream.writeFloat(format.getFrameRate());
        stream.writeBoolean(format.isBigEndian());
        return this;
    }

    public PacketBuilder writeInt(int integer) throws IOException {
        stream.writeInt(integer);
        return this;
    }

    // TODO : All your standard data writing methods

    public PacketBuilder writeBoolean(boolean bool) throws IOException {
        stream.writeBoolean(bool);
        return this;
    }

    public PacketBuilder writeBytes(byte[] bytes) throws IOException {
        return writeBytes(bytes, 0, bytes.length);
    }

    public PacketBuilder writeBytes(byte[] bytes, int offset, int length) throws IOException {
        stream.writeInt(length);
        stream.write(bytes, offset, length);
        return this;
    }

    public byte[] build() throws IOException {
        stream.flush();
        stream.close();
        byteStream.close();

        return byteStream.toByteArray();
    }

    public static PacketBuilder create() throws IOException {
        return new PacketBuilder().writePrefix();
    }
}
