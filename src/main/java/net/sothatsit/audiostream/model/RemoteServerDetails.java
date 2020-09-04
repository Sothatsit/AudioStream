package net.sothatsit.audiostream.model;

import net.sothatsit.audiostream.encryption.EncryptionVerification;
import net.sothatsit.audiostream.communication.packet.PacketBuilder;
import net.sothatsit.audiostream.communication.packet.PacketReader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Contains information about a remote server.
 *
 * @author Paddy Lamont
 */
public class RemoteServerDetails {

    public final InetSocketAddress controlAddress;
    public final RemoteAudioServerDetails audioServerDetails;
    public final EncryptionVerification encryptionVerification;

    public RemoteServerDetails(InetSocketAddress controlAddress,
                               RemoteAudioServerDetails audioServerDetails,
                               EncryptionVerification encryptionVerification) {

        this.controlAddress = controlAddress;
        this.audioServerDetails = audioServerDetails;
        this.encryptionVerification = encryptionVerification;
    }

    public boolean hasAudioServer() {
        return audioServerDetails != null;
    }

    public void writeTo(PacketBuilder builder) throws IOException {
        builder.writeInt(controlAddress.getPort());

        builder.writeBoolean(audioServerDetails != null);
        if (audioServerDetails != null) {
            audioServerDetails.writeTo(builder);
        }

        encryptionVerification.writeTo(builder);
    }

    public static RemoteServerDetails readFrom(PacketReader reader,
                                               InetAddress remoteAddress) throws IOException {

        int controlPort = reader.readInt();
        InetSocketAddress controlAddress = new InetSocketAddress(remoteAddress, controlPort);

        boolean hasAudioServer = reader.readBoolean();
        RemoteAudioServerDetails audioServerDetails = null;
        if (hasAudioServer) {
            audioServerDetails = RemoteAudioServerDetails.readFrom(reader, remoteAddress);
        }

        EncryptionVerification encryptionVerification = EncryptionVerification.readFrom(reader);

        return new RemoteServerDetails(controlAddress, audioServerDetails, encryptionVerification);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass()))
            return false;

        RemoteServerDetails other = (RemoteServerDetails) obj;
        return Objects.equals(controlAddress, other.controlAddress)
                && Objects.equals(audioServerDetails, other.audioServerDetails)
                && Objects.equals(encryptionVerification, other.encryptionVerification);
    }
}
