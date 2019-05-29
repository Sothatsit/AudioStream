package net.sothatsit.property;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Properties to decide how a ChangeListener should be invoked.
 *
 * @author Paddy Lamont
 */
public class ChangeListenerProperties {

    public final boolean invokeOnEDT;
    public final boolean useWeakReference;

    public ChangeListenerProperties(boolean invokeOnEDT, boolean useWeakReference) {
        this.invokeOnEDT = invokeOnEDT;
        this.useWeakReference = useWeakReference;
    }

    public void invoke(ChangeListener listener, ChangeEvent e) {
        if (!invokeOnEDT || SwingUtilities.isEventDispatchThread()) {
            try {
                listener.stateChanged(e);
            } catch (Exception exception) {
                new RuntimeException("There was an error invoking ChangeListener", exception).printStackTrace();
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                listener.stateChanged(e);
            } catch (Exception exception) {
                new RuntimeException("There was an error invoking ChangeListener", exception).printStackTrace();
            }
        });
    }

    public static ChangeListenerProperties create() {
        return new ChangeListenerProperties(false, false);
    }

    public static ChangeListenerProperties createWeak() {
        return new ChangeListenerProperties(false, true);
    }

    public static ChangeListenerProperties createOnEDT() {
        return new ChangeListenerProperties(true, false);
    }

    public static ChangeListenerProperties createWeakOnEDT() {
        return new ChangeListenerProperties(true, true);
    }
}
