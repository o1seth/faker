package net.java.faker.ui.tab;

import net.java.faker.Proxy;
import net.java.faker.WinRedirect;
import net.java.faker.ui.GBC;
import net.java.faker.ui.I18n;
import net.java.faker.ui.UITab;
import net.java.faker.ui.Window;
import net.java.faker.util.network.NetworkInterface;
import net.java.faker.util.network.NetworkUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.Inet4Address;
import java.util.List;

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
    JComboBox<NetworkInterface> networkAdapters;
    ActionListener networkAdapterListener;
    private NetworkInterface lastSelectedAdapter;

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
                networkAdapters = new JComboBox<>(new DefaultComboBoxModel<>());
                networkAdapters.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (!networkAdapters.isPopupVisible() && e.getButton() == MouseEvent.BUTTON1 && networkAdapters.isEnabled()) {
                            fillAdapters(false);
                        }
                    }
                });
                networkAdapterListener = e -> {
                    if (networkAdapters.getSelectedItem() instanceof NetworkInterface ni) {
                        if (!ni.equalsNameAndMac(lastSelectedAdapter)) {
                            if (ni.hasInternetAccess()) {
                                SwingUtilities.invokeLater(() -> Window.showWarning(String.format(I18n.get("tab.advanced.error.internet_adapter"), ni)));
                            }
                            lastSelectedAdapter = ni;
                        }
                    }
                };
                networkAdapters.addActionListener(networkAdapterListener);
                checkboxes.add(networkAdapters);

                fillAdapters(true);
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

        JPanel dhcpSetting = new JPanel();
        dhcpSetting.setBorder(BorderFactory.createTitledBorder("DHCP server"));
        dhcpSetting.setLayout(new GridLayout(0, 2, BORDER_PADDING / 3, BORDER_PADDING / 3));
        {
            JPanel ipAddressBody = new JPanel();
            ipAddressBody.setLayout(new GridBagLayout());
            JLabel ipAddressLabel = new JLabel("IP address");
            GBC.create(ipAddressBody).grid(0, gridy++).insets(2, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(ipAddressLabel);

            JTextField ipAddressField = new JTextField();
            GBC.create(ipAddressBody).grid(0, gridy++).insets(0, BORDER_PADDING, 0, BORDER_PADDING).weightx(1).fill(GBC.HORIZONTAL).add(ipAddressField);
            dhcpSetting.add(ipAddressBody);
        }
        {
            JPanel maskBody = new JPanel();
            maskBody.setLayout(new GridBagLayout());
            JLabel maskLabel = new JLabel("Mask");
            GBC.create(maskBody).grid(0, gridy++).insets(2, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(maskLabel);

            JTextField maskField = new JTextField();
            GBC.create(maskBody).grid(0, gridy++).insets(0, BORDER_PADDING, 0, BORDER_PADDING).weightx(1).fill(GBC.HORIZONTAL).add(maskField);
            dhcpSetting.add(maskBody);
        }

        {
            JPanel startIpBody = new JPanel();
            startIpBody.setLayout(new GridBagLayout());
            JLabel startIpLabel = new JLabel("Start ip");
            GBC.create(startIpBody).grid(0, gridy++).insets(2, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(startIpLabel);

            JTextField startIpField = new JTextField();
            GBC.create(startIpBody).grid(0, gridy++).insets(0, BORDER_PADDING, 0, BORDER_PADDING).weightx(1).fill(GBC.HORIZONTAL).add(startIpField);
            dhcpSetting.add(startIpBody);
        }

        {
            JPanel endIpBody = new JPanel();
            endIpBody.setLayout(new GridBagLayout());
            JLabel endIpLabel = new JLabel("End ip");
            GBC.create(endIpBody).grid(0, gridy++).insets(2, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(endIpLabel);

            JTextField endIpField = new JTextField();
            GBC.create(endIpBody).grid(0, gridy++).insets(0, BORDER_PADDING, 0, BORDER_PADDING).weightx(1).fill(GBC.HORIZONTAL).add(endIpField);
            dhcpSetting.add(endIpBody);
        }

        GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BODY_BLOCK_PADDING).fill(GBC.BOTH).weight(1, 1).add(dhcpSetting);

        parent.add(body, BorderLayout.NORTH);
    }

    void updateNetworkAdapterEnabled(ActionEvent e) {
        if (!this.routerSpoof.isEnabled()) {
            this.networkAdapters.setEnabled(false);
            return;
        }
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
                        NetworkInterface potentialInterface = findPotentialWifiHotspotInterface(interfaces);
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
                } else {
                    networkAdapters.addActionListener(networkAdapterListener);
                }

            });
        }).start();
    }

    public void applyGuiState() {
        Proxy.getConfig().onlineMode.set(this.proxyOnlineMode.isSelected());
        Proxy.getConfig().signChat.set(this.chatSigning.isSelected());
        if (WinRedirect.isSupported()) {
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

    private static NetworkInterface findPotentialWifiHotspotInterface(List<NetworkInterface> interfaces) {
        for (NetworkInterface ni : interfaces) {
            if (ni.getDisplayName().equals("Microsoft Wi-Fi Direct Virtual Adapter #2") || ni.getDisplayName().equals("Microsoft Wi-Fi Direct Virtual Adapter #4")) {
                Inet4Address ipv4 = ni.getFirstIpv4Address();
                if (ipv4.getHostAddress().equals("192.168.137.1")) {
                    return ni;
                }
            }
        }

        //windows 10
        for (NetworkInterface ni : interfaces) {
            if (ni.getDisplayName().equals("Microsoft Wi-Fi Direct Virtual Adapter #2")) {
                return ni;
            }
        }
        //windows 11
        for (NetworkInterface ni : interfaces) {
            if (ni.getDisplayName().equals("Microsoft Wi-Fi Direct Virtual Adapter #4")) {
                return ni;
            }
        }

        for (NetworkInterface ni : interfaces) {
            if (ni.getDisplayName().equals("Microsoft Wi-Fi Direct Virtual Adapter")) {
                Inet4Address ipv4 = ni.getFirstIpv4Address();
                if (ipv4.getHostAddress().equals("192.168.137.1")) {
                    return ni;
                }
            }
        }
        return null;
    }
}