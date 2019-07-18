package net.sothatsit.audiostream.communication.io;

import net.sothatsit.audiostream.communication.packet.PacketInputStream;
import net.sothatsit.audiostream.communication.packet.PacketOutputStream;
import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.util.ServiceState;
import net.sothatsit.property.Property;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A connection with another TCP server.
 *
 * @author Paddy Lamont
 */
public class TCPConnection {

    private final Socket socket;
    private final PacketInputStream inputStream;
    private final PacketOutputStream outputStream;
    private final LoopedThread receiverThread;
    private final List<Consumer<DatagramPacket>> listeners;

    private boolean running = true;

    public TCPConnection(String name, Socket socket) throws IOException {
        this.socket = socket;
        this.inputStream = new PacketInputStream(socket.getInputStream());
        this.outputStream = new PacketOutputStream(socket.getOutputStream());
        this.receiverThread = new LoopedThread(name + "-receiver-thread", this::processNext);
        this.listeners = new CopyOnWriteArrayList<>();

        receiverThread.start();
    }

    public SocketAddress getRemoteAddress() {
        return socket.getRemoteSocketAddress();
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

    public synchronized void close() throws IOException {
        if (!running)
            throw new IllegalStateException("Connection already stopped");

        receiverThread.stop();
        socket.close();

        running = false;
    }

    public void send(byte[] bytes) throws IOException {
        outputStream.writePacket(bytes);
    }

    private void processNext() {
        DatagramPacket packet;
        try {
            byte[] data = inputStream.readPacket();
            packet = new DatagramPacket(data, 0, data.length, socket.getRemoteSocketAddress());
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
