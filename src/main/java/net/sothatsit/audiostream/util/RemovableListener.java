package net.sothatsit.audiostream.util;

/**
 * Allows easy removal of listeners, which are otherwise difficult to keep track of.
 *
 * e.g. Listeners that rely on OS dependant behaviour.
 *
 * @author Paddy Lamont
 */
public interface RemovableListener {

    public void remove();
}
