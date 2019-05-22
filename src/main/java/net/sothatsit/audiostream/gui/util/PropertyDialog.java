package net.sothatsit.audiostream.gui.util;

import net.sothatsit.audiostream.property.Attribute;
import net.sothatsit.audiostream.property.Property;

import javax.swing.*;
import java.awt.*;

/**
 * Controls a Container using Property's.
 *
 * @author Paddy Lamont
 */
public class PropertyDialog extends PropertyComponent<JDialog> {

    // Purely done to have a clearer name than component
    protected final JDialog dialog;

    private final Attribute<String> title;

    public PropertyDialog() {
        this(new JDialog());
    }

    public PropertyDialog(Window parent) {
        this(new JDialog(parent));
    }

    public PropertyDialog(Window parent, String title) {
        this(new JDialog(parent, title));
    }

    public PropertyDialog(JDialog dialog) {
        super(dialog);

        this.dialog = dialog;

        this.title = Attribute.createNonNull("title", dialog.getTitle(), dialog::setTitle);
    }

    public void setLayout(LayoutManager layoutManager) {
        dialog.setLayout(layoutManager);
    }

    public Property<String> getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public void setTitle(Property<String> title) {
        this.title.set(title);
    }

    public void add(String text) {
        add(new JLabel(text));
    }

    public void add(PropertyComponent<?> component) {
        add(component.getComponent());
    }

    public void add(Component component) {
        dialog.add(component);
    }

    public void add(String text, Object constraints) {
        add(new JLabel(text), constraints);
    }

    public void add(PropertyComponent<?> component, Object constraints) {
        add(component.getComponent(), constraints);
    }

    public void add(Component component, Object constraints) {
        dialog.add(component, constraints);
    }
}
