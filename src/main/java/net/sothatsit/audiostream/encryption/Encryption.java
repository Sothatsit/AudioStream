package net.sothatsit.audiostream.encryption;

import com.google.crypto.tink.subtle.AesGcmJce;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.net.DatagramPacket;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Shared secret based encryption.
 *
 * Encryption:
 *  1. Generate random salt
 *  2. Generate key using salt and secret
 *  3. Encrypt message using generated key
 *  4. Transmit salt and encrypted message together
 *
 * Decryption:
 *  1. Receive salt and encrypted message
 *  2. Generate key using received salt and secret
 *  3. Decrypt message using generated key
 *
 * @author Paddy Lamont
 */
public class Encryption {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_LENGTH_BYTES = 128;

    private static final String KEY_DERIVATION_FUNCTION = "PBKDF2WithHmacSHA1";
    private static final int KEY_ITERATIONS = 1024;
    private static final int KEY_LENGTH_BYTES = 16;

    private final char[] secret;

    public Encryption(String secret) {
        this(secret.toCharArray());
    }

    public Encryption(char[] secret) {
        this.secret = secret;
    }

    /**
     * @return {@param message} encrypted using the secret.
     */
    public byte[] encrypt(byte[] message) {
        // Generate salt
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        RANDOM.nextBytes(salt);

        // Generate key
        AesGcmJce encryption;
        try {
            byte[] key = deriveKey(secret, salt);
            encryption = new AesGcmJce(key);
        } catch (Exception e) {
            throw new RuntimeException("Exception creating encryption key and AesGcmJce", e);
        }

        // Encrypt message, and return salt and encrypted concatenated together
        try {
            byte[] encryptedMessage = encryption.encrypt(message, salt);
            return concatenate(salt, encryptedMessage);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Unable to encrypt message", e);
        }
    }

    /**
     * @return A copy of {@param encryptedPacket} that has been encrypt using the secret.
     */
    public DatagramPacket encrypt(DatagramPacket packet) {
        int offset = packet.getOffset();
        int length = packet.getLength();
        byte[] message = Arrays.copyOfRange(packet.getData(), offset, offset + length);
        byte[] encrypted = encrypt(message);
        return new DatagramPacket(encrypted, 0, encrypted.length, packet.getSocketAddress());
    }

    /**
     * @return {@param encrypted} decrypted using the secret.
     */
    public byte[] decrypt(byte[] encrypted) {
        try {
            byte[] salt = Arrays.copyOfRange(encrypted, 0, SALT_LENGTH_BYTES);
            byte[] encryptedMessage = Arrays.copyOfRange(encrypted, SALT_LENGTH_BYTES, encrypted.length);

            byte[] key = deriveKey(secret, salt);
            AesGcmJce encryption = new AesGcmJce(key);

            return encryption.decrypt(encryptedMessage, salt);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Unable to decrypt message", e);
        }
    }

    /**
     * @return A copy of {@param encryptedPacket} that has been unencrypted using the secret.
     */
    public DatagramPacket decrypt(DatagramPacket packet) {
        int offset = packet.getOffset();
        int length = packet.getLength();
        byte[] encrypted = Arrays.copyOfRange(packet.getData(), offset, offset + length);
        byte[] message = decrypt(encrypted);
        return new DatagramPacket(message, 0, message.length, packet.getSocketAddress());
    }

    /**
     * @return A new byte array containing the contents of {@param one} followed by the contents of {@param two}.
     */
    private static byte[] concatenate(byte[] one, byte[] two) {
        byte[] res = new byte[one.length + two.length];
        System.arraycopy(one, 0, res, 0, one.length);
        System.arraycopy(two, 0, res, one.length, two.length);
        return res;
    }

    /**
     * @return A fixed length key to use for encryption that is derived from {@param secret} and {@param salt}.
     */
    private static byte[] deriveKey(char[] secret, byte[] salt) {
        try {
            SecretKeyFactory kf = SecretKeyFactory.getInstance(KEY_DERIVATION_FUNCTION);
            KeySpec specs = new PBEKeySpec(secret, salt, KEY_ITERATIONS, KEY_LENGTH_BYTES * 8);
            SecretKey key = kf.generateSecret(specs);
            return key.getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Exception deriving key from secret and salt", e);
        }
    }
}
