package com.tomkeuper.bedwars.arena.feature;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.ArrayList;
import java.util.List;

public class AntiDropFeature implements Listener {
    private static AntiDropFeature instance;
    public AntiDropFeature() {
        Bukkit.getPluginManager().registerEvents(this, BedWars.plugin);
    }

    public static void init() {
        if (BedWars.config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_ENABLE_ANTI_DROP)) {
            if (instance == null) {
                instance = new AntiDropFeature();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null) return;
        if (arena.getStatus() != GameState.playing) return;
        if (player.getVelocity().getY() < -0.5) e.setCancelled(true); // Cancel the item drop if the player is falling
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryInteract(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null) return;
        if (arena.getStatus() != GameState.playing) return;
        if (player.getVelocity().getY() < -0.5) e.setCancelled(true); // Cancel the inventory interaction if the player is falling
    }
}
