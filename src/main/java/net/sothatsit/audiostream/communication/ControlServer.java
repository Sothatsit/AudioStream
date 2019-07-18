package net.sothatsit.audiostream.communication;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.communication.io.Multicast;
import net.sothatsit.audiostream.communication.io.TCPServer;
import net.sothatsit.audiostream.communication.packet.PacketBuilder;
import net.sothatsit.audiostream.communication.packet.PacketReader;
import net.sothatsit.audiostream.communication.packet.PacketType;
import net.sothatsit.audiostream.model.RemoteServerDetails;
import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.property.Property;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A class that allows the discovery of remote AudioStream servers.
 *
 * @author Paddy Lamont
 */
public class ControlServer {

    private static final int BROADCAST_INTERVAL_MS = 3 * 1000;

    private final Multicast multicast;
    private final TCPServer server;
    private final LoopedThread broadcastThread;
    private final Property<RemoteServerDetails> serverDetails;
    private final List<Consumer<RemoteServerDetails>> discoveryListeners;

    public ControlServer(int serverPort,
                         Property<RemoteServerDetails> serverDetails) {

        this.multicast = new Multicast("discovery-multicast", AudioStream.MULTICAST_SOCKET_ADDRESS);
        this.server = new TCPServer("discovery-server", serverPort);
        this.broadcastThread = new LoopedThread(
                "discovery-broadcast", this::broadcastDetailsRequest, BROADCAST_INTERVAL_MS
        );
        this.serverDetails = serverDetails;
        this.discoveryListeners = new CopyOnWriteArrayList<>();

        multicast.addListener(this::receivePacket);
        server.addListener(this::receivePacket);

        serverDetails.addChangeListener(event -> broadcastDetails());
    }

    public void addDiscoveryListener(Consumer<RemoteServerDetails> listener) {
        discoveryListeners.add(listener);
    }

    public void removeDiscoveryListener(Consumer<RemoteServerDetails> listener) {
        discoveryListeners.remove(listener);
    }

    public void open() throws IOException {
        multicast.open();
        server.open();
        broadcastThread.start();
    }

    public void close() throws IOException {
        broadcastThread.stop();
        multicast.close();
        server.close();
    }

    private byte[] createRequestPacket() throws IOException {
        return PacketBuilder.create()
                            .writeType(PacketType.DISCOVERY_REQUEST)
                            .writeInt(server.getPort())
                            .build();
    }

    private byte[] createResponsePacket() throws IOException {
        PacketBuilder builder = PacketBuilder.create();

        builder.writeType(PacketType.DISCOVERY_RESPONSE);
        serverDetails.get().writeTo(builder);

        return builder.build();
    }

    public void broadcastDetailsRequest() {
        try {
            multicast.broadcast(createRequestPacket());
        } catch (IOException exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error broadcasting server details request, " + exceptionString);
        }
    }

    public void broadcastDetails() {
        try {
            multicast.broadcast(createResponsePacket());
        } catch (IOException exception) {
            exception.printStackTrace();
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error broadcasting server details, " + exceptionString);
        }
    }

    public void sendDetailsRequest(InetSocketAddress address) {
        try {
            server.send(createRequestPacket(), address);
        } catch (IOException exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error requesting server details, " + exceptionString);
        }
    }

    private void receivePacket(DatagramPacket packet) {
        PacketReader reader;
        PacketType type;
        try {
            reader = PacketReader.create(packet);
            type = reader.readType();
        } catch (IOException exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error reading discovery response packet, " + exceptionString);
            return;
        }

        switch (type) {
            case DISCOVERY_REQUEST:
                receiveDiscoveryRequest(reader, packet.getAddress());
                break;

            case DISCOVERY_RESPONSE:
                receiveDiscoveryResponse(reader, packet.getAddress());
                break;

            default:
                throw new IllegalArgumentException("Invalid packet type");
        }
    }

    private void receiveDiscoveryRequest(PacketReader reader, InetAddress remoteAddress) {
        int port;
        try {
            port = reader.readInt();
        } catch (IOException exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error reading discovery request packet, " + exceptionString);
            return;
        }

        InetSocketAddress address = new InetSocketAddress(remoteAddress, port);

        try {
            server.send(createResponsePacket(), address);
        } catch (IOException exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error sending discovery response packet, " + exceptionString);
            return;
        }
    }

    private void receiveDiscoveryResponse(PacketReader reader, InetAddress remoteAddress) {
        RemoteServerDetails remoteServerDetails;
        try {
            remoteServerDetails = RemoteServerDetails.readFrom(reader, remoteAddress);
        } catch (IOException exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error reading discovery response packet, " + exceptionString);
            return;
        }

        for (Consumer<RemoteServerDetails> listener : discoveryListeners) {
            try {
                listener.accept(remoteServerDetails);
            } catch (Exception exception) {
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append("Error passing discovered server details to listener ");
                errorMessage.append(listener.getClass());
                errorMessage.append(", ");
                errorMessage.append(exception.getClass()).append(": ").append(exception.getMessage());
                System.err.println(errorMessage);
            }
        }
    }
}
