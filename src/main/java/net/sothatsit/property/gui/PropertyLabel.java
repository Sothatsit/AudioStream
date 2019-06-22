package net.sothatsit.property.gui;

import net.sothatsit.property.Property;

import javax.swing.*;

/**
 * Controls a JLabel using Property's.
 *
 * @author Paddy Lamont
 */
public class PropertyLabel extends PropertyJComponent<JLabel> {

    private final Property<String> text;

    public PropertyLabel(String text) {
        this(Property.constant("text", text));
    }

    public PropertyLabel(Property<String> text) {
        this(new JLabel(), text); // Interested if the JComponent needs to hold a reference to these property gui wrappers
    }

    public PropertyLabel(JLabel component, Property<String> text) {
        super(component);

        this.text = text;

        text.addEDTChangeListener(e -> component.setText(text.get()));
        component.setText(text.get());
    }
}
