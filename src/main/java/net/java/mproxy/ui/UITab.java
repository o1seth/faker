package net.java.mproxy.ui;

import javax.swing.*;

public abstract class UITab {

    protected final Window window;
    private final String name;
    private final String translationKey;
    protected final JPanel contentPane;
    private int index;
    JTabbedPane owner;

    public UITab(final Window viaProxyWindow, final String name) {
        this.window = viaProxyWindow;
        this.translationKey = "tab." + name + ".name";
        this.name = I18n.get(translationKey);
        this.contentPane = new JPanel();

        this.contentPane.setLayout(null);
        this.init(this.contentPane);
    }

    public String getName() {
        return this.name;
    }

    public void add(final JTabbedPane tabbedPane) {
        index = tabbedPane.getComponentCount();
        this.owner = tabbedPane;
        tabbedPane.addTab(this.name, this.contentPane);
        I18n.link(this, this.translationKey);
    }

    public JTabbedPane getOwner() {
        return owner;
    }

    public int getIndex() {
        return index;
    }

    protected abstract void init(final JPanel contentPane);

    protected void onTabOpened() {
    }

}
