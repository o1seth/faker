package net.java.faker.proxy.client2proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.java.faker.proxy.PacketRegistry;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;

import java.util.function.Supplier;

public class Client2ProxyChannelInitializer extends MinecraftChannelInitializer {


    public Client2ProxyChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        channel.pipeline().addLast(HandshakeCodec.HANDSHAKE_HANDLER_NAME, new HandshakeCodec());
        super.initChannel(channel);

        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(new PacketRegistry(false, -1));
    }

}
