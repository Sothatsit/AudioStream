package net.sothatsit.audiostream.client;

import net.sothatsit.audiostream.RemoteAudioServer;
import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.util.StreamMonitor;

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

    private boolean connected;
    private String status;

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

        this.connected = false;
        this.status = "Disconnected";
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

            case RUNNING:
                if (connected) {
                    return ClientState.CONNECTED;
                } else {
                    return ClientState.CONNECTING;
                }

            case ERRORED:
                return ClientState.ERRORED;

            default:
                throw new IllegalStateException("Unknown ThreadState " + threadState);
        }
    }

    public synchronized String getStatus() {
        return status;
    }

    public Exception getException() {
        return thread.getException();
    }

    private synchronized void setStatus(boolean connected, String status) {
        this.connected = connected;
        this.status = status;
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
            try {
                // Connect to the socket and play all received audio
                socket = audioServer.connectToSocket();
                InputStream stream = socket.getInputStream();

                setStatus(true, "Connected");

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
                    setStatus(true, monitor.update(buffer, 0, frameAlignedBytes));

                    int leftOver = totalBytes - frameAlignedBytes;
                    if (leftOver > 0) {
                        System.arraycopy(buffer, frameAlignedBytes, buffer, 0, leftOver);
                    }
                    totalBytes = leftOver;
                }
            } catch (ConnectException exception) {
                System.err.println("Couldn't connect to " + audioServer.getAddressString());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                setStatus(false, "Disconnected");

                // Close the socket if it is open
                if (socket != null && socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException exception) {
                        new RuntimeException("Error closing socket", exception).printStackTrace();
                    }
                }
            }
        } finally {
            outLine.stop();
        }
    }
}
