package net.java.mproxy.ui.tab;

import net.java.mproxy.Proxy;
import net.java.mproxy.ui.GBC;
import net.java.mproxy.ui.I18n;
import net.java.mproxy.ui.UITab;
import net.java.mproxy.ui.Window;

import javax.swing.*;
import java.awt.*;

import static net.java.mproxy.ui.Window.BODY_BLOCK_PADDING;
import static net.java.mproxy.ui.Window.BORDER_PADDING;

public class AdvancedTab extends UITab {


    JCheckBox proxyOnlineMode;
    JCheckBox chatSigning;
    JCheckBox tracerouteFix;
    JCheckBox mdnsDisable;

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

        GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BODY_BLOCK_PADDING).fill(GBC.BOTH).weight(1, 1).add(checkboxes);

        parent.add(body, BorderLayout.NORTH);
    }


    //    @EventHandler(events = UICloseEvent.class)
    void applyGuiState() {

        Proxy.getConfig().onlineMode.set(this.proxyOnlineMode.isSelected());
        Proxy.getConfig().signChat.set(this.chatSigning.isSelected());
    }

}