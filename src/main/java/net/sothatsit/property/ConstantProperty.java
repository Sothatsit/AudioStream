package net.sothatsit.property;

import net.sothatsit.property.event.ChangeListenerProperties;

import javax.swing.event.ChangeListener;
import java.util.Objects;

/**
 * A Property whose value is constant.
 *
 * @author Paddy Lamont
 */
public class ConstantProperty<T> implements Property<T> {

    private final String name;
    private final T value;

    public ConstantProperty(String name, T value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addChangeListener(ChangeListener listener, ChangeListenerProperties properties) {
        // The value is never going to change, so no need to bother registering the listener
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        // We don't let them add listeners, so can just ignore this removal
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass()))
            return false;

        ConstantProperty<?> other = (ConstantProperty<?>) obj;
        return Objects.equals(value, other.value);
    }
}
