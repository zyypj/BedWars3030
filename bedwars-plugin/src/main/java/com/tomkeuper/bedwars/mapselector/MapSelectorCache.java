package com.tomkeuper.bedwars.mapselector;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.configuration.ConfigManager;
import com.tomkeuper.bedwars.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Per player state of the map selector: favourite maps and how many times each map was joined.
 * <p>
 * Written straight to a yml, which is enough for what it holds: losing it costs a player their star markers,
 * nothing that affects a game.
 */
public class MapSelectorCache extends ConfigManager {

    public MapSelectorCache(Plugin plugin, String name, String dir) {
        super(plugin, name, dir);
        getYml().options().header("Favoritos e contagem de entradas por mapa do /bwmenu.\n");
        save();
    }

    private static String favouritePath(@NotNull UUID uuid, @NotNull String map) {
        return uuid + ".favorite-maps." + map;
    }

    private static String joinsPath(@NotNull UUID uuid, @NotNull String map) {
        return uuid + ".per-map-times-joined." + map;
    }

    public boolean isFavourite(@NotNull Player player, @NotNull String map) {
        return getYml().getBoolean(favouritePath(player.getUniqueId(), map), false);
    }

    public void setFavourite(@NotNull Player player, @NotNull String map, boolean favourite) {
        if (favourite) {
            set(favouritePath(player.getUniqueId(), map), true);
        } else {
            // Drop the key instead of storing false, so the file only ever grows with real favourites.
            set(favouritePath(player.getUniqueId(), map), null);
        }
    }

    /**
     * @return the joinable arenas of a group that the player marked as favourite
     */
    public @NotNull List<IArena> getFavourites(@NotNull Player player, @NotNull String group) {
        List<IArena> favourites = new ArrayList<>();
        List<String> groups = MapSelectorUtils.splitGroups(group);

        for (IArena arena : Arena.getArenas()) {
            if (!groups.contains(arena.getGroup())) continue;
            if (arena.getStatus() != GameState.waiting && arena.getStatus() != GameState.starting) continue;
            if (isFavourite(player, arena.getArenaName())) favourites.add(arena);
        }
        return favourites;
    }

    public int getJoins(@NotNull Player player, @NotNull String map) {
        return getYml().getInt(joinsPath(player.getUniqueId(), map), 0);
    }

    public void addJoin(@NotNull Player player, @NotNull String map) {
        set(joinsPath(player.getUniqueId(), map), getJoins(player, map) + 1);
    }

    public static MapSelectorCache get() {
        return BedWars.getMapSelectorCache();
    }
}
