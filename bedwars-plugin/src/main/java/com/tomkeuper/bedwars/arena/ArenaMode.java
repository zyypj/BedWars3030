package com.tomkeuper.bedwars.arena;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public enum ArenaMode {

    SOLO("Solo", 1, 4, 8),
    DUPLAS("Duplas", 2, 6, 8),
    TRIOS("Trios", 3, 6, 4),
    QUARTETOS("Quartetos", 4, 8, 4),
    ONE_VS_ONE("1v1", 1, 2, 2),
    TWO_VS_TWO("2v2", 2, 4, 2),
    THREE_VS_THREE("3v3", 3, 6, 2),
    FOUR_VS_FOUR("4v4", 4, 8, 2);

    private final String group;
    private final int maxInTeam;
    private final int minPlayers;
    private final int teams;

    ArenaMode(String group, int maxInTeam, int minPlayers, int teams) {
        this.group = group;
        this.maxInTeam = maxInTeam;
        this.minPlayers = minPlayers;
        this.teams = teams;
    }

    @NotNull
    public String getGroup() {
        return group;
    }

    public int getMaxInTeam() {
        return maxInTeam;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getTeams() {
        return teams;
    }

    @Nullable
    public static ArenaMode getByGroup(@Nullable String group) {
        if (group == null) return null;
        for (ArenaMode mode : values()) {
            if (mode.group.equalsIgnoreCase(group)) return mode;
        }
        return null;
    }

    @NotNull
    public static List<String> getGroups() {
        List<String> groups = new ArrayList<>();
        for (ArenaMode mode : values()) {
            groups.add(mode.group);
        }
        return groups;
    }
}
