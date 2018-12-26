package net.sothatsit.audiostream.gui;

import java.awt.*;

/**
 * A class used to build GridBagConstraints.
 *
 * @author Paddy Lamont
 */
public class GBCBuilder {

    private final GridLocation location;
    private final GridBagConstraints constraints;

    public GBCBuilder() {
        this(new GridLocation(), new GridBagConstraints());
    }

    private GBCBuilder(GridLocation location, GridBagConstraints constraints) {
        this.location = location;
        this.constraints = constraints;
    }

    private GridBagConstraints cloneConstraints() {
        return (GridBagConstraints) constraints.clone();
    }

    public void nextColumn() {
        nextColumn(1);
    }

    public void nextColumn(int width) {
        location.x += width;
    }

    public void nextRow() {
        location.x = 0;
        location.y += 1;
    }

    public GBCBuilder anchor(int anchor) {
        GridBagConstraints constraints = cloneConstraints();
        constraints.anchor = anchor;
        return new GBCBuilder(location, constraints);
    }

    public GBCBuilder fill(int fill) {
        GridBagConstraints constraints = cloneConstraints();
        constraints.fill = fill;
        return new GBCBuilder(location, constraints);
    }

    public GBCBuilder pad(int x, int y) {
        GridBagConstraints constraints = cloneConstraints();
        constraints.ipadx = x;
        constraints.ipady = y;
        return new GBCBuilder(location, constraints);
    }

    public GBCBuilder padX(int x) {
        GridBagConstraints constraints = cloneConstraints();
        constraints.ipadx = x;
        return new GBCBuilder(location, constraints);
    }

    public GBCBuilder padY(int y) {
        GridBagConstraints constraints = cloneConstraints();
        constraints.ipady = y;
        return new GBCBuilder(location, constraints);
    }

    public GBCBuilder insets(int left, int right, int top, int bottom) {
        GridBagConstraints constraints = cloneConstraints();
        constraints.insets = new Insets(top, left, bottom, right);
        return new GBCBuilder(location, constraints);
    }

    public GBCBuilder weight(double x, double y) {
        GridBagConstraints constraints = cloneConstraints();
        constraints.weightx = x;
        constraints.weighty = y;
        return new GBCBuilder(location, constraints);
    }

    public GBCBuilder weightX(double x) {
        GridBagConstraints constraints = cloneConstraints();
        constraints.weightx = x;
        return new GBCBuilder(location, constraints);
    }

    public GBCBuilder weightY(double y) {
        GridBagConstraints constraints = cloneConstraints();
        constraints.weighty = y;
        return new GBCBuilder(location, constraints);
    }

    public GridBagConstraints build() {
        return build(1);
    }

    public GridBagConstraints build(int width) {
        GridBagConstraints constraints = cloneConstraints();
        constraints.gridx = location.x;
        constraints.gridy = location.y;
        constraints.gridwidth = width;
        constraints.gridheight = 1;
        nextColumn(width);
        return constraints;
    }

    private static class GridLocation {
        public int x;
        public int y;
    }
}
