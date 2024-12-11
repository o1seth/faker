package net.java.mproxy.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import net.java.mproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Window extends JFrame {
    public static final int BORDER_PADDING = 10;
    public static final int BODY_BLOCK_PADDING = 10;
//    public final LambdaManager eventManager = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator(JavaBypass.TRUSTED_LOOKUP));

    public static final int BORDER_PADDING = 10;
    public static final int BODY_BLOCK_PADDING = 10;

    public final JTabbedPane contentPane = new JTabbedPane();
    private final List<UITab> tabs = new ArrayList<>();

    public final GeneralTab generalTab = registerTab(new GeneralTab(this));
    public final AdvancedTab advancedTab = registerTab(new AdvancedTab(this));
    public final AccountsTab accountsTab = registerTab(new AccountsTab(this));
    //    public final RealmsTab realmsTab = registerTab(new RealmsTab(this));
    public final UISettingsTab uiSettingsTab = registerTab(new UISettingsTab(this));

    //    private ImageIcon icon;


    public Window() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> showException(e));
//        this.eventManager.register(this);

        this.setLookAndFeel();
        this.loadIcons();
        this.initWindow();
        this.initTabs();

//        FlatInspector.install("ctrl shift I");
//        FlatUIDefaultsInspector.install("ctrl shift O");
        ToolTipManager.sharedInstance().setInitialDelay(100);
        ToolTipManager.sharedInstance().setDismissDelay(10_000);
        SwingUtilities.updateComponentTreeUI(this);
        this.setVisible(true);
    }

    private void setLookAndFeel() {
        try {
            FlatDarkLaf.setup();

            UIManager.getLookAndFeelDefaults().put("TextComponent.arc", 5);
            UIManager.getLookAndFeelDefaults().put("Button.arc", 5);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void loadIcons() {
//        this.icon = new ImageIcon(this.getClass().getClassLoader().getResource("assets/viaproxy/icons/icon.png"));
    }

    private void initWindow() {
        this.setTitle("Faker");
//        this.setIconImage(this.icon.getImage());
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
//                Window.this.eventManager.call(new UICloseEvent());
//                ViaProxy.getConfig().save();
//                ViaProxy.getSaveManager().save();
            }
        });
        this.setSize(500, 380);
        this.setMinimumSize(this.getSize());
        this.setLocationRelativeTo(null);
        this.setContentPane(this.contentPane);
    }

    private <T extends UITab> T registerTab(UITab tab) {
        this.tabs.add(tab);
        return (T) tab;
    }

    private void initTabs() {
        for (UITab tab : this.tabs) {
            tab.add(this.contentPane);
        }

        this.contentPane.addChangeListener(e -> {
            int selectedIndex = contentPane.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < Window.this.tabs.size())
                Window.this.tabs.get(selectedIndex).onTabOpened();
        });
    }

    public static void openURL(final String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Throwable t) {
            showInfo("generic.could_not_open_url " + url);
        }
    }

    public static void showException(final Throwable t) {
        Logger.LOGGER.error("Caught exception in thread " + Thread.currentThread().getName(), t);
        StringBuilder builder = new StringBuilder("An error occurred:\n");
        builder.append("[").append(t.getClass().getSimpleName()).append("] ").append(t.getMessage()).append("\n");
        for (StackTraceElement element : t.getStackTrace()) builder.append(element.toString()).append("\n");
        showError(builder.toString());
    }

    public static void showInfo(final String message) {
        showNotification(message, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarning(final String message) {
        showNotification(message, JOptionPane.WARNING_MESSAGE);
    }

    public static void showError(final String message) {
        showNotification(message, JOptionPane.ERROR_MESSAGE);
    }

    public static void showNotification(final String message, final int type) {
//        JOptionPane.showMessageDialog(ViaProxy.getForegroundWindow(), message, "ViaProxy", type);
    }

}