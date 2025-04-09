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

package net.java.faker.ui.tab;

import net.java.faker.Proxy;
import net.java.faker.WinRedirect;
import net.java.faker.ui.GBC;
import net.java.faker.ui.I18n;
import net.java.faker.ui.UITab;
import net.java.faker.ui.Window;
import net.java.faker.ui.elements.NetworkAdapterComboBox;
import net.java.faker.util.network.NetworkInterface;
import net.java.faker.util.network.NetworkUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.InetSocketAddress;

import static net.java.faker.ui.Window.BODY_BLOCK_PADDING;
import static net.java.faker.ui.Window.BORDER_PADDING;

public class AdvancedTab extends UITab {
    public static boolean showDebug;
    JCheckBox showKickErrors;
    JCheckBox proxyOnlineMode;
    JCheckBox chatSigning;
    JCheckBox tracerouteFix;
    JCheckBox mdnsDisable;
    JCheckBox routerSpoof;
    JCheckBox blockTraffic;

    JCheckBox allowDirectConnection;
    NetworkAdapterComboBox networkAdapters;
    JTextField proxy;

    public AdvancedTab(final Window frame) {
        super(frame, "advanced");
    }

    @Override
    protected void init(JPanel contentPane) {
        contentPane.setLayout(new BorderLayout());
        this.addBody(contentPane);
    }

    private void addBody(final Container parent) {
        JPanel body = new JPanel();
        body.setLayout(new GridBagLayout());

        JPanel checkboxes = new JPanel();
        checkboxes.setLayout(new GridLayout(0, 2, BORDER_PADDING, BORDER_PADDING));

        int gridy = 0;


        {
            this.proxyOnlineMode = new JCheckBox();
            I18n.link(proxyOnlineMode, "tab.advanced.proxy_online_mode.label");
            I18n.linkTooltip(proxyOnlineMode, "tab.advanced.proxy_online_mode.tooltip");
            this.proxyOnlineMode.setSelected(Proxy.getConfig().onlineMode.get());
            checkboxes.add(this.proxyOnlineMode);
        }

        {
            this.chatSigning = new JCheckBox();
            I18n.link(chatSigning, "tab.advanced.chat_signing.label");
            I18n.linkTooltip(chatSigning, "tab.advanced.chat_signing.tooltip");
            this.chatSigning.setSelected(Proxy.getConfig().signChat.get());
            checkboxes.add(this.chatSigning);
        }
        if (WinRedirect.isSupported()) {
            {
                this.tracerouteFix = new JCheckBox();
                I18n.link(tracerouteFix, "tab.advanced.traceroute_fix.label");
                I18n.linkTooltip(tracerouteFix, "tab.advanced.traceroute_fix.tooltip");
                this.tracerouteFix.setSelected(Proxy.getConfig().tracerouteFix.get());
                checkboxes.add(this.tracerouteFix);
            }

            {
                this.mdnsDisable = new JCheckBox();
                I18n.link(mdnsDisable, "tab.advanced.mdns_disable.label");
                I18n.linkTooltip(mdnsDisable, "tab.advanced.mdns_disable.tooltip");
                this.mdnsDisable.setSelected(Proxy.getConfig().mdnsDisable.get());
                checkboxes.add(this.mdnsDisable);
            }

            {
                this.routerSpoof = new JCheckBox();
                I18n.link(routerSpoof, "tab.advanced.router_spoof.label");
                I18n.linkTooltip(routerSpoof, "tab.advanced.router_spoof.tooltip");
                this.routerSpoof.setSelected(Proxy.getConfig().routerSpoof.get());
                this.routerSpoof.addActionListener(this::updateNetworkAdapterEnabled);
                checkboxes.add(this.routerSpoof);
            }

            {
                this.blockTraffic = new JCheckBox();
                I18n.link(blockTraffic, "tab.advanced.block_traffic.label");
                I18n.linkTooltip(blockTraffic, "tab.advanced.block_traffic.tooltip");
                this.blockTraffic.setSelected(Proxy.getConfig().blockTraffic.get());
                this.blockTraffic.addActionListener(this::updateNetworkAdapterEnabled);
                checkboxes.add(this.blockTraffic);
            }

            {

                JLabel networkInterfaceLabel = new JLabel();
                I18n.link(networkInterfaceLabel, "tab.advanced.network_interface.label");
                checkboxes.add(networkInterfaceLabel);
                networkAdapters = new NetworkAdapterComboBox(ni -> {
                    if (ni.hasInternetAccess()) {
                        SwingUtilities.invokeLater(() -> Window.showWarning(String.format(I18n.get("tab.advanced.error.internet_adapter"), ni)));
                    }
                });
                networkAdapters.setValueCanBeNull(true);
                checkboxes.add(networkAdapters);
                networkAdapters.fillAdapters(interfaces -> {
                    if (Proxy.getConfig().targetAdapter.get() == null) {
                        NetworkInterface potentialInterface = NetworkUtil.findPotentialWifiHotspotInterface(interfaces);
                        if (potentialInterface != null) {
                            networkAdapters.setSelectedItem(potentialInterface);
                        }
                    } else if (Proxy.getConfig().targetAdapter.get().equals("null")) {
                        networkAdapters.setSelectedItem(0);
                    } else {
                        String targetAdapter = Proxy.getConfig().targetAdapter.get();
                        for (NetworkInterface ni : interfaces) {
                            if (targetAdapter.equals(NetworkUtil.toWindowsMac(ni.getHardwareAddress()))) {
                                networkAdapters.setSelectedItem(ni);
                                break;
                            }
                        }
                    }
                });
                updateNetworkAdapterEnabled(null);
            }
        }


        {
            this.showKickErrors = new JCheckBox();
            I18n.link(showKickErrors, "tab.advanced.show_kick_errors.label");
            I18n.linkTooltip(showKickErrors, "tab.advanced.show_kick_errors.tooltip");
            this.showKickErrors.setSelected(Proxy.getConfig().showKickErrors.get());
            checkboxes.add(this.showKickErrors);
        }

        {
            this.allowDirectConnection = new JCheckBox();
            I18n.link(allowDirectConnection, "tab.advanced.allow_direct_connection.label");
            I18n.linkTooltip(allowDirectConnection, "tab.advanced.allow_direct_connection.tooltip");
            this.allowDirectConnection.setSelected(Proxy.getConfig().allowDirectConnection.get());
            this.allowDirectConnection.addActionListener((e) -> {
                if (this.allowDirectConnection.isSelected()) {
                    Proxy.proxyAddress = new InetSocketAddress("0.0.0.0", 25565);
                } else {
                    Proxy.proxyAddress = new InetSocketAddress("127.0.0.1", 25565);
                }
            });

            checkboxes.add(this.allowDirectConnection);
        }

        {
            //just empty
            checkboxes.add(Box.createHorizontalBox());
        }

        if (WinRedirect.isSupported() && (System.console() != null || showDebug)) {
            JCheckBox debugMode = new JCheckBox("print debug");
            debugMode.addActionListener(e -> {
                if (WinRedirect.isSupported()) {
                    if (debugMode.isSelected()) {
                        WinRedirect.setLogLevel(1);
                    } else {
                        WinRedirect.setLogLevel(2);
                    }
                }
            });
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BODY_BLOCK_PADDING).fill(GBC.BOTH).weight(1, 1).add(debugMode);
        }


        GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BODY_BLOCK_PADDING).fill(GBC.BOTH).weight(1, 1).add(checkboxes);
        {
            JLabel proxyLabel = new JLabel();
            I18n.link(proxyLabel, "tab.advanced.proxy_url.label");
            I18n.linkTooltip(proxyLabel, "tab.advanced.proxy_url.tooltip");
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(proxyLabel);

            this.proxy = new JTextField();
            I18n.linkTooltip(proxy, "tab.advanced.proxy_url.tooltip");
            this.proxy.setText(Proxy.getConfig().proxy.get());
            GBC.create(body).grid(0, gridy++).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(this.proxy);
        }

        parent.add(body, BorderLayout.NORTH);
    }

    void updateNetworkAdapterEnabled(ActionEvent e) {
        if (!this.routerSpoof.isEnabled()) {
            this.networkAdapters.setEnabled(false);
            return;
        }
        this.networkAdapters.setEnabled(this.blockTraffic.isSelected() || this.routerSpoof.isSelected());
    }


    public void applyGuiState() {
        Proxy.getConfig().onlineMode.set(this.proxyOnlineMode.isSelected());
        Proxy.getConfig().signChat.set(this.chatSigning.isSelected());
        Proxy.getConfig().proxy.set(this.proxy.getText());
        if (WinRedirect.isSupported()) {
            Proxy.getConfig().tracerouteFix.set(this.tracerouteFix.isSelected());
            Proxy.getConfig().mdnsDisable.set(this.mdnsDisable.isSelected());

            Proxy.getConfig().blockTraffic.set(this.blockTraffic.isSelected());
            Proxy.getConfig().routerSpoof.set(this.routerSpoof.isSelected());
            Proxy.getConfig().allowDirectConnection.set(this.allowDirectConnection.isSelected());
            if (this.networkAdapters.getSelectedItem() == NetworkInterface.NULL) {
                Proxy.getConfig().targetAdapter.set("null");
            } else if (this.networkAdapters.getSelectedItem() instanceof NetworkInterface ni) {
                Proxy.getConfig().targetAdapter.set(NetworkUtil.toWindowsMac(ni.getHardwareAddress()));
            }
        }
    }
}