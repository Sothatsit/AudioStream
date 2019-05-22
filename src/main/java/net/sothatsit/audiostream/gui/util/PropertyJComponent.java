package net.sothatsit.audiostream.gui.util;

import net.sothatsit.audiostream.property.Attribute;
import net.sothatsit.audiostream.property.Property;

import javax.swing.*;
import javax.swing.border.Border;

/**
 * Controls a JComponent using Property's.
 *
 * @author Paddy Lamont
 */
public class PropertyJComponent<C extends JComponent> extends PropertyComponent<C> {

    private final Attribute<Border> border;

    public PropertyJComponent(C component) {
        super(component);

        this.border = Attribute.createNullable("border", component.getBorder(), component::setBorder);
    }

    public void setBorder(Border border) {
        this.border.set(border);
    }

    public void setBorder(Property<Border> border) {
        this.border.set(border);
    }

    public Property<Border> getBorder() {
        return border;
    }
}
