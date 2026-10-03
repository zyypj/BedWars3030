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

import org.jetbrains.annotations.Nullable;

/**
 * What happens when a join NPC is clicked.
 */
public enum NPCType {

    /** Opens the map menu of one arena group straight away. */
    MODE,
    /** Opens a picker of several groups first, for the 1v1 / 2v2 / 3v3 / 4v4 style NPC. */
    MULTI;

    public static @Nullable NPCType byName(@Nullable String name) {
        if (name == null) return null;
        for (NPCType type : values()) {
            if (type.name().equalsIgnoreCase(name)) return type;
        }
        return null;
    }
}
