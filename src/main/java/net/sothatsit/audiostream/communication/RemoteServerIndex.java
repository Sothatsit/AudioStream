package net.sothatsit.audiostream.communication;

import net.sothatsit.audiostream.model.RemoteServerDetails;
import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.util.RetryingLoopedThread;
import net.sothatsit.property.Property;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Maintains an index of remote audio servers that can be reached through the local network.
 *
 * @author Paddy Lamont
 */
public class RemoteServerIndex {

    // TODO : If a remote server changes its port, the audio connection may still remain at the same address,
    //        while the remote server's control address changes. This may cause issues if an entry is removed
    //        from this index, but we are still connected to it for audio.

    private static final int PURGE_DEAD_DELAY_MS = 3 * 1000;
    private static final long SERVER_NO_RESPONSE_PURGE_MS = 6 * 1000;

    private final Property<ControlServer> controlServer;
    private final LoopedThread updateThread;

    private final List<RemoteServer> foundServers;
    private final List<RemoteServer> manualServers;

    public RemoteServerIndex(Property<ControlServer> controlServer) {
        this.controlServer = controlServer;
        // TODO : Purging dead connections should be done as soon as their TCPConnection state changes
        this.updateThread = new RetryingLoopedThread("updateThread", this::purgeDeadConnections, PURGE_DEAD_DELAY_MS);
        this.foundServers = new ArrayList<>();
        this.manualServers = new ArrayList<>();

        controlServer.addValueListener(this::updateControlServer);
    }

    public void start() {
        updateThread.start();
    }

    public void stop() {
        updateThread.stop();
    }

    private void updateControlServer(ControlServer controlServer) {
        if (controlServer == null)
            return;

        controlServer.addDiscoveryListener(this::updateServerDetails);
        controlServer.broadcastDetails();
    }

    public synchronized RemoteServer getServer(InetSocketAddress address) {
        for (RemoteServer server : foundServers) {
            if (server.is(address))
                return server;
        }

        for (RemoteServer server : manualServers) {
            if (server.is(address))
                return server;
        }

        return null;
    }

    private synchronized RemoteServer findOrCreateServer(InetSocketAddress address) {
        RemoteServer existing = getServer(address);
        if (existing != null)
            return existing;

        RemoteServer server = new RemoteServer(address);
        foundServers.add(server);

        return server;
    }

    private synchronized void updateServerDetails(RemoteServerDetails serverDetails) {
        findOrCreateServer(serverDetails.controlAddress).updateDetails(serverDetails);
    }

    public synchronized boolean isManuallyAddedServer(RemoteServer server) {
        return manualServers.contains(server);
    }

    public synchronized RemoteServer addManualServer(InetSocketAddress address) throws IOException {
        for (RemoteServer server : manualServers) {
            if (server.is(address))
                return server;
        }

        RemoteServer server = null;
        Iterator<RemoteServer> servers = foundServers.iterator();
        while (servers.hasNext()) {
            RemoteServer foundServer = servers.next();

            if (foundServer.is(address)) {
                server = foundServer;
                servers.remove();
                break;
            }
        }

        if (server == null) {
            server = new RemoteServer(address);

            ControlServer controlServer = this.controlServer.get();
            if (controlServer != null) {
                controlServer.sendDetailsRequest(address);
            }
        }

        manualServers.add(server);
        return server;
    }

    public synchronized void removeManualServer(RemoteServer server) {
        manualServers.remove(server);
    }

    public synchronized List<RemoteServer> getServers() {
        List<RemoteServer> servers = new ArrayList<>();
        servers.addAll(foundServers);
        servers.addAll(manualServers);
        return Collections.unmodifiableList(servers);
    }

    private synchronized void purgeDeadConnections() {
        Iterator<RemoteServer> servers = foundServers.iterator();
        while (servers.hasNext()) {
            RemoteServer server = servers.next();

            if (server.getTimeSinceUpdateMS() >= SERVER_NO_RESPONSE_PURGE_MS) {
                servers.remove();
                System.err.println("Purge " + server);
            }
        }
    }
}
