package net.sothatsit.property.gui;

import net.sothatsit.audiostream.util.Exceptions;

import javax.swing.*;
import java.awt.*;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains some useful methods for creating a GUI.
 *
 * @author Paddy Lamont
 */
public class GuiUtils {

    public static JPanel buildCenteredPanel(PropertyComponent... propertyComponents) {
        Component[] components = new Component[propertyComponents.length];

        for (int i=0; i < propertyComponents.length; ++i) {
            components[i] = propertyComponents[i].getComponent();
        }

        return buildCenteredPanel(components);
    }

    public static JPanel buildCenteredPanel(Action... actions) {
        Component[] buttons = new Component[actions.length];

        for (int i=0; i < actions.length; ++i) {
            buttons[i] = new JButton(actions[i]);
        }

        return buildCenteredPanel(buttons);
    }

    public static JPanel buildCenteredPanel(Component... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GBCBuilder builder = new GBCBuilder()
                .anchor(GridBagConstraints.CENTER)
                .fill(GridBagConstraints.BOTH)
                .insets(0, 0, 0, 0);

        for (Component component : components) {
            panel.add(component, builder.weight(1.0, 1.0).build());
        }

        return panel;
    }

    public static JPanel buildVerticalPanel(JComponent... components) {
        return buildVerticalPanel(components, null);
    }

    public static JPanel buildVerticalPanel(JComponent[] components, float[] verticalWeights) {
        if (components == null)
            throw new IllegalArgumentException("components cannot be null");
        if (verticalWeights != null && components.length != verticalWeights.length)
            throw new IllegalArgumentException("components and verticalWeights must have the same length");

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.CENTER)
                .fill(GridBagConstraints.BOTH)
                .insets(0, 0, 0, 0);

        for (int index = 0; index < components.length; ++index) {
            JComponent component = components[index];
            float weightY = (verticalWeights != null ? verticalWeights[index] : 0);

            panel.add(component, constraints.weight(1.0, weightY).build());
            constraints.nextRow();
        }

        return panel;
    }

    public static void reportError(Exception exception) {
        reportError(null, exception);
    }

    public static void reportError(JComponent parent, Exception exception) {
        new RuntimeException("An error occurred", exception).printStackTrace();

        if (exception instanceof UnknownHostException) {
            String message = "Could not connect to host: " + exception.getMessage();
            JOptionPane.showMessageDialog(parent, message, "Connection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (exception instanceof SocketException) {
            String message = "Error communicating with socket: " + exception.getMessage();
            JOptionPane.showMessageDialog(parent, message, "Socket Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String error = Exceptions.exceptionToString(exception);
        JOptionPane.showMessageDialog(parent, error, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void reportErrorFatal(Exception exception) {
        reportErrorFatal(null, exception);
    }

    public static void reportErrorFatal(JComponent parent, Exception exception) {
        new RuntimeException("A fatal error occurred", exception).printStackTrace();
        runOnEDT(() -> {
            String error = Exceptions.exceptionToString(exception);
            JOptionPane.showMessageDialog(parent, "There was a fatal error:\n" + error);
            System.exit(1);
        });
    }

    public static void runOnEDT(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }

        AtomicReference<Exception> runnableException = new AtomicReference<>(null);
        Exception runException = null;

        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    runnable.run();
                } catch (Exception exception) {
                    runnableException.set(exception);
                }
            });
        } catch (Exception exception) {
            runException = exception;
        }

        Exception runnableExceptionValue = runnableException.getAndSet(null);
        if (runnableExceptionValue != null) {
            if (runException != null) {
                runException.addSuppressed(runnableExceptionValue);
            } else {
                runException = runnableExceptionValue;
            }
        }

        if (runException != null)
            throw new RuntimeException("Exception invoking runnable on EDT", runException);
    }
}
