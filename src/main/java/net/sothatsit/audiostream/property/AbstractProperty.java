package net.sothatsit.audiostream.property;

/**
 * A holder that stores a value, and notifies listeners whenever this value is changed.
 *
 * @author Paddy Lamont
 */
public abstract class AbstractProperty<T> extends ChangeEventSource implements Property<T> {

    private final String name;

    public AbstractProperty(String name) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");

        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void compareAndSet(T expectedValue, T updatedValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "Property(name=" + name + ")";
    }
}
