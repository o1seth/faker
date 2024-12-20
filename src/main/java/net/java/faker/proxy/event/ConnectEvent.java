package net.java.faker.proxy.event;

import net.java.faker.proxy.session.ProxyConnection;

public class ConnectEvent extends Event {
    private final ProxyConnection connection;

    public ConnectEvent(ProxyConnection connection) {
        this.connection = connection;
    }

    public ProxyConnection getConnection() {
        return connection;
    }
}
