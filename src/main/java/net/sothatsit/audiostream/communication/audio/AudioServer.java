package net.sothatsit.audiostream.communication.audio;

import net.sothatsit.audiostream.audio.AudioReader;
import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.util.ServiceState;
import net.sothatsit.property.awt.GuiUtils;
import net.sothatsit.property.Property;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The server transmitting the audio.
 *
 * @author Paddy Lamont
 */
public class AudioServer {

    private final Property<AudioServerSettings> settings;
    private final List<AudioServerConnection> connections;
    private final AtomicReference<Exception> threadException;
    private final LoopedThread thread;
    private final Property<Boolean> running;
    private ServerSocket serverSocket;

    public AudioServer(Property<AudioServerSettings> settings) {
        super();

        this.settings = settings;
        this.connections = new CopyOnWriteArrayList<>();
        this.threadException = new AtomicReference<>();
        this.thread = new LoopedThread("connectionAccepting", this::runServer);
        this.running = Property.createNonNull("running", false);
        this.serverSocket = null;

        // Interrupting the thread triggers restarting the server
        // TODO : This seems like it could possibly lead to unwanted restarts
        settings.addChangeListener(event -> thread.interruptIfRunning());
    }

    public Property<Boolean> isRunning() {
        return running.readOnly();
    }

    public boolean getIsRunning() {
        return running.get();
    }

    public int getConnectionCount() {
        return connections.size();
    }

    public List<AudioServerConnection> getConnections() {
        return new ArrayList<>(connections);
    }

    public Exception takeThreadException() {
        return threadException.getAndSet(null);
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        killServerSocket();
        thread.stop();
    }

    private Socket acceptSocket() throws IOException {
        try {
            return serverSocket.accept();
        } catch (SocketException exception) {
            if ("Socket closed".equals(exception.getMessage()))
                return null;

            throw exception;
        }
    }

    private void addConnection(AudioServerConnection connection) {
        connection.getState().addValueListener(state -> {
            if (state.getType() == ServiceState.Type.STOPPED) {
                connections.remove(connection);
            }
        });
        connection.start();
        connections.add(connection);
    }

    private void killServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException exception) {
            GuiUtils.reportError(exception);
        } finally {
            serverSocket = null;
        }
    }

    private synchronized void runServer(Property<Boolean> running) {
        AudioServerSettings settings = this.settings.get();
        AudioReader audioReader = null;
        try {
            serverSocket = new ServerSocket(settings.port);
            audioReader = new AudioReader(settings.mixer, settings.format, settings.bufferSize);
            audioReader.start();

            this.running.set(true);

            while (running.get() && !Thread.interrupted()) {
                Socket socket = acceptSocket();
                if (socket == null)
                    continue;

                addConnection(new AudioServerConnection(settings, audioReader, socket));
            }
        } catch (Exception exception) {
            threadException.set(exception);
        } finally {
            this.running.set(false);

            if (audioReader != null) {
                audioReader.stop();
            }

            connections.forEach(AudioServerConnection::stop);
            connections.clear();

            killServerSocket();
        }
    }
}
