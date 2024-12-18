package net.java.mproxy.proxy.event;

import net.java.mproxy.proxy.session.DualConnection;

public class ProxyStateEvent extends Event {
    public enum State {
        STOPPED, STARTING, STARTED, STOPPING
    }

    private final State state;

    public ProxyStateEvent(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }
}
