package com.tomkeuper.bedwars.mapselector.menu;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.mapselector.MapSelectorConfig;
import com.tomkeuper.bedwars.support.papi.SupportPAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One configured slot of a map selector menu: where it sits, what it looks like and what it does.
 */
public class MenuItem {

    private int slot;
    private final String material;
    private final int data;
    private final String name;
    private final List<String> lore;
    private final Map<String, String> replacements = new LinkedHashMap<>();

    private MenuClickHandler handler;
    private Player player;

    private MenuItem(int slot, String material, int data, String name, List<String> lore) {
        this.slot = slot;
        this.material = material;
        this.data = data;
        this.name = name;
        this.lore = lore;
    }

    /**
     * Read an item out of the config. A missing material falls back to stone rather than dropping the slot, so a
     * typo shows up in game as an obviously wrong item instead of an invisible hole.
     */
    public static MenuItem parse(@NotNull String path, @NotNull MapSelectorConfig config) {
        String material = config.getYml().getString(path + ".material", "STONE");
        int data = config.getYml().getInt(path + ".data", 0);
        int slot = config.getYml().getInt(path + ".slot", 0);
        String name = config.getYml().getString(path + ".name");
        List<String> lore = config.getYml().getStringList(path + ".lore");

        return new MenuItem(slot, material == null ? "STONE" : material, data, name, lore);
    }

    public MenuItem replacement(@NotNull String placeholder, @Nullable String value) {
        replacements.put(placeholder, value == null ? "" : value);
        return this;
    }

    public MenuItem event(@Nullable MenuClickHandler handler) {
        this.handler = handler;
        return this;
    }

    public MenuItem player(@Nullable Player player) {
        this.player = player;
        return this;
    }

    public int getSlot() {
        return slot;
    }

    /**
     * Paginated items are all configured on the same slot and only learn their real one when the page is laid
     * out, so the slot has to stay writable.
     */
    public MenuItem slotOverride(int slot) {
        this.slot = slot;
        return this;
    }

    public @Nullable MenuClickHandler getHandler() {
        return handler;
    }

    public @NotNull ItemStack build() {
        ItemStack item = BedWars.nms.createItemStack(material, 1, (short) data);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(apply(name));

            if (lore != null && !lore.isEmpty()) {
                List<String> lines = new ArrayList<>();
                for (String line : lore) lines.add(apply(line));
                meta.setLore(lines);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Menu placeholders run first so a value they produce can still be read by PlaceholderAPI, which is how the
     * default lore counts the players of a group.
     */
    private String apply(@NotNull String input) {
        String result = input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        if (player != null) {
            result = SupportPAPI.getSupportPAPI().replace(player, result);
        }
        return ChatColor.translateAlternateColorCodes('&', result);
    }
}
