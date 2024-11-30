package net.java.mproxy.proxy.util.chat;


import java.util.UUID;

public class MessageLink {

    private final int index;
    private final UUID sender;
    private final UUID sessionId;

    public MessageLink(final UUID sender, final UUID sessionId) {
        this(0, sender, sessionId);
    }

    public MessageLink(final int index, final UUID sender, final UUID sessionId) {
        this.index = index;
        this.sender = sender;
        this.sessionId = sessionId;
    }

    public void update(final DataConsumer dataConsumer) {
        dataConsumer.accept(this.sender);
        dataConsumer.accept(this.sessionId);
        dataConsumer.accept(Ints.toByteArray(this.index));
    }

    public MessageLink next() {
        return this.index == Integer.MAX_VALUE ? null : new MessageLink(this.index + 1, this.sender, this.sessionId);
    }

}
