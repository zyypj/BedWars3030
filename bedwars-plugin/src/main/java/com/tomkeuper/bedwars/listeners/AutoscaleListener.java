package com.tomkeuper.bedwars.listeners;

import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.tomkeuper.bedwars.BedWars;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AutoscaleListener implements Listener {

    /**
     * A game just left the joinable states, so top the arena back up. This is what keeps at
     * least one game of every map open for new players.
     */
    @EventHandler
    public void onPlaying(GameStateChangeEvent e) {
        if (e.getNewState() != GameState.playing) return;
        BedWars.arenaManager.ensureAvailability(e.getArena().getArenaName());
    }
}
