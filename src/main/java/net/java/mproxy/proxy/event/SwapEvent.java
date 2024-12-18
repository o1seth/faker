package net.java.mproxy.proxy.event;

import net.java.mproxy.proxy.session.DualConnection;
import net.java.mproxy.proxy.session.ProxyConnection;

public class SwapEvent extends Event {
    private final ProxyConnection newController;
    public SwapEvent(ProxyConnection newController) {
        this.newController = newController;
    }

    public ProxyConnection getNewController() {
        return newController;
    }
}
