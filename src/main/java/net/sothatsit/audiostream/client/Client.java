package net.sothatsit.audiostream.client;

import net.sothatsit.audiostream.util.StreamMonitor;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.Socket;

/**
 * The client receiving the audio.
 *
 * @author Paddy Lamont
 */
public class Client extends Thread {

    private static final int RECONNECT_MILLIS = 500;

    private final ClientSettings settings;

    private boolean running;
    private boolean connected;
    private String status;
    private Exception threadException;

    public Client(ClientSettings settings) {
        super("Client");

        this.settings = settings;

        this.running = false;
        this.connected = false;
        this.status = "";
        this.threadException = null;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    private synchronized void setRunning(boolean running) {
        this.running = running;
    }

    public synchronized boolean isConnected() {
        return connected;
    }

    private synchronized void setConnected(boolean connected) {
        this.connected = connected;
    }

    public synchronized String getStatus() {
        return status;
    }

    private synchronized void setStatus(String status) {
        this.status = status;
    }

    public synchronized Exception takeThreadException() {
        Exception exception = threadException;
        threadException = null;
        return exception;
    }

    private synchronized void setThreadException(Exception threadException) {
        this.threadException = threadException;
    }

    public synchronized void stopGracefully() throws InterruptedException {
        if (!isAlive())
            return;

        running = false;
        connected = false;

        if (Thread.currentThread() != this) {
            wait();
        }
    }

    @Override
    public void run() {
        try {
            setRunning(true);
            runClient();
        } catch (Exception exception) {
            setThreadException(exception);
        } finally {
            setRunning(false);
            setConnected(false);
        }
    }

    public void runClient() throws IOException, LineUnavailableException, InterruptedException {
        SourceDataLine outLine = settings.getSourceDataLine();
        outLine.open(settings.format, settings.bufferSize);
        outLine.start();

        int frameSize = settings.format.getFrameSize();

        while (isRunning()) {
            Socket socket = null;
            try {
                // Connect to the socket and play all received audio
                socket = settings.connectToSocket();
                InputStream stream = socket.getInputStream();
                setConnected(true);

                byte[] buffer = new byte[settings.bufferSize];
                int totalBytes = 0;

                StreamMonitor monitor = settings.createStreamMonitor();
                while (isRunning()) {
                    int read = stream.read(buffer, totalBytes, buffer.length - totalBytes);
                    if (read < 0)
                        break;

                    totalBytes += read;
                    int totalFrames = totalBytes / frameSize;

                    if (totalFrames == 0)
                        continue;

                    int frameAlignedBytes = totalFrames * frameSize;

                    outLine.write(buffer, 0, frameAlignedBytes);
                    setStatus(monitor.update(buffer, 0, frameAlignedBytes));

                    int leftOver = totalBytes - frameAlignedBytes;
                    if (leftOver > 0) {
                        System.arraycopy(buffer, frameAlignedBytes, buffer, 0, leftOver);
                    }
                    totalBytes = leftOver;
                }
            } catch (ConnectException exception) {
                // If there was an error connecting to the server, sleep and try again
                System.err.println("Couldn't connect to " + settings.getAddress() + ", retrying...");
                setConnected(false);
                Thread.sleep(RECONNECT_MILLIS);
            } finally {
                // Close the socket if it is open
                if (socket != null && socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException exception) {
                        new RuntimeException("Error closing socket", exception).printStackTrace();
                    }
                }
                setConnected(false);
            }
        }
    }
}
