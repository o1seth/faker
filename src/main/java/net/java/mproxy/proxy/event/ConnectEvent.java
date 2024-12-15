package net.java.mproxy.proxy.event;

import net.java.mproxy.proxy.session.ProxyConnection;

public class ConnectEvent extends Event {
    private final ProxyConnection connection;

    public ConnectEvent(ProxyConnection connection) {
        this.connection = connection;
    }

    public ProxyConnection getConnection() {
        return connection;
    }
}
