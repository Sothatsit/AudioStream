package net.sothatsit.audiostream.communication.io;

import net.sothatsit.audiostream.communication.packet.PacketChannel;
import net.sothatsit.audiostream.util.Exceptions;
import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.util.ServiceState;
import net.sothatsit.property.Property;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A connection with another TCP server.
 *
 * @author Paddy Lamont
 */
public class TCPConnection implements AutoCloseable {

    private final SocketChannel socket;
    private final PacketChannel channel;
    private final LoopedThread receiverThread;
    private final List<Consumer<DatagramPacket>> listeners;

    private boolean running = true;

    public TCPConnection(String name, SocketChannel socket) throws IOException {
        this.socket = socket;
        this.channel = new PacketChannel(socket);
        this.receiverThread = new LoopedThread(name + "-clientReceiverThread", this::processNext);
        this.listeners = new CopyOnWriteArrayList<>();

        receiverThread.start();
    }

    public SocketAddress getRemoteAddress() {
        try {
            return socket.getRemoteAddress();
        } catch (IOException exception) {
            throw new RuntimeException("Well, this is unexpected", exception);
        }
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

    @Override
    public synchronized void close() throws IOException {
        if (!running)
            throw new IllegalStateException("Connection already stopped");

        Exceptions.closeManyIO(receiverThread, socket);

        running = false;
    }

    public void send(byte[] bytes) throws IOException {
        channel.writePacket(bytes);
    }

    private void processNext() {
        DatagramPacket packet;
        try {
            byte[] data = channel.readPacket();
            packet = new DatagramPacket(data, 0, data.length, socket.getRemoteAddress());
        } catch (ClosedByInterruptException exception) {
            // Interrupts are used to stop this thread
            // TODO : If this happens in the middle of the reading of a packet,
            //        will part of the packet be lost? If it is, then this would
            //        cause potentially all future read packets to be nonsense.
            return;
        } catch (UnexpectedStreamEndException exception) {
            // If the other end of the connection was closed, this will be thrown
            // TODO : This should update the state of this connection
            receiverThread.stopNextLoop();
            return;
        } catch (Exception exception) {
            throw new IllegalStateException("Exception receiving TCP packet", exception);
        }

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
}
