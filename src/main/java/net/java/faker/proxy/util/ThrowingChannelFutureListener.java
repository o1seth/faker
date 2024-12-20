package net.java.faker.proxy.util;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public interface ThrowingChannelFutureListener extends ChannelFutureListener {

    @Override
    default void operationComplete(ChannelFuture future) {
        try {
            this.operationComplete0(future);
        } catch (Throwable cause) {
            future.channel().pipeline().fireExceptionCaught(cause);
        }
    }

    void operationComplete0(ChannelFuture future) throws Throwable;

}
