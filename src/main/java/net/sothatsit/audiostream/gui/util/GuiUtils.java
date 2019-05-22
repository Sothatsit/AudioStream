package net.sothatsit.audiostream.gui.util;

import net.sothatsit.audiostream.property.ListProperty;
import net.sothatsit.audiostream.property.Property;
import net.sothatsit.audiostream.util.Exceptions;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.List;

/**
 * Contains some useful methods for creating a GUI.
 *
 * @author Paddy Lamont
 */
public class GuiUtils {

    public static final Color VALID_FOREGROUND = Color.BLACK;
    public static final Color FOCUSED_FOREGROUND = Color.BLACK;
    public static final Color INVALID_FOREGROUND = Color.RED;
    public static final Border VALID_BORDER = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
    public static final Border FOCUSED_BORDER = BorderFactory.createLineBorder(Color.GRAY);
    public static final Border INVALID_BORDER = BorderFactory.createLineBorder(Color.RED);

    public static <E> PropertyComboBox<E> createComboBox(E[] availableValues,
                                                         Property<E> selectedValue) {
        return createComboBox(
                ListProperty.constant("comboBox_availableValues", availableValues),
                selectedValue
        );
    }

    public static <E> PropertyComboBox<E> createComboBox(List<E> availableValues,
                                                         Property<E> selectedValue) {
        return createComboBox(
                ListProperty.constant("comboBox_availableValues", availableValues),
                selectedValue
        );
    }

    public static <E> PropertyComboBox<E> createComboBox(ListProperty<E> availableValues,
                                                         Property<E> selectedValue) {

        return new PropertyComboBox<>(new JComboBox<>(), availableValues, selectedValue);
    }

    public static <E> PropertyComboBox<E> createComboBox(E[] availableValues,
                                                         Property<E> selectedValue,
                                                         Function<E, Object> toDisplayValueFn) {
        return createComboBox(
                ListProperty.constant("comboBox_availableValues", availableValues),
                selectedValue,
                toDisplayValueFn
        );
    }

    public static <E> PropertyComboBox<E> createComboBox(List<E> availableValues,
                                                         Property<E> selectedValue,
                                                         Function<E, Object> toDisplayValueFn) {
        return createComboBox(
                ListProperty.constant("comboBox_availableValues", availableValues),
                selectedValue,
                toDisplayValueFn
        );
    }

    public static <E> PropertyComboBox<E> createComboBox(ListProperty<E> availableValues,
                                                         Property<E> selectedValue,
                                                         Function<E, Object> toDisplayValueFn) {

        ListCellRenderer<E> renderer = new MappedListCellRenderer<>(
                PropertyComboBox.getDefaultCellRenderer(),
                toDisplayValueFn
        );

        return createComboBox(availableValues, selectedValue, renderer);
    }

    public static <E> PropertyComboBox<E> createComboBox(E[] availableValues,
                                                         Property<E> selectedValue,
                                                         ListCellRenderer<E> renderer) {
        return createComboBox(
                ListProperty.constant("comboBox_availableValues", availableValues),
                selectedValue,
                renderer
        );
    }

    public static <E> PropertyComboBox<E> createComboBox(List<E> availableValues,
                                                         Property<E> selectedValue,
                                                         ListCellRenderer<E> renderer) {
        return createComboBox(
                ListProperty.constant("comboBox_availableValues", availableValues),
                selectedValue,
                renderer
        );
    }

    public static <E> PropertyComboBox<E> createComboBox(ListProperty<E> availableValues,
                                                         Property<E> selectedValue,
                                                         ListCellRenderer<E> renderer) {

        return new PropertyComboBox<>(availableValues, selectedValue, renderer);
    }

    public static PropertyTextField createTextField(Property<String> text) {
        return new PropertyTextField(text);
    }

    public static PropertyTextField createTextFieldWithValidation(Property<String> text,
                                                                  Function<String, Boolean> isValidFn) {

        return createTextFieldWithValidation(text, text.map("isValid", isValidFn));
    }

    public static PropertyTextField createTextFieldWithValidation(Property<String> text,
                                                                  Property<Boolean> isValid) {

        PropertyTextField textField = createTextField(text);
        decorateWithValidationBorder(textField, isValid);

        return textField;
    }

    public static PropertyLabel createLabel(String text) {
        return createLabel(Property.constant("label-text", text));
    }

    public static PropertyLabel createLabel(Property<String> text) {
        return new PropertyLabel(new JLabel(), text);
    }

    public static PropertyLabel createLabelWithValidation(String text,
                                                          Property<Boolean> isValid) {

        return createLabelWithValidation(Property.constant("label-text", text), isValid);
    }

    public static PropertyLabel createLabelWithValidation(Property<String> text,
                                                          Property<Boolean> isValid) {

        PropertyLabel label = createLabel(text);
        decorateWithValidationForeground(label, isValid);

        return label;
    }

    public static class TextFieldAndLabel extends JPanel {

        public final PropertyLabel label;
        public final PropertyTextField field;

        public TextFieldAndLabel(PropertyLabel label,
                                 PropertyTextField field) {

            this.label = label;
            this.field = field;

            setLayout(new GridBagLayout());
            GBCBuilder gbc = new GBCBuilder()
                    .anchor(GridBagConstraints.WEST)
                    .fill(GridBagConstraints.HORIZONTAL)
                    .insets(5, 5, 5, 5);

            addTo(this, gbc);
        }

        public void addTo(Container container, GBCBuilder gbc) {
            label.addTo(container, gbc.weightX(0.0).build());
            field.addTo(container, gbc.weightX(1.0).build());
        }
    }

    public static TextFieldAndLabel createTextFieldAndLabel(String labelText, Property<String> fieldText) {

        PropertyLabel label = createLabel(labelText);
        PropertyTextField field = createTextField(fieldText);

        return new TextFieldAndLabel(label, field);
    }

    public static TextFieldAndLabel createTextFieldAndLabel(Property<String> labelText, Property<String> fieldText) {

        PropertyLabel label = createLabel(labelText);
        PropertyTextField field = createTextField(fieldText);

        return new TextFieldAndLabel(label, field);
    }

    public static TextFieldAndLabel createTextFieldAndLabelWithValidation(String labelText,
                                                                          Property<String> fieldText,
                                                                          Function<String, Boolean> isValidFn) {

        return createTextFieldAndLabelWithValidation(labelText, fieldText, fieldText.map("isValid", isValidFn));
    }

    public static TextFieldAndLabel createTextFieldAndLabelWithValidation(Property<String> labelText,
                                                                          Property<String> fieldText,
                                                                          Function<String, Boolean> isValidFn) {

        return createTextFieldAndLabelWithValidation(labelText, fieldText, fieldText.map("isValid", isValidFn));
    }

    public static TextFieldAndLabel createTextFieldAndLabelWithValidation(String labelText,
                                                                          Property<String> fieldText,
                                                                          Property<Boolean> isValid) {

        PropertyLabel label = createLabelWithValidation(labelText, isValid);
        PropertyTextField field = createTextFieldWithValidation(fieldText, isValid);

        return new TextFieldAndLabel(label, field);
    }

    public static TextFieldAndLabel createTextFieldAndLabelWithValidation(Property<String> labelText,
                                                                          Property<String> fieldText,
                                                                          Property<Boolean> isValid) {

        PropertyLabel label = createLabelWithValidation(labelText, isValid);
        PropertyTextField field = createTextFieldWithValidation(fieldText, isValid);

        return new TextFieldAndLabel(label, field);
    }

    public static class ComboBoxAndLabel<E> extends JPanel {

        public final PropertyLabel label;
        public final PropertyComboBox<E> comboBox;

        public ComboBoxAndLabel(PropertyLabel label,
                                PropertyComboBox<E> comboBox) {

            this.label = label;
            this.comboBox = comboBox;

            setLayout(new GridBagLayout());
            GBCBuilder gbc = new GBCBuilder()
                    .anchor(GridBagConstraints.WEST)
                    .fill(GridBagConstraints.HORIZONTAL)
                    .insets(5, 5, 5, 5);

            addTo(this, gbc);
        }

        public void addTo(Container container, GBCBuilder gbc) {
            label.addTo(container, gbc.weightX(0.0).build());
            comboBox.addTo(container, gbc.weightX(1.0).build());
        }
    }

    public static <E> ComboBoxAndLabel<E> createComboBoxAndLabel(Property<String> labelText,
                                                                 ListProperty<E> availableValues,
                                                                 Property<E> selectedValue) {

        PropertyLabel label = createLabel(labelText);
        PropertyComboBox<E> comboBox = createComboBox(availableValues, selectedValue);

        return new ComboBoxAndLabel<>(label, comboBox);
    }

    public static class Separator extends JPanel {

        public final PropertyLabel label;
        public final JSeparator separator;

        public Separator(PropertyLabel label, JSeparator separator) {

            this.label = label;
            this.separator = separator;

            setLayout(new GridBagLayout());
            GBCBuilder gbc = new GBCBuilder()
                    .anchor(GridBagConstraints.WEST)
                    .fill(GridBagConstraints.HORIZONTAL)
                    .insets(5, 5, 0, 0);

            addTo(this, gbc);
        }

        public void addTo(Container container, GBCBuilder gbc) {
            label.addTo(container, gbc.weightX(0.0).build());
            container.add(separator, gbc.weightX(1.0).build());
        }
    }

    public static Separator createSeparator(String labelText) {
        return createSeparator(Property.constant("separator-label-text", labelText));
    }

    public static Separator createSeparator(Property<String> labelText) {
        PropertyLabel label = createLabel(labelText);
        label.setFont(Property.constant("separator-label-font", label.getComponent().getFont().deriveFont(Font.BOLD)));

        JSeparator separator = new JSeparator();
        separator.setForeground(Color.GRAY);

        return new Separator(label, separator);
    }

    public static void decorateWithValidationBorder(PropertyJComponent<?> component, Property<Boolean> isValid) {
        Property<Border> border = Property.map("isValid-border", isValid, component.hasFocus(), (valid, focused) -> {
            if (!valid)
                return INVALID_BORDER;

            return (focused ? FOCUSED_BORDER : VALID_BORDER);
        });

        component.setBorder(border);
    }

    public static void decorateWithValidationForeground(PropertyComponent<?> component, Property<Boolean> isValid) {
        Property<Color> foreground = Property.map("isValid-foreground", isValid, component.hasFocus(), (valid, focused) -> {
            if (!valid)
                return INVALID_FOREGROUND;

            return (focused ? FOCUSED_FOREGROUND : VALID_FOREGROUND);
        });

        component.setForeground(foreground);
    }

    public static JComponent buildCenteredPanel(PropertyComponent... propertyComponents) {
        Component[] components = new Component[propertyComponents.length];

        for (int i=0; i < propertyComponents.length; ++i) {
            components[i] = propertyComponents[i].getComponent();
        }

        return buildCenteredPanel(components);
    }

    public static JComponent buildCenteredPanel(Action... actions) {
        Component[] buttons = new Component[actions.length];

        for (int i=0; i < actions.length; ++i) {
            buttons[i] = new JButton(actions[i]);
        }

        return buildCenteredPanel(buttons);
    }

    public static JComponent buildCenteredPanel(Component... components) {
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

            Exception runnableExceptionValue = runnableException.getAndSet(null);
            if (runnableExceptionValue != null) {
                runException.addSuppressed(runnableExceptionValue);
            }
        }

        if (runException != null)
            throw new RuntimeException("Exception invoking runnable on EDT", runException);
    }
}
