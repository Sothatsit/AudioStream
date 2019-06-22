package net.sothatsit.audiostream.packet;

import net.sothatsit.audiostream.encryption.Encryption;
import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.property.Attribute;
import net.sothatsit.property.Property;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

/**
 * A class to be used to publish and receive signals across local networks.
 *
 * @author Paddy Lamont
 */
public class Multicast {

    private final InetAddress address;
    private final int port;

    private final LoopedThread receiverThread;
    private final List<Consumer<DatagramPacket>> listeners;

    private MulticastSocket receiverSocket;
    private DatagramSocket publisherSocket;

    private final Attribute<Encryption> encryption;

    public Multicast(InetAddress address, int port) {
        this.address = address;
        this.port = port;

        this.encryption = Attribute.createNullable("encryption", null);
        this.receiverThread = new LoopedThread("receive-multicast-packets-thread", () -> {
            try {
                receivePacket();
            } catch (Exception ignored) {
                // If another client uses different encryption or another program
                // is using the same port we could hit an exception here.
            }
        }, 0);
        this.listeners = new ArrayList<>();
    }

    public void addListener(Consumer<DatagramPacket> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<DatagramPacket> listener) {
        listeners.remove(listener);
    }

    public void setEncryption(Encryption encryption) {
        this.encryption.set(encryption);
    }

    public void setEncryption(Property<Encryption> encryption) {
        this.encryption.set(encryption);
    }

    private DatagramPacket encrypt(DatagramPacket packet) {
        Encryption encryption = this.encryption.get();
        if (encryption == null)
            return packet;

        return encryption.encrypt(packet);
    }

    private DatagramPacket decrypt(DatagramPacket packet) {
        Encryption encryption = this.encryption.get();
        if (encryption == null)
            return packet;

        return encryption.decrypt(packet);
    }

    public void send(byte[] bytes) throws IOException {
        send(bytes, address);
    }

    public void send(byte[] bytes, InetAddress address) throws IOException {
        if (publisherSocket == null)
            throw new IllegalStateException("Multicast is not running");
        if (bytes == null)
            throw new IllegalArgumentException("bytes cannot be null");

        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
        publisherSocket.send(encrypt(packet));
    }

    public void open() throws IOException {
        if (receiverSocket != null || publisherSocket != null)
            throw new IllegalStateException("Multicast already started");

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

        receiverSocket = new MulticastSocket(port);
        publisherSocket = new DatagramSocket();

        receiverSocket.setNetworkInterface(networkInterface);
        receiverSocket.joinGroup(address);

        receiverThread.start();
    }

    private void receivePacket() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        receiverSocket.receive(packet);
        packet = decrypt(packet);

        for (Consumer<DatagramPacket> listener : listeners) {
            listener.accept(packet);
        }
    }

    public void close() throws IOException {
        if (receiverSocket == null || publisherSocket == null)
            throw new IllegalStateException("Multicast has already been stopped");

        receiverThread.stopGracefully();
        receiverSocket.close();
        publisherSocket.close();

        receiverSocket = null;
        publisherSocket = null;
    }
}
