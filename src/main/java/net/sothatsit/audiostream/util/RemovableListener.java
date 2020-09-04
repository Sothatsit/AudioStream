package net.sothatsit.audiostream.util;

import java.awt.*;
import java.awt.desktop.AppReopenedListener;

/**
 * Allows easy removal of listeners, which are otherwise difficult to keep track of.
 *
 * e.g. Listeners that rely on OS dependant behaviour.
 *
 * @author Paddy Lamont
 */
public interface RemovableListener {

    public void remove();

    public static RemovableListener createAppReopenedListener(Runnable runnable) {
        AppReopenedListener listener = e -> runnable.run();
        Desktop.getDesktop().addAppEventListener(listener);
        return () -> Desktop.getDesktop().removeAppEventListener(listener);
    }
}
