package net.sothatsit.audiostream;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
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
        return address + ":" + port;
    }

    public long getTimeSinceCommunicationMS() {
        return System.currentTimeMillis() - lastCommunication;
    }

    public boolean is(InetAddress address, int port) {
        return this.address.equals(address) && this.port == port;
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
}
