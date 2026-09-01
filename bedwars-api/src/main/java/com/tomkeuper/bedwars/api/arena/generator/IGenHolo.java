package com.tomkeuper.bedwars.api.arena.generator;

import com.tomkeuper.bedwars.api.hologram.containers.IHologram;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Set;

public interface IGenHolo {

    /**
     * Set timer hologram display text.
     */
    void setTimerName(String timer);

    /**
     * Set tier hologram display text.
     */
    void setTierName(String tier);

    /**
     * Get language iso associated with this hologram.
     */
    String getIso();

    /**
     * Get the player associated with this hologram.
     */
    Set<Player> getPlayers();

    /**
     * Add a player to this hologram.
     */
    void addPlayer(Player player);

    /**
     * Remove a player from this hologram.
     */
    void removePlayer(Player player);

    /**
     * Get the generator associated with this hologram.
     */
    IGenerator getGenerator();

    /**
     * Update the hologram.
     */
    void update();

    /**
     * Update the hologram for a player.
     */
    void update(Player player);

    /**
     * This must be called when disabling the generator {@link IGenerator#disable()}
     */
    void destroy();

    /**
     * Get the hologram instance associated with this generator hologram.
     * @return The hologram instance associated with this generator hologram.
     */
    IHologram getHologram();
}
