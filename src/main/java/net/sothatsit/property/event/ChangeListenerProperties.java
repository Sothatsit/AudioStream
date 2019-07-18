package net.sothatsit.property.event;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Properties to decide how a ChangeListener should be invoked.
 *
 * @author Paddy Lamont
 */
public class ChangeListenerProperties {

    public final InvocationType invocationType;
    public final boolean useWeakReference;

    public ChangeListenerProperties(InvocationType invocationType, boolean useWeakReference) {
        this.invocationType = invocationType;
        this.useWeakReference = useWeakReference;
    }

    public enum InvocationType {
        ON_EDT,
        OFF_EDT,
        SAME_THREAD
    }

    private void invokeOnThisThread(ChangeListener listener, ChangeEvent event) {
        try {
            listener.stateChanged(event);
        } catch (Exception exception) {
            new RuntimeException("There was an error invoking ChangeListener", exception).printStackTrace();
        }
    }

    public void invoke(final ChangeListener listener, final ChangeEvent event) {
        switch (invocationType) {
            case ON_EDT: {
                SwingUtilities.invokeLater(() -> invokeOnThisThread(listener, event));
                break;
            }

            case OFF_EDT: {
                SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
                    @Override
                    protected Object doInBackground() throws Exception {
                        invokeOnThisThread(listener, event);
                        return null;
                    }
                };
                worker.execute();
                break;
            }

            case SAME_THREAD: {
                invokeOnThisThread(listener, event);
                break;
            }
        }
    }

    public static ChangeListenerProperties create() {
        return new ChangeListenerProperties(InvocationType.SAME_THREAD, false);
    }

    public static ChangeListenerProperties createWeak() {
        return new ChangeListenerProperties(InvocationType.SAME_THREAD, true);
    }

    public static ChangeListenerProperties createOnEDT() {
        return new ChangeListenerProperties(InvocationType.ON_EDT, false);
    }

    public static ChangeListenerProperties createWeakOnEDT() {
        return new ChangeListenerProperties(InvocationType.ON_EDT, true);
    }

    public static ChangeListenerProperties createOffEDT() {
        return new ChangeListenerProperties(InvocationType.OFF_EDT, false);
    }

    public static ChangeListenerProperties createWeakOffEDT() {
        return new ChangeListenerProperties(InvocationType.OFF_EDT, true);
    }
}
