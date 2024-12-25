package net.java.faker.ui.tab;

import net.java.faker.Proxy;
import net.java.faker.proxy.dhcp.Dhcp;
import net.java.faker.save.Config;
import net.java.faker.ui.GBC;
import net.java.faker.ui.I18n;
import net.java.faker.ui.UITab;
import net.java.faker.ui.Window;
import net.java.faker.ui.elements.NetworkAdapterComboBox;
import net.java.faker.util.logging.Logger;
import net.java.faker.util.network.NetworkInterface;
import net.java.faker.util.network.NetworkUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Random;

import static net.java.faker.ui.Window.BODY_BLOCK_PADDING;
import static net.java.faker.ui.Window.BORDER_PADDING;

public class DHCPTab extends UITab {

    NetworkAdapterComboBox networkAdapters;

    public DHCPTab(final Window frame) {
        super(frame, "dhcp");
    }

    JTextField dns2Field;
    JTextField dns1Field;
    JTextField endIpField;
    JTextField startIpField;
    JTextField maskField;
    JTextField ipAddressField;
    JButton start;
    String lastGeneratedIp;

    @Override
    protected void init(JPanel parent) {
        parent.setLayout(new BorderLayout());
        JPanel body = new JPanel();
        body.setLayout(new GridBagLayout());

        int gridY = 0;
        JLabel description = new JLabel();
        I18n.link(description, "tab.dhcp.description.label", (t, s) -> t.setText("<html><p>" + I18n.get(s) + "</p></html>"));
        GBC.create(body).grid(0, gridY++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BODY_BLOCK_PADDING).fill(GBC.BOTH).weight(1, 1).add(description);

        JPanel dhcpSetting = new JPanel();
        dhcpSetting.setLayout(new GridLayout(0, 2, BORDER_PADDING, BORDER_PADDING));

        {
            JPanel ipAddressBody = new JPanel();
            ipAddressBody.setLayout(new GridBagLayout());
            JLabel ipAddressLabel = new JLabel();
            I18n.link(ipAddressLabel, "tab.dhcp.network_interface.label");
            GBC.create(ipAddressBody).grid(0, gridY++).anchor(GBC.NORTHWEST).add(ipAddressLabel);
            networkAdapters = new NetworkAdapterComboBox(ni -> {
                if (ni.hasInternetAccess()) {
                    SwingUtilities.invokeLater(() -> Window.showWarning(String.format(I18n.get("tab.dhcp.error.internet_adapter"), ni)));
                }
            });

            dhcpSetting.add(networkAdapters);
            networkAdapters.fillAdapters(interfaces -> {
                if (Proxy.getConfig().dhcp_interface.get() == null) {
                    NetworkInterface potentialInterface = NetworkUtil.findPotentialWifiHotspotInterface(interfaces);
                    if (potentialInterface != null) {
                        networkAdapters.setSelectedItem(potentialInterface);
                    }
                } else {
                    String targetAdapter = Proxy.getConfig().dhcp_interface.get();
                    for (NetworkInterface ni : interfaces) {
                        if (targetAdapter.equals(NetworkUtil.toWindowsMac(ni.getHardwareAddress()))) {
                            networkAdapters.setSelectedItem(ni);
                            break;
                        }
                    }
                }
                if (Proxy.getConfig().dhcp_started.get()) {
                    if (networkAdapters.getSelectedItem() instanceof NetworkInterface ni) {
                        Config config = Proxy.getConfig();
                        start(ni, config.dhcp_ip.get(), config.dhcp_mask.get(), config.dhcp_startIp.get(), config.dhcp_endIp.get(), new String[]{config.dhcp_dns1.get(), config.dhcp_dns2.get()});
                    } else {
                        Window.showError("Failed to start DHCP: network interface not found");
                        Proxy.getConfig().dhcp_started.set(false);
                        Proxy.getConfig().save();
                    }
                }
            });
            GBC.create(ipAddressBody).grid(0, gridY++).weightx(1).fill(GBC.HORIZONTAL).add(networkAdapters);

            GBC.create(body).grid(0, gridY++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BODY_BLOCK_PADDING).fill(GBC.BOTH).weight(1, 1).add(ipAddressBody);
        }

        {
            JPanel ipAddressBody = new JPanel();
            ipAddressBody.setLayout(new GridBagLayout());
            JLabel ipAddressLabel = new JLabel("IP address");
            GBC.create(ipAddressBody).grid(0, gridY++).anchor(GBC.NORTHWEST).add(ipAddressLabel);
            ipAddressField = new JTextField();
            ipAddressField.setText(Proxy.getConfig().dhcp_ip.get());
            lastGeneratedIp = Proxy.getConfig().dhcp_ip.get();
            ipAddressField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    changed();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    changed();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    changed();
                }

                private void changed() {
                    if (start != null) {
                        if (ipAddressField.getText() == null || ipAddressField.getText().isEmpty()) {
                            I18n.link(start, "tab.dhcp.generate");
                        } else {
                            I18n.link(start, "tab.general.state.start");
                        }
                    }
                }

            });
            GBC.create(ipAddressBody).grid(0, gridY++).weightx(1).fill(GBC.HORIZONTAL).add(ipAddressField);
            dhcpSetting.add(ipAddressBody);
        }
        {
            JPanel maskBody = new JPanel();
            maskBody.setLayout(new GridBagLayout());
            JLabel maskLabel = new JLabel("Mask");
            GBC.create(maskBody).grid(0, gridY++).anchor(GBC.NORTHWEST).add(maskLabel);

            maskField = new JTextField();
            maskField.setText(Proxy.getConfig().dhcp_mask.get());
            GBC.create(maskBody).grid(0, gridY++).weightx(1).fill(GBC.HORIZONTAL).add(maskField);
            dhcpSetting.add(maskBody);
        }

        {
            JPanel startIpBody = new JPanel();
            startIpBody.setLayout(new GridBagLayout());
            JLabel startIpLabel = new JLabel("Start ip");
            GBC.create(startIpBody).grid(0, gridY++).anchor(GBC.NORTHWEST).add(startIpLabel);

            startIpField = new JTextField();
            startIpField.setText(Proxy.getConfig().dhcp_startIp.get());
            GBC.create(startIpBody).grid(0, gridY++).weightx(1).fill(GBC.HORIZONTAL).add(startIpField);
            dhcpSetting.add(startIpBody);
        }

        {
            JPanel endIpBody = new JPanel();
            endIpBody.setLayout(new GridBagLayout());
            JLabel endIpLabel = new JLabel("End ip");
            GBC.create(endIpBody).grid(0, gridY++).anchor(GBC.NORTHWEST).add(endIpLabel);

            endIpField = new JTextField();
            endIpField.setText(Proxy.getConfig().dhcp_endIp.get());
            GBC.create(endIpBody).grid(0, gridY++).weightx(1).fill(GBC.HORIZONTAL).add(endIpField);
            dhcpSetting.add(endIpBody);
        }

        {
            JPanel dns1Body = new JPanel();
            dns1Body.setLayout(new GridBagLayout());
            JLabel dns1Label = new JLabel("DNS 1");
            GBC.create(dns1Body).grid(0, gridY++).anchor(GBC.NORTHWEST).add(dns1Label);

            dns1Field = new JTextField();
            dns1Field.setText(Proxy.getConfig().dhcp_dns1.get());
            GBC.create(dns1Body).grid(0, gridY++).weightx(1).fill(GBC.HORIZONTAL).add(dns1Field);
            dhcpSetting.add(dns1Body);
        }

        {
            JPanel dns2Body = new JPanel();
            dns2Body.setLayout(new GridBagLayout());
            JLabel dns2Label = new JLabel("DNS 2");
            GBC.create(dns2Body).grid(0, gridY++).anchor(GBC.NORTHWEST).add(dns2Label);

            dns2Field = new JTextField();
            dns2Field.setText(Proxy.getConfig().dhcp_dns2.get());
            GBC.create(dns2Body).grid(0, gridY++).weightx(1).fill(GBC.HORIZONTAL).add(dns2Field);
            dhcpSetting.add(dns2Body);
        }

        GBC.create(body).grid(0, gridY++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BODY_BLOCK_PADDING).fill(GBC.BOTH).weight(1, 1).add(dhcpSetting);

        parent.add(body, BorderLayout.NORTH);
        JPanel footer = new JPanel();
        footer.setLayout(new GridBagLayout());
        start = new JButton();
        I18n.link(start, "tab.general.state.start");
        start.addActionListener(e -> {
            try {
                if (start.getText().equals(I18n.get("tab.general.state.stop"))) {
                    Proxy.getConfig().dhcp_started.set(false);
                    Proxy.getConfig().save();
                    stop();
                    return;
                }
                if (networkAdapters.getSelectedItem() == null) {
                    Window.showError("Network interface is not selected");
                    return;
                }
                NetworkInterface ni = (NetworkInterface) networkAdapters.getSelectedItem();
                if (ni.hasInternetAccess()) {
                    Window.showError("Network interface " + ni.getDisplayName() + " has internet access. Please choose another");
                    return;
                }
                String ip = ipAddressField.getText();
                String startIp = startIpField.getText();
                String endIp = endIpField.getText();
                String dns1 = dns1Field.getText();
                String dns2 = dns2Field.getText();
                boolean shouldStart = true;
                if (ip.isEmpty()) {
                    ip = generateIp();

                    shouldStart = false;
                }
                if (shouldStart) {
                    if (!NetworkUtil.isIpv4(ip)) {
                        Window.showError("'" + ip + "' is not valid ip address");
                        return;
                    }
                    if (!startIp.isEmpty() && !NetworkUtil.isIpv4(startIp)) {
                        Window.showError("Start ip '" + startIp + "' is not valid ip address");
                        return;
                    }
                    if (!endIp.isEmpty() && !NetworkUtil.isIpv4(endIp)) {
                        Window.showError("End ip '" + ip + "' is not valid ip address");
                        return;
                    }
                    if (!dns1.isEmpty() && !NetworkUtil.isIpv4(dns1)) {
                        Window.showError("DNS 1 '" + dns1 + "' is not valid ip address");
                        return;
                    }
                    if (!dns2.isEmpty() && !NetworkUtil.isIpv4(dns2)) {
                        Window.showError("DNS 2 '" + dns2 + "' is not valid ip address");
                        return;
                    }
                }
                if (ip.startsWith("192.168.137")) {
                    Window.showError("'" + ip + "' cannot be used");
                    return;
                }
                Inet4Address address = (Inet4Address) InetAddress.getByName(ip);
                String mask = maskField.getText();
                if (mask.isEmpty()) {
                    mask = "255.255.255.0";
                    maskField.setText(mask);
                } else if (NetworkUtil.getPrefix(mask) == -1) {
                    Window.showError("'" + mask + "' is not valid mask");
                    return;
                }
                int prefix = NetworkUtil.getPrefix(mask);

                if (startIp.isEmpty()) {
                    int start = NetworkUtil.getStartInt(address, NetworkUtil.getPrefix(mask));
                    startIp = NetworkUtil.fromIntAddress(start + 1).getHostAddress();
                } else if (!NetworkUtil.isIpInSameNetwork(prefix, ip, startIp)) {
                    if (shouldStart) {
                        Window.showError("'" + ip + "' and '" + startIp + "' is not in same network");
                        return;
                    } else {
                        int start = NetworkUtil.getStartInt(address, NetworkUtil.getPrefix(mask));
                        startIp = NetworkUtil.fromIntAddress(start + 1).getHostAddress();
                    }
                }

                if (endIp.isEmpty()) {
                    int end = NetworkUtil.getEndInt(address, NetworkUtil.getPrefix(mask));
                    endIp = NetworkUtil.fromIntAddress(end).getHostAddress();
                } else if (!NetworkUtil.isIpInSameNetwork(prefix, ip, endIp)) {
                    if (shouldStart) {
                        Window.showError("'" + ip + "' and '" + endIp + "' is not in same network");
                        return;
                    } else {
                        int end = NetworkUtil.getEndInt(address, NetworkUtil.getPrefix(mask));
                        endIp = NetworkUtil.fromIntAddress(end).getHostAddress();
                    }
                }

                if (dns1.isEmpty()) {
                    dns1 = ip;
                } else if (!NetworkUtil.isIpv4(dns1)) {
                    if (shouldStart) {
                        Window.showError("DNS 1 '" + dns1 + "' is not valid ip address");
                        return;
                    } else {
                        dns1 = ip;
                    }
                } else if (dns1.equals(this.lastGeneratedIp)) {
                    dns1 = ip;
                }

                if (!dns2.isEmpty() && !NetworkUtil.isIpv4(dns2)) {
                    if (shouldStart) {
                        Window.showError("DNS 2 '" + dns2 + "' is not valid ip address");
                        return;
                    } else {
                        dns2 = "";
                    }
                }
                this.lastGeneratedIp = ip;
                this.ipAddressField.setText(ip);
                this.maskField.setText(mask);
                this.startIpField.setText(startIp);
                this.endIpField.setText(endIp);
                this.dns1Field.setText(dns1);
                this.dns2Field.setText(dns2);
                applyGui();

                if (shouldStart) {
                    Proxy.getConfig().dhcp_started.set(true);
                    start(ni, ip, mask, startIp, endIp, new String[]{dns1, dns2});
                }
                Proxy.getConfig().save();
            } catch (Exception ex) {
                Logger.error("Failed to parse dhcp", ex);
            }
        });

        GBC.create(footer).grid(0, 1).weightx(1).insets(0, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).anchor(GBC.WEST).fill(GBC.HORIZONTAL).add(start);

        parent.add(footer, BorderLayout.SOUTH);

    }

    private void start(NetworkInterface ni, String ip, String mask, String startIp, String endIp, String[] dns) {
        I18n.link(start, "tab.general.state.starting");
        start.setEnabled(false);
        setState(false);
        new Thread(() -> {
            try {
                Dhcp.start(ni, ip, mask, startIp, endIp, dns);
                Logger.info("Dhcp started successfully");
                SwingUtilities.invokeLater(() -> {
                    start.setEnabled(true);
                    I18n.link(start, "tab.general.state.stop");
                });
                networkAdapters.fillAdapters(null);

            } catch (Throwable e) {
                Logger.error("Failed to start dhcp", e);
                SwingUtilities.invokeLater(() -> {
                    start.setEnabled(true);
                    setState(true);
                    I18n.link(start, "tab.general.state.start");
                    Window.showError("Failed to start dhcp: " + e.getMessage());
                });
            }

        }).start();
    }

    private void stop() {

        start.setEnabled(false);
        new Thread(() -> {
            try {
                Dhcp.stop();
                Logger.info("Dhcp stopped successfully");
                SwingUtilities.invokeLater(() -> {
                    start.setEnabled(true);
                    setState(true);
                    I18n.link(start, "tab.general.state.start");
                });
            } catch (Throwable e) {
                Logger.error("Failed to stop dhcp", e);
                SwingUtilities.invokeLater(() -> {
                    start.setEnabled(true);
                    I18n.link(start, "tab.general.state.stop");
                    Window.showError("Failed to stop dhcp: " + e.getMessage());
                });
            }
        }).start();

    }

    Random random = new Random();

    private String generateIp() {
        for (int i = 0; i < 100; i++) {
            int num = random.nextInt(255);
            String ip = "192.168." + num + ".1";
            if (ip.equals("192.168.137.1")) {
                continue;
            }
            if (NetworkUtil.localIpExists(ip, null)) {
                continue;
            }
            return ip;
        }

        return "";

    }

    private void setState(boolean state) {
        this.networkAdapters.setEnabled(state);
        this.ipAddressField.setEnabled(state);
        this.maskField.setEnabled(state);
        this.startIpField.setEnabled(state);
        this.endIpField.setEnabled(state);
        this.dns1Field.setEnabled(state);
        this.dns2Field.setEnabled(state);
    }

    private void applyGui() {
        Config config = Proxy.getConfig();
        config.dhcp_ip.set(this.ipAddressField.getText());
        config.dhcp_mask.set(this.maskField.getText());
        config.dhcp_startIp.set(this.startIpField.getText());
        config.dhcp_endIp.set(this.endIpField.getText());
        config.dhcp_dns1.set(this.dns1Field.getText());
        config.dhcp_dns2.set(this.dns2Field.getText());
        if (this.networkAdapters.getSelectedItem() instanceof NetworkInterface ni) {
            Proxy.getConfig().dhcp_interface.set(NetworkUtil.toWindowsMac(ni.getHardwareAddress()));
        } else {
            Proxy.getConfig().dhcp_interface.set(null);
        }
    }

}
