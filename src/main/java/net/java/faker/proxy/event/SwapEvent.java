package net.java.faker.proxy.event;

import net.java.faker.proxy.session.ProxyConnection;

public class SwapEvent extends Event {
    private final ProxyConnection newController;
    public SwapEvent(ProxyConnection newController) {
        this.newController = newController;
    }

    public ProxyConnection getNewController() {
        return newController;
    }
}
