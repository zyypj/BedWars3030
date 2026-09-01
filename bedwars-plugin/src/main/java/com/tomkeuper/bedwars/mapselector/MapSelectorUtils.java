package com.tomkeuper.bedwars.mapselector;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.arena.Arena;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Shared behaviour of the map selector: which groups exist, who may pick a map, and joining an arena.
 */
public final class MapSelectorUtils {

    private MapSelectorUtils() {
    }

    private static MapSelectorConfig config() {
        return BedWars.getMapSelectorConfig();
    }

    /**
     * A group argument may list several groups separated by commas, which lets one menu cover, say, doubles and
     * trios at once.
     */
    public static @NotNull List<String> splitGroups(@NotNull String group) {
        return Arrays.asList(group.split(","));
    }

    /**
     * @return every group that currently has an arena, in no particular order
     */
    public static @NotNull List<String> getExistingGroups() {
        List<String> groups = new ArrayList<>();
        for (IArena arena : Arena.getArenas()) {
            if (!groups.contains(arena.getGroup())) groups.add(arena.getGroup());
        }
        return groups;
    }

    /**
     * @return true if every part of the argument names a group that exists
     */
    public static boolean groupsExist(@NotNull String group) {
        List<String> existing = getExistingGroups();
        for (String part : splitGroups(group)) {
            if (!existing.contains(part)) return false;
        }
        return true;
    }

    /**
     * Translate a group for display. A comma separated argument has no single name, so it is shown as typed.
     */
    public static @NotNull String getDisplayGroup(@NotNull Player player, @NotNull String group) {
        if (group.contains(",")) return group;
        return Language.getPlayerLanguage(player).m(Messages.ARENA_DISPLAY_GROUP_PATH + group.toLowerCase());
    }

    /**
     * Picking a specific map is a perk; joining a random one is not.
     */
    public static boolean canSelect(@NotNull Player player) {
        String permission = config().getYml().getString(MapSelectorConfig.SELECTION_PERMISSION);
        return permission != null && player.hasPermission(permission);
    }

    public static @NotNull String getSelectionsType(@NotNull Player player) {
        return canSelect(player) ? config().getMessage(MapSelectorConfig.SELECTION_UNLIMITED) : "0";
    }

    /**
     * @return the joinable arenas of a group, i.e. waiting or starting and not full
     */
    public static @NotNull List<IArena> getJoinable(@NotNull String group) {
        List<String> groups = splitGroups(group);
        List<IArena> arenas = new ArrayList<>();

        for (IArena arena : Arena.getArenas()) {
            if (!groups.contains(arena.getGroup())) continue;
            if (arena.getStatus() != GameState.waiting && arena.getStatus() != GameState.starting) continue;
            if (arena.getPlayers().size() >= arena.getMaxPlayers()) continue;
            arenas.add(arena);
        }
        return arenas;
    }

    /**
     * Join the fullest joinable arena of a group, which fills games up instead of scattering players across
     * empty ones. The shuffle first keeps the choice from always landing on the same arena when several are
     * equally full.
     *
     * @param favourite restrict the pick to the player's favourite maps
     */
    public static void joinRandomGroup(@NotNull Player player, @NotNull String group, boolean favourite) {
        List<IArena> candidates;
        String emptyMessage;

        if (favourite) {
            candidates = new ArrayList<>(MapSelectorCache.get().getFavourites(player, group));
            candidates.removeIf(arena -> arena.getPlayers().size() >= arena.getMaxPlayers());
            emptyMessage = config().getMessage(MapSelectorConfig.MSG_NO_FAVORITES);
        } else {
            candidates = getJoinable(group);
            emptyMessage = config().getMessage(MapSelectorConfig.MSG_NO_MAPS);
        }

        if (candidates.isEmpty()) {
            player.sendMessage(emptyMessage);
            return;
        }

        Collections.shuffle(candidates);
        candidates.sort((a, b) -> Integer.compare(b.getPlayers().size(), a.getPlayers().size()));

        join(player, candidates.get(0));
    }

    /**
     * Join a named arena of a group, or tell the player it is no longer joinable.
     */
    public static void joinArena(@NotNull Player player, @NotNull String arenaName, @NotNull String group) {
        for (IArena arena : getJoinable(group)) {
            if (arena.getArenaName().equals(arenaName)) {
                join(player, arena);
                return;
            }
        }
        player.sendMessage(config().getMessage(MapSelectorConfig.MSG_NO_MAPS));
    }

    /**
     * Put the player, or their whole party, into the arena. Only the party owner may drag everyone along.
     */
    private static void join(@NotNull Player player, @NotNull IArena arena) {
        if (BedWars.getPartyManager().hasParty(player) && BedWars.getPartyManager().getMembers(player).size() > 1) {
            if (!BedWars.getPartyManager().isOwner(player)) {
                player.sendMessage(config().getMessage(MapSelectorConfig.MSG_NOT_PARTY_LEADER));
                return;
            }
            for (Player member : BedWars.getPartyManager().getMembers(player)) {
                arena.addPlayer(member, false);
            }
        } else {
            arena.addPlayer(player, false);
        }

        MapSelectorCache.get().addJoin(player, arena.getArenaName());
    }
}
