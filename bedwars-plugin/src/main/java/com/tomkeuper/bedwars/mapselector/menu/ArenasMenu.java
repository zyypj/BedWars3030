package com.tomkeuper.bedwars.mapselector.menu;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.mapselector.MapSelectorCache;
import com.tomkeuper.bedwars.mapselector.MapSelectorConfig;
import com.tomkeuper.bedwars.mapselector.MapSelectorUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The map picker.
 * <p>
 * Auto scale means one map can back several games at once, so maps are listed once and carry a count of how
 * many of their games are joinable, instead of showing the same map many times over.
 */
public class ArenasMenu extends PaginatedMapSelectorMenu {

    private final String group;

    public ArenasMenu(@NotNull Player player, @NotNull String group) {
        super(player, config().getYml().getInt(MapSelectorConfig.MAPS_SLOTS, 45) / 9);
        this.group = group;
    }

    private static MapSelectorConfig config() {
        return BedWars.getMapSelectorConfig();
    }

    @Override
    public List<Integer> getPaginatedSlots() {
        return config().getSlots(MapSelectorConfig.MAPS_MAP_SLOTS);
    }

    @Override
    public String getPaginationTitle() {
        String title = config().getYml().getString(MapSelectorConfig.MAPS_TITLE);
        if (title == null) return "";
        return title.replace("{groupName}", MapSelectorUtils.getDisplayGroup(player, group));
    }

    @Override
    public List<MenuItem> getAllPageItems() {
        List<MenuItem> items = new ArrayList<>();
        List<String> groups = MapSelectorUtils.splitGroups(group);

        /* One entry per map, with the joinable and total game counts behind it. */
        Map<String, IArena> firstJoinable = new HashMap<>();
        Map<String, Integer> availableByMap = new HashMap<>();
        Map<String, Integer> totalByMap = new HashMap<>();
        List<String> order = new ArrayList<>();

        for (IArena arena : Arena.getArenas()) {
            if (!groups.contains(arena.getGroup())) continue;

            GameState status = arena.getStatus();
            boolean available = status == GameState.waiting || status == GameState.starting;
            if (!available && status != GameState.playing) continue;

            String key = arena.getGroup() + "|" + arena.getDisplayName();
            totalByMap.merge(key, 1, Integer::sum);

            if (available) {
                availableByMap.merge(key, 1, Integer::sum);
                if (!firstJoinable.containsKey(key)) {
                    firstJoinable.put(key, arena);
                    order.add(key);
                }
            }
        }

        if (order.isEmpty()) {
            player.sendMessage(config().getMessage(MapSelectorConfig.MSG_NO_MAPS));
            return items;
        }

        order.sort(Comparator.comparing(key -> firstJoinable.get(key).getDisplayName()));

        String displayGroup = MapSelectorUtils.getDisplayGroup(player, group);
        String unlimited = config().getMessage(MapSelectorConfig.SELECTION_UNLIMITED);

        for (String key : order) {
            IArena arena = firstJoinable.get(key);
            boolean favourite = MapSelectorCache.get().isFavourite(player, arena.getArenaName());
            String path = MapSelectorConfig.MAPS_ITEMS + "." + (favourite ? "map-favorite" : "map");

            items.add(MenuItem.parse(path, config())
                    .player(player)
                    .replacement("{mapName}", arena.getDisplayName())
                    .replacement("{groupName}", displayGroup)
                    .replacement("{availableGames}", String.valueOf(availableByMap.getOrDefault(key, 0)))
                    .replacement("{totalGames}", String.valueOf(totalByMap.getOrDefault(key, 0)))
                    .replacement("{timesJoined}", String.valueOf(MapSelectorCache.get().getJoins(player, arena.getArenaName())))
                    .replacement("{selectionsType}", MapSelectorUtils.getSelectionsType(player))
                    .replacement("{remainingUses}", unlimited)
                    .replacement("{status}", arena.getStatus().name())
                    .replacement("{on}", String.valueOf(arena.getPlayers().size()))
                    .replacement("{max}", String.valueOf(arena.getMaxPlayers()))
                    .event(event -> {
                        if (event.isRightClick()) {
                            MapSelectorCache.get().setFavourite(player, arena.getArenaName(), !favourite);
                            update();
                            return;
                        }
                        MapSelectorUtils.joinArena(player, arena.getArenaName(), arena.getGroup());
                        player.closeInventory();
                    }));
        }

        return items;
    }

    @Override
    public List<MenuItem> getGlobalItems() {
        List<MenuItem> items = new ArrayList<>();
        String displayGroup = MapSelectorUtils.getDisplayGroup(player, group);
        String unlimited = config().getMessage(MapSelectorConfig.SELECTION_UNLIMITED);

        for (String key : config().getKeys(MapSelectorConfig.MAPS_ITEMS)) {
            // The map entries are drawn per arena, and the page arrows are placed by the pagination itself.
            if (key.startsWith("map") || key.contains("page")) continue;

            items.add(MenuItem.parse(MapSelectorConfig.MAPS_ITEMS + "." + key, config())
                    .player(player)
                    .replacement("{groupName}", displayGroup)
                    .replacement("{selectionsType}", MapSelectorUtils.getSelectionsType(player))
                    .replacement("{remainingUses}", unlimited)
                    .event(event -> {
                        switch (key) {
                            case "random-map":
                                MapSelectorUtils.joinRandomGroup(player, group, false);
                                player.closeInventory();
                                break;
                            case "random-favourite":
                                MapSelectorUtils.joinRandomGroup(player, group, true);
                                player.closeInventory();
                                break;
                            case "back":
                                new SelectorMenu(player, group).open();
                                break;
                            default:
                                // Decoration added by the admin.
                        }
                    }));
        }
        return items;
    }

    @Override
    public @Nullable MenuItem getNextPageItem() {
        return MenuItem.parse(MapSelectorConfig.MAPS_ITEMS + ".next-page", config())
                .player(player)
                .replacement("{nextPage}", String.valueOf(getPage() + 1));
    }

    @Override
    public @Nullable MenuItem getPreviousPageItem() {
        return MenuItem.parse(MapSelectorConfig.MAPS_ITEMS + ".previous-page", config())
                .player(player)
                .replacement("{previousPage}", String.valueOf(getPage() - 1));
    }
}
