package net.java.mproxy.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import net.java.mproxy.Proxy;
import net.java.mproxy.ui.tab.AccountsTab;
import net.java.mproxy.ui.tab.AdvancedTab;
import net.java.mproxy.ui.tab.GeneralTab;
import net.java.mproxy.ui.tab.UISettingsTab;
import net.java.mproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Window extends JFrame {
    private static Window INSTANCE;

    public static synchronized Window getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Window();
        }
        return INSTANCE;
    }

    public static final int BORDER_PADDING = 10;
    public static final int BODY_BLOCK_PADDING = 10;


    public final JTabbedPane contentPane = new JTabbedPane();
    private final List<UITab> tabs = new ArrayList<>();

    public final GeneralTab generalTab = registerTab(new GeneralTab(this));
    public final AdvancedTab advancedTab = registerTab(new AdvancedTab(this));
    public final AccountsTab accountsTab = registerTab(new AccountsTab(this));
    public final UISettingsTab uiSettingsTab = registerTab(new UISettingsTab(this));

    PopupMenu trayMenu;
    TrayIcon trayIcon;

    private Window() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> showException(e));
        this.setLookAndFeel();
        this.loadIcons();
        this.initWindow();
        this.initTabs();
        this.initTray();
        ToolTipManager.sharedInstance().setInitialDelay(100);
        ToolTipManager.sharedInstance().setDismissDelay(10_000);

        SwingUtilities.updateComponentTreeUI(this);

        this.setVisible(true);
    }

    public void hideTray() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            tray.remove(this.trayIcon);
        }
    }

    public void showTray() {
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                tray.add(this.trayIcon);
            }
        } catch (AWTException e) {
            System.err.println(e);
        }
    }

    private void initTray() {

        if (SystemTray.isSupported()) {
            Image image = Toolkit.getDefaultToolkit().getImage(Window.class.getResource("/assets/faker/icon/tray.png"));

            ActionListener listener = e -> this.setVisible(true);

            this.trayMenu = new PopupMenu();
            MenuItem showButton = new MenuItem();
            I18n.link(showButton, "tray.show");
            showButton.addActionListener(listener);
            this.trayMenu.add(showButton);

            MenuItem exitButton = new MenuItem();
            I18n.link(exitButton, "tray.exit");
            exitButton.addActionListener(e -> {
                Proxy.getConfig().save();
                System.exit(0);
            });
            this.trayMenu.add(exitButton);
            this.trayIcon = new TrayIcon(image, "Faker", this.trayMenu);
            this.trayIcon.addActionListener(listener);
        }
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
        try {
            List<Image> icons = new ArrayList<>();
            icons.add(Toolkit.getDefaultToolkit().getImage(Window.class.getResource("/assets/faker/icon/icon_32.png")));
            icons.add(Toolkit.getDefaultToolkit().getImage(Window.class.getResource("/assets/faker/icon/icon_64.png")));
            this.setIconImages(icons);
        } catch (Exception ignored) {

        }

//        this.setIconImage(this.icon.getImage());
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                generalTab.applyGuiState();
                advancedTab.applyGuiState();
                Proxy.getConfig().save();
                if (!Proxy.isStarted()) {
                    System.exit(0);
                }
            }

        });

        this.addComponentListener(new ComponentAdapter() {
            public void componentHidden(ComponentEvent e) {
                showTray();
            }

            public void componentShown(ComponentEvent e) {
                hideTray();
            }
        });

        this.setSize(500, 398);
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
        JOptionPane.showMessageDialog(Window.getInstance(), message, "ViaProxy", type);
    }

}