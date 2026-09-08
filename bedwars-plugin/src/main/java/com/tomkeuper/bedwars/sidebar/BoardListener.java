package com.tomkeuper.bedwars.sidebar;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.tomkeuper.bedwars.api.server.ServerType;
import com.tomkeuper.bedwars.api.sidebar.IBossBar;
import com.tomkeuper.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class BoardListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        BoardManager manager = BoardManager.getInstance();
        if (manager == null) return;

        Player player = event.getPlayer();
        manager.getOrCreateBoard(player);
        manager.refreshFormatting();

        if (BedWars.getServerType() == ServerType.SHARED
                && !player.getWorld().getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
            return;
        }

        // Give time for player to be put in arena player list.
        Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> {
            if (!player.isOnline()) return;
            manager.giveTabFeatures(player, Arena.getArenaByPlayer(player), false);
        }, 5);
    }

    @EventHandler
    public void onArenaLeave(PlayerLeaveArenaEvent event) {
        BoardManager manager = BoardManager.getInstance();
        if (manager == null) return;

        Player player = event.getPlayer();
        IArena arena = event.getArena();
        if (arena != null) {
            List<IBossBar> dragonBossbars = arena.getDragonBossbars();
            if (dragonBossbars != null) {
                for (IBossBar bossBar : dragonBossbars) {
                    bossBar.removePlayer(player);
                }
            }
        }

        manager.refreshFormatting(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BoardManager manager = BoardManager.getInstance();
        if (manager == null) return;
        manager.cleanupPlayer(event.getPlayer());
    }
}
