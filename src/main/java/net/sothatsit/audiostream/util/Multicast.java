package net.sothatsit.audiostream.util;

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

    public Multicast(InetAddress address, int port) {
        this.address = address;
        this.port = port;

        this.receiverThread = new LoopedThread("Receive-multicast-packets-thread", () -> {
            try {
                receievePacket();
            } catch (Exception e) {
                throw new RuntimeException("Exception while reading packet", e);
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

    public void send(byte[] bytes) throws IOException {
        send(bytes, address);
    }

    public void send(byte[] bytes, InetAddress address) throws IOException {
        if (publisherSocket == null)
            throw new IllegalStateException("Multicast is not running");
        if (bytes == null)
            throw new IllegalArgumentException("bytes cannot be null");

        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);

        publisherSocket.send(packet);
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

    private void receievePacket() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        receiverSocket.receive(packet);

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

    public static boolean isAddressLocalhost(InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;

        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
    }
}
