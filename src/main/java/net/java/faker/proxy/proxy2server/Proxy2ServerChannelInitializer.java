package net.java.faker.proxy.proxy2server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.java.faker.proxy.PacketRegistry;
import net.java.faker.proxy.session.ProxyConnection;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;

import java.util.function.Supplier;

public class Proxy2ServerChannelInitializer extends MinecraftChannelInitializer {

    public Proxy2ServerChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        super.initChannel(channel);
        ProxyConnection proxyConnection = ProxyConnection.fromChannel(channel);
        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(new PacketRegistry(true, proxyConnection.getVersion()));
    }
}
