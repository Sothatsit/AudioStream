package net.sothatsit.audiostream.packet;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.audio.AudioUtils;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;

/**
 * A class used to read setup communication packets from a stream.
 *
 * @author Paddy Lamont
 */
public class PacketReader {

    private final ObjectInputStream stream;

    private PacketReader(byte[] bytes) throws IOException {
        this.stream = new ObjectInputStream(new ByteArrayInputStream(bytes));
    }

    private PacketReader readPrefix() throws IOException {
        String prefix = stream.readUTF();
        if (!AudioStream.AUDIOSTREAM_PREFIX.equals(prefix))
            throw new IllegalArgumentException("Prefix did not match AudioStream prefix");

        return this;
    }

    public PacketType readType() throws IOException {
        int typeOrdinal = stream.readInt();
        return PacketType.values()[typeOrdinal];
    }

    public InetAddress readAddress() throws IOException {
        try {
            return (InetAddress) stream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Exception reading InetAddress from stream", e);
        }
    }

    public int readInt() throws IOException {
        return stream.readInt();
    }

    public AudioFormat readAudioFormat() throws IOException {
        String encodingString = stream.readUTF();
        float sampleRate = stream.readFloat();
        int sampleSizeInBits = stream.readInt();
        int channels = stream.readInt();
        int frameSize = stream.readInt();
        float frameRate = stream.readFloat();
        boolean isBigEndian = stream.readBoolean();

        AudioFormat.Encoding encoding = AudioUtils.AUDIO_FORMAT_ENCODINGS.get(encodingString);
        if (encoding == null)
            throw new IllegalArgumentException("Unknown encoding " + encodingString);

        return new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, isBigEndian);
    }

    public static PacketReader create(byte[] bytes) throws IOException {
        return new PacketReader(bytes).readPrefix();
    }
}
