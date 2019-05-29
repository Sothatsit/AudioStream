package net.sothatsit.property;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.*;

/**
 * Provides an implementation for firing change events to many listeners.
 *
 * @author Paddy Lamont
 */
public class ChangeEventSource implements ChangeListenable {

    private final Object lock = new Object();
    private final Map<ChangeListener, ChangeListenerProperties> listeners = new HashMap<>();
    private final Map<ChangeListener, ChangeListenerProperties> weakListeners = new WeakHashMap<>();

    public void fireChangeEvent() {
        fireChangeEvent(new ChangeEvent(this));
    }

    public void fireChangeEvent(ChangeEvent event) {
        fireChangeEventIgnoreListeners(event, Collections.emptySet());
    }

    public void fireChangeEventIgnoreListeners(ChangeEvent event, Collection<ChangeListener> ignoreListeners) {
        synchronized (lock) {
            Map<ChangeListener, ChangeListenerProperties> allListeners = new HashMap<>();

            allListeners.putAll(listeners);
            allListeners.putAll(weakListeners);

            for (Map.Entry<ChangeListener, ChangeListenerProperties> entry : allListeners.entrySet()) {
                ChangeListener listener = entry.getKey();
                ChangeListenerProperties properties = entry.getValue();

                if (ignoreListeners.contains(listener))
                    continue;

                properties.invoke(listener, event);
            }
        }
    }

    @Override
    public void addChangeListener(ChangeListener listener, ChangeListenerProperties properties) {
        synchronized (lock) {
            // Make sure the listener can't be added to both the weak and non-weak maps
            removeChangeListener(listener);

            if (properties.useWeakReference) {
                weakListeners.put(listener, properties);
            } else {
                listeners.put(listener, properties);
            }
        }
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        synchronized (lock) {
            listeners.remove(listener);
            weakListeners.remove(listener);
        }
    }
}
