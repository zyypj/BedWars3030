package com.tomkeuper.bedwars.api.events.gameplay;

import com.tomkeuper.bedwars.api.arena.IArena;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EggBridgeThrowEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final IArena arena;
    private boolean cancelled = false;

    /**
     * Called when a player throw an egg bridge and it starts building
     * Called when a player throws an egg bridge
     */
    public EggBridgeThrowEvent(Player player, IArena arena) {
        this.player = player;
        this.arena = arena;
    }

    /**
     * Get player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get arena
     */
    public IArena getArena() {
        return arena;
    }

    /**
     * Used to cancel the event
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Used to check if whether the event is cancelled
     * @return whether the event is cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
