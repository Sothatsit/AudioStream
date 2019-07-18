package net.sothatsit.audiostream.encryption;

import net.sothatsit.audiostream.communication.packet.PacketBuilder;
import net.sothatsit.audiostream.communication.packet.PacketReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Contains information regarding the encryption of a remote server.
 *
 * @author Paddy Lamont
 */
public class EncryptionVerification {

    private static final byte[] VERIFICATION_BYTES = "AudioStreamIsBestStream".getBytes(StandardCharsets.US_ASCII);
    private static final EncryptionVerification UNENCRYPTED = new EncryptionVerification(VERIFICATION_BYTES);

    public final byte[] encryptedBytes;
    public final boolean isEncrypted;

    public EncryptionVerification(byte[] encryptedBytes) {
        this.encryptedBytes = encryptedBytes;
        this.isEncrypted = !Arrays.equals(encryptedBytes, VERIFICATION_BYTES);
    }

    public void writeTo(PacketBuilder builder) throws IOException {
        builder.writeBytes(encryptedBytes);
    }

    public static EncryptionVerification readFrom(PacketReader reader) throws IOException {
        return new EncryptionVerification(reader.readBytes());
    }

    /**
     * @return Whether {@param encryption} is able to decrypt this remote encryption.
     */
    public boolean matchesEncryption(Encryption encryption) {
        try {
            byte[] decrypted = encryption.decrypt(encryptedBytes);
            return Arrays.equals(decrypted, VERIFICATION_BYTES);
        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass()))
            return false;

        EncryptionVerification other = (EncryptionVerification) obj;
        return Arrays.equals(encryptedBytes, other.encryptedBytes);
    }

    /**
     * @return An EncryptionVerification that can be used to verify {@param encryption}.
     */
    public static EncryptionVerification create(Encryption encryption) {
        if (encryption == null)
            return UNENCRYPTED;

        byte[] encryptedBytes = encryption.encrypt(VERIFICATION_BYTES);
        return new EncryptionVerification(encryptedBytes);
    }


}
