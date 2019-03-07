package net.sothatsit.audiostream.client;

/**
 * The state that a Client is in.
 *
 * @author Paddy Lamont
 */
public enum ClientState {

    STOPPED,
    CONNECTING,
    CONNECTED,
    ERRORED
}
