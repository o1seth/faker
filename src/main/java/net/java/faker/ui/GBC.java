/*
 * This file is part of faker - https://github.com/o1seth/faker
 * Copyright (C) 2024 o1seth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.java.faker.ui;

import javax.swing.*;
import java.awt.*;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Supplier;

//https://github.com/Lenni0451/commons/blob/main/commons-swing/src/main/java/net/lenni0451/commons/swing/GBC.java
public class GBC {//Size
    public static final int RELATIVE = GridBagConstraints.RELATIVE;
    public static final int REMAINDER = GridBagConstraints.REMAINDER;
    //Fill
    public static final int HORIZONTAL = GridBagConstraints.HORIZONTAL;
    public static final int NONE = GridBagConstraints.NONE;
    public static final int BOTH = GridBagConstraints.BOTH;
    public static final int VERTICAL = GridBagConstraints.VERTICAL;
    //Anchor
    public static final int CENTER = GridBagConstraints.CENTER;
    public static final int NORTH = GridBagConstraints.NORTH;
    public static final int NORTHEAST = GridBagConstraints.NORTHEAST;
    public static final int EAST = GridBagConstraints.EAST;
    public static final int SOUTHEAST = GridBagConstraints.SOUTHEAST;
    public static final int SOUTH = GridBagConstraints.SOUTH;
    public static final int SOUTHWEST = GridBagConstraints.SOUTHWEST;
    public static final int WEST = GridBagConstraints.WEST;
    public static final int NORTHWEST = GridBagConstraints.NORTHWEST;
    public static final int PAGE_START = GridBagConstraints.PAGE_START;
    public static final int PAGE_END = GridBagConstraints.PAGE_END;
    public static final int LINE_START = GridBagConstraints.LINE_START;
    public static final int LINE_END = GridBagConstraints.LINE_END;
    public static final int FIRST_LINE_START = GridBagConstraints.FIRST_LINE_START;
    public static final int FIRST_LINE_END = GridBagConstraints.FIRST_LINE_END;
    public static final int LAST_LINE_START = GridBagConstraints.LAST_LINE_START;
    public static final int LAST_LINE_END = GridBagConstraints.LAST_LINE_END;
    public static final int BASELINE = GridBagConstraints.BASELINE;
    public static final int BASELINE_LEADING = GridBagConstraints.BASELINE_LEADING;
    public static final int BASELINE_TRAILING = GridBagConstraints.BASELINE_TRAILING;
    public static final int ABOVE_BASELINE = GridBagConstraints.ABOVE_BASELINE;
    public static final int ABOVE_BASELINE_LEADING = GridBagConstraints.ABOVE_BASELINE_LEADING;
    public static final int ABOVE_BASELINE_TRAILING = GridBagConstraints.ABOVE_BASELINE_TRAILING;
    public static final int BELOW_BASELINE = GridBagConstraints.BELOW_BASELINE;
    public static final int BELOW_BASELINE_LEADING = GridBagConstraints.BELOW_BASELINE_LEADING;
    public static final int BELOW_BASELINE_TRAILING = GridBagConstraints.BELOW_BASELINE_TRAILING;

    /**
     * Create a new GBC with the parent set to null.<br>
     * The {@code add} methods will throw an {@link IllegalStateException} if used.
     *
     * @return The new GBC
     */
    public static GBC create() {
        return create(null);
    }

    /**
     * Create a new GBC with the parent set to the specified container.
     *
     * @param parent The parent container
     * @return The new GBC
     */
    public static GBC create(final Container parent) {
        return new GBC(parent);
    }

    /**
     * Get the current {@code gridx} value for the specified container.<br>
     * This will return the highest {@code gridx} for the last {@code gridy}.
     *
     * @param container The container
     * @return The current {@code gridx} value or empty if there are no components
     */
    public static OptionalInt currentGridX(final Container container) {
        GridBagLayout layout = (GridBagLayout) container.getLayout();
        Integer gridx = null;
        Integer gridy = null;
        for (Component component : container.getComponents()) {
            GridBagConstraints gbc = layout.getConstraints(component);
            if (gridy == null || gbc.gridy > gridy) {
                gridx = gbc.gridx;
                gridy = gbc.gridy;
            } else if (gbc.gridy == gridy && gbc.gridx > gridx) {
                gridx = gbc.gridx;
            }
        }
        if (gridx == null) return OptionalInt.empty();
        return OptionalInt.of(gridx);
    }

    /**
     * Get the current {@code gridy} value for the specified container.
     *
     * @param container The container
     * @return The current {@code gridy} value or empty if there are no components
     */
    public static OptionalInt currentGridY(final Container container) {
        GridBagLayout layout = (GridBagLayout) container.getLayout();
        Integer gridy = null;
        for (Component component : container.getComponents()) {
            GridBagConstraints gbc = layout.getConstraints(component);
            if (gridy == null || gbc.gridy > gridy) gridy = gbc.gridy;
        }
        if (gridy == null) return OptionalInt.empty();
        return OptionalInt.of(gridy);
    }

    /**
     * Add a filler component that will fill the remaining horizontal space.<br>
     * This will align all components to the top of the container.<br>
     * This will return the new {@code gridy} value.
     *
     * @param parent The parent container
     * @return The new {@code gridy} value
     * @see #currentGridY(Container)
     * @see Box#createHorizontalGlue()
     */
    public static int fillVerticalSpace(final Container parent) {
        int gridy = currentGridY(parent).orElse(-1);
        fillVerticalSpace(parent, ++gridy);
        return gridy + 1;
    }

    /**
     * Add a filler component that will fill the remaining horizontal space.<br>
     * This will align all components to the top of the container.
     *
     * @param parent The parent container
     * @param gridy  The {@code gridy} value
     * @see #currentGridY(Container)
     * @see Box#createHorizontalGlue()
     */
    public static void fillVerticalSpace(final Container parent, final int gridy) {
        GBC.create(parent).gridy(gridy).anchor(GridBagConstraints.WEST).weighty(1).fill(GridBagConstraints.HORIZONTAL).add(Box.createVerticalGlue());
    }


    private final Container parent;
    private final GridBagConstraints constraints;

    private GBC(final Container parent) {
        this.parent = parent;
        this.constraints = new GridBagConstraints();
    }

    /**
     * Set the {@code gridx} and {@code gridy} of the constraints to the next column.
     *
     * @return This GBC
     */
    public GBC nextColumn() {
        this.constraints.gridx = currentGridX(this.parent).orElse(-1) + 1;
        this.constraints.gridy = currentGridY(this.parent).orElse(0);
        return this;
    }

    /**
     * Set the {@code gridx} and {@code gridy} of the constraints to the next row.
     *
     * @return This GBC
     */
    public GBC nextRow() {
        this.constraints.gridx = 0;
        this.constraints.gridy = currentGridY(this.parent).orElse(-1) + 1;
        return this;
    }

    /**
     * Set the {@code gridx} of the constraints.
     *
     * @param gridx The gridx
     * @return This GBC
     * @see GridBagConstraints#gridx
     */
    public GBC gridx(final int gridx) {
        this.constraints.gridx = gridx;
        return this;
    }

    /**
     * Set the {@code gridy} of the constraints.
     *
     * @param gridy The gridy
     * @return This GBC
     * @see GridBagConstraints#gridy
     */
    public GBC gridy(final int gridy) {
        this.constraints.gridy = gridy;
        return this;
    }

    /**
     * Set the {@code gridx} and {@code gridy} of the constraints.
     *
     * @param gridx The gridx
     * @param gridy The gridy
     * @return This GBC
     * @see GridBagConstraints#gridx
     * @see GridBagConstraints#gridy
     */
    public GBC grid(final int gridx, final int gridy) {
        this.constraints.gridx = gridx;
        this.constraints.gridy = gridy;
        return this;
    }

    /**
     * Set the {@code gridwidth} of the constraints.
     *
     * @param gridwidth The gridwidth
     * @return This GBC
     * @see GridBagConstraints#gridwidth
     */
    public GBC width(final int gridwidth) {
        this.constraints.gridwidth = gridwidth;
        return this;
    }

    /**
     * Set the {@code gridheight} of the constraints.
     *
     * @param gridheight The gridheight
     * @return This GBC
     * @see GridBagConstraints#gridheight
     */
    public GBC height(final int gridheight) {
        this.constraints.gridheight = gridheight;
        return this;
    }

    /**
     * Set the {@code gridwidth} and {@code gridheight} of the constraints.
     *
     * @param gridwidth  The gridwidth
     * @param gridheight The gridheight
     * @return This GBC
     * @see GridBagConstraints#gridwidth
     * @see GridBagConstraints#gridheight
     */
    public GBC size(final int gridwidth, final int gridheight) {
        this.constraints.gridwidth = gridwidth;
        this.constraints.gridheight = gridheight;
        return this;
    }

    /**
     * Set the {@code weightx} of the constraints.
     *
     * @param weightx The weightx
     * @return This GBC
     * @see GridBagConstraints#weightx
     */
    public GBC weightx(final double weightx) {
        this.constraints.weightx = weightx;
        return this;
    }

    /**
     * Set the {@code weighty} of the constraints.
     *
     * @param weighty The weighty
     * @return This GBC
     * @see GridBagConstraints#weighty
     */
    public GBC weighty(final double weighty) {
        this.constraints.weighty = weighty;
        return this;
    }

    /**
     * Set the {@code weightx} and {@code weighty} of the constraints.
     *
     * @param weightx The weightx
     * @param weighty The weighty
     * @return This GBC
     * @see GridBagConstraints#weightx
     * @see GridBagConstraints#weighty
     */
    public GBC weight(final double weightx, final double weighty) {
        this.constraints.weightx = weightx;
        this.constraints.weighty = weighty;
        return this;
    }

    /**
     * Set the {@code anchor} of the constraints.
     *
     * @param anchor The anchor
     * @return This GBC
     * @see GridBagConstraints#anchor
     */
    public GBC anchor(final int anchor) {
        this.constraints.anchor = anchor;
        return this;
    }

    /**
     * Set the {@code fill} of the constraints.
     *
     * @param fill The fill
     * @return This GBC
     * @see GridBagConstraints#fill
     */
    public GBC fill(final int fill) {
        this.constraints.fill = fill;
        return this;
    }

    /**
     * Set the {@code insets} of the constraints.
     *
     * @param insets The insets
     * @return This GBC
     * @see GridBagConstraints#insets
     */
    public GBC insets(final Insets insets) {
        this.constraints.insets = insets;
        return this;
    }

    /**
     * Set the {@code insets} of the constraints to the same value for all sides.
     *
     * @param all The value for all sides
     * @return This GBC
     * @see GridBagConstraints#insets
     */
    public GBC insets(final int all) {
        this.constraints.insets = new Insets(all, all, all, all);
        return this;
    }

    /**
     * Set the {@code insets} of the constraints to the same value for the top and bottom and the same value for the left and right.
     *
     * @param topBottom The value for the top and bottom
     * @param leftRight The value for the left and right
     * @return This GBC
     * @see GridBagConstraints#insets
     */
    public GBC insets(final int topBottom, final int leftRight) {
        this.constraints.insets = new Insets(topBottom, leftRight, topBottom, leftRight);
        return this;
    }

    /**
     * Set the {@code insets} of the constraints.
     *
     * @param top    The value for the top
     * @param left   The value for the left
     * @param bottom The value for the bottom
     * @param right  The value for the right
     * @return This GBC
     * @see GridBagConstraints#insets
     */
    public GBC insets(final int top, final int left, final int bottom, final int right) {
        this.constraints.insets = new Insets(top, left, bottom, right);
        return this;
    }

    /**
     * Set the {@code ipadx} of the constraints.
     *
     * @param ipadx The ipadx
     * @return This GBC
     * @see GridBagConstraints#ipadx
     */
    public GBC ipadx(final int ipadx) {
        this.constraints.ipadx = ipadx;
        return this;
    }

    /**
     * Set the {@code ipady} of the constraints.
     *
     * @param ipady The ipady
     * @return This GBC
     * @see GridBagConstraints#ipady
     */
    public GBC ipady(final int ipady) {
        this.constraints.ipady = ipady;
        return this;
    }

    /**
     * Set the {@code ipadx} and {@code ipady} of the constraints.
     *
     * @param ipadx The ipadx
     * @param ipady The ipady
     * @return This GBC
     * @see GridBagConstraints#ipadx
     * @see GridBagConstraints#ipady
     */
    public GBC ipad(final int ipadx, final int ipady) {
        this.constraints.ipadx = ipadx;
        this.constraints.ipady = ipady;
        return this;
    }

    /**
     * Add the specified component to the parent container with the constraints.
     *
     * @param component The component to add
     * @return This GBC
     */
    public GBC add(final Component component) {
        if (this.parent == null) throw new IllegalStateException("Cannot add component without parent");
        this.parent.add(component, this.constraints);
        return this;
    }

    /**
     * Add the specified component to the parent container with the constraints.<br>
     * The runnable will be called before the component is added to the parent container.
     *
     * @param component The component to add
     * @param runnable  The runnable to configure the component
     * @return This GBC
     */
    public GBC add(final Component component, final Runnable runnable) {
        if (this.parent == null) throw new IllegalStateException("Cannot add component without parent");
        runnable.run();
        this.parent.add(component, this.constraints);
        return this;
    }

    /**
     * Add the specified component to the parent container with the constraints.<br>
     * The consumer will be called before the component is added to the parent container.
     *
     * @param component The component to add
     * @param consumer  The consumer to configure the component
     * @param <C>       The type of the component
     * @return This GBC
     */
    public <C extends Component> GBC add(final C component, final Consumer<C> consumer) {
        if (this.parent == null) throw new IllegalStateException("Cannot add component without parent");
        consumer.accept(component);
        this.parent.add(component, this.constraints);
        return this;
    }

    /**
     * Add the component created by the supplier to the parent container with the constraints.
     *
     * @param supplier The supplier to create the component
     * @return This GBC
     */
    public GBC add(final Supplier<Component> supplier) {
        if (this.parent == null) throw new IllegalStateException("Cannot add component without parent");
        this.parent.add(supplier.get(), this.constraints);
        return this;
    }

    /**
     * @return The constraints
     */
    public GridBagConstraints get() {
        return this.constraints;
    }

}
