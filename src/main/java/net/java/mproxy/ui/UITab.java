package net.java.mproxy.ui;

import javax.swing.*;

public abstract class UITab {

    protected final Window viaProxyWindow;
    private final String name;
    protected final JPanel contentPane;

    public UITab(final Window viaProxyWindow, final String name) {
        this.viaProxyWindow = viaProxyWindow;
        this.name = name;
        this.contentPane = new JPanel();

        this.contentPane.setLayout(null);
        this.init(this.contentPane);
    }

    public String getName() {
        return this.name;
    }

    public void add(final JTabbedPane tabbedPane) {
        tabbedPane.addTab(this.name, this.contentPane);
    }

    protected abstract void init(final JPanel contentPane);

    protected void onTabOpened() {
    }

}
