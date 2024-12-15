package net.java.mproxy.proxy.event;

import net.java.mproxy.proxy.session.ProxyConnection;

public class DisconnectEvent extends Event {
    private final ProxyConnection connection;

    public DisconnectEvent(ProxyConnection connection) {
        this.connection = connection;
    }

    public ProxyConnection getConnection() {
        return connection;
    }
}
