package com.tomkeuper.bedwars.arena;

import org.jetbrains.annotations.NotNull;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
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

    /**
     * The position a group takes in every list players see.
     *
     * @return the index of the mode, or a number past the end for a group that is not one
     */
    /**
     * Write a display name for every mode into the language files.
     * <p>
     * {@code Arena} only names a group when an arena of it loads, which leaves a configured mode with no map
     * yet showing MISSING_LANG wherever it is listed. Existing names are never overwritten.
     */
    public static void registerDisplayNames() {
        for (ArenaMode mode : values()) {
            Language.saveIfNotExists(Messages.ARENA_DISPLAY_GROUP_PATH + mode.getGroup().toLowerCase(),
                    mode.getGroup());
        }
    }

    public static int order(@Nullable String group) {
        ArenaMode mode = getByGroup(group);
        return mode == null ? values().length : mode.ordinal();
    }

    /**
     * Put groups in the order the modes are declared in: Solo, Duplas, Trios, Quartetos, then 1v1 to 4v4.
     * <p>
     * Anything that is not a known mode keeps to the back, sorted by name, so a custom group never pushes the
     * standard ones out of the order players are used to.
     *
     * @return the same list, sorted in place
     */
    @NotNull
    public static List<String> sortGroups(@NotNull List<String> groups) {
        groups.sort((a, b) -> {
            int byMode = Integer.compare(order(a), order(b));
            return byMode != 0 ? byMode : a.compareToIgnoreCase(b);
        });
        return groups;
    }
}
