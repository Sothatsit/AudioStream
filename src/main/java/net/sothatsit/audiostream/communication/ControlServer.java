package net.sothatsit.audiostream.communication;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.communication.io.Multicast;
import net.sothatsit.audiostream.communication.io.TCPServer;
import net.sothatsit.audiostream.communication.packet.PacketBuilder;
import net.sothatsit.audiostream.communication.packet.PacketReader;
import net.sothatsit.audiostream.communication.packet.PacketType;
import net.sothatsit.audiostream.model.RemoteServerDetails;
import net.sothatsit.audiostream.util.Exceptions;
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

    private static final boolean DEBUG_REPORT_PACKETS = false;
    private static final int BROADCAST_INTERVAL_MS = 3 * 1000;

    private final Multicast multicast;
    private final TCPServer server;
    private final LoopedThread broadcastThread;
    private final Property<RemoteServerDetails> serverDetails;
    private final List<Consumer<RemoteServerDetails>> discoveryListeners;

    public ControlServer(int serverPort, Property<RemoteServerDetails> serverDetails) {
        this.multicast = new Multicast("controlMulticast", AudioStream.MULTICAST_SOCKET_ADDRESS);
        this.server = new TCPServer("controlServer", serverPort);
        this.broadcastThread = new LoopedThread(
                "controlBroadcastThread", this::broadcastDetailsRequest, BROADCAST_INTERVAL_MS
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
        Exceptions.closeManyIO(multicast, server, broadcastThread);
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
            if (DEBUG_REPORT_PACKETS) {
                System.err.println("ControlServer: broadcastDetailsRequest " + serverDetails.get());
            }

            multicast.broadcast(createRequestPacket());
        } catch (IOException exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error broadcasting server details request, " + exceptionString);
        }
    }

    public void broadcastDetails() {
        try {
            if (DEBUG_REPORT_PACKETS) {
                System.err.println("ControlServer: broadcastDetails");
            }

            multicast.broadcast(createResponsePacket());
        } catch (IOException exception) {
            exception.printStackTrace();
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error broadcasting server details, " + exceptionString);
        }
    }

    public void sendDetailsRequest(InetSocketAddress address) {
        try {
            if (DEBUG_REPORT_PACKETS) {
                System.err.println("ControlServer: sendDetailsRequest to " + address);
            }

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

            if (DEBUG_REPORT_PACKETS) {
                System.err.println("ControlServer: receivePacket " + type + " from "
                        + packet.getSocketAddress() + " of size " + packet.getLength() + " bytes");
            }
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

        InetSocketAddress socketAddress = new InetSocketAddress(remoteAddress, port);

        try {
            server.send(createResponsePacket(), socketAddress);
        } catch (IOException exception) {
            String exceptionString = exception.getClass() + ": " + exception.getMessage();
            System.err.println("Error sending discovery response packet to " + socketAddress + ", " + exceptionString);
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
                System.err.println(
                        "Error passing discovered server details to listener " + listener.getClass()
                        + ", " + exception.getClass() + ": " + exception.getMessage()
                );
            }
        }
    }
}
