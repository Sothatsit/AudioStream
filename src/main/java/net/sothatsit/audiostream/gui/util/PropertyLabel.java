package net.sothatsit.audiostream.gui.util;

import net.sothatsit.audiostream.property.Property;

import javax.swing.*;

/**
 * Controls a JLabel using Property's.
 *
 * @author Paddy Lamont
 */
public class PropertyLabel extends PropertyJComponent<JLabel> {

    private final Property<String> text;

    public PropertyLabel(JLabel component, Property<String> text) {
        super(component);

        this.text = text;

        text.addEDTChangeListener(e -> component.setText(text.get()));
        component.setText(text.get());
    }
}
