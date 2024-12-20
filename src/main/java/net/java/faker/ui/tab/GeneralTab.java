package net.java.faker.ui.tab;

import net.java.faker.Proxy;
import net.java.faker.WinRedirect;
import net.java.faker.auth.Account;
import net.java.faker.proxy.event.DisconnectEvent;
import net.java.faker.proxy.event.LoginEvent;
import net.java.faker.proxy.event.RedirectStateChangeEvent;
import net.java.faker.proxy.event.SwapEvent;
import net.java.faker.proxy.session.ProxyConnection;
import net.java.faker.ui.GBC;
import net.java.faker.ui.I18n;
import net.java.faker.ui.UITab;
import net.java.faker.ui.Window;
import net.java.faker.ui.elements.LinkLabel;
import net.java.faker.util.Util;
import net.java.faker.util.logging.Logger;
import net.java.faker.util.network.NetworkInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;

import static net.java.faker.ui.Window.BODY_BLOCK_PADDING;
import static net.java.faker.ui.Window.BORDER_PADDING;

public class GeneralTab extends UITab {
    static ImageIcon activeDeviceIcon;
    static ImageIcon inactiveDeviceIcon;
    static ImageIcon swapIcon;
    static ImageIcon emptyIcon;

    static {
        activeDeviceIcon = Util.getResourceImageIcon("/assets/faker/icon/computer_active.png");
        inactiveDeviceIcon = Util.getResourceImageIcon("/assets/faker/icon/computer_gray.png");
        swapIcon = Util.getResourceImageIcon("/assets/faker/icon/swap_32.png");
        if (activeDeviceIcon != null) {
            emptyIcon = new ImageIcon(new BufferedImage(activeDeviceIcon.getIconWidth(), activeDeviceIcon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR));
        }
    }

    private static ImageIcon getIconFromResources(String path) {
        URL url = GeneralTab.class.getResource(path);
        if (url == null) {
            return null;
        }
        return new ImageIcon(url);
    }


    JTextField serverAddress;

    JComboBox<Account> accounts;
    JLabel stateLabel;
    JButton startButton;
    JButton pauseButton;

    JPanel leftPanel;
    JPanel rightPanel;

    JLabel leftDevice;
    JLabel rightDevice;
    JButton swap;

    JLabel leftStatus;
    JLabel rightStatus;
    ProxyConnection leftConnection;
    ProxyConnection rightConnection;

    public GeneralTab(final Window frame) {
        super(frame, "general");
    }

    @Override
    protected void init(JPanel contentPane) {
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());

        contentPane.setLayout(new BorderLayout());
        contentPane.add(top, BorderLayout.NORTH);

        this.addHeader(top);
        this.addBody(top);
        this.addFooter(contentPane);
    }

    private void addHeader(final Container parent) {
        JPanel header = new JPanel();
        header.setLayout(new GridBagLayout());

        LinkLabel discord = new LinkLabel("Discord", "https://discord.gg/vW6naJkMqJ");
        GBC.create(header).grid(0, 0).width(0).insets(BORDER_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(discord);

        JLabel title = new JLabel("Faker");
        title.setFont(title.getFont().deriveFont(30F));
        GBC.create(header).grid(1, 0).weightx(1).width(0).insets(BORDER_PADDING, 0, 0, 0).anchor(GBC.CENTER).add(title);
        parent.add(header, BorderLayout.NORTH);
    }

    private void addBody(final Container parent) {
        JPanel body = new JPanel();
        body.setLayout(new GridBagLayout());

        int gridy = 0;
        {
            JLabel serverAddressLabel = new JLabel();
            I18n.link(serverAddressLabel, "tab.general.server_address.label");
            I18n.linkTooltip(serverAddressLabel, "tab.general.server_address.tooltip");
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(serverAddressLabel);

            this.serverAddress = new JTextField();
            I18n.linkTooltip(serverAddress, "tab.general.server_address.tooltip");
            this.serverAddress.setText(Proxy.getConfig().getServerAddress());
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(this.serverAddress);

            JLabel minecraftAccountLabel = new JLabel();
            I18n.link(minecraftAccountLabel, "tab.general.minecraft_account.label");
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(minecraftAccountLabel);

            java.util.List<Account> accountList = new ArrayList<>();
            accountList.add(null);
            accountList.addAll(Proxy.getAccountManager().getAccounts());
            accounts = new JComboBox<>(accountList.toArray(new Account[0]));

            accounts.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    DefaultListCellRenderer component = (DefaultListCellRenderer) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    Account account = (Account) value;
                    if (account == null) {
                        I18n.link(component, "tab.general.minecraft_account.option_no_account");
                    } else {
                        component.setText(account.getName());
                    }
                    return component;
                }
            });
            if (Proxy.getConfig().account.get() != null) {
                for (Account account : Proxy.getAccountManager().getAccounts()) {
                    if (Proxy.getConfig().account.get().equals(account.getName())) {
                        this.accounts.setSelectedItem(account);
                        break;
                    }
                }
            }
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(accounts);

            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(Box.createVerticalStrut(10));

            JPanel devices = new JPanel();

            leftDevice = new JLabel(activeDeviceIcon);
            leftDevice.setAlignmentX(Component.CENTER_ALIGNMENT);

            rightDevice = new JLabel(inactiveDeviceIcon);
            rightDevice.setAlignmentX(Component.CENTER_ALIGNMENT);

            swap = new JButton(swapIcon);
            swap.addActionListener(e -> {
                if (Proxy.dualConnection != null) {
                    Proxy.dualConnection.swapController();
                }
            });

            leftPanel = new JPanel();

            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
            leftPanel.add(leftDevice);
            leftStatus = new JLabel();
            leftStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
            leftPanel.add(leftStatus);

            rightPanel = new JPanel();
            rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
            rightPanel.add(rightDevice);
            rightStatus = new JLabel();


            rightStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
            rightPanel.add(rightStatus);

            leftPanel.setVisible(false);
            swap.setVisible(false);
            rightPanel.setVisible(false);

            devices.add(leftPanel);
            devices.add(swap);
            devices.add(rightPanel);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(devices);
            Proxy.registerEvent(e -> {
                SwingUtilities.invokeLater(() -> {
                    if (e instanceof SwapEvent swapEvent) {

                        if (swapEvent.getNewController() == leftConnection) {
                            leftDevice.setIcon(activeDeviceIcon);
                            rightDevice.setIcon(inactiveDeviceIcon);
                        } else if (swapEvent.getNewController() == rightConnection) {
                            rightDevice.setIcon(activeDeviceIcon);
                            leftDevice.setIcon(inactiveDeviceIcon);
                        } else {
                            Logger.u_warn("Shouldn't happen", swapEvent.getNewController(), "Unknown swap controller");
                        }
                    } else if (e instanceof LoginEvent login) {
                        if (leftConnection == null) {
                            leftConnection = login.getConnection();
                            leftPanel.setVisible(true);
                            leftStatus.setText(leftConnection.getRealSrcAddress().getAddress().getHostAddress());
                            leftDevice.setIcon(activeDeviceIcon);
                            rightDevice.setIcon(inactiveDeviceIcon);
                        } else if (rightConnection == null) {
                            rightConnection = login.getConnection();
                            swap.setVisible(true);
                            rightPanel.setVisible(true);
                            rightStatus.setText(leftConnection.getRealSrcAddress().getAddress().getHostAddress());
                        }
                    } else if (e instanceof DisconnectEvent disconnect) {
                        if (disconnect.getConnection() == rightConnection) {
                            rightConnection = null;
                            rightPanel.setVisible(false);
                            swap.setVisible(false);
                            rightStatus.setText("");
                        } else if (disconnect.getConnection() == leftConnection) {
                            leftConnection = null;
                            leftPanel.setVisible(false);
                            swap.setVisible(false);
                            leftStatus.setText("");
                        }
                    }


                });

            });
        }
        parent.add(body, BorderLayout.CENTER);
    }

    public void updateAccounts() {
        DefaultComboBoxModel<Account> model = (DefaultComboBoxModel<Account>) this.accounts.getModel();
        Account selected = (Account) this.accounts.getSelectedItem();
        model.removeAllElements();
        model.addElement(null);
        for (Account account : Proxy.getAccountManager().getAccounts()) {
            model.addElement(account);
        }
        this.accounts.setSelectedItem(selected);
    }

    private void addFooter(final Container parent) {
        JPanel footer = new JPanel();
        footer.setLayout(new GridBagLayout());

        this.stateLabel = new JLabel("");
        this.stateLabel.setVisible(false);
        GBC.create(footer).grid(0, 0).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).anchor(GBC.WEST).fill(GBC.HORIZONTAL).add(this.stateLabel);


        this.startButton = new JButton();
        I18n.link(this.startButton, "tab.general.state.start");
        this.startButton.addActionListener(event -> {
            if (this.startButton.getText().equalsIgnoreCase(I18n.get("tab.general.state.start"))) {
                this.start();
            } else if (this.startButton.getText().equalsIgnoreCase(I18n.get("tab.general.state.stop"))) {
                this.stop();
            }
        });
        if (WinRedirect.isSupported()) {
            this.pauseButton = new JButton();
            I18n.link(this.pauseButton, "tab.general.pause.suspend");
            I18n.linkTooltip(this.pauseButton, "tab.general.pause.suspend.tooltip");
            this.pauseButton.addActionListener(event -> {
                if (this.pauseButton.getText().equalsIgnoreCase(I18n.get("tab.general.pause.suspend"))) {
                    Proxy.suspendRedirect();
                } else if (this.pauseButton.getText().equalsIgnoreCase(I18n.get("tab.general.pause.resume"))) {
                    Proxy.resumeRedirect();
                }
            });
            Proxy.registerEvent(event -> {
                if (event instanceof RedirectStateChangeEvent changeEvent) {
                    if (changeEvent.getState() == RedirectStateChangeEvent.State.PAUSED) {
                        this.pauseButton.setText(I18n.get("tab.general.pause.resume"));
                        this.pauseButton.setToolTipText(I18n.get("tab.general.pause.resume.tooltip"));
                    } else {
                        this.pauseButton.setText(I18n.get("tab.general.pause.suspend"));
                        this.pauseButton.setToolTipText(I18n.get("tab.general.pause.suspend.tooltip"));
                    }
                }
            });
            this.pauseButton.setEnabled(false);
        }
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, 2, BORDER_PADDING, 0));
        buttons.add(this.startButton);
        if (this.pauseButton != null) {
            buttons.add(this.pauseButton);
        }

        JPanel padding = new JPanel();
        padding.setLayout(new GridBagLayout());
//        GBC.create(padding).grid(0, 0).weightx(1).insets(0, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GBC.HORIZONTAL).add(buttons);
//
//        parent.add(padding, BorderLayout.SOUTH);

        GBC.create(footer).grid(0, 1).weightx(1).insets(0, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).anchor(GBC.WEST).fill(GBC.HORIZONTAL).add(buttons);

        parent.add(footer, BorderLayout.SOUTH);


    }

    private void setComponentsEnabled(final boolean state) {
        this.serverAddress.setEnabled(state);
        this.accounts.setEnabled(state);

        this.window.advancedTab.proxyOnlineMode.setEnabled(state);

        this.window.advancedTab.chatSigning.setEnabled(state);
        if (WinRedirect.isSupported()) {
            this.window.advancedTab.tracerouteFix.setEnabled(state);
            this.window.advancedTab.mdnsDisable.setEnabled(state);
            this.window.advancedTab.routerSpoof.setEnabled(state);
            this.window.advancedTab.blockTraffic.setEnabled(state);
            this.window.advancedTab.updateNetworkAdapterEnabled(null);
        }
    }

    private void updateStateLabel() {
        I18n.link(this.stateLabel, "tab.general.state.running", (t, s) -> {
            t.setText(I18n.get(s, Proxy.proxyAddress.getHostName() + ":" + Proxy.proxyAddress.getPort()));
        });
        this.stateLabel.setForeground(Color.GREEN);
        this.stateLabel.setVisible(true);
    }

    private void start() {

        this.setComponentsEnabled(false);
        this.startButton.setEnabled(false);

        I18n.link(GeneralTab.this.startButton, "tab.general.state.starting");
        new Thread(() -> {
            final String serverAddress = this.serverAddress.getText().trim();
            try {
                try {

                    try {
                        Proxy.getConfig().setServerAddress(serverAddress);
                    } catch (Throwable t) {
                        throw new IllegalArgumentException(I18n.get("tab.general.error.invalid_server_address"));
                    }
                    if (this.accounts.getSelectedItem() instanceof Account account) {
                        Proxy.setAccount(account);
                        if (account.refresh()) {
                            Proxy.getAccountManager().save();
                        }
                    } else {
                        Proxy.setAccount(null);
                    }

//                    if (Proxy.getAccount() == null) {
//                        this.window.accountsTab.markSelected(0);
//                    }
                    if (this.window.advancedTab.networkAdapters != null && this.window.advancedTab.networkAdapters.getSelectedItem() instanceof NetworkInterface ni) {
                        Proxy.setTargetAdapter(ni);
                    }

                    this.applyGuiState();
                    this.window.advancedTab.applyGuiState();
                    Proxy.getConfig().save();
                } catch (Throwable t) {
                    SwingUtilities.invokeLater(() -> window.showError(t.getMessage()));
                    throw t;
                }

                try {
                    Proxy.startProxy();
                } catch (Throwable e) {
                    SwingUtilities.invokeLater(() -> window.showError(I18n.get("tab.general.error.failed_to_start", e.getMessage())));
                    throw e;
                }

                SwingUtilities.invokeLater(() -> {
                    this.updateStateLabel();
                    if (this.pauseButton != null) {
                        this.pauseButton.setEnabled(true);
                        I18n.link(GeneralTab.this.pauseButton, "tab.general.pause.suspend");
                    }
                    this.startButton.setEnabled(true);
                    I18n.link(GeneralTab.this.startButton, "tab.general.state.stop");
                });
            } catch (Throwable e) {
                Logger.error("Error while starting Proxy", e);
                SwingUtilities.invokeLater(() -> {
                    this.setComponentsEnabled(true);
                    if (this.pauseButton != null) {
                        this.pauseButton.setEnabled(false);
                        I18n.link(GeneralTab.this.startButton, "tab.general.pause.suspend");
                    }
                    this.startButton.setEnabled(true);
                    I18n.link(GeneralTab.this.startButton, "tab.general.state.start");
                    this.stateLabel.setVisible(false);
                });
            }
        }).start();
    }

    private void stop() {
        this.startButton.setEnabled(false);
        if (this.pauseButton != null) {
            this.pauseButton.setEnabled(false);
        }
        Proxy.stopProxy();

        int delay = 800;
        Timer timer = new Timer(delay, e -> {
            GeneralTab.this.stateLabel.setVisible(false);
            GeneralTab.this.startButton.setEnabled(true);
            I18n.link(GeneralTab.this.startButton, "tab.general.state.start");
            GeneralTab.this.setComponentsEnabled(true);
            if (GeneralTab.this.pauseButton != null) {
                GeneralTab.this.pauseButton.setEnabled(false);
                I18n.link(GeneralTab.this.pauseButton, "tab.general.pause.suspend");
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void applyGuiState() {
        Proxy.getConfig().setServerAddress(this.serverAddress.getText());
        if (this.accounts.getSelectedItem() instanceof Account account) {
            Proxy.getConfig().account.set(account.getName());
        } else {
            Proxy.getConfig().account.set(null);
        }

    }

}