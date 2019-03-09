package net.sothatsit.audiostream.client;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A class used to represent a remote audio server that can be connected to.
 *
 * @author Paddy Lamont
 */
public class RemoteAudioServer {

    private final InetAddress address;
    private final int port;

    private final List<Consumer<RemoteAudioServer>> audioFormatListeners;

    private AudioFormat audioFormat;
    private long lastCommunication;

    public RemoteAudioServer(InetAddress address, int port) {
        this.address = address;
        this.port = port;

        this.audioFormatListeners = new ArrayList<>();

        updateLastCommunicationTime();
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public String getAddressString() {
        String addressString;
        if (isAddressLocalhost(address)) {
            addressString = "localhost";
        } else {
            addressString = address.getHostName();
            if (addressString == null) {
                addressString = address.getHostAddress();
            }
        }

        return addressString + ":" + port;
    }

    public long getTimeSinceCommunicationMS() {
        return System.currentTimeMillis() - lastCommunication;
    }

    public boolean is(InetAddress address, int port) {
        if (this.port != port)
            return false;
        if (this.address.equals(address))
            return true;
        return isAddressLocalhost(this.address) && isAddressLocalhost(address);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass()))
            return false;

        RemoteAudioServer other = (RemoteAudioServer) obj;
        return is(other.address, other.port);
    }

    @Override
    public int hashCode() {
        return address.hashCode() ^ Integer.hashCode(port);
    }

    public void addAudioFormatListener(Consumer<RemoteAudioServer> listener) {
        audioFormatListeners.add(listener);
    }

    public void removeAudioFormatListener(Consumer<RemoteAudioServer> listener) {
        audioFormatListeners.remove(listener);
    }

    public void setAudioFormat(AudioFormat format) {
        this.audioFormat = format;

        for (Consumer<RemoteAudioServer> listener : audioFormatListeners) {
            listener.accept(this);
        }
    }

    public void updateLastCommunicationTime() {
        this.lastCommunication = System.currentTimeMillis();
    }

    public Socket connectToSocket() throws IOException {
        return new Socket(address, port);
    }

    @Override
    public String toString() {
        return "RemoteAudioServer(" + address + ":" + port + ")";
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
