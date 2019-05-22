package net.sothatsit.audiostream.property;

/**
 * A Property whose value is always non-null (even if its delegate property is null).
 * This Property does not allow itself to be set to null values, although its delegate may.
 *
 * @author Paddy Lamont
 */
public class NonNullProperty<T> extends DelegatedProperty<T> {

    private final T nullReplacementValue;

    public NonNullProperty(Property<T> delegate, T nullReplacementValue) {
        super(delegate);

        if (nullReplacementValue == null)
            throw new IllegalArgumentException("The null replacement value cannot be null");

        this.nullReplacementValue = nullReplacementValue;
    }

    @Override
    public void compareAndSet(T expectedValue, T updatedValue) {
        if (updatedValue == null)
            throw new IllegalArgumentException("Cannot set this non-null property to null");

        super.compareAndSet(expectedValue, updatedValue);
    }

    @Override
    public void set(T value) {
        if (value == null)
            throw new IllegalArgumentException("Cannot set this non-null property to null");

        super.set(value);
    }

    @Override
    public T get() {
        return getOrDefault(nullReplacementValue);
    }
}
