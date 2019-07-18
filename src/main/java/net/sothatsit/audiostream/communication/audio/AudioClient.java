package net.sothatsit.audiostream.communication.audio;

import net.sothatsit.audiostream.audio.AudioWriter;
import net.sothatsit.audiostream.communication.RemoteServer;
import net.sothatsit.audiostream.encryption.Encryption;
import net.sothatsit.audiostream.communication.packet.PacketInputStream;
import net.sothatsit.audiostream.model.RemoteServerDetails;
import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.StreamMonitor;
import net.sothatsit.audiostream.util.ServiceState;
import net.sothatsit.property.Property;

import javax.sound.sampled.*;
import javax.swing.event.ChangeListener;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * The client receiving the audio.
 *
 * @author Paddy Lamont
 */
public class AudioClient {

    private static final int RECONNECT_MILLIS = 1000;

    private final RemoteServer audioServer;
    private final Property<RemoteServerDetails> serverDetails;
    private final Property<AudioClientSettings> settings;
    private final LoopedThread thread;

    private final ServiceState.StateProperty state;

    public AudioClient(RemoteServer audioServer, Property<AudioClientSettings> settings) {
        this.audioServer = audioServer;
        this.serverDetails = audioServer.getDetails();
        this.settings = settings;
        this.thread = new LoopedThread("" +
                "AudioClient(" + audioServer.getAddressString() + ")",
                this::run,
                RECONNECT_MILLIS
        );
        this.thread.setInterruptStrategy(LoopedThread.InterruptStrategy.SKIP_WAIT);

        this.state = new ServiceState.StateProperty("state");

        ChangeListener restartClient = event -> thread.interruptIfRunning();
        serverDetails.addChangeListener(restartClient);
        settings.addChangeListener(restartClient);
    }

    public RemoteServer getServer() {
        return audioServer;
    }

    public Property<RemoteServerDetails> getServerDetails() {
        return serverDetails;
    }

    public Property<AudioClientSettings> getSettings() {
        return settings;
    }

    public Property<ServiceState> getState() {
        return state.readOnly();
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        thread.stop();
    }

    private void run(Property<Boolean> enabled) {
        try {
            runClient(enabled);
        } catch (IOException | LineUnavailableException exception) {
            throw new RuntimeException(exception);
        } finally {
            state.setToStopped("Stopped");
        }
    }

    private void runClient(Property<Boolean> running) throws IOException, LineUnavailableException {
        RemoteServerDetails serverDetails = this.serverDetails.get();
        AudioClientSettings settings = this.settings.get();

        if (serverDetails == null) {
            state.setToStopped("Missing details about server", false);
            return;
        }

        if (serverDetails.audioServerDetails == null) {
            state.setToStopped("Missing audio details about server", false);
            return;
        }

        if (settings == null) {
            state.setToStopped("Missing settings for clients", false);
            return;
        }

        Encryption encryption = settings.encryption;

        // TODO : Being able to enter a password when connecting to a server instead of
        //        using a single application-wide encryption secret woud be nice.
        if (!serverDetails.encryptionVerification.isEncrypted) {
            encryption = null;
        } else if (!serverDetails.encryptionVerification.matchesEncryption(encryption)) {
            state.setToStopped("Mismatching encryption", false);
            return;
        }

        state.setToStarting("Establishing connections", false);

        Mixer.Info outputMixer = settings.mixer;
        AudioFormat audioFormat = serverDetails.audioServerDetails.format;
        int bufferBytes = settings.bufferBytes;
        InetSocketAddress address = serverDetails.audioServerDetails.address;

        StreamMonitor monitor = settings.createStreamMonitor(audioFormat);
        AudioWriter audioWriter = new AudioWriter(outputMixer, audioFormat, bufferBytes);

        Socket socket = null;
        String exitStatus = "Disconnected";
        Exception connectionException = null;

        try {
            // Start a writer to play the audio
            audioWriter.start();

            // Connect to the socket
            socket = new Socket(address.getAddress(), address.getPort());
            PacketInputStream stream = new PacketInputStream(socket.getInputStream());

            state.setToRunning("Connected");

            // Receive and play audio
            while (running.get() && !Thread.interrupted()) {
                byte[] packet = stream.readPacket();

                if (encryption != null) {
                    packet = encryption.decrypt(packet);
                }

                audioWriter.write(packet);
                state.setToRunning(monitor.update(packet, 0, packet.length));
            }
        } catch (ConnectException e) {
            connectionException = e;
            exitStatus = "Unable to connect: " + e.getMessage();
        } catch (RuntimeException e) {
            connectionException = e;
            exitStatus = "There was an error: " + e.getMessage();
            throw e;
        } finally {
            state.setToStopping(exitStatus, false, connectionException);

            audioWriter.stop();

            if (socket != null && socket.isConnected()) {
                try {
                    socket.close();
                } catch (IOException exception) {
                    new RuntimeException("Error closing socket", exception).printStackTrace();
                }
            }
        }
    }
}
