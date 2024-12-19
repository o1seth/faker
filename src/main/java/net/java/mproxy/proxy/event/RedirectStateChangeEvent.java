package net.java.mproxy.proxy.event;

import net.java.mproxy.proxy.session.ProxyConnection;

public class RedirectStateChangeEvent extends Event {
    public enum State {
        RESUMED, PAUSED
    }

    private final State state;

    public RedirectStateChangeEvent(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }
}
