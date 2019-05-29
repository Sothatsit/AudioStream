package net.sothatsit.property.gui;

import net.sothatsit.property.Property;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Controls a JButton using Property's.
 *
 * @author Paddy Lamont
 */
public class PropertyButton extends PropertyJComponent<JButton> {

    public PropertyButton(String text, Runnable onClick) {
        this(Property.constant("text", text), onClick);
    }

    public PropertyButton(Property<String> text, Runnable onClick) {
        this(new PropertyButtonAction(text, onClick));
    }

    public PropertyButton(Action action) {
        this(new JButton(action));
    }

    public PropertyButton(JButton component) {
        super(component);
    }

    private static class PropertyButtonAction extends AbstractAction {

        private final Property<String> text;
        private final Runnable onClick;

        public PropertyButtonAction(Property<String> text, Runnable onClick) {
            this.text = text;
            this.onClick = onClick;

            text.addEDTChangeListener(event -> updateName());
            updateName();
        }

        private void updateName() {
            putValue(Action.NAME, text.get());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            onClick.run();
        }
    }
}
