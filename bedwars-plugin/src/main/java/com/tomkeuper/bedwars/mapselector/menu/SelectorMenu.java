package com.tomkeuper.bedwars.mapselector.menu;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.mapselector.MapSelectorConfig;
import com.tomkeuper.bedwars.mapselector.MapSelectorUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The screen {@code /bwmenu <mode>} opens: join a random game of the mode, or go pick a map.
 */
public class SelectorMenu extends MapSelectorMenu {

    private final String group;

    public SelectorMenu(@NotNull Player player, @NotNull String group) {
        super(player, config().getYml().getInt(MapSelectorConfig.MENU_SLOTS, 27) / 9);
        this.group = group;
    }

    private static MapSelectorConfig config() {
        return BedWars.getMapSelectorConfig();
    }

    @Override
    public List<MenuItem> getItems() {
        List<MenuItem> items = new ArrayList<>();
        String displayGroup = MapSelectorUtils.getDisplayGroup(player, group);

        for (String key : config().getKeys(MapSelectorConfig.MENU_ITEMS)) {
            items.add(MenuItem.parse(MapSelectorConfig.MENU_ITEMS + "." + key, config())
                    .player(player)
                    .replacement("{groupName}", displayGroup)
                    .event(event -> handle(key)));
        }
        return items;
    }

    private void handle(@NotNull String key) {
        switch (key) {
            case "join-random":
                MapSelectorUtils.joinRandomGroup(player, group, false);
                player.closeInventory();
                break;

            case "map-selector":
                // Picking a specific map is permission gated; joining a random one above is not.
                if (!MapSelectorUtils.canSelect(player)) {
                    player.sendMessage(config().getMessage(MapSelectorConfig.MSG_NO_PERMISSION));
                    player.closeInventory();
                    return;
                }
                new ArenasMenu(player, group).open();
                break;

            case "rejoin":
                player.closeInventory();
                player.performCommand("rejoin");
                break;

            case "close":
                player.closeInventory();
                break;

            default:
                // An item the admin added by hand: show it, but do nothing on click.
        }
    }

    @Override
    public String getTitle() {
        String title = config().getYml().getString(MapSelectorConfig.MENU_TITLE);
        if (title == null) return "";
        return title.replace("{groupName}", MapSelectorUtils.getDisplayGroup(player, group));
    }
}
