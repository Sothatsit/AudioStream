package net.sothatsit.property.awt;

import net.sothatsit.property.ListProperty;
import net.sothatsit.property.Property;
import net.sothatsit.function.Unchecked;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.function.Function;

/**
 * Controls a JComboBox using Property's.
 *
 * @author Paddy Lamont
 */
public class PropertyComboBox<E> extends PropertyJComponent<JComboBox<E>> {

    private final ListProperty<E> availableValues;
    private final Property<E> selectedValue;

    public PropertyComboBox(E[] availableValues,
                            Property<E> selectedValue) {

        this(ListProperty.constant("availableValues", availableValues), selectedValue);
    }

    public PropertyComboBox(E[] availableValues,
                            Property<E> selectedValue,
                            Function<E, Object> toDisplayValueFn) {

        this(ListProperty.constant("availableValues", availableValues), selectedValue, toDisplayValueFn);
    }

    public PropertyComboBox(E[] availableValues,
                            Property<E> selectedValue,
                            ListCellRenderer<? super E> renderer) {

        this(ListProperty.constant("availableValues", availableValues), selectedValue, renderer);
    }

    public PropertyComboBox(List<E> availableValues,
                            Property<E> selectedValue) {

        this(ListProperty.constant("availableValues", availableValues), selectedValue);
    }

    public PropertyComboBox(List<E> availableValues,
                            Property<E> selectedValue,
                            Function<E, Object> toDisplayValueFn) {

        this(ListProperty.constant("availableValues", availableValues), selectedValue, toDisplayValueFn);
    }

    public PropertyComboBox(List<E> availableValues,
                            Property<E> selectedValue,
                            ListCellRenderer<? super E> renderer) {

        this(ListProperty.constant("availableValues", availableValues), selectedValue, renderer);
    }

    public PropertyComboBox(ListProperty<E> availableValues,
                            Property<E> selectedValue) {

        this(availableValues, selectedValue, getDefaultCellRenderer());
    }

    public PropertyComboBox(ListProperty<E> availableValues,
                            Property<E> selectedValue,
                            Function<E, Object> toDisplayValueFn) {

        this(availableValues, selectedValue, createMappedRenderer(toDisplayValueFn));
    }

    public PropertyComboBox(ListProperty<E> availableValues,
                            Property<E> selectedValue,
                            ListCellRenderer<? super E> renderer) {

        this(new JComboBox<>(), availableValues, selectedValue, renderer);
    }

    public PropertyComboBox(JComboBox<E> component,
                            E[] availableValues,
                            Property<E> selectedValue) {

        this(component, ListProperty.constant("availableValues", availableValues), selectedValue);
    }

    public PropertyComboBox(JComboBox<E> component,
                            E[] availableValues,
                            Property<E> selectedValue,
                            Function<E, Object> toDisplayValueFn) {

        this(component, ListProperty.constant("availableValues", availableValues), selectedValue, toDisplayValueFn);
    }

    public PropertyComboBox(JComboBox<E> component,
                            E[] availableValues,
                            Property<E> selectedValue,
                            ListCellRenderer<? super E> renderer) {

        this(component, ListProperty.constant("availableValues", availableValues), selectedValue, renderer);
    }

    public PropertyComboBox(JComboBox<E> component,
                            List<E> availableValues,
                            Property<E> selectedValue) {

        this(component, ListProperty.constant("availableValues", availableValues), selectedValue);
    }

    public PropertyComboBox(JComboBox<E> component,
                            List<E> availableValues,
                            Property<E> selectedValue,
                            Function<E, Object> toDisplayValueFn) {

        this(component, ListProperty.constant("availableValues", availableValues), selectedValue, toDisplayValueFn);
    }

    public PropertyComboBox(JComboBox<E> component,
                            List<E> availableValues,
                            Property<E> selectedValue,
                            ListCellRenderer<? super E> renderer) {

        this(component, ListProperty.constant("availableValues", availableValues), selectedValue, renderer);
    }

    public PropertyComboBox(JComboBox<E> component,
                            ListProperty<E> availableValues,
                            Property<E> selectedValue) {

        this(component, availableValues, selectedValue, getDefaultCellRenderer());
    }

    public PropertyComboBox(JComboBox<E> component,
                            ListProperty<E> availableValues,
                            Property<E> selectedValue,
                            Function<E, Object> toDisplayValueFn) {

        this(component, availableValues, selectedValue, createMappedRenderer(toDisplayValueFn));
    }

    public PropertyComboBox(JComboBox<E> component,
                            ListProperty<E> availableValues,
                            Property<E> selectedValue,
                            ListCellRenderer<? super E> renderer) {

        super(component);

        this.availableValues = availableValues;
        this.selectedValue = selectedValue;

        component.setRenderer(renderer);

        component.addItemListener(this::updateSelectedItem);
        selectedValue.addEDTChangeListener(e -> component.setSelectedItem(selectedValue.get()));
        availableValues.addEDTChangeListener(this::updateAvailableValues);
        updateAvailableValues(null);
    }

    private void updateSelectedItem(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
            E selection = Unchecked.cast(event.getItem());
            selectedValue.set(selection);
        }
    }

    private boolean applyAvailableValuesChangeEvent(ChangeEvent event) {
        if (event == null)
            return false;

        if (event instanceof ListProperty.ListManyChangeEvent) {
            ListProperty.ListManyChangeEvent<E> manyEvent = Unchecked.cast(event);

            boolean success = true;
            for (ChangeEvent subEvent : manyEvent.getChanges()) {
                success = applyAvailableValuesChangeEvent(subEvent);
                if (!success)
                    break;
            }

            return success;
        }

        if (event instanceof ListProperty.ListAddChangeEvent) {
            ListProperty.ListAddChangeEvent<E> addEvent = Unchecked.cast(event);

            if (addEvent.getIndex() > component.getItemCount())
                return false;

            if (addEvent.getIndex() == component.getItemCount()) {
                component.addItem(addEvent.getValue());
            } else {
                component.insertItemAt(addEvent.getValue(), addEvent.getIndex());
            }

            return true;
        }

        if (event instanceof ListProperty.ListRemoveChangeEvent) {
            ListProperty.ListRemoveChangeEvent<E> removeEvent = Unchecked.cast(event);

            if (removeEvent.getIndex() >= component.getItemCount())
                return false;
            if (component.getItemAt(removeEvent.getIndex()) != removeEvent.getValue())
                return false;

            component.removeItemAt(removeEvent.getIndex());
            return true;
        }

        return false;
    }

    private boolean doAvailableItemsMatch(List<E> elements) {
        if (elements.size() != component.getItemCount())
            return false;

        for (int index = 0; index < component.getItemCount(); ++index) {
            if (component.getItemAt(index) != elements.get(index))
                return false;
        }

        return true;
    }

    private void updateAvailableValues(ChangeEvent event) {
        List<E> newElements = availableValues.get();

        // Attempt to apply the change event to this combo box, as
        // often it can help us avoid unnecessarily removing elements
        applyAvailableValuesChangeEvent(event);

        // Check if the contents of this combo box and availableValues are consistent
        if (doAvailableItemsMatch(newElements))
            return;

        // If they are not, forcefully reset all the elements in the combo box
        Object selected = selectedValue.get();

        component.removeAllItems();
        newElements.forEach(component::addItem);

        if (selected != null && newElements.contains(selected)) {
            component.setSelectedItem(selected);
        }

        // Everything should now match up
        assert doAvailableItemsMatch(newElements);
    }

    public static ListCellRenderer<Object> getDefaultCellRenderer() {
        return Unchecked.cast(new BasicComboBoxRenderer());
    }

    public static <E> MappedListCellRenderer<E, Object> createMappedRenderer(Function<E, Object> toDisplayValue) {
        return new MappedListCellRenderer<>(getDefaultCellRenderer(), toDisplayValue);
    }
}
