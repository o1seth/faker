package net.java.faker.proxy.event;

import net.java.faker.proxy.session.DualConnection;
import net.java.faker.proxy.session.ProxyConnection;

public class LoginEvent extends Event {
    private final ProxyConnection connection;
    private final DualConnection dualConnection;

    public LoginEvent(ProxyConnection connection, DualConnection dualConnection) {
        this.connection = connection;
        this.dualConnection = dualConnection;
    }

    public ProxyConnection getConnection() {
        return connection;
    }

    public DualConnection getDualConnection() {
        return dualConnection;
    }
}
