package net.sothatsit.audiostream.server;

import net.sothatsit.audiostream.gui.GuiUtils;
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

    private boolean running;
    private boolean started;
    private List<Socket> connected;
    private ServerSocket serverSocket;
    private Exception threadException;

    public Server(ServerSettings settings) {
        super("Server");

        this.settings = settings;
        this.running = false;
        this.connected = new ArrayList<>();
        this.serverSocket = null;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    private synchronized void setRunning(boolean running) {
        this.running = running;
    }

    public synchronized boolean hasStarted() {
        return started;
    }

    private synchronized void setStarted(boolean started) {
        this.started = started;
    }

    public synchronized Socket[] getConnectedSockets() {
        return connected.toArray(new Socket[0]);
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
        running = false;

        if (!serverSocket.isClosed()) {
            serverSocket.close();
        }

        started = false;

        if (!isAlive())
            throw new IllegalStateException("Thread is not running");

        if (Thread.currentThread() != this) {
            wait();
        }
    }

    @Override
    public void run() {
        try {
            setRunning(true);
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

            while (isRunning()) {
                final Socket socket = serverSocket.accept();
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

            System.out.println("Writing audio to " + socket + "...");

            byte[] buffer = new byte[settings.bufferSize];
            while (isRunning()) {
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
