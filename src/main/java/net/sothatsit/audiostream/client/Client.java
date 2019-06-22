package net.sothatsit.audiostream.client;

import net.sothatsit.audiostream.encryption.Encryption;
import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.StreamMonitor;
import net.sothatsit.property.Attribute;
import net.sothatsit.property.Property;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The client receiving the audio.
 *
 * @author Paddy Lamont
 */
public class Client {

    private static final int RECONNECT_MILLIS = 500;

    private final RemoteAudioServer audioServer;
    private final ClientSettings settings;
    private final LoopedThread thread;
    private final Attribute<Encryption> encryption;

    private final Property<ClientStatus> status;

    public Client(RemoteAudioServer audioServer, ClientSettings settings) {
        this.audioServer = audioServer;
        this.settings = settings;
        this.thread = new LoopedThread("Client(" + audioServer + ")", enabled -> {
            try {
                runClient(enabled);
            } catch (IOException | LineUnavailableException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, RECONNECT_MILLIS);
        this.encryption = Attribute.createNullable("encryption", null);

        this.status = Property.createNonNull("status", new ClientStatus(false, "Disconnected"));
    }

    public RemoteAudioServer getServer() {
        return audioServer;
    }

    public ClientSettings getSettings() {
        return settings;
    }

    public ClientState getState() {
        LoopedThread.ThreadState threadState = thread.getState();
        switch (threadState) {
            case STOPPED:
                return ClientState.STOPPED;

            case RUNNING: {
                ClientStatus status = this.status.get();
                if (status.connected) {
                    return ClientState.CONNECTED;
                } else if (status.isErrored()) {
                    return ClientState.ERRORED;
                } else {
                    return ClientState.CONNECTING;
                }
            }

            case ERRORED:
                return ClientState.ERRORED;

            default:
                throw new IllegalStateException("Unknown ThreadState " + threadState);
        }
    }

    public Property<ClientStatus> getStatus() {
        return status.readOnly();
    }

    public Property<Encryption> getEncryption() {
        return encryption.readOnly();
    }

    public void setEncryption(Property<Encryption> encryption) {
        this.encryption.set(encryption);
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        thread.stopGracefully();
    }

    private void runClient(AtomicBoolean enabled) throws IOException, LineUnavailableException, InterruptedException {
        SourceDataLine outLine = settings.getSourceDataLine();
        try {
            outLine.open(settings.format, settings.bufferSize);
            outLine.start();

            int frameSize = settings.format.getFrameSize();

            Socket socket = null;
            String exitStatus = "Disconnected";
            Exception connectionException = null;
            try {
                // Connect to the socket and play all received audio
                socket = audioServer.connectToSocket();
                InputStream stream = socket.getInputStream();

                status.set(new ClientStatus(true, "Connected"));

                byte[] buffer = new byte[settings.bufferSize];
                int totalBytes = 0;

                StreamMonitor monitor = settings.createStreamMonitor();
                while (enabled.get()) {
                    int read = stream.read(buffer, totalBytes, buffer.length - totalBytes);
                    if (read < 0)
                        break;

                    totalBytes += read;
                    int totalFrames = totalBytes / frameSize;
                    if (totalFrames == 0)
                        continue;

                    int frameAlignedBytes = totalFrames * frameSize;

                    outLine.write(buffer, 0, frameAlignedBytes);
                    status.set(new ClientStatus(true, monitor.update(buffer, 0, frameAlignedBytes)));

                    int leftOver = totalBytes - frameAlignedBytes;
                    if (leftOver > 0) {
                        System.arraycopy(buffer, frameAlignedBytes, buffer, 0, leftOver);
                    }
                    totalBytes = leftOver;
                }
            } catch (ConnectException e) {
                connectionException = e;
                exitStatus = "Unable to connect: " + e.getMessage();
            } catch (RuntimeException e) {
                connectionException = e;
                exitStatus = "There was an error: " + e.getMessage();
                throw e;
            } finally {
                status.set(new ClientStatus(false, exitStatus, connectionException));

                // Close the socket if it is open
                if (socket != null && socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        new RuntimeException("Error closing socket", e).printStackTrace();
                    }
                }
            }
        } finally {
            outLine.stop();
        }
    }
}
