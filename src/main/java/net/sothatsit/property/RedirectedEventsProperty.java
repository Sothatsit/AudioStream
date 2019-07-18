package net.sothatsit.property;

import net.sothatsit.property.event.ChangeEventSource;
import net.sothatsit.property.event.ChangeListenerProperties;

import javax.swing.event.ChangeListener;

/**
 * A Property that propagates change events to different targets (e.g. on/off the EDT).
 *
 * @author Paddy Lamont
 */
public class RedirectedEventsProperty<T> extends DelegatedProperty<T> {

    private final ChangeEventSource source;

    public RedirectedEventsProperty(Property<T> delegate, ChangeListenerProperties properties) {
        super(delegate);

        if (properties.invocationType == ChangeListenerProperties.InvocationType.SAME_THREAD)
            throw new IllegalArgumentException("Same thread listeners make no redirection of threads");

        this.source = new ChangeEventSource();

        delegate.addChangeListener(source::fireChangeEvent, properties);
    }

    @Override
    public void addChangeListener(ChangeListener listener, ChangeListenerProperties properties) {
        source.addChangeListener(listener, properties);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        source.removeChangeListener(listener);
    }
}
