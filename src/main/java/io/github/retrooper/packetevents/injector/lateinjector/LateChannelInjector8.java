/*
 * MIT License
 *
 * Copyright (c) 2020 retrooper
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.retrooper.packetevents.injector.lateinjector;

import io.github.retrooper.packetevents.PacketEvents;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.bukkit.entity.Player;

import java.util.NoSuchElementException;

public class LateChannelInjector8 implements LateInjector {
    @Override
    public void injectPlayerSync(Player player) {
        final ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
                packet = PacketEvents.get().packetHandlerInternal.read(player, ctx.channel(), packet);
                if (packet != null) {
                    super.channelRead(ctx, packet);
                    PacketEvents.get().packetHandlerInternal.postRead(player, ctx.channel(), packet);
                }
            }

            @Override
            public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
                packet = PacketEvents.get().packetHandlerInternal.write(player, ctx.channel(), packet);
                if (packet != null) {
                    super.write(ctx, packet, promise);
                    PacketEvents.get().packetHandlerInternal.postWrite(player, ctx.channel(), packet);
                }
            }
        };
        final Channel channel = (Channel) PacketEvents.get().packetHandlerInternal.getChannel(player);
        channel.pipeline().addBefore("packet_handler", PacketEvents.HANDLER_NAME, channelDuplexHandler);
    }

    @Override
    public void ejectPlayerSync(Player player) {
        final Channel channel = (Channel) PacketEvents.get().packetHandlerInternal.getChannel(player);
        if (channel.pipeline().get(PacketEvents.HANDLER_NAME) != null) {
            try {
                channel.pipeline().remove(PacketEvents.HANDLER_NAME);
            } catch (NoSuchElementException ignored) {

            }
        }
        PacketEvents.get().getPlayerUtils().clientVersionsMap.remove(player.getAddress());
        PacketEvents.get().getPlayerUtils().tempClientVersionMap.remove(player.getAddress());
    }

    @Override
    public void injectPlayerAsync(Player player) {
        PacketEvents.get().injectAndEjectExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                injectPlayerSync(player);
            }
        });
    }

    @Override
    public void ejectPlayerAsync(Player player) {
        PacketEvents.get().injectAndEjectExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                final Channel channel = (Channel) PacketEvents.get().packetHandlerInternal.getChannel(player);
                if (channel.pipeline().get(PacketEvents.HANDLER_NAME) != null) {
                    try {
                        channel.pipeline().remove(PacketEvents.HANDLER_NAME);
                    } catch (NoSuchElementException ignored) {

                    }
                }
                PacketEvents.get().packetHandlerInternal.keepAliveMap.remove(player.getUniqueId());
                PacketEvents.get().packetHandlerInternal.channelMap.remove(player.getName());
                PacketEvents.get().getPlayerUtils().clientVersionsMap.remove(player.getAddress());
                PacketEvents.get().getPlayerUtils().tempClientVersionMap.remove(player.getAddress());
            }
        });
    }

    @Override
    public void sendPacket(Object rawChannel, Object packet) {
        Channel channel = (Channel) rawChannel;
        channel.pipeline().writeAndFlush(packet);
    }
}