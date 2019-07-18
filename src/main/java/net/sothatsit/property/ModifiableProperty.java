package net.sothatsit.property;

import javax.swing.event.ChangeEvent;
import java.util.Objects;

/**
 * A holder that stores a value, and notifies listeners whenever this value is changed.
 *
 * @author Paddy Lamont
 */
public class ModifiableProperty<T> extends AbstractProperty<T> {

    private final Object lock = new Object();

    protected T value;

    public ModifiableProperty(String name) {
        this(name, null);
    }

    public ModifiableProperty(String name, T value) {
        super(name);

        this.value = value;
    }

    @Override
    public void compareAndSet(T expectedValue, T updatedValue) {
        synchronized (lock) {
            if (!Objects.equals(expectedValue, this.value))
                return;

            set(updatedValue);
        }
    }

    @Override
    public void set(T value) {
        ChangeEvent event = null;

        synchronized (lock) {
            if (value == this.value)
                return;

            if (!Objects.equals(this.value, value)) {
                event = new PropertyChangeEvent<>(this, this.value, value);
            }

            this.value = value;
        }

        if (event != null) {
            fireChangeEvent(event);
        }
    }

    @Override
    public T get() {
        return value;
    }

    public static class PropertyChangeEvent<T> extends ChangeEvent {

        private final T previousValue;
        private final T newValue;

        public PropertyChangeEvent(Object source, T previousValue, T newValue) {
            super(source);

            this.previousValue = previousValue;
            this.newValue = newValue;
        }

        public T getPreviousValue() {
            return previousValue;
        }

        public T getNewValue() {
            return newValue;
        }
    }
}
