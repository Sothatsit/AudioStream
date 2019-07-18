package net.sothatsit.property.event;

import javax.swing.event.ChangeListener;

/**
 * Set of methods to listen to changes in an object.
 *
 * @author Paddy Lamont
 */
public interface ChangeListenable {

    /**
     * Add a change listener to be invoked using the given properties.
     */
    public void addChangeListener(ChangeListener listener, ChangeListenerProperties properties);

    /**
     * Add a change listener.
     */
    public default void addChangeListener(ChangeListener listener) {
        addChangeListener(listener, ChangeListenerProperties.create());
    }

    /**
     * Add a change listener that is held using a weak reference.
     */
    public default void addWeakChangeListener(ChangeListener listener) {
        addChangeListener(listener, ChangeListenerProperties.createWeak());
    }

    /**
     * Add a change listener that should be invoked on the EDT.
     */
    public default void addEDTChangeListener(ChangeListener listener) {
        addChangeListener(listener, ChangeListenerProperties.createOnEDT());
    }

    /**
     * Add a change listener that should be invoked on the EDT.
     */
    public default void addWeakEDTChangeListener(ChangeListener listener) {
        addChangeListener(listener, ChangeListenerProperties.createWeakOnEDT());
    }

    /**
     * Add a change listener that should be invoked on the EDT.
     */
    public default void addOffEDTChangeListener(ChangeListener listener) {
        addChangeListener(listener, ChangeListenerProperties.createOffEDT());
    }

    /**
     * Add a change listener that should be invoked on the EDT.
     */
    public default void addWeakOffEDTChangeListener(ChangeListener listener) {
        addChangeListener(listener, ChangeListenerProperties.createWeakOffEDT());
    }

    /**
     * Stop invoking the given ChangeListener on change events.
     */
    public void removeChangeListener(ChangeListener listener);
}
