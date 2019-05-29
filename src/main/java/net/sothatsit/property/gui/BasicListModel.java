package net.sothatsit.property.gui;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A basic implementation of an AbstractListModel that
 * allows replacing the entire contents of the list.
 *
 * @author Paddy Lamont
 */
public class BasicListModel<E> extends AbstractListModel<E> {

    private List<E> contents = new ArrayList<>();

    public synchronized void replaceAll(Collection<E> elements) {
        // Find all removed elements
        for (int index = 0; index < contents.size(); ++index) {
            E element = contents.get(index);
            if (elements.contains(element))
                continue;

            contents.remove(index);
            fireIntervalRemoved(this, index, index);

            index -= 1;
        }

        // Add all new elements
        List<E> newElements = new ArrayList<>();
        for (E element : elements) {
            if (contents.contains(element))
                continue;

            newElements.add(element);
        }

        if (newElements.size() == 0)
            return;

        int fromIndex = contents.size();
        contents.addAll(newElements);
        int toIndex = contents.size() - 1;

        fireIntervalAdded(this, fromIndex, toIndex);
    }

    @Override
    public synchronized int getSize() {
        return contents.size();
    }

    @Override
    public synchronized E getElementAt(int index) {
        return contents.get(index);
    }
}
