/*
 * This file is part of NetMinecraft - https://github.com/RaphiMC/NetMinecraft
 * Copyright (C) 2022-2025 RK_01/RaphiMC and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.netminecraft.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.DecoderException;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.constants.MCVersion;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.registry.PacketRegistry;

import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class PacketCompressor extends ByteToMessageCodec<ByteBuf> {

    private final byte[] deflateBuffer = new byte[8192];

    // only allocated if needed
    private Deflater deflater;
    private Inflater inflater;

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);

        if (this.inflater != null) this.inflater.end();
        if (this.deflater != null) this.deflater.end();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (ctx.channel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get() < 0) {
            out.add(in.readBytes(in.readableBytes()));
            return;
        }
        if (this.inflater == null) this.inflater = new Inflater();

        if (in.readableBytes() != 0) {
            final int uncompressedLength = PacketTypes.readVarInt(in);
            if (uncompressedLength == 0) {
                out.add(in.readBytes(in.readableBytes()));
            } else {
                if (uncompressedLength < ctx.channel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get()) {
                    throw new DecoderException("Badly compressed packet - size of " + uncompressedLength + " is below server threshold of " + ctx.channel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get());
                }
                int maxUncompressed = 8388608;
                PacketRegistry packetRegistry = ctx.channel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).get();
                if (packetRegistry != null) {
                    int protocol = packetRegistry.getProtocolVersion();
                    if (protocol >= 0 && protocol < MCVersion.v1_17_1) {
                        maxUncompressed = 2097152;
                    }
                }
                if (uncompressedLength > maxUncompressed) {
                    throw new DecoderException("Badly compressed packet - size of " + uncompressedLength + " is larger than protocol maximum of " + maxUncompressed);
                }

                final byte[] compressedData = new byte[in.readableBytes()];
                in.readBytes(compressedData);
                this.inflater.setInput(compressedData);
                final byte[] uncompressedData = new byte[uncompressedLength];
                this.inflater.inflate(uncompressedData);
                out.add(in.alloc().buffer().writeBytes(uncompressedData));
                this.inflater.reset();
            }
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
        if (ctx.channel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get() < 0) {
            out.writeBytes(in);
            return;
        }
        if (this.deflater == null) this.deflater = new Deflater();

        final int packetSize = in.readableBytes();
        if (packetSize < ctx.channel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get()) {
            PacketTypes.writeVarInt(out, 0);
            out.writeBytes(in);
        } else {
            final byte[] uncompressedData = new byte[packetSize];
            in.readBytes(uncompressedData);
            PacketTypes.writeVarInt(out, uncompressedData.length);
            this.deflater.setInput(uncompressedData, 0, packetSize);
            this.deflater.finish();

            while (!this.deflater.finished()) {
                out.writeBytes(this.deflateBuffer, 0, this.deflater.deflate(this.deflateBuffer));
            }
            this.deflater.reset();
        }
    }

}
