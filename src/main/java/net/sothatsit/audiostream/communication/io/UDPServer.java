package net.sothatsit.audiostream.communication.io;

import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.util.ServiceState;
import net.sothatsit.property.Property;

import java.io.IOException;
import java.net.*;
import java.nio.BufferOverflowException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A class to be used to create a UDP server to interact with many clients.
 *
 * @author Paddy Lamont
 */
public class UDPServer {

    private static final int MAX_PACKET_SIZE = 10 * 1024;

    private final LoopedThread receiverThread;
    private final List<Consumer<DatagramPacket>> listeners;

    private final InetSocketAddress address;
    private DatagramSocket socket;

    public UDPServer(String name, InetSocketAddress address) {
        this.address = address;

        this.receiverThread = new LoopedThread(name + "-receiver-thread", this::processNext);
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public InetSocketAddress getSocketAddress() {
        return address;
    }

    // TODO : Should manage its own state property which inherits errors from the thread's state.
    //        Then could make the starting and stopping states more accurate.
    public Property<ServiceState> getState() {
        return receiverThread.getState();
    }

    public void addListener(Consumer<DatagramPacket> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<DatagramPacket> listener) {
        listeners.remove(listener);
    }

    public void open() throws IOException {
        if (socket != null)
            throw new IllegalStateException("Service already started");

        socket = new DatagramSocket(address);
        receiverThread.start();
    }

    public void close() throws IOException {
        if (socket == null)
            throw new IllegalStateException("Service has already been stopped");

        receiverThread.stop();

        socket.close();
        socket = null;
    }

    public void send(byte[] bytes, InetSocketAddress address) throws IOException {
        if (socket == null)
            throw new IllegalStateException("Service is not running");
        if (bytes == null)
            throw new IllegalArgumentException("bytes cannot be null");
        if (bytes.length >= MAX_PACKET_SIZE)
            throw new IllegalArgumentException("bytes exceeds maximum packet size, " + MAX_PACKET_SIZE);

        socket.send(new DatagramPacket(bytes, bytes.length, address));
    }

    private DatagramPacket receivePacket() throws IOException {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        socket.receive(packet);

        // Reject packets of the maximum size under
        // the assumption that they were truncated
        if (packet.getLength() >= MAX_PACKET_SIZE)
            throw new BufferOverflowException();

        return packet;
    }

    private void processNext() {
        DatagramPacket packet;
        try {
            packet = receivePacket();
        } catch (Exception exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error receiving service packet, " + exceptionString);
            return;
        }

        for (Consumer<DatagramPacket> listener : listeners) {
            try {
                listener.accept(packet);
            } catch (Exception exception) {
                String listenerString = listener.getClass().toString();
                String exceptionString = exception.getClass() + ": " + exception.getMessage();
                System.err.println("Error passing multicast to listener " + listenerString + ", " + exceptionString);
            }
        }
    }
}
