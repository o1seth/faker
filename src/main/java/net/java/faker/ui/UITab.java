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

public abstract class UITab {

    protected final Window window;
    private final String name;
    private final String translationKey;
    protected final JPanel contentPane;
    private int index;
    JTabbedPane owner;

    public UITab(final Window window, final String name) {
        this.window = window;
        this.translationKey = "tab." + name + ".name";
        this.name = I18n.get(translationKey);
        this.contentPane = new JPanel();

        this.contentPane.setLayout(null);
        this.init(this.contentPane);
    }

    public String getName() {
        return this.name;
    }

    public void add(final JTabbedPane tabbedPane) {
        index = tabbedPane.getComponentCount();
        this.owner = tabbedPane;
        tabbedPane.addTab(this.name, this.contentPane);
        I18n.link(this, this.translationKey);
    }

    public JTabbedPane getOwner() {
        return owner;
    }

    public int getIndex() {
        return index;
    }

    protected abstract void init(final JPanel contentPane);

    protected void onTabOpened() {
    }

}
