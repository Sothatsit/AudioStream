package net.sothatsit.audiostream.model;

import net.sothatsit.audiostream.communication.packet.PacketBuilder;
import net.sothatsit.audiostream.communication.packet.PacketReader;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Contains information about a remote audio server.
 *
 * @author Paddy Lamont
 */
public class RemoteAudioServerDetails {

    public final InetSocketAddress address;
    public final AudioFormat format;

    public RemoteAudioServerDetails(InetSocketAddress address,
                                    AudioFormat format) {

        this.address = address;
        this.format = format;
    }

    public void writeTo(PacketBuilder builder) throws IOException {
        builder.writeInt(address.getPort());
        builder.writeAudioFormat(format);
    }

    public static RemoteAudioServerDetails readFrom(PacketReader reader,
                                                    InetAddress remoteAddress) throws IOException {

        int audioPort = reader.readInt();
        InetSocketAddress audioAddress = new InetSocketAddress(remoteAddress, audioPort);
        AudioFormat audioFormat = reader.readAudioFormat();

        return new RemoteAudioServerDetails(audioAddress, audioFormat);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass()))
            return false;

        RemoteAudioServerDetails other = (RemoteAudioServerDetails) obj;
        return Objects.equals(address, other.address) && format.matches(other.format);
    }
}
