package net.sothatsit.audiostream.communication.io;

import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.util.ServiceState;
import net.sothatsit.property.Property;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A class to be used to create a TCP server to interact with many clients.
 *
 * @author Paddy Lamont
 */
public class TCPServer {

    private final String name;
    private final int port;
    private final LoopedThread connectThread;
    private final List<Consumer<DatagramPacket>> listeners;

    private final List<TCPConnection> connections;
    private ServerSocket socket;

    public TCPServer(String name, int port) {
        this.name = name;
        this.port = port;
        this.connectThread = new LoopedThread(name + "-connect-thread", this::receiveConnection);
        this.listeners = new CopyOnWriteArrayList<>();
        this.connections = new CopyOnWriteArrayList<>();
    }

    public int getPort() {
        return port;
    }

    // TODO : Should manage its own state property which inherits errors from the thread's state.
    //        Then could make the starting and stopping states more accurate.
    public Property<ServiceState> getState() {
        return connectThread.getState();
    }

    public void addListener(Consumer<DatagramPacket> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<DatagramPacket> listener) {
        listeners.remove(listener);
    }

    private void propagatePacket(DatagramPacket packet) {
        for (Consumer<DatagramPacket> listener : listeners) {
            try {
                listener.accept(packet);
            } catch (Exception exception) {
                String listenerString = listener.getClass().toString();
                String exceptionString = exception.getClass() + ": " + exception.getMessage();
                System.err.println("Error passing packet to listener " + listenerString + ", " + exceptionString);
            }
        }
    }

    public synchronized void open() throws IOException {
        if (socket != null)
            throw new IllegalStateException("This server has already started");

        socket = new ServerSocket(port);
        connectThread.start();
    }

    public synchronized void close() throws IOException {
        if (socket == null)
            throw new IllegalStateException("This server has is stopped");

        connectThread.stop();

        for (TCPConnection connection : connections) {
            try {
                connection.close();
            } catch (IOException exception) {
                String exceptionString = exception.getClass() + ": " + exception.getMessage();
                System.err.println("Error closing TCP connection, " + exceptionString);
            }
        }
        connections.clear();

        socket.close();
        socket = null;
    }

    private void addConnection(TCPConnection connection) {
        connection.addListener(this::propagatePacket);

        synchronized (this) {
            connections.add(connection);
        }
    }

    private void receiveConnection() {
        TCPConnection connection;
        try {
            Socket newSocket = socket.accept();
            connection = new TCPConnection(name, newSocket);
        } catch (IOException exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error receiving new TCP connection, " + exceptionString);
            return;
        }

        addConnection(connection);
    }

    private TCPConnection openNewConnection(InetSocketAddress address) throws IOException {
        Socket socket = new Socket(address.getAddress(), address.getPort());
        return new TCPConnection(name, socket);
    }

    public synchronized TCPConnection getConnection(InetSocketAddress address) {
        for (TCPConnection connection : connections) {
            if (address.equals(connection.getRemoteAddress()))
                return connection;
        }
        return null;
    }

    public TCPConnection getOrOpenConnection(InetSocketAddress address) throws IOException {
        TCPConnection connection = getConnection(address);
        if (connection != null)
            return connection;

        connection = openNewConnection(address);

        TCPConnection raceConnection;
        synchronized (this) {
            // May occur due to a race condition
            raceConnection = getConnection(address);
            if (raceConnection == null) {
                addConnection(connection);
                return connection;
            }
        }

        connection.close();
        return raceConnection;
    }

    public void sendToAll(byte[] bytes) throws IOException {
        IOException caughtException = null;
        for (TCPConnection connection : connections) {
            try {
                connection.send(bytes);
            } catch (IOException exception) {
                if (caughtException == null) {
                    caughtException = exception;
                } else {
                    caughtException.addSuppressed(exception);
                }
            }
        }

        if (caughtException != null) {
            throw caughtException;
        }
    }

    public void send(byte[] bytes, InetSocketAddress address) throws IOException {
        TCPConnection connection = getOrOpenConnection(address);

        connection.send(bytes);
    }
}
