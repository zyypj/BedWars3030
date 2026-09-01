package com.tomkeuper.bedwars.mapselector.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base of the map selector screens.
 * <p>
 * The menu is its own {@link InventoryHolder}, so the click listener recovers it from the inventory instead of
 * keeping a global registry that has to be cleaned up when a player disconnects mid click.
 */
public abstract class MapSelectorMenu implements InventoryHolder {

    protected final Player player;
    private final int rows;
    private final Map<Integer, MenuClickHandler> handlers = new HashMap<>();

    private Inventory inventory;

    protected MapSelectorMenu(@NotNull Player player, int rows) {
        this.player = player;
        this.rows = Math.min(6, Math.max(1, rows));
    }

    public void open() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, rows * 9, colorize(getTitle()));
        }
        update();
        player.openInventory(inventory);
    }

    /**
     * Redraw the screen in place, which is how a favourite toggle shows up without closing the menu.
     */
    public void update() {
        if (inventory == null) {
            open();
            return;
        }
        inventory.clear();
        handlers.clear();

        for (MenuItem item : getItems()) {
            setItem(item);
        }
        player.updateInventory();
    }

    protected void setItem(@Nullable MenuItem item) {
        if (item == null) return;
        if (inventory == null) return;
        if (item.getSlot() < 0 || item.getSlot() >= inventory.getSize()) return;

        inventory.setItem(item.getSlot(), item.build());
        if (item.getHandler() != null) handlers.put(item.getSlot(), item.getHandler());
    }

    protected void clearSlots() {
        if (inventory != null) inventory.clear();
        handlers.clear();
    }

    public @Nullable MenuClickHandler getHandler(int slot) {
        return handlers.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected static String colorize(@Nullable String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }

    public abstract List<MenuItem> getItems();

    public abstract String getTitle();
}
