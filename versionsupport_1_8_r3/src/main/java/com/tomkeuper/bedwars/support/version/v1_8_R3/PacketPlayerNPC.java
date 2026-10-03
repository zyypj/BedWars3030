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

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.tomkeuper.bedwars.api.npc.IPlayerNPC;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityHeadRotation;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A join NPC drawn as a real player, built out of packets.
 * <p>
 * There is no entity on the server: the client is told a player exists, where it stands and what it looks
 * like, and nothing else. Clicks are not handled here, because a packet entity raises no Bukkit event: the
 * plugin keeps an invisible armour stand on the same spot to catch them.
 */
public class PacketPlayerNPC implements IPlayerNPC {

    /**
     * Every skin layer on: without this the NPC shows up without its hat or jacket.
     */
    private static final byte ALL_SKIN_LAYERS = (byte) 0x7F;
    /**
     * How long the NPC stays in the tab list. It has to be listed for the client to accept the skin, and is
     * dropped right after so the player list stays clean.
     */
    private static final long TAB_REMOVE_DELAY = 40L;

    private final Plugin plugin;
    private final Location location;
    private final EntityPlayer npc;
    private final Set<UUID> viewers = new HashSet<>();

    public PacketPlayerNPC(Plugin plugin, Location location, String value, String signature) {
        this.plugin = plugin;
        this.location = location.clone();

        MinecraftServer server = MinecraftServer.getServer();
        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();

        GameProfile profile = new GameProfile(UUID.randomUUID(), blankName());
        // An unsigned texture still renders; the signature is only added when there is one to add.
        profile.getProperties().put("textures", signature == null
                ? new Property("textures", value)
                : new Property("textures", value, signature));

        npc = new EntityPlayer(server, world, profile, new PlayerInteractManager(world));
        npc.setLocation(location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        npc.getDataWatcher().watch(10, ALL_SKIN_LAYERS);
    }

    /**
     * A name nobody reads: the hologram carries the text, and 1.8 always draws the profile name above the
     * head, so it is made of colour codes that render as nothing. It still has to be unique per NPC.
     */
    private static String blankName() {
        String unique = Integer.toHexString((int) (Math.random() * 0xFFFF));
        StringBuilder name = new StringBuilder();
        for (char c : unique.toCharArray()) name.append(ChatColor.COLOR_CHAR).append(c);
        return name.length() > 16 ? name.substring(0, 16) : name.toString();
    }

    @Override
    public void show(Player player) {
        if (player == null || !player.isOnline()) return;
        viewers.add(player.getUniqueId());

        CraftPlayer craft = (CraftPlayer) player;
        craft.getHandle().playerConnection.sendPacket(
                new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc));
        craft.getHandle().playerConnection.sendPacket(new PacketPlayOutNamedEntitySpawn(npc));

        // The spawn packet carries the body yaw; the head needs its own packet or the NPC stares north.
        byte yaw = (byte) (location.getYaw() * 256.0F / 360.0F);
        craft.getHandle().playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(npc, yaw));
        craft.getHandle().playerConnection.sendPacket(
                new PacketPlayOutEntityMetadata(npc.getId(), npc.getDataWatcher(), true));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(
                    new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, npc));
        }, TAB_REMOVE_DELAY);
    }

    @Override
    public void hide(Player player) {
        if (player == null || !player.isOnline()) return;
        viewers.remove(player.getUniqueId());

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(
                new PacketPlayOutEntityDestroy(npc.getId()));
    }

    @Override
    public void destroy() {
        for (UUID viewer : new HashSet<>(viewers)) {
            Player player = Bukkit.getPlayer(viewer);
            if (player != null) hide(player);
        }
        viewers.clear();
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public int getEntityId() {
        return npc.getId();
    }
}
