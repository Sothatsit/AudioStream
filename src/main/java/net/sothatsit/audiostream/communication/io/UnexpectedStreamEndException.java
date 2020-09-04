package net.sothatsit.audiostream.communication.io;

import java.io.IOException;

/**
 * Caused by a stream unexpectedly ending.
 *
 * @author Paddy Lamont
 */
public class UnexpectedStreamEndException extends IOException {

    public UnexpectedStreamEndException() {
        super();
    }

    public UnexpectedStreamEndException(String message) {
        super(message);
    }

    public UnexpectedStreamEndException(Throwable cause) {
        super(cause);
    }

    public UnexpectedStreamEndException(String message, Throwable cause) {
        super(message, cause);
    }
}
