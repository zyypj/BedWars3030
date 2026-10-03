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

package com.tomkeuper.bedwars.api.npc;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * A fake player standing in the world.
 * <p>
 * It exists only as packets, so it has no entity on the server: nothing can hit it, it costs no ticks, and it
 * has to be sent to each player who should see it.
 */
public interface IPlayerNPC {

    /**
     * Show the NPC to one player. Sending it twice to the same player is harmless.
     */
    void show(Player player);

    /**
     * Stop showing the NPC to one player.
     */
    void hide(Player player);

    /**
     * Drop the NPC for everyone who can currently see it.
     */
    void destroy();

    /**
     * @return where the NPC is standing
     */
    Location getLocation();

    /**
     * @return the entity id the client knows it by, which is what a click packet carries
     */
    int getEntityId();
}
