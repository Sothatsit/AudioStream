package net.sothatsit.audiostream.communication.audio;

import net.sothatsit.audiostream.audio.AudioReader;
import net.sothatsit.audiostream.communication.packet.PacketOutputStream;
import net.sothatsit.audiostream.util.LoopedThread;
import net.sothatsit.audiostream.util.ServiceState;
import net.sothatsit.audiostream.util.VariableBuffer;
import net.sothatsit.property.Property;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Streams audio from an AudioServer.
 *
 * @author Paddy Lamont
 */
public class AudioServerConnection {

    private final AudioServerSettings settings;
    private final AudioReader reader;
    private final Socket socket;
    private final LoopedThread thread;

    public AudioServerConnection(AudioServerSettings settings, AudioReader reader, Socket socket) {
        this.settings = settings;
        this.reader = reader;
        this.socket = socket;
        this.thread = new LoopedThread("streamingThread", this::stream);
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        thread.stop();
    }

    // TODO : Should manage its own state property which inherits errors from the thread's state.
    //        Then could make the starting and stopping states more accurate.
    public Property<ServiceState> getState() {
        return thread.getState();
    }

    private void stream(Property<Boolean> running) {
        PacketOutputStream outStream;
        VariableBuffer inBuffer = null;
        try {
            // Size the buffer such that it only contains whole audio frames
            int frameSizeBytes = settings.format.getFrameSize();
            int bufferSize = (settings.bufferSize / frameSizeBytes) * frameSizeBytes;

            outStream = new PacketOutputStream(socket.getOutputStream());
            inBuffer = new VariableBuffer(2 * bufferSize);
            reader.addOutBuffer(inBuffer);

            byte[] buffer = new byte[bufferSize];
            while (running.get()) {
                inBuffer.pop(buffer, 0, buffer.length);

                byte[] packet = buffer;
                if (settings.encryption != null) {
                    packet = settings.encryption.encrypt(buffer);
                }

                outStream.writePacket(packet, 0, packet.length);
            }
        } catch (IOException exception) {
            // When the other end of the connection is closed this exception will be thrown
            // TODO : Is there a more robust way to do this than to check an error message?
            if (SocketException.class.equals(exception.getClass()) && "Broken pipe".equals(exception.getMessage())) {
                thread.stopNextLoop();
                return;
            }

            throw new RuntimeException("There was an error streaming audio to client", exception);
        } finally {
            if (inBuffer != null) {
                reader.removeOutBuffer(inBuffer);
            }
        }
    }
}
