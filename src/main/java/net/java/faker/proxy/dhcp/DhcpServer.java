package net.java.faker.proxy.dhcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.directory.server.dhcp.io.DhcpInterfaceManager;
import org.apache.directory.server.dhcp.service.DhcpService;
import org.apache.directory.server.dhcp.service.manager.LeaseManager;
import org.apache.directory.server.dhcp.service.manager.LeaseManagerDhcpService;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.ThreadFactory;

public class DhcpServer extends DhcpInterfaceManager {


    private final DhcpService service;
    private final int port;
    private Channel channel;

    public DhcpServer(@Nonnull DhcpService service, @Nonnegative int port) {
        this.service = service;
        this.port = port;
    }

    public DhcpServer(@Nonnull DhcpService service) {
        this(service, DhcpService.SERVER_PORT);
    }

    public DhcpServer(@Nonnull LeaseManager manager, @Nonnegative int port) {
        this(new LeaseManagerDhcpService(manager), port);
    }

    public DhcpServer(@Nonnull LeaseManager manager) {
        this(new LeaseManagerDhcpService(manager));
    }

    public void start() throws IOException, InterruptedException {
        super.start();

        ThreadFactory factory = new DefaultThreadFactory("dhcp-server");
        EventLoopGroup group = new NioEventLoopGroup(0, factory);

        Bootstrap b = new Bootstrap();
        b.group(group);
        b.channel(NioDatagramChannel.class);
        b.option(ChannelOption.SO_BROADCAST, true);
        b.handler(new DhcpHandler(service, this));
        channel = b.bind(port).sync().channel();
    }

    public void stop() throws IOException, InterruptedException {
        EventLoop loop = channel.eventLoop();
        channel.close().sync();
        channel = null;
        loop.shutdownGracefully();
        super.stop();
    }

    public Channel getChannel() {
        return channel;
    }
}