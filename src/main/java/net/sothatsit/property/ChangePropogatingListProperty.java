package net.sothatsit.property;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Collections;
import java.util.List;

/**
 * A ListProperty that listens to and propogates any change events from ChangeListenable values in this list.
 *
 * @author Paddy Lamont
 */
public class ChangePropogatingListProperty<E> extends ListProperty<E> {

    private final ChangeListener propagatingListener;

    public ChangePropogatingListProperty(String name) {
        this(name, Collections.emptyList());
    }

    public ChangePropogatingListProperty(String name, List<E> defaultValue) {
        super(name, defaultValue);

        ChangeListener onListChangeListener = this::updatePropertyListeners;

        // We don't want propagated events to invoke onListChangeListener
        propagatingListener = event -> {
            fireChangeEventIgnoreListeners(event, Collections.singleton(onListChangeListener));
        };

        addChangeListener(onListChangeListener);
    }

    private void updatePropertyListeners(ChangeEvent event) {
        if (event == null)
            throw new IllegalArgumentException("event shouldn't be null");

        if (event instanceof ListManyChangeEvent) {
            ((ListManyChangeEvent<?>) event).getChanges().forEach(this::updatePropertyListeners);
            return;
        }

        if (event instanceof ListPermutationChangeEvent)
            return;

        if (event instanceof ListAddChangeEvent) {
            handleAddEvent(Unchecked.cast(event));
            return;
        }

        if (event instanceof ListRemoveChangeEvent) {
            handleRemoveEvent(Unchecked.cast(event));
            return;
        }

        throw new IllegalArgumentException("Unknown ChangeEvent type " + event.getClass());
    }

    private void handleAddEvent(ListProperty.ListAddChangeEvent<E> event) {
        E value = event.getValue();
        if (value instanceof ChangeListenable) {
            ((ChangeListenable) value).addWeakChangeListener(propagatingListener);
        }
    }

    private void handleRemoveEvent(ListProperty.ListRemoveChangeEvent<E> event) {
        E value = event.getValue();
        if (value instanceof ChangeListenable) {
            ((ChangeListenable) value).removeChangeListener(propagatingListener);
        }
    }
}
