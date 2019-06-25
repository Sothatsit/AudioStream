package net.sothatsit.audiostream.client;

import net.sothatsit.audiostream.encryption.Encryption;
import net.sothatsit.property.Attribute;
import net.sothatsit.property.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages a list of connected clients.
 *
 * @author Paddy Lamont
 */
public class ClientManager {

    private final ClientSettings settings;
    private final List<Client> clients;
    private final Attribute<Encryption> encryption;

    public ClientManager(ClientSettings settings) {
        this.settings = settings;
        this.clients = new ArrayList<>();
        this.encryption = Attribute.createNullable("encryption", null);
    }

    public ClientSettings getSettings() {
        return settings;
    }

    public List<Client> getClients() {
        return clients;
    }

    public List<RemoteAudioServer> getServers() {
        return clients.stream()
                .map(Client::getServer)
                .collect(Collectors.toList());
    }

    public Client getClient(RemoteAudioServer server) {
        for (Client client : clients) {
            if (client.getServer().equals(server))
                return client;
        }
        return null;
    }

    public Property<Encryption> getEncryption() {
        return encryption.readOnly();
    }

    public void setEncryption(Property<Encryption> encryption) {
        this.encryption.set(encryption);
    }

    public void connectAll(List<RemoteAudioServer> servers) {
        servers.stream().forEach(this::connect);
    }

    public void connect(RemoteAudioServer server) {
        Client client = getClient(server);
        if (client != null) {
            disconnect(client);
        }

        client = new Client(server, settings);
        client.setEncryption(encryption);

        client.start();
        clients.add(client);
    }

    public void disconnectAll() {
        List<Client> clientsCopy = new ArrayList<>(clients);
        clientsCopy.stream().forEach(this::disconnect);
    }

    public void disconnect(RemoteAudioServer server) {
        Client client = getClient(server);
        if (client == null)
            throw new IllegalArgumentException(server + " is not connected");

        disconnect(client);
    }

    public void disconnect(Client client) {
        if (client == null)
            throw new IllegalArgumentException("client cannot be null");
        if (!clients.contains(client))
            throw new IllegalArgumentException(client + " is not a part of this manager");

        try {
            client.stop();
        } finally {
            clients.remove(client);
        }
    }
}
