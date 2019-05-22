package net.sothatsit.audiostream.gui.util;

import net.sothatsit.audiostream.property.Property;

import javax.swing.*;
import java.awt.*;

/**
 * A seperator that contains text and that can be controlled through Property's.
 *
 * @author Paddy Lamont
 */
public class PropertySeparator extends PropertyPanel {

    public final PropertyLabel label;
    public final JSeparator separator;

    public PropertySeparator(String text) {
        this(Property.constant("label", text));
    }

    public PropertySeparator(Property<String> text) {
        this(new PropertyLabel(text), new JSeparator());
    }

    public PropertySeparator(PropertyLabel label, JSeparator separator) {

        this.label = label;
        this.separator = separator;

        setLayout(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder()
                .anchor(GridBagConstraints.WEST)
                .fill(GridBagConstraints.HORIZONTAL)
                .insets(5, 5, 0, 0);

        add(label, gbc.weightX(0.0).build());
        add(separator, gbc.weightX(1.0).build());
    }
}