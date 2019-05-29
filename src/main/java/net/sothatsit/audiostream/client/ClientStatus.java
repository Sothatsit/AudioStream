package net.sothatsit.audiostream.client;

/**
 * The status of a client.
 *
 * @author Paddy Lamont
 */
public class ClientStatus {

    public final boolean connected;
    public final String message;
    public final Exception exception;

    public ClientStatus(boolean connected, String message) {
        this(connected, message, null);
    }

    public ClientStatus(boolean connected, String message, Exception exception) {
        if (message == null)
            throw new IllegalArgumentException("message cannot be null");

        this.connected = connected;
        this.message = message;
        this.exception = exception;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getMessage() {
        return message;
    }

    public boolean isErrored() {
        return exception != null;
    }

    public Exception getException() {
        return exception;
    }
}
