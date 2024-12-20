package net.java.faker.proxy.event;

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
