package net.java.mproxy.ui.tab;

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
//            this.proxyOnlineMode.setSelected(Proxy.getConfig().isProxyOnlineMode());
            checkboxes.add(this.proxyOnlineMode);
        }

        {
            this.chatSigning = new JCheckBox(I18n.get("tab.advanced.chat_signing.label"));
            this.chatSigning.setToolTipText(I18n.get("tab.advanced.chat_signing.tooltip"));
//            this.chatSigning.setSelected(Proxy.getConfig().shouldSignChat());
            checkboxes.add(this.chatSigning);
        }

        GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BODY_BLOCK_PADDING).fill(GBC.BOTH).weight(1, 1).add(checkboxes);

        parent.add(body, BorderLayout.NORTH);
    }


//    @EventHandler(events = UICloseEvent.class)
//    void applyGuiState() {
//        ViaProxy.getSaveManager().uiSave.put("bind_address", this.bindAddress.getText());
//        ViaProxy.getSaveManager().uiSave.put("proxy", this.proxy.getText());
//        ViaProxy.getConfig().setProxyOnlineMode(this.proxyOnlineMode.isSelected());
//        ViaProxy.getSaveManager().uiSave.put("legacy_skin_loading", String.valueOf(this.legacySkinLoading.isSelected()));
//        ViaProxy.getConfig().setChatSigning(this.chatSigning.isSelected());
//        ViaProxy.getConfig().setIgnoreProtocolTranslationErrors(this.ignorePacketTranslationErrors.isSelected());
//        ViaProxy.getConfig().setAllowBetaPinging(this.allowBetaPinging.isSelected());
//        ViaProxy.getConfig().setSimpleVoiceChatSupport(this.simpleVoiceChatSupport.isSelected());
//        ViaProxy.getConfig().setFakeAcceptResourcePacks(this.fakeAcceptResourcePacks.isSelected());
//    }

}