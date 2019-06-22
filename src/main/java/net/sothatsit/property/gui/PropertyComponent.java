package net.sothatsit.property.gui;

import net.sothatsit.property.Attribute;
import net.sothatsit.property.Property;

import java.awt.*;
import java.awt.event.*;

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

        // We add a dummy listener that does nothing to the component so that this object
        // is kept in memory always whilst the component itself is kept in memory.
        ComponentListener referenceListener = new ComponentAdapter() {
            private final PropertyComponent<C> reference = PropertyComponent.this;
        };
        component.addComponentListener(referenceListener);
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
        return enabled.readOnly();
    }

    public void setVisible(boolean visible) {
        this.visible.set(visible);
    }

    public void setVisible(Property<Boolean> visible) {
        this.visible.set(visible);
    }

    public Property<Boolean> isVisible() {
        return visible.readOnly();
    }

    public void setBackground(Color background) {
        this.background.set(background);
    }

    public void setBackground(Property<Color> background) {
        this.background.set(background);
    }

    public Property<Color> getBackground() {
        return background.readOnly();
    }

    public void setForeground(Color foreground) {
        this.foreground.set(foreground);
    }

    public void setForeground(Property<Color> foreground) {
        this.foreground.set(foreground);
    }

    public Property<Color> getForeground() {
        return foreground.readOnly();
    }

    public void setFont(Font font) {
        this.font.set(font);
    }

    public void setFont(Property<Font> font) {
        this.font.set(font);
    }

    public Property<Font> getFont() {
        return font.readOnly();
    }

    public Font deriveFont(int style) {
        return font.get().deriveFont(style);
    }

    public void setCursor(Cursor cursor) {
        this.cursor.set(cursor);
    }

    public void setCursor(Property<Cursor> cursor) {
        this.cursor.set(cursor);
    }

    public Property<Cursor> getCursor() {
        return cursor.readOnly();
    }
}
