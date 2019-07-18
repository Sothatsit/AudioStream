package net.sothatsit.audiostream.communication.audio;

import net.sothatsit.audiostream.communication.RemoteServer;
import net.sothatsit.audiostream.util.ServiceState;
import net.sothatsit.property.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Manages a list of connected clients.
 *
 * @author Paddy Lamont
 */
public class AudioClientManager {

    private final Property<AudioClientSettings> settings;
    private final List<AudioClient> clients;

    public AudioClientManager(Property<AudioClientSettings> settings) {
        this.settings = settings;
        this.clients = new CopyOnWriteArrayList<>();
    }

    public List<AudioClient> getClients() {
        return new ArrayList<>(clients);
    }

    public boolean hasClientsRunning() {
        for (AudioClient client : clients) {
            if (client.getState().get().getType() == ServiceState.Type.RUNNING)
                return true;
        }
        return false;
    }

    public List<RemoteServer> getServers() {
        return clients.stream()
                .map(AudioClient::getServer)
                .collect(Collectors.toList());
    }

    public AudioClient getClient(RemoteServer server) {
        for (AudioClient client : clients) {
            if (client.getServer().equals(server))
                return client;
        }
        return null;
    }

    public void connectAll(List<RemoteServer> servers) {
        servers.stream().forEach(this::connect);
    }

    public void connect(RemoteServer server) {
        AudioClient client = getClient(server);
        if (client != null) {
            disconnect(client);
        }

        client = new AudioClient(server, settings);

        client.start();
        clients.add(client);
    }

    public void disconnectAll() {
        List<AudioClient> clientsCopy = new ArrayList<>(clients);
        clientsCopy.stream().forEach(this::disconnect);
    }

    public void disconnect(RemoteServer server) {
        AudioClient client = getClient(server);
        if (client == null)
            throw new IllegalArgumentException(server + " is not connected");

        disconnect(client);
    }

    public void disconnect(AudioClient client) {
        if (client == null)
            throw new IllegalArgumentException("audio cannot be null");
        if (!clients.contains(client))
            throw new IllegalArgumentException(client + " is not a part of this manager");

        try {
            client.stop();
        } finally {
            clients.remove(client);
        }
    }
}
