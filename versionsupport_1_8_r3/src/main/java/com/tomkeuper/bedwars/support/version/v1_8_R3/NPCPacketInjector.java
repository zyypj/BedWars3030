/*
 * BedWars2023 - A bed wars mini-game.
 * Copyright (C) 2024 Tomas Keuper
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: contact@fyreblox.com
 */

package com.tomkeuper.bedwars.support.version.v1_8_R3;

import com.tomkeuper.bedwars.api.npc.IPlayerNPCClickListener;
import com.tomkeuper.bedwars.api.server.VersionSupport;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.server.v1_8_R3.PacketPlayInUseEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;

/**
 * Watches for right clicks on packet NPCs.
 * <p>
 * The client aims at an entity id the server has never heard of, so the click never becomes a Bukkit event.
 * The only place it can be seen is on the way in, which is why this sits in the player's netty pipeline and
 * reads the id straight off {@link PacketPlayInUseEntity}.
 */
public class NPCPacketInjector implements Listener {

    private static final String HANDLER_NAME = "bedwars_npc";

    private final Plugin plugin;
    private final VersionSupport versionSupport;

    /** 1.8 obfuscates the entity id as {@code a} and it is private, so it is read reflectively once. */
    private final Field entityIdField;

    public NPCPacketInjector(Plugin plugin, VersionSupport versionSupport) {
        this.plugin = plugin;
        this.versionSupport = versionSupport;

        Field field = null;
        try {
            field = PacketPlayInUseEntity.class.getDeclaredField("a");
            field.setAccessible(true);
        } catch (NoSuchFieldException error) {
            plugin.getLogger().warning("Nao foi possivel ler o id do pacote de clique: NPCs nao serao clicaveis.");
        }
        this.entityIdField = field;
    }

    /**
     * Hook up everyone who is already connected, for a plugin reload.
     */
    public void injectOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) inject(player);
    }

    public void removeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) remove(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        inject(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer());
    }

    private void inject(Player player) {
        if (entityIdField == null) return;

        Channel channel = channelOf(player);
        if (channel == null || channel.pipeline().get(HANDLER_NAME) != null) return;

        channel.pipeline().addBefore("packet_handler", HANDLER_NAME, new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                if (message instanceof PacketPlayInUseEntity) {
                    handle(player, (PacketPlayInUseEntity) message);
                }
                super.channelRead(context, message);
            }
        });
    }

    private void remove(Player player) {
        Channel channel = channelOf(player);
        if (channel == null) return;

        // Off the netty thread the pipeline must not be edited directly.
        channel.eventLoop().submit(() -> {
            if (channel.pipeline().get(HANDLER_NAME) != null) channel.pipeline().remove(HANDLER_NAME);
            return null;
        });
    }

    /**
     * Read the id and hand it to the plugin on the main thread.
     * <p>
     * Never cancels the packet: the click may well have been aimed at a real entity, and the server has to go
     * on handling it as usual.
     */
    private void handle(Player player, PacketPlayInUseEntity packet) {
        IPlayerNPCClickListener listener = versionSupport.getNPCClickListener();
        if (listener == null) return;

        // 1.8 sends INTERACT and INTERACT_AT for one right click, so only one of them is acted on.
        if (packet.a() != PacketPlayInUseEntity.EnumEntityUseAction.INTERACT) return;

        int entityId;
        try {
            entityId = entityIdField.getInt(packet);
        } catch (IllegalAccessException error) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) listener.onClick(player, entityId);
        });
    }

    private Channel channelOf(Player player) {
        try {
            return ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
        } catch (Throwable error) {
            return null;
        }
    }
}
