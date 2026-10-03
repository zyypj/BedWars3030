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

package com.tomkeuper.bedwars.npc;

import com.tomkeuper.bedwars.BedWars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Turns clicks on a join NPC into the menu it stands for, and keeps the stands from being knocked about.
 */
public class NPCListener implements Listener {

    /**
     * 1.8 can send more than one use packet for a single right click, so a click is ignored when it lands on
     * the heels of the last one.
     */
    private static final long CLICK_COOLDOWN = 400L;

    private final Map<UUID, Long> lastClick = new HashMap<>();

    public NPCListener() {
        // The version support reads the click off the wire; matching the id to an npc is this side's job.
        BedWars.nms.setNPCClickListener((player, entityId) -> {
            if (NPCManager.getInstance() == null) return;

            BedWarsNPC npc = NPCManager.getInstance().byEntityId(entityId);
            if (npc == null) return;
            if (onCooldown(player)) return;

            open(player, npc);
        });
    }

    private boolean onCooldown(@NotNull Player player) {
        long now = System.currentTimeMillis();
        Long previous = lastClick.put(player.getUniqueId(), now);
        return previous != null && now - previous < CLICK_COOLDOWN;
    }

    /**
     * Newer clients send the "at" variant, older ones the plain one, so both are handled and routed through
     * the same guard.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        handleClick(event.getPlayer(), NPCManager.getInstance() == null
                ? null : NPCManager.getInstance().byEntity(event.getRightClicked()), event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event instanceof PlayerInteractAtEntityEvent) return;
        handleClick(event.getPlayer(), NPCManager.getInstance() == null
                ? null : NPCManager.getInstance().byEntity(event.getRightClicked()), event);
    }

    private void handleClick(@NotNull Player player, BedWarsNPC npc,
                             @NotNull org.bukkit.event.Cancellable event) {
        if (npc == null) return;
        event.setCancelled(true);

        if (onCooldown(player)) return;
        open(player, npc);
    }

    /**
     * A MODE npc goes straight to the maps of its group; a MULTI one asks which group first.
     */
    private void open(@NotNull Player player, @NotNull BedWarsNPC npc) {
        if (npc.getType() == NPCType.MULTI) {
            NPCModesMenu.open(player, npc);
            return;
        }
        player.performCommand("bwmenu " + npc.getGroup());
    }

    @EventHandler
    public void onModesMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof NPCModesMenu.Holder)) return;

        event.setCancelled(true);

        String group = NPCModesMenu.readGroup(event.getCurrentItem());
        if (group == null) return;

        player.closeInventory();
        // Next tick, so the map menu is not opened straight into a closing inventory.
        Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
            if (player.isOnline()) player.performCommand("bwmenu " + group);
        });
    }

    /**
     * The stands are scenery: nothing may damage them or take the head off.
     */
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (NPCManager.getInstance() == null) return;
        if (NPCManager.getInstance().byEntity(event.getEntity()) == null) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onManipulate(PlayerArmorStandManipulateEvent event) {
        if (NPCManager.getInstance() == null) return;
        if (NPCManager.getInstance().byEntity(event.getRightClicked()) == null) return;

        event.setCancelled(true);
        open(event.getPlayer(), NPCManager.getInstance().byEntity(event.getRightClicked()));
    }

    /**
     * A packet NPC exists only for the clients that were told about it, so everyone arriving gets a copy.
     * <p>
     * Delayed a moment because a player who just logged in is not ready to receive entity packets on the same
     * tick the join event fires.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (NPCManager.getInstance() == null) return;

        Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> {
            if (!event.getPlayer().isOnline()) return;
            for (BedWarsNPC npc : NPCManager.getInstance().getNPCs()) npc.show(event.getPlayer());
        }, 20L);
    }

    /**
     * Walking into the lobby world has to reveal its npcs, and walking out has to drop them.
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (NPCManager.getInstance() == null) return;

        for (BedWarsNPC npc : NPCManager.getInstance().getNPCs()) {
            if (npc.getLocation().getWorld() == null) continue;

            if (npc.getLocation().getWorld().equals(event.getPlayer().getWorld())) {
                npc.show(event.getPlayer());
            } else {
                npc.hide(event.getPlayer());
            }
        }
    }

    /**
     * A world that loads after the plugin did still needs its npcs put up.
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (NPCManager.getInstance() == null) return;

        Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> {
            // An npc parked at startup because its world was missing can be read now.
            NPCManager.getInstance().loadWaiting();

            for (BedWarsNPC npc : NPCManager.getInstance().getNPCs()) {
                if (npc.isSpawned()) continue;
                if (npc.getLocation().getWorld() == null) continue;
                if (!npc.getLocation().getWorld().equals(event.getWorld())) continue;

                npc.spawn();
            }
        }, 20L);
    }
}
