package net.sothatsit.audiostream.communication.io;

import net.sothatsit.audiostream.util.Exceptions;
import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.util.ServiceState;
import net.sothatsit.property.Property;

import java.io.IOException;
import java.net.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A class to be used to create a TCP server to interact with many clients.
 *
 * @author Paddy Lamont
 */
public class TCPServer implements AutoCloseable {

    private final String name;
    private final int port;
    private final LoopedThread connectThread;
    private final List<Consumer<DatagramPacket>> listeners;

    private final List<TCPConnection> connections;
    private ServerSocketChannel socket;

    public TCPServer(String name, int port) {
        this.name = name;
        this.port = port;
        this.connectThread = new LoopedThread(name + "-connectThread", this::receiveConnection);
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

        socket = ServerSocketChannel.open();
        socket.bind(new InetSocketAddress(port));
        connectThread.start();
    }

    @Override
    public synchronized void close() throws IOException {
        if (socket == null)
            throw new IllegalStateException("This server has is stopped");

        List<AutoCloseable> toClose = new ArrayList<>();

        toClose.add(connectThread);
        toClose.addAll(connections);
        toClose.add(socket);

        Exceptions.closeManyIO(toClose);

        connections.clear();
        socket = null;
    }

    private void addConnection(TCPConnection connection) {
        // TODO : Also listen for the closing of the connection, and when its closed remove it as a connection.
        //        (Currently it waits for a send to the connection to fail before removing it.)
        connection.addListener(this::propagatePacket);

        synchronized (this) {
            connections.add(connection);
        }
    }

    private void closeConnection(TCPConnection connection) {
        try {
            connection.close();
        } catch (IOException exception) {
            new RuntimeException(
                    "Exception closing connection to " + connection.getRemoteAddress(),
                    exception
            ).printStackTrace();
        } finally {
            connections.remove(connection);
        }
    }

    private void receiveConnection() {
        TCPConnection connection;
        try {
            SocketChannel newSocket = socket.accept();
            connection = new TCPConnection(name, newSocket);
        } catch (ClosedByInterruptException exception) {
            // Interrupts are used to stop the multicast receiver thread
            return;
        } catch (IOException exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error receiving new TCP connection, " + exceptionString);
            return;
        }

        addConnection(connection);
    }

    private TCPConnection openNewConnection(InetSocketAddress address) throws IOException {
        SocketChannel socket = SocketChannel.open(address);

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

        if (caughtException != null)
            throw caughtException;
    }

    public void send(byte[] bytes, InetSocketAddress address) throws IOException {
        if (bytes == null)
            throw new IllegalArgumentException("bytes cannot be null");
        if (address == null)
            throw new IllegalArgumentException("address cannot be null");

        TCPConnection connection = getOrOpenConnection(address);

        try {
            connection.send(bytes);
        } catch (IOException exception) {
            closeConnection(connection);
            throw exception;
        }
    }
}
