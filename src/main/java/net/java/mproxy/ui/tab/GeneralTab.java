package net.java.mproxy.ui.tab;

import net.java.mproxy.Proxy;
import net.java.mproxy.ui.GBC;
import net.java.mproxy.ui.I18n;
import net.java.mproxy.ui.UITab;
import net.java.mproxy.ui.Window;
import net.java.mproxy.ui.elements.LinkLabel;

import javax.swing.*;
import java.awt.*;

import static net.java.mproxy.ui.Window.BODY_BLOCK_PADDING;
import static net.java.mproxy.ui.Window.BORDER_PADDING;

public class GeneralTab extends UITab {

    JTextField serverAddress;


    //    JLabel stateLabel;
    JButton startButton;
    JButton pauseButton;

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

        LinkLabel discord = new LinkLabel("Discord", "https://discord.gg/viaversion");
        GBC.create(header).grid(0, 0).width(0).insets(BORDER_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(discord);

        JLabel title = new JLabel("Faker");
        title.setFont(title.getFont().deriveFont(30F));
        GBC.create(header).grid(1, 0).weightx(1).width(0).insets(BORDER_PADDING, 0, 0, 0).anchor(GBC.CENTER).add(title);

//        JLabel copyright = new JLabel("©© © RK_01 & Lenni0451");
//        GBC.create(header).grid(2, 0).width(0).insets(BORDER_PADDING, 0, 0, BORDER_PADDING).anchor(GBC.NORTHEAST).add(copyright);

        parent.add(header, BorderLayout.NORTH);
    }

    private void addBody(final Container parent) {
        JPanel body = new JPanel();
        body.setLayout(new GridBagLayout());

        int gridy = 0;
        {
            JLabel serverAddressLabel = new JLabel(I18n.get("tab.general.server_address.label"));
            serverAddressLabel.setToolTipText(I18n.get("tab.general.server_address.tooltip"));
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(serverAddressLabel);

            this.serverAddress = new JTextField();
            this.serverAddress.setToolTipText(I18n.get("tab.general.server_address.tooltip"));
            Proxy.getSaveManager().loadTextField("server_address", this.serverAddress);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(this.serverAddress);
        }


        parent.add(body, BorderLayout.CENTER);
    }

    private void addFooter(final Container parent) {


        this.startButton = new JButton(I18n.get("tab.general.state.start"));
        this.startButton.addActionListener(event -> {
            if (this.startButton.getText().equalsIgnoreCase(I18n.get("tab.general.state.start"))) {
                this.start();
            } else if (this.startButton.getText().equalsIgnoreCase(I18n.get("tab.general.state.stop"))) {
                this.stop();
            }
        });

        this.pauseButton = new JButton(I18n.get("tab.general.pause.suspend"));
        this.pauseButton.addActionListener(event -> {

        });
        this.pauseButton.setEnabled(false);
        JPanel footer = new JPanel();
        footer.setLayout(new GridLayout(1, 2, BORDER_PADDING, 0));
        footer.add(this.startButton);
        footer.add(this.pauseButton);
        JPanel padding = new JPanel();
        padding.setLayout(new GridBagLayout());
        GBC.create(padding).grid(0, 0).weightx(1).insets(0, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GBC.HORIZONTAL).add(footer);

        parent.add(padding, BorderLayout.SOUTH);

    }

    private void setComponentsEnabled(final boolean state) {
        this.serverAddress.setEnabled(state);


        this.window.advancedTab.proxyOnlineMode.setEnabled(state);

        this.window.advancedTab.chatSigning.setEnabled(state);

    }

    private void updateStateLabel() {
//        if (Proxy.getConfig().getBindAddress() instanceof InetSocketAddress isa) {
//            this.stateLabel.setText(I18n.get("tab.general.state.running", "1.7+", "127.0.0.1:" + isa.getPort()));
//        } else {
//            this.stateLabel.setText(I18n.get("tab.general.state.running", "1.7+", AddressUtil.toString(Proxy.getConfig().getBindAddress())));
//        }
//        this.stateLabel.setForeground(Color.GREEN);
//        this.stateLabel.setVisible(true);
    }

    private void start() {
        this.pauseButton.setEnabled(true);
//        if (Proxy.getSaveManager().uiSave.get("notice.ban_warning") == null) {
//            Proxy.getSaveManager().uiSave.put("notice.ban_warning", "true");
//            Proxy.getSaveManager().save();
//
//            window.showWarning("<html><div style='text-align: center;'>" + I18n.get("tab.general.warning.ban_warning.line1") + "<br><b>" + I18n.get("tab.general.warning.risk") + "</b></div></html>");
//        }


        this.setComponentsEnabled(false);
        this.startButton.setEnabled(false);
        this.startButton.setText(I18n.get("tab.general.state.starting"));

//        new Thread(() -> {
//            final String serverAddress = this.serverAddress.getText().trim();
//            final String bindAddress = this.window.advancedTab.bindAddress.getText().trim();
//            final String proxyUrl = this.window.advancedTab.proxy.getText().trim();
//
//            try {
//                try {
//
//                    try {
//                        Proxy.getConfig().setTargetAddress(AddressUtil.parse(serverAddress, serverVersion));
//                    } catch (Throwable t) {
//                        throw new IllegalArgumentException(I18n.get("tab.general.error.invalid_server_address"));
//                    }
//                    if (Proxy.getAccount() == null) {
//                        this.window.accountsTab.markSelected(0);
//                    }
//
//
//                    try {
//                        Proxy.getConfig().setBindAddress(AddressUtil.parse(bindAddress, null));
//                    } catch (Throwable t) {
//                        throw new IllegalArgumentException(I18n.get("tab.general.error.invalid_bind_address"));
//                    }
//
//                    this.applyGuiState();
//                    this.window.advancedTab.applyGuiState();
//                    Proxy.getConfig().save();
//                    Proxy.getSaveManager().save();
//                } catch (Throwable t) {
//                    SwingUtilities.invokeLater(() -> window.showError(t.getMessage()));
//                    throw t;
//                }
//
//                try {
//                    Proxy.startProxy();
//                } catch (Throwable e) {
//                    SwingUtilities.invokeLater(() -> window.showError(I18n.get("tab.general.error.failed_to_start")));
//                    throw e;
//                }
//
//                SwingUtilities.invokeLater(() -> {
//                    this.updateStateLabel();
//                    this.stateButton.setEnabled(true);
//                    this.stateButton.setText(I18n.get("tab.general.state.stop"));
//                });
//            } catch (Throwable e) {
//                Logger.LOGGER.error("Error while starting Proxy", e);
//                SwingUtilities.invokeLater(() -> {
//                    this.setComponentsEnabled(true);
//                    this.stateButton.setEnabled(true);
//                    this.stateButton.setText(I18n.get("tab.general.state.start"));
//                    this.stateLabel.setVisible(false);
//                });
//            }
//        }).start();
    }

    private void stop() {
        Proxy.stopProxy();

//        this.stateLabel.setVisible(false);
        this.startButton.setText(I18n.get("tab.general.state.start"));
        this.setComponentsEnabled(true);
    }

    //    @EventHandler(events = UICloseEvent.class)
    void applyGuiState() {
        Proxy.getSaveManager().put("server_address", this.serverAddress.getText());
    }

}