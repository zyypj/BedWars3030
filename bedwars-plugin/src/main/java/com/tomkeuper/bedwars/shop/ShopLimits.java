package com.tomkeuper.bedwars.shop;

import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts how often a limited shop item has been bought in a match.
 * <p>
 * The counters live here rather than in {@code ShopCache} because a team limit is shared: the cache is created
 * per player and destroyed when they leave, which is exactly the wrong lifetime for an allowance the whole team
 * draws on. Everything is keyed by arena and cleared when the arena is recycled, so nothing survives into the
 * next match on the same world.
 */
public final class ShopLimits {

    private static final Map<String, Map<String, Integer>> COUNTS = new ConcurrentHashMap<>();

    private ShopLimits() {
    }

    /**
     * The key a purchase counts against: the player for a personal limit, the team for a shared one.
     */
    public static String key(String contentIdentifier, Player player, ITeam team) {
        String owner = team != null ? "team:" + team.getName() : "player:" + player.getUniqueId();
        return owner + "|" + contentIdentifier;
    }

    public static int get(IArena arena, String key) {
        if (arena == null) return 0;
        Map<String, Integer> counts = COUNTS.get(arena.getWorldName());
        if (counts == null) return 0;

        Integer count = counts.get(key);
        return count == null ? 0 : count;
    }

    public static void increment(IArena arena, String key) {
        if (arena == null) return;
        COUNTS.computeIfAbsent(arena.getWorldName(), name -> new ConcurrentHashMap<>())
                .merge(key, 1, Integer::sum);
    }

    /**
     * Forget everything counted for an arena, called as it is recycled.
     */
    public static void clear(IArena arena) {
        if (arena == null || arena.getWorldName() == null) return;
        COUNTS.remove(arena.getWorldName());
    }
}
