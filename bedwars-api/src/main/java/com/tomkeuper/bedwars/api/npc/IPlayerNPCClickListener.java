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

import org.bukkit.entity.Player;

/**
 * Told when a player right clicks a packet NPC.
 * <p>
 * A packet NPC has no entity on the server, so Bukkit never raises an interact event for it: the client aims
 * at an id only it knows about. The version support reads that id off the incoming packet and reports it here.
 */
public interface IPlayerNPCClickListener {

    /**
     * Always called on the main thread.
     *
     * @param player   who clicked
     * @param entityId the id the client aimed at, matching {@link IPlayerNPC#getEntityId()}
     */
    void onClick(Player player, int entityId);
}
