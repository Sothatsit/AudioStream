package net.sothatsit.audiostream.communication.io;

import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.util.ServiceState;
import net.sothatsit.property.Property;

import java.io.IOException;
import java.net.*;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A class to be used to publish and receive multicast signals across local networks.
 *
 * @author Paddy Lamont
 */
public class Multicast implements AutoCloseable {

    private static final int MAX_PACKET_SIZE = 10 * 1024;

    private final LoopedThread receiverThread;
    private final List<Consumer<DatagramPacket>> listeners;

    private final InetSocketAddress address;
    private DatagramChannel receiverSocket;
    private DatagramChannel publisherSocket;

    public Multicast(String name, InetSocketAddress address) {
        this.address = address;

        this.receiverThread = new LoopedThread(name + "-receiverThread", this::processNext);
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
        if (receiverSocket != null)
            throw new IllegalStateException("Multicast already started");

        // We must find a network interface that supports multicast
        NetworkInterface networkInterface = null;
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

        interfacesLoop: while (networkInterfaces.hasMoreElements()) {
            NetworkInterface network = networkInterfaces.nextElement();

            if (!network.supportsMulticast() || network.isPointToPoint())
                continue;

            Enumeration<InetAddress> addresses = network.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress i = addresses.nextElement();
                if (i instanceof Inet4Address) {
                    networkInterface = network;
                    break interfacesLoop;
                }
            }
        }

        if (networkInterface == null)
            throw new IllegalStateException("Could not find valid network interface");

        receiverSocket = DatagramChannel.open(StandardProtocolFamily.INET);
        publisherSocket = DatagramChannel.open(StandardProtocolFamily.INET);

        receiverSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        receiverSocket.bind(address);
        receiverSocket.join(address.getAddress(), networkInterface);

        receiverThread.start();
    }

    @Override
    public void close() throws IOException {
        if (receiverSocket == null)
            throw new IllegalStateException("Multicast has already been stopped");

        receiverThread.stop();

        try {
            receiverSocket.close();
            publisherSocket.close();
        } finally {
            receiverSocket = null;
            publisherSocket = null;
        }
    }

    public void broadcast(byte[] bytes) throws IOException {
        if (receiverSocket == null)
            throw new IllegalStateException("Multicast is not running");
        if (bytes == null)
            throw new IllegalArgumentException("bytes cannot be null");
        if (bytes.length >= MAX_PACKET_SIZE)
            throw new IllegalArgumentException("bytes exceeds maximum packet size, " + MAX_PACKET_SIZE);

        publisherSocket.send(ByteBuffer.wrap(bytes), address);
    }

    private DatagramPacket receivePacket() throws IOException {
        byte[] array = new byte[MAX_PACKET_SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(array);
        SocketAddress address = receiverSocket.receive(buffer);

        DatagramPacket packet = new DatagramPacket(array, 0, buffer.position(), address);

        // Reject packets of the maximum size under
        // the assumption that they were truncated
        if (packet.getLength() >= MAX_PACKET_SIZE) {
            throw new IOException(
                    "Packet length " + packet.getLength() + " exceeds maximum packet size " + MAX_PACKET_SIZE,
                    new BufferOverflowException()
            );
        }

        return packet;
    }

    private void processNext() {
        DatagramPacket packet;
        try {
            packet = receivePacket();
        } catch (ClosedByInterruptException exception) {
            // Interrupts are used to stop the multicast receiver thread
            return;
        } catch (Exception exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error receiving multicast, " + exceptionString);
            return;
        }

        for (Consumer<DatagramPacket> listener : listeners) {
            try {
                listener.accept(packet);
            } catch (Exception exception) {
                String listenerString = listener.getClass().toString();
                String exceptionString = exception.getClass() + ": " + exception.getMessage();
                System.err.println("Error passing multicast to listener " + listenerString + ", " + exceptionString);
                exception.printStackTrace();
            }
        }
    }
}
