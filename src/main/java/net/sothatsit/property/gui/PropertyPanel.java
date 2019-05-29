package net.sothatsit.property.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Controls a JPanel using Property's.
 *
 * @author Paddy Lamont
 */
public class PropertyPanel extends PropertyJComponent<JPanel> {

    // Purely done to have a clearer name than component
    protected final JPanel panel;

    public PropertyPanel() {
        this(new JPanel());
    }

    public PropertyPanel(JPanel panel) {
        super(panel);

        this.panel = panel;
    }

    public void setLayout(LayoutManager layoutManager) {
        panel.setLayout(layoutManager);
    }

    public void add(String text) {
        add(new JLabel(text));
    }

    public void add(PropertyComponent<?> component) {
        add(component.getComponent());
    }

    public void add(Component component) {
        panel.add(component);
    }

    public void add(String text, Object constraints) {
        add(new JLabel(text), constraints);
    }

    public void add(PropertyComponent<?> component, Object constraints) {
        add(component.getComponent(), constraints);
    }

    public void add(Component component, Object constraints) {
        panel.add(component, constraints);
    }
}
