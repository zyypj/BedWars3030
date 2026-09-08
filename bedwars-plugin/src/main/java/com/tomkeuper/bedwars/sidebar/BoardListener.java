package com.tomkeuper.bedwars.sidebar;

import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.tomkeuper.bedwars.api.sidebar.IBossBar;
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
        manager.getOrCreateBoard(event.getPlayer());
        manager.refreshFormatting();
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
