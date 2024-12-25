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

package net.java.faker.ui.elements;

import net.java.faker.ui.Window;
import net.java.faker.util.network.NetworkInterface;
import net.java.faker.util.network.NetworkUtil;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

public class NetworkAdapterComboBox extends JComboBox<NetworkInterface> {
    ActionListener selectListener;
    private NetworkInterface lastSelectedAdapter;
    protected boolean valueCanBeNull;

    public NetworkAdapterComboBox(Consumer<NetworkInterface> onChange) {
        super(new DefaultComboBoxModel<>());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isPopupVisible() && e.getButton() == MouseEvent.BUTTON1 && isEnabled()) {
                    fillAdapters(null);
                }
            }
        });

        selectListener = e -> {
            if (getSelectedItem() instanceof NetworkInterface ni) {
                if (!ni.equalsNameAndMac(lastSelectedAdapter)) {
                    if (onChange != null) {
                        onChange.accept(ni);
                    }
                }
                lastSelectedAdapter = ni;
            }
        };
        addActionListener(selectListener);
    }

    public void setValueCanBeNull(boolean valueCanBeNull) {
        this.valueCanBeNull = valueCanBeNull;
    }

    public boolean isValueCanBeNull() {
        return valueCanBeNull;
    }

    public void fillAdapters(Consumer<List<NetworkInterface>> event) {
        fillAdapters0(event);
    }

    private long lastFillTime;

    private void fillAdapters0(Consumer<List<NetworkInterface>> event) {
        if (System.currentTimeMillis() - lastFillTime > 5000) {
            lastFillTime = System.currentTimeMillis();
        } else {
            return;
        }
        new Thread(() -> {
            List<NetworkInterface> interfaces = NetworkUtil.getNetworkInterfaces();

            if (event != null) {
                //wait for constructor ends and window to be fully initialized
                Window.getInstance();
            }
            SwingUtilities.invokeLater(() -> {
                if (event == null) {
                    removeActionListener(selectListener);
                }

                NetworkInterface selected = (NetworkInterface) getSelectedItem();

                DefaultComboBoxModel<NetworkInterface> model = (DefaultComboBoxModel<NetworkInterface>) getModel();
                int wasCount = model.getSize();
                model.removeAllElements();
                if (isValueCanBeNull()) {
                    model.addElement(NetworkInterface.NULL);
                }

                model.addAll(interfaces);

                if (selected != null) {
                    for (NetworkInterface ni : interfaces) {
                        if (ni.equalsNameAndMac(selected)) {
                            setSelectedItem(ni);
                            break;
                        }
                    }
                }
                if (event != null) {
                    event.accept(interfaces);
                } else {
                    addActionListener(selectListener);
                }

                if (model.getSize() != wasCount) {
                    if (isShowing() && isPopupVisible()) {
                        try {
                            hidePopup();
                            showPopup();
                        } catch (Throwable ignored) {
                        }
                    }
                }
            });
        }).start();
    }

}
