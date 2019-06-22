package net.sothatsit.audiostream.server;

import net.sothatsit.audiostream.encryption.Encryption;
import net.sothatsit.property.Attribute;
import net.sothatsit.property.gui.GuiUtils;
import net.sothatsit.property.Property;
import net.sothatsit.audiostream.util.VariableBuffer;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * The server transmitting the audio.
 *
 * @author Paddy Lamont
 */
public class Server extends Thread {

    private final ServerSettings settings;
    private final Attribute<Encryption> encryption;
    private final Property<Boolean> running;
    private final Property<Boolean> started;

    private List<Socket> connected;
    private ServerSocket serverSocket;
    private Exception threadException;

    public Server(ServerSettings settings) {
        super("Server");

        this.settings = settings;
        this.encryption = Attribute.createNullable("encryption", null);
        this.running = Property.create("running");
        this.started = Property.create("started");

        this.connected = new ArrayList<>();
        this.serverSocket = null;
    }

    public ServerSettings getSettings() {
        return settings;
    }

    public boolean isRunning() {
        return running.get();
    }

    public Property<Boolean> getIsRunning() {
        return running.readOnly();
    }

    public boolean hasStarted() {
        return started.get();
    }

    public Property<Boolean> getHasStarted() {
        return started.readOnly();
    }

    public synchronized Socket[] getConnectedSockets() {
        return connected.toArray(new Socket[0]);
    }

    public Property<Encryption> getEncryption() {
        return encryption.readOnly();
    }

    public void setEncryption(Property<Encryption> encryption) {
        this.encryption.set(encryption);
    }

    // TODO: Also save Thread with socket
    private synchronized void addConnectedSocket(Socket socket) {
        connected.add(socket);
    }

    private synchronized void removeConnectedSocket(Socket socket) {
        connected.remove(socket);
    }

    public synchronized Exception takeThreadException() {
        Exception exception = threadException;
        threadException = null;
        return exception;
    }

    private synchronized void setThreadException(Exception threadException) {
        this.threadException = threadException;
    }

    public synchronized void stopGracefully() throws InterruptedException, IOException {
        running.set(false);

        if (!serverSocket.isClosed()) {
            serverSocket.close();
        }

        for (Socket connection : connected) {
            connection.close();
        }

        started.set(false);

        if (!isAlive())
            throw new IllegalStateException("Thread is not running");

        if (Thread.currentThread() != this) {
            wait();
        }
    }

    @Override
    public void run() {
        try {
            running.set(true);
            runServer();
        } catch (Exception exception) {
            setThreadException(exception);
        } finally {
            try {
                stopGracefully();
            } catch (InterruptedException | IOException exception) {
                GuiUtils.reportErrorFatal(exception);
            }
        }
    }

    public void runServer() throws IOException, LineUnavailableException {
        serverSocket = new ServerSocket(settings.port);

        int bufferSizeSamples = settings.bufferSize / (settings.format.getSampleSizeInBits() / 8);
        final AudioReader reader = new AudioReader(settings.mixer, settings.format, bufferSizeSamples);
        try {
            reader.start();

            while (running.get()) {
                final Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (SocketException exception) {
                    // TODO : This feels like a hack... there should be a way to
                    //        not have to hit this error when closing the server
                    if ("Socket closed".equals(exception.getMessage()))
                        break;

                    throw exception;
                }

                if (socket == null)
                    continue;

                Thread thread = new Thread("Socket " + socket.getInetAddress()) {
                    @Override
                    public void run() {
                        try {
                            addConnectedSocket(socket);
                            streamToClient(reader, socket);
                        } catch (Exception exception) {
                            GuiUtils.reportErrorFatal(exception);
                        } finally {
                            removeConnectedSocket(socket);

                            if (!socket.isClosed()) {
                                try {
                                    socket.close();
                                } catch (IOException exception) {
                                    GuiUtils.reportError(exception);
                                }
                            }
                        }
                    }
                };
                thread.start();
            }
        } finally {
            reader.stop();
        }
    }

    public void streamToClient(AudioReader reader, Socket socket) throws IOException {
        OutputStream outStream;
        VariableBuffer inBuffer = null;
        try {
            outStream = socket.getOutputStream();
            inBuffer = new VariableBuffer(settings.bufferSize);
            reader.addOutBuffer(inBuffer);

            byte[] buffer = new byte[settings.bufferSize];
            while (running.get()) {
                inBuffer.pop(buffer, 0, buffer.length);
                outStream.write(buffer, 0, buffer.length);
            }
        } catch (SocketException exception) {
            new RuntimeException("There was an error streaming to client", exception).printStackTrace();
        } finally {
            if (inBuffer != null) {
                reader.removeOutBuffer(inBuffer);
            }
        }
    }
}
