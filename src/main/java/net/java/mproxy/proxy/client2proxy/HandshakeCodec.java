package net.java.mproxy.proxy.client2proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.DecoderException;
import net.raphimc.netminecraft.netty.sizer.VarIntByteDecoder;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.impl.handshaking.C2SHandshakingClientIntentionPacket;

import java.util.List;

public class HandshakeCodec extends ByteToMessageCodec<ByteBuf> {

    private boolean handshake = true;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (handshake) {
            in.markReaderIndex();
            ByteBuf read = in.readBytes(in.readableBytes());
            in.resetReaderIndex();
            final VarIntByteDecoder reader = new VarIntByteDecoder();

            int varintEnd = in.forEachByte(reader);
            if (varintEnd == -1) {
                // We tried to go beyond the end of the buffer. This is probably a good sign that the
                // buffer was too short to hold a proper varint.
                if (reader.getResult() == VarIntByteDecoder.DecodeResult.RUN_OF_ZEROES) {
                    // Special case where the entire packet is just a run of zeroes. We ignore them all.
                    in.clear();
                }
                return;
            }

            if (reader.getResult() == VarIntByteDecoder.DecodeResult.RUN_OF_ZEROES) {
                // this will return to the point where the next varint starts
                in.readerIndex(varintEnd);
            } else if (reader.getResult() == VarIntByteDecoder.DecodeResult.SUCCESS) {
                int readVarint = reader.getReadVarint();
                int bytesRead = reader.getBytesRead();
                if (readVarint < 0) {
                    in.clear();
                    throw new DecoderException("Bad packet length");
                } else if (readVarint == 0) {
                    // skip over the empty packet(s) and ignore it
                    in.readerIndex(varintEnd + 1);
                } else {
                    int minimumRead = bytesRead + readVarint;
                    if (in.isReadable(minimumRead)) {

                        in.markReaderIndex();
                        {
                            ByteBuf packetBytes = in.slice(varintEnd + 1, readVarint);
                            PacketTypes.readVarInt(packetBytes);//packet id
                            C2SHandshakingClientIntentionPacket packet = new C2SHandshakingClientIntentionPacket();
                            packet.read(packetBytes, -1);
                            ctx.channel().attr(Client2ProxyHandler.CLIENT_2_PROXY_ATTRIBUTE_KEY).get().onHandshake(packet);
                        }
                        in.resetReaderIndex();

                        out.add(read);
                        in.skipBytes(in.readableBytes());
                        handshake = false;

                    }
                }
            } else if (reader.getResult() == VarIntByteDecoder.DecodeResult.TOO_BIG) {
                in.clear();
                throw new DecoderException("VarInt too big");
            }

        } else {
            out.add(in.readBytes(in.readableBytes()));
        }
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
        out.writeBytes(in);
    }
}
