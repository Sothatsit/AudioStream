package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.util.Exceptions;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Contains some useful methods for creating a GUI.
 *
 * @author Paddy Lamont
 */
public class GuiUtils {

    public static final Color LABEL_VALID_COLOUR = Color.BLACK;
    public static final Color LABEL_FOCUSED_COLOUR = Color.BLACK;
    public static final Color LABEL_INVALID_COLOUR = Color.RED;
    public static final Border TF_VALID_BORDER = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
    public static final Border TF_FOCUSED_BORDER = BorderFactory.createLineBorder(Color.GRAY);
    public static final Border TF_INVALID_BORDER = BorderFactory.createLineBorder(Color.RED);

    public static <V> WrappedComboBox<V> createComboBox(V[] values) {
        return createComboBox(values, (V) null);
    }

    public static <V> WrappedComboBox<V> createComboBox(V[] values, V defaultValue) {
        WrappedComboBox<V> combo = new WrappedComboBox<V>(values) {
            @Override
            protected V convertToValue(Object value) {
                return Unchecked.cast(value);
            }

            @Override
            public void setAvailableValues(V[] values) {
                setAvailableItems(values);
            }
        };
        combo.setSelectedValue(defaultValue);
        return combo;
    }

    public static <V> WrappedComboBox<V> createComboBox(V[] values, Function<V, String> toStringFn) {
        return createComboBox(values, null, toStringFn);
    }

    public static <V> WrappedComboBox<V> createComboBox(V[] values, V defaultValue, Function<V, String> toStringFn) {
        WrappedComboBox<V> combo = new CustomToStringComboBox<>(values, toStringFn);
        combo.setSelectedValue(defaultValue);
        return combo;
    }

    public static abstract class WrappedComboBox<V> extends JComboBox<Object> {

        public WrappedComboBox(Object[] values) {
            super(values);
        }

        protected abstract V convertToValue(Object obj);

        public abstract void setAvailableValues(V[] values);

        public void setAvailableItems(Object[] values) {
            // Avoid updating the items in this combo box if they haven't changed
            int count = getItemCount();
            if (count == values.length) {
                boolean changed = false;
                for (int index = 0; index < count; ++index) {
                    if (!Objects.equals(values[index], getItemAt(index))) {
                        changed = true;
                        break;
                    }
                }
                if (!changed)
                    return;
            }

            Object selected = getSelectedItem();

            removeAllItems();
            for (Object obj : values) {
                addItem(obj);
            }

            if (selected != null && arrayContains(values, selected)) {
                setSelectedItem(selected);
            }
        }

        public boolean setSelectedValue(V value) {
            for (int index = 0; index < getItemCount(); ++index) {
                V valueAtIndex = convertToValue(getItemAt(index));
                if (Objects.equals(value, valueAtIndex)) {
                    setSelectedIndex(index);
                    return true;
                }
            }
            return false;
        }

        public V getSelectedValue() {
            return convertToValue(super.getSelectedItem());
        }

        private static boolean arrayContains(Object[] array, Object value) {
            for (Object obj : array) {
                if (Objects.equals(value, obj))
                    return true;
            }
            return false;
        }
    }

    public static class CustomToStringComboBox<V> extends WrappedComboBox<V> {

        private final Function<V, String> toStringFn;

        public CustomToStringComboBox(V[] values, Function<V, String> toStringFn) {
            super(CustomToStringValue.wrap(values, toStringFn));

            this.toStringFn = toStringFn;
        }

        @Override
        protected V convertToValue(Object obj) {
            if (!(obj instanceof CustomToStringValue))
                return null;

            return Unchecked.cast(((CustomToStringValue) obj).value);
        }

        @Override
        public void setAvailableValues(V[] values) {
            setAvailableItems(CustomToStringValue.wrap(values, toStringFn));
        }
    }

    public static class CustomToStringValue<V> {

        public final V value;
        public final String stringValue;

        public CustomToStringValue(V value, String stringValue) {
            this.value = value;
            this.stringValue = stringValue;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (!obj.getClass().equals(getClass()))
                return false;
            return Objects.equals(value, ((CustomToStringValue) obj).value);
        }

        public static <V> CustomToStringValue[] wrap(V[] values, Function<V, String> toStringFn) {
            List<CustomToStringValue<V>> wrapped = new ArrayList<>();

            for (V value : values) {
                wrapped.add(new CustomToStringValue<>(value, toStringFn.apply(value)));
            }

            return wrapped.toArray(new CustomToStringValue[0]);
        }
    }

    public static JPanel createSeparator(String labelText) {
        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());
        GBCBuilder builder = new GBCBuilder()
                .anchor(GridBagConstraints.WEST)
                .fill(GridBagConstraints.HORIZONTAL)
                .insets(5, 5, 0, 0);

        JLabel label = new JLabel(labelText);
        JSeparator separator = new JSeparator();

        label.setFont(label.getFont().deriveFont(Font.BOLD));
        separator.setForeground(Color.GRAY);

        panel.add(label, builder.weightX(0.0).build());
        panel.add(separator, builder.weightX(1.0).build());

        return panel;
    }

    public static class TextFieldAndLabel {

        public final JLabel label;
        public final JTextField field;

        public TextFieldAndLabel(JLabel label, JTextField field) {
            this.label = label;
            this.field = field;
        }

        public String getText() {
            return field.getText();
        }
    }

    public static TextFieldAndLabel createTextFieldAndLabel(Object labelText,
                                                            Object defaultText) {
        return createTextFieldAndLabel(labelText, defaultText, text -> true);
    }

    public static TextFieldAndLabel createTextFieldAndLabel(Object labelText,
                                                            Object defaultText,
                                                            Function<String, Boolean> isValid) {
        final JLabel label = new JLabel(Objects.toString(labelText));
        final JTextField field = new JTextField(Objects.toString(defaultText));

        field.setBorder(TF_VALID_BORDER);
        Runnable update = () -> {
            boolean valid = isValid.apply(field.getText());
            if (valid) {
                if (field.hasFocus()) {
                    label.setForeground(LABEL_FOCUSED_COLOUR);
                    field.setBorder(TF_FOCUSED_BORDER);
                } else {
                    label.setForeground(LABEL_VALID_COLOUR);
                    field.setBorder(TF_VALID_BORDER);
                }
            } else {
                label.setForeground(LABEL_INVALID_COLOUR);
                field.setBorder(TF_INVALID_BORDER);
            }
        };

        field.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                update.run();
            }
            @Override
            public void focusLost(FocusEvent e) {
                update.run();
            }
        });
        addTextChangeListener(field, event -> update.run());

        return new TextFieldAndLabel(label, field);
    }

    public static JComponent buildCenteredPanel(Action... actions) {
        JComponent[] buttons = new JComponent[actions.length];

        for (int i=0; i < actions.length; ++i) {
            buttons[i] = new JButton(actions[i]);
        }

        return buildCenteredPanel(buttons);
    }

    public static JComponent buildCenteredPanel(JComponent... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GBCBuilder builder = new GBCBuilder()
                .anchor(GridBagConstraints.CENTER)
                .fill(GridBagConstraints.BOTH)
                .insets(0, 0, 0, 0);

        for (JComponent component : components) {
            panel.add(component, builder.weight(1.0, 1.0).build());
        }

        return panel;
    }

    public static JComponent buildVerticalPanel(JComponent... components) {
        return buildVerticalPanel(components, null);
    }

    public static JComponent buildVerticalPanel(JComponent[] components, float[] verticalWeights) {
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

    /**
     * This function was created by Boann on StackOverflow.
     *
     * Installs a listener to receive notification when the text of any
     * {@code JTextComponent} is changed. Internally, it installs a
     * {@link DocumentListener} on the text component's {@link Document},
     * and a {@link PropertyChangeListener} on the text component to detect
     * if the {@code Document} itself is replaced.
     *
     * @param text any text component, such as a {@link JTextField}
     *        or {@link JTextArea}
     * @param changeListener a listener to receieve {@link ChangeEvent}s
     *        when the text is changed; the source object for the events
     *        will be the text component
     * @throws NullPointerException if either parameter is null
     */
    public static void addTextChangeListener(JTextComponent text, ChangeListener changeListener) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(changeListener);
        DocumentListener dl = new DocumentListener() {
            private int lastChange = 0, lastNotifiedChange = 0;

            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                lastChange++;
                SwingUtilities.invokeLater(() -> {
                    if (lastNotifiedChange != lastChange) {
                        lastNotifiedChange = lastChange;
                        changeListener.stateChanged(new ChangeEvent(text));
                    }
                });
            }
        };
        text.addPropertyChangeListener("document", (PropertyChangeEvent e) -> {
            Document d1 = (Document)e.getOldValue();
            Document d2 = (Document)e.getNewValue();
            if (d1 != null) d1.removeDocumentListener(dl);
            if (d2 != null) d2.addDocumentListener(dl);
            dl.changedUpdate(null);
        });
        Document d = text.getDocument();
        if (d != null) d.addDocumentListener(dl);
    }

    public static void enableAll(List<? extends JComponent> components) {
        setEnabledAll(components, true);
    }

    public static void disableAll(List<? extends JComponent> components) {
        setEnabledAll(components, false);
    }

    public static void setEnabledAll(List<? extends JComponent> components, boolean enabled) {
        for (JComponent component : components) {
            component.setEnabled(enabled);
        }
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

        AtomicReference<Exception> exceptionHolder = new AtomicReference<>(null);
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    exceptionHolder.set(e);
                }
            });
        } catch (Exception e) {
            exceptionHolder.set(e);
        }

        Exception exception = exceptionHolder.get();
        if (exception != null)
            throw new RuntimeException("Exception invoking runnable on EDT", exception);
    }
}
