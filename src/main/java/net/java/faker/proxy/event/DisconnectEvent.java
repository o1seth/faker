package net.java.faker.proxy.event;

import net.java.faker.proxy.session.ProxyConnection;

public class DisconnectEvent extends Event {
    private final ProxyConnection connection;

    public DisconnectEvent(ProxyConnection connection) {
        this.connection = connection;
    }

    public ProxyConnection getConnection() {
        return connection;
    }
}
