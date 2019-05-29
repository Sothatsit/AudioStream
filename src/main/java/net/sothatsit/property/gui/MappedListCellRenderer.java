package net.sothatsit.property.gui;

import net.sothatsit.property.Unchecked;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

/**
 * A ListCellRenderer that performs a value conversion before delegating to another ListCellRenderer.
 *
 * @author Paddy Lamont
 */
class MappedListCellRenderer<E, D> implements ListCellRenderer<E> {

    private final ListCellRenderer<D> renderer;
    private final Function<E, D> toDisplayValueFn;

    public MappedListCellRenderer(ListCellRenderer<D> renderer,
                                  Function<E, D> toDisplayValueFn) {

        this.renderer = renderer;
        this.toDisplayValueFn = toDisplayValueFn;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends E> list,
                                                  E value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {

        D mappedValue = toDisplayValueFn.apply(value);

        return renderer.getListCellRendererComponent(
                Unchecked.cast(list),
                mappedValue,
                index,
                isSelected,
                cellHasFocus
        );
    }
}
