package net.java.faker.proxy.dhcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.anarres.dhcp.common.MDCUtils;
import org.anarres.dhcp.common.address.InterfaceAddress;
import org.apache.directory.server.dhcp.io.*;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.service.DhcpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;


@ChannelHandler.Sharable
public class DhcpHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger LOG = LoggerFactory.getLogger(DhcpHandler.class);
    private final DhcpService dhcpService;
    private final DhcpInterfaceManager interfaceManager;
    private final DhcpMessageDecoder decoder = new DhcpMessageDecoder();
    private final DhcpMessageEncoder encoder = new DhcpMessageEncoder();

    public DhcpHandler(@Nonnull DhcpService dhcpService, @Nonnull DhcpInterfaceManager interfaceManager) {
        this.dhcpService = dhcpService;
        this.interfaceManager = interfaceManager;
    }

    private static void debug(@Nonnull String event, @Nonnull SocketAddress src, @Nonnull SocketAddress dst, @Nonnull DhcpMessage msg) {
        if (LOG.isDebugEnabled())
            LOG.debug("{} {} -> {} {}", event, src, dst, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        DhcpMessage request = decoder.decode(msg.content().nioBuffer());

        DhcpRequestContext context = interfaceManager.newRequestContext(
                (InetSocketAddress) ctx.channel().localAddress(),
                msg.sender(),
                msg.recipient(),
                request
        );
        if (context == null) {
            debug("IGNQUERY", msg.sender(), msg.recipient(), request);
            return;
        }
        // debug("READ", msg.sender(), msg.recipient(), request);

        MDCUtils.init(context, request);
        try {
            DhcpMessage reply = dhcpService.getReplyFor(context, request);
            if (reply == null) {
                debug("NOREPLY", msg.sender(), msg.recipient(), request);
                return;
            }

            InterfaceAddress localAddress = interfaceManager.getResponseInterface(
                    request.getRelayAgentAddress(),
                    request.getCurrentClientAddress(),
                    msg.sender().getAddress(),
                    reply
            );
            if (localAddress == null) {
                debug("NOIFACE", msg.recipient(), msg.sender(), reply);
                return;
            }

            debug("READ", msg.sender(), msg.recipient(), request);

            InetSocketAddress isa = DhcpInterfaceUtils.determineMessageDestination(
                    request, reply,
                    localAddress, msg.sender().getPort());

            ByteBuf buf = ctx.alloc().buffer(1024);
            ByteBuffer buffer = buf.nioBuffer(buf.writerIndex(), buf.writableBytes());
            encoder.encode(buffer, reply);
            buffer.flip();
            buf.writerIndex(buf.writerIndex() + buffer.remaining());
            DatagramPacket packet = new DatagramPacket(buf, isa);
            debug("WRITE", packet.sender(), packet.recipient(), reply);
            ctx.write(packet, ctx.voidPromise());
        } finally {
            MDCUtils.fini();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("Error on channel: " + cause, cause);
        // ctx.close();
    }
}