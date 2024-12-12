package net.java.mproxy.ui.tab;

import net.java.mproxy.ui.GBC;
import net.java.mproxy.ui.I18n;
import net.java.mproxy.ui.Window;
import net.java.mproxy.ui.elements.LinkLabel;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import static net.java.mproxy.ui.Window.BORDER_PADDING;
import static net.java.mproxy.ui.Window.BODY_BLOCK_PADDING;

public class AddAccountPopup extends JDialog {

    private final Window parent;
    private final StepMsaDeviceCode.MsaDeviceCode deviceCode;
    private boolean externalClose;

    public AddAccountPopup(final Window parent, final StepMsaDeviceCode.MsaDeviceCode deviceCode, final Consumer<AddAccountPopup> popupConsumer, final Runnable closeListener) {
        super(parent, true);
        this.parent = parent;
        this.deviceCode = deviceCode;
        popupConsumer.accept(this);

        this.initWindow(closeListener);
        this.initComponents();
        this.pack();
        this.setVisible(true);
    }

    private void initWindow(final Runnable closeListener) {
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!AddAccountPopup.this.externalClose) closeListener.run();
            }
        });
        this.setTitle(I18n.get("popup.login_account.title"));
        this.setSize(400, 140);
        this.setResizable(false);
        this.setLocationRelativeTo(this.parent);
    }

    private void initComponents() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        {
            JLabel browserLabel = new JLabel("<html><p>" + I18n.get("popup.login_account.instructions.browser") + "</p></html>");
            GBC.create(contentPane).grid(0, 0).weightx(1).insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(browserLabel);

            GBC.create(contentPane).grid(0, 1).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(new LinkLabel(this.deviceCode.getDirectVerificationUri(), AddAccountPopup.this.deviceCode.getDirectVerificationUri()));

            JLabel closeInfo = new JLabel("<html><p>" + I18n.get("popup.login_account.instructions.close") + "</p></html>");
            GBC.create(contentPane).grid(0, 2).weightx(1).insets(BODY_BLOCK_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GBC.HORIZONTAL).add(closeInfo);
        }
        this.setContentPane(contentPane);
    }

    public void markExternalClose() {
        this.externalClose = true;
    }


}
