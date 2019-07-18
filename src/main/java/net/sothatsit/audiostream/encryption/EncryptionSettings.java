package net.sothatsit.audiostream.encryption;

/**
 * Settings to be used to encrypt the traffic of AudioStream.
 *
 * @author Paddy Lamont
 */
public class EncryptionSettings {

    public static EncryptionSettings NO_ENCRYPTION = new EncryptionSettings(false, "");

    public final boolean doEncrypt;
    public final String secret;

    public EncryptionSettings(boolean doEncrypt, String secret) {
        this.doEncrypt = doEncrypt;
        this.secret = secret;
    }

    public Encryption createEncryption() {
        return (doEncrypt ? new Encryption(secret) : null);
    }
}
