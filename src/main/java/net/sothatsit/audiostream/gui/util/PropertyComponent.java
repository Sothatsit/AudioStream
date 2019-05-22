package net.sothatsit.audiostream.gui.util;

import net.sothatsit.audiostream.property.Attribute;
import net.sothatsit.audiostream.property.Property;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Controls a JComponent using Property's.
 *
 * @author Paddy Lamont
 */
public class PropertyComponent<C extends Component> {

    protected final C component;

    private final Property<Boolean> focused;

    private final Attribute<Boolean> enabled;
    private final Attribute<Boolean> visible;
    private final Attribute<Color> background;
    private final Attribute<Color> foreground;
    private final Attribute<Font> font;
    private final Attribute<Cursor> cursor;

    public PropertyComponent(C component) {
        this.component = component;

        this.focused = Property.create("focused", component.hasFocus());
        component.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                focused.set(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                focused.set(false);
            }
        });

        this.enabled = Attribute.createNonNull("enabled", component.isEnabled(), component::setEnabled);
        this.visible = Attribute.createNonNull("visible", component.isVisible(), component::setVisible);
        this.background = Attribute.createNonNull("background", component.getBackground(), component::setBackground);
        this.foreground = Attribute.createNonNull("foreground", component.getForeground(), component::setForeground);
        this.font = Attribute.createNonNull("font", component.getFont(), component::setFont);
        this.cursor = Attribute.createNonNull("cursor", component.getCursor(), component::setCursor);
    }

    public C getComponent() {
        return component;
    }

    public void addTo(Container container) {
        container.add(component);
    }

    public void addTo(Container container, Object constraints) {
        container.add(component, constraints);
    }

    public Property<Boolean> hasFocus() {
        return focused.readOnly();
    }

    public void setSize(int width, int height) {
        component.setSize(width, height);
    }

    public void setSize(Dimension dimension) {
        component.setSize(dimension);
    }

    public void setPreferredSize(int width, int height) {
        setPreferredSize(new Dimension(width, height));
    }

    public void setPreferredSize(Dimension dimension) {
        component.setPreferredSize(dimension);
    }

    public void setMaximumSize(int width, int height) {
        setMaximumSize(new Dimension(width, height));
    }

    public void setMaximumSize(Dimension dimension) {
        component.setMaximumSize(dimension);
    }

    public void setMinimumSize(int width, int height) {
        setMinimumSize(new Dimension(width, height));
    }

    public void setMinimumSize(Dimension dimension) {
        component.setMinimumSize(dimension);
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public void setEnabled(Property<Boolean> enabled) {
        this.enabled.set(enabled);
    }

    public Property<Boolean> isEnabled() {
        return enabled;
    }

    public void setVisible(boolean visible) {
        this.visible.set(visible);
    }

    public void setVisible(Property<Boolean> visible) {
        this.visible.set(visible);
    }

    public Property<Boolean> isVisible() {
        return visible;
    }

    public void setBackground(Color background) {
        this.background.set(background);
    }

    public void setBackground(Property<Color> background) {
        this.background.set(background);
    }

    public Property<Color> getBackground() {
        return background;
    }

    public void setForeground(Color foreground) {
        this.foreground.set(foreground);
    }

    public void setForeground(Property<Color> foreground) {
        this.foreground.set(foreground);
    }

    public Property<Color> getForeground() {
        return foreground;
    }

    public void setFont(Font font) {
        this.font.set(font);
    }

    public void setFont(Property<Font> font) {
        this.font.set(font);
    }

    public Property<Font> getFont() {
        return font;
    }

    public void setCursor(Cursor cursor) {
        this.cursor.set(cursor);
    }

    public void setCursor(Property<Cursor> cursor) {
        this.cursor.set(cursor);
    }

    public Property<Cursor> getCursor() {
        return cursor;
    }
}
