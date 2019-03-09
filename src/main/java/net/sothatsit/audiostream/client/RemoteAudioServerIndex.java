package net.sothatsit.audiostream.client;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.packet.PacketBuilder;
import net.sothatsit.audiostream.packet.PacketReader;
import net.sothatsit.audiostream.packet.PacketType;
import net.sothatsit.audiostream.server.ServerSettings;
import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.packet.Multicast;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.*;

// TODO : Tagging system
//
// Allow clients and servers to set a tag (or password?) where
// clients can automatically connect to servers with the same
// tag. This would allow AudioStream to automatically connect
// to all audio broadcasters that have the correct tag.
//
// For security, the tag could be used to encrypt itself, the
// server's address and its port so that bad actors cannot
// easily find and use the tag to play audio on systems
// without authorisation, where the authorisation is
// simply knowing the correct tag to use.
//
// This would involve decrypting the block of encrypted information
// using the client's requested tag, and checking that the decrypted
// tag and address matches the client's tag and the packet's address.
// If they match, you know the encrypted block was encrypted using the
// client's tag.

/**
 * Maintains an index of remote audio servers that can be reached through the local network.
 *
 * @author Paddy Lamont
 */
public class RemoteAudioServerIndex {

    private static final int UPDATE_DELAY_MS = 5 * 1000;

    private final AudioStream audioStream;
    private final Multicast multicaster;
    private final LoopedThread updateThread;
    private final List<RemoteAudioServer> foundServers;
    private final List<RemoteAudioServer> manualServers;

    public RemoteAudioServerIndex(AudioStream audioStream) {
        this.audioStream = audioStream;
        this.multicaster = new Multicast(AudioStream.MULTICAST_ADDRESS, AudioStream.MULTICAST_PORT);
        this.updateThread = new LoopedThread("RemoteAudioServerIndex-thread", () -> {
            try {
                update();
            } catch (Exception e) {
                throw new RuntimeException("Exception while updating audio server index", e);
            }
        }, UPDATE_DELAY_MS);
        this.foundServers = new ArrayList<>();
        this.manualServers = new ArrayList<>();

        multicaster.addListener(packet -> {
            try {
                receivePacket(packet);
            } catch (Exception e) {
                new RuntimeException("Exception while reading packet", e).printStackTrace();
            }
        });
    }

    public void start() throws IOException {
        multicaster.open();
        updateThread.start();
    }

    public void stop() throws IOException {
        multicaster.close();
        updateThread.stopGracefully();
    }

    public synchronized RemoteAudioServer getServer(InetAddress address, int port) {
        for (RemoteAudioServer server : foundServers) {
            if (server.is(address, port))
                return server;
        }

        for (RemoteAudioServer server : manualServers) {
            if (server.is(address, port))
                return server;
        }

        return null;
    }

    public synchronized boolean containsServer(InetAddress address, int port) {
        return getServer(address, port) != null;
    }

    public synchronized boolean isManuallyAddedServer(RemoteAudioServer server) {
        return manualServers.contains(server);
    }

    public synchronized RemoteAudioServer addManualServer(InetAddress address, int port) throws IOException {
        for (RemoteAudioServer server : manualServers) {
            if (server.is(address, port))
                return server;
        }

        RemoteAudioServer server = null;
        Iterator<RemoteAudioServer> servers = foundServers.iterator();
        while (servers.hasNext()) {
            RemoteAudioServer foundServer = servers.next();

            if (foundServer.is(address, port)) {
                server = foundServer;
                servers.remove();
                break;
            }
        }

        if (server == null) {
            server = new RemoteAudioServer(address, port);
            requestServerInformation(server);
        }

        manualServers.add(server);
        return server;
    }

    public synchronized void removeManualServer(RemoteAudioServer server) {
        manualServers.remove(server);
    }

    public synchronized List<RemoteAudioServer> getServers() {
        List<RemoteAudioServer> servers = new ArrayList<>();
        servers.addAll(foundServers);
        servers.addAll(manualServers);
        return Collections.unmodifiableList(servers);
    }

    private synchronized RemoteAudioServer findOrCreateServer(InetAddress address, int port) {
        RemoteAudioServer existing = getServer(address, port);
        if (existing != null)
            return existing;

        RemoteAudioServer server = new RemoteAudioServer(address, port);
        foundServers.add(server);

        return server;
    }

    private synchronized void updateServerAudioFormat(InetAddress address, int port, AudioFormat format) {
        RemoteAudioServer server = findOrCreateServer(address, port);
        server.updateLastCommunicationTime();
        server.setAudioFormat(format);
    }

    private synchronized void purgeDeadConnections() {
        Iterator<RemoteAudioServer> servers = foundServers.iterator();
        while (servers.hasNext()) {
            RemoteAudioServer server = servers.next();

            if (server.getTimeSinceCommunicationMS() >= AudioStream.SERVER_NO_RESPONSE_PURGE_MS) {
                servers.remove();
            }
        }
    }

    public void update() throws IOException {
        searchForServers();
        purgeDeadConnections();
    }

    private byte[] constructSearchPacket() throws IOException {
        return PacketBuilder.create()
                            .writeType(PacketType.SERVER_SEARCH)
                            .build();
    }

    public void requestServerInformation(RemoteAudioServer server) throws IOException {
        multicaster.send(constructSearchPacket(), server.getAddress());
    }

    public void searchForServers() throws IOException {
        multicaster.send(constructSearchPacket());

        // Copy the manual servers list so we can use it without locking
        List<RemoteAudioServer> manualServers;
        synchronized (this) {
            manualServers = new ArrayList<>(this.manualServers);
        }

        for (RemoteAudioServer server : manualServers) {
            requestServerInformation(server);
        }
    }

    public void receivePacket(DatagramPacket packet) throws IOException {
        PacketReader reader = PacketReader.create(Arrays.copyOfRange(
                packet.getData(),
                packet.getOffset(),
                packet.getOffset() + packet.getLength()
        ));

        PacketType type = reader.readType();
        switch (type) {
            case SERVER_SEARCH_RESPONSE:

                int port = reader.readInt();
                AudioFormat format = reader.readAudioFormat();

                updateServerAudioFormat(
                        packet.getAddress(),
                        port,
                        format
                );
                break;

            case SERVER_SEARCH:
                if (!audioStream.isServerRunning())
                    return;

                ServerSettings settings = audioStream.getServer().getSettings();

                multicaster.send(
                        PacketBuilder.create()
                                .writeType(PacketType.SERVER_SEARCH_RESPONSE)
                                .writeInt(settings.port)
                                .writeAudioFormat(settings.format)
                                .build(),
                        packet.getAddress()
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown packet type " + type);
        }
    }
}
