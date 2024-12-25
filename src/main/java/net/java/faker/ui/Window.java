/*
 * This file is part of faker - https://github.com/o1seth/faker
 * Copyright (C) 2024 o1seth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.java.faker.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import net.java.faker.Proxy;
import net.java.faker.proxy.dhcp.Dhcp;
import net.java.faker.ui.tab.*;
import net.java.faker.util.Util;
import net.java.faker.util.logging.Logger;

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
    public final DHCPTab dhcpTab = registerTab(new DHCPTab(this));
    PopupMenu trayMenu;
    TrayIcon trayIcon;

    private Window() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> showException(e));
        this.setLookAndFeel();
        this.initWindow();
        this.initTabs();
        this.initTray();
        ToolTipManager.sharedInstance().setInitialDelay(100);
        ToolTipManager.sharedInstance().setDismissDelay(10_000);

        SwingUtilities.updateComponentTreeUI(this);

        this.setVisible(true);

    }

    public void hideTray() {
        if (this.trayIcon != null) {
            SystemTray tray = SystemTray.getSystemTray();
            tray.remove(this.trayIcon);
        }
    }

    public void showTray() {
        try {
            if (this.trayIcon != null) {
                SystemTray tray = SystemTray.getSystemTray();
                tray.add(this.trayIcon);
            }
        } catch (AWTException e) {
            Logger.error(e.getMessage());
        }
    }

    private void initTray() {

        if (SystemTray.isSupported()) {
            Image image = Util.getResourceImage("/assets/faker/icon/icon_16.png");
            if (image == null) {
                return;
            }
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
            Logger.error("Failed set look and feel", t);
        }
    }


    private void initWindow() {
        this.setTitle("Faker " + Proxy.VERSION);
        try {
            List<Image> icons = new ArrayList<>();
            Image icon32 = Util.getResourceImage("/assets/faker/icon/icon_32.png");
            Image icon64 = Util.getResourceImage("/assets/faker/icon/icon_64.png");
            if (icon32 != null) {
                icons.add(icon32);
            }
            if (icon64 != null) {
                icons.add(icon64);
            }
            this.setIconImages(icons);
        } catch (Exception ignored) {
        }

        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                generalTab.applyGuiState();
                advancedTab.applyGuiState();
                Proxy.getConfig().save();
                if (!Proxy.isStarted() && !Dhcp.isStarted()) {
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

        this.setSize(528, 398);
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
        Logger.error("Caught exception in thread " + Thread.currentThread().getName() + t);
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
        JOptionPane.showMessageDialog(Window.getInstance(), message, "Faker", type);
    }

}