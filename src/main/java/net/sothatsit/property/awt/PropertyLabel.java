package net.sothatsit.property.awt;

import net.sothatsit.property.Attribute;
import net.sothatsit.property.Property;

import javax.swing.*;

/**
 * Controls a JLabel using Property's.
 *
 * @author Paddy Lamont
 */
public class PropertyLabel extends PropertyJComponent<JLabel> {

    // TODO : Really these should be consistent with how other attributes are set in other Property components
    public final Attribute<String> text;
    public final Attribute<Icon> icon;

    public PropertyLabel() {
        this(new JLabel(), null, null);
    }

    public PropertyLabel(String text) {
        this(Property.constant("text", text));
    }

    public PropertyLabel(Property<String> text) {
        this(new JLabel(), text, null);
    }

    public PropertyLabel(String text, Property<Icon> icon) {
        this(new JLabel(), Property.constant("text", text), icon);
    }

    public PropertyLabel(Property<String> text, Property<Icon> icon) {
        this(new JLabel(), text, icon);
    }

    public PropertyLabel(JLabel component, Property<String> text, Property<Icon> icon) {
        super(component);

        this.text = Attribute.createNullable("text", component.getText(), component::setText);
        this.icon = Attribute.createNullable("icon", component.getIcon(), component::setIcon);

        this.text.set(text);
        this.icon.set(icon);

        if (text != null) component.setText(text.get());
        if (icon != null) component.setIcon(icon.get());
    }
}
