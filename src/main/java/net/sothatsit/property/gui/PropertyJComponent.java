package net.sothatsit.property.gui;

import net.sothatsit.property.Attribute;
import net.sothatsit.property.Property;

import javax.swing.*;
import javax.swing.border.Border;

/**
 * Controls a JComponent using Property's.
 *
 * @author Paddy Lamont
 */
public class PropertyJComponent<C extends JComponent> extends PropertyComponent<C> {

    private final Attribute<Border> border;
    private final Attribute<String> tooltip;

    public PropertyJComponent(C component) {
        super(component);

        this.border = Attribute.createNullable("border", component.getBorder(), component::setBorder);
        this.tooltip = Attribute.createNullable("tooltip", component.getToolTipText(), component::setToolTipText);
    }

    public void setBorder(Border border) {
        this.border.set(border);
    }

    public void setBorder(Property<Border> border) {
        this.border.set(border);
    }

    public Property<Border> getBorder() {
        return border.readOnly();
    }

    public void setToolTipText(String tooltip) {
        this.tooltip.set(tooltip);
    }

    public void setToolTipText(Property<String> tooltip) {
        this.tooltip.set(tooltip);
    }

    public Property<String> getToolTipText() {
        return tooltip.readOnly();
    }
}
