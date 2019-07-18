package net.sothatsit.audiostream.model;

import net.sothatsit.audiostream.communication.ControlServer;
import net.sothatsit.audiostream.communication.RemoteServerIndex;
import net.sothatsit.audiostream.communication.audio.AudioClientManager;
import net.sothatsit.audiostream.communication.audio.AudioClientSettings;
import net.sothatsit.audiostream.communication.audio.AudioServer;
import net.sothatsit.audiostream.communication.audio.AudioServerSettings;
import net.sothatsit.audiostream.encryption.Encryption;
import net.sothatsit.audiostream.encryption.EncryptionSettings;
import net.sothatsit.audiostream.encryption.EncryptionVerification;
import net.sothatsit.property.Attribute;
import net.sothatsit.property.Property;
import net.sothatsit.property.SelfMappedProperty;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * The state of AudioStream.
 *
 * @author Paddy Lamont
 */
public class AudioStreamModel {

    public final Property<Boolean> running;

    public final Attribute<EncryptionSettings> encryptionSettings;
    public final Property<Encryption> encryption;
    public final Property<EncryptionVerification> encryptionVerification;

    public final Attribute<AudioServerSettings> audioServerSettings;
    public final AudioServer audioServer;

    public final Attribute<Integer> controlServerPort;
    public final Property<RemoteAudioServerDetails> localAudioServerDetails;
    public final Property<RemoteServerDetails> localServerDetails;

    public final Property<ControlServer> controlServer;
    public final RemoteServerIndex remoteServerIndex;

    public final Attribute<AudioClientSettings> audioClientSettings;
    public final AudioClientManager audioClientManager;

    public AudioStreamModel() {
        this.running = Property.createNonNull("running", false);

        this.encryptionSettings = Attribute.createNonNull("encryptionSettings", EncryptionSettings.NO_ENCRYPTION);
        this.encryption = encryptionSettings.map("encryption", EncryptionSettings::createEncryption);
        this.encryptionVerification = encryption.map("encryptionVerification", EncryptionVerification::create);

        this.audioServerSettings = Attribute.createNullable("audioServerSettings");
        this.audioServer = new AudioServer(audioServerSettings);

        this.controlServerPort = Attribute.createNullable("controlServerPort");
        this.localAudioServerDetails = audioServerSettings.map("localAudioServerDetails", settings -> {
            if (settings == null)
                return null;
            InetSocketAddress address = new InetSocketAddress(settings.port);
           return new RemoteAudioServerDetails(address, settings.format);
        });
        this.localServerDetails = Property.map(
                "localServerDetails", controlServerPort, localAudioServerDetails, encryptionVerification,
                (port, audioServerDetails, verification) -> {
                    if (port == null)
                        return null;

                    InetSocketAddress controlAddress = new InetSocketAddress(port);
                    return new RemoteServerDetails(controlAddress, audioServerDetails, verification);
                }
        );

        this.controlServer = SelfMappedProperty.map(
                "controlServer", running, controlServerPort, this::updateControlServer
        );
        this.remoteServerIndex = new RemoteServerIndex(controlServer);

        this.audioClientSettings = Attribute.createNullable("audioClientSettings");
        this.audioClientManager = new AudioClientManager(audioClientSettings);
    }

    public synchronized void start() throws IOException {
        if (running.get())
            throw new IllegalStateException("Already started");

        running.set(true);
        remoteServerIndex.start();
    }

    public synchronized void stop() throws IOException {
        if (!running.get())
            throw new IllegalStateException("Not running");

        running.set(false);
        remoteServerIndex.stop();
    }

    private ControlServer updateControlServer(ControlServer previousServer, boolean running, Integer port) {
        // Close down the previous server
        if (previousServer != null) {
            try {
                previousServer.close();
            } catch (IOException exception) {
                String exceptionString = exception.getClass() + ": " + exception.getMessage();
                System.err.println("Error closing previous control server, " + exceptionString);
            }
        }

        // Check if we want to open a new server
        if (!running || port == null)
            return null;

        // Open a new server
        ControlServer newServer = new ControlServer(port, localServerDetails);
        try {
            newServer.open();
        } catch (IOException exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error opening control server, " + exceptionString);

            try {
                newServer.close();
            } catch (IOException ignored) {}

            return null;
        }

        return newServer;
    }
}
