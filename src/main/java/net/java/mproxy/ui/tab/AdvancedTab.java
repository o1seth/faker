package net.java.mproxy.ui.tab;

import net.java.mproxy.Proxy;
import net.java.mproxy.ui.GBC;
import net.java.mproxy.ui.I18n;
import net.java.mproxy.ui.UITab;
import net.java.mproxy.ui.Window;
import net.java.mproxy.util.network.NetworkInterface;
import net.java.mproxy.util.network.NetworkUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static net.java.mproxy.ui.Window.BODY_BLOCK_PADDING;
import static net.java.mproxy.ui.Window.BORDER_PADDING;

public class AdvancedTab extends UITab {
    JCheckBox proxyOnlineMode;
    JCheckBox chatSigning;
    JCheckBox tracerouteFix;
    JCheckBox mdnsDisable;
    JCheckBox routerSpoof;
    JCheckBox blockTraffic;
    JComboBox<NetworkInterface> networkAdapters;
    ActionListener networkAdapterListener;

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
            this.proxyOnlineMode = new JCheckBox(I18n.get("tab.advanced.proxy_online_mode.label"));
            this.proxyOnlineMode.setToolTipText(I18n.get("tab.advanced.proxy_online_mode.tooltip"));
            this.proxyOnlineMode.setSelected(Proxy.getConfig().onlineMode.get());
            checkboxes.add(this.proxyOnlineMode);
        }

        {
            this.chatSigning = new JCheckBox(I18n.get("tab.advanced.chat_signing.label"));
            this.chatSigning.setToolTipText(I18n.get("tab.advanced.chat_signing.tooltip"));
            this.proxyOnlineMode.setSelected(Proxy.getConfig().signChat.get());
            checkboxes.add(this.chatSigning);
        }

        {
            this.tracerouteFix = new JCheckBox(I18n.get("tab.advanced.traceroute_fix.label"));
            this.tracerouteFix.setToolTipText(I18n.get("tab.advanced.traceroute_fix.tooltip"));
            this.tracerouteFix.setSelected(Proxy.getConfig().tracerouteFix.get());
            checkboxes.add(this.tracerouteFix);
        }

        {
            this.mdnsDisable = new JCheckBox(I18n.get("tab.advanced.mdns_disable.label"));
            this.mdnsDisable.setToolTipText(I18n.get("tab.advanced.mdns_disable.tooltip"));
            this.mdnsDisable.setSelected(Proxy.getConfig().mdnsDisable.get());
            checkboxes.add(this.mdnsDisable);
        }

        {
            this.routerSpoof = new JCheckBox(I18n.get("tab.advanced.router_spoof.label"));
            this.routerSpoof.setToolTipText(I18n.get("tab.advanced.router_spoof.tooltip"));
            this.routerSpoof.setSelected(Proxy.getConfig().routerSpoof.get());
            this.routerSpoof.addActionListener(this::updateNetworkAdapterEnabled);
            checkboxes.add(this.routerSpoof);
        }

        {
            this.blockTraffic = new JCheckBox(I18n.get("tab.advanced.block_traffic.label"));
            this.blockTraffic.setToolTipText(I18n.get("tab.advanced.block_traffic.tooltip"));
            this.blockTraffic.setSelected(Proxy.getConfig().blockTraffic.get());
            this.blockTraffic.addActionListener(this::updateNetworkAdapterEnabled);
            checkboxes.add(this.blockTraffic);
        }

        {

            JLabel networkInterfaceLabel = new JLabel(I18n.get("tab.advanced.network_interface.label"));
            checkboxes.add(networkInterfaceLabel);
            networkAdapters = new JComboBox<>(new DefaultComboBoxModel<>());
            networkAdapters.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!networkAdapters.isPopupVisible() && e.getButton() == MouseEvent.BUTTON1 && networkAdapters.isEnabled()) {
                        fillAdapters(false);
                    }
                }
            });
            networkAdapterListener = new ActionListener() {
                NetworkInterface lastSelected;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (networkAdapters.getSelectedItem() instanceof NetworkInterface ni) {
                        if (!ni.equalsNameAndMac(lastSelected)) {
                            if (ni.hasInternetAccess()) {
                                SwingUtilities.invokeLater(() -> Window.showWarning(String.format(I18n.get("tab.advanced.error.internet_adapter"), ni)));
                            }
                            lastSelected = ni;
                        }
                    }
                }
            };
            networkAdapters.addActionListener(networkAdapterListener);
            checkboxes.add(networkAdapters);

            fillAdapters(true);
            updateNetworkAdapterEnabled(null);
        }

        GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BODY_BLOCK_PADDING).fill(GBC.BOTH).weight(1, 1).add(checkboxes);

        parent.add(body, BorderLayout.NORTH);
    }

    private void updateNetworkAdapterEnabled(ActionEvent e) {
        this.networkAdapters.setEnabled(this.blockTraffic.isSelected() || this.routerSpoof.isSelected());
    }

    private long lastFillTime;

    private void fillAdapters(boolean firstCall) {
        if (System.currentTimeMillis() - lastFillTime > 5000) {
            lastFillTime = System.currentTimeMillis();
        } else {
            return;
        }
        new Thread(() -> {
            List<NetworkInterface> interfaces = NetworkUtil.getNetworkInterfaces();

            if (firstCall) {
                //wait for constructor ends and window to be fully initialized
                Window.getInstance();
            }
            SwingUtilities.invokeLater(() -> {
                if (!firstCall) {
                    networkAdapters.removeActionListener(networkAdapterListener);
                }

                NetworkInterface selected = (NetworkInterface) networkAdapters.getSelectedItem();

                DefaultComboBoxModel<NetworkInterface> model = (DefaultComboBoxModel<NetworkInterface>) networkAdapters.getModel();
                model.removeAllElements();

                model.addElement(NetworkInterface.NULL);
                model.addAll(interfaces);

                if (selected != null) {
                    for (NetworkInterface ni : interfaces) {
                        if (ni.equalsNameAndMac(selected)) {
                            networkAdapters.setSelectedItem(ni);
                            break;
                        }
                    }
                }
                if (firstCall) {
                    if (Proxy.getConfig().targetAdapter.get() == null) {
                        for (NetworkInterface ni : interfaces) {
                            if (ni.getDisplayName().equals("Microsoft Wi-Fi Direct Virtual Adapter #2")) {
                                networkAdapters.setSelectedItem(ni);
                                break;
                            }
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
                } else {
                    networkAdapters.addActionListener(networkAdapterListener);
                }

            });
        }).start();
    }

    void applyGuiState() {
        Proxy.getConfig().onlineMode.set(this.proxyOnlineMode.isSelected());
        Proxy.getConfig().signChat.set(this.chatSigning.isSelected());
        Proxy.getConfig().tracerouteFix.set(this.tracerouteFix.isSelected());
        Proxy.getConfig().mdnsDisable.set(this.mdnsDisable.isSelected());

        Proxy.getConfig().blockTraffic.set(this.blockTraffic.isSelected());
        Proxy.getConfig().routerSpoof.set(this.routerSpoof.isSelected());
        if (this.networkAdapters.getSelectedItem() == NetworkInterface.NULL) {
            Proxy.getConfig().targetAdapter.set("null");
        } else if (this.networkAdapters.getSelectedItem() instanceof NetworkInterface ni) {
            Proxy.getConfig().targetAdapter.set(NetworkUtil.toWindowsMac(ni.getHardwareAddress()));
        }
    }

}