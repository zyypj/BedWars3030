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

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The picker a MULTI npc opens: one button per arena group it offers.
 * <p>
 * Choosing one leads to the same map menu a MODE npc would have opened, so both kinds of npc end in the same
 * place and there is only one screen to keep working.
 */
public final class NPCModesMenu {

    /** Custom data prefix, followed by the group the button leads to. */
    public static final String BUTTON_IDENTIFIER = "bwnpcmode=";

    private static final int[] SLOTS = {10, 12, 14, 16, 19, 21, 23, 25};

    private NPCModesMenu() {
    }

    public static void open(@NotNull Player player, @NotNull BedWarsNPC npc) {
        List<String> groups = npc.getGroups();

        Inventory inv = Bukkit.createInventory(new Holder(), 27,
                ChatColor.translateAlternateColorCodes('&', title(player)));

        for (int i = 0; i < groups.size() && i < SLOTS.length; i++) {
            String group = groups.get(i).trim();
            inv.setItem(SLOTS[i], button(player, group));
        }

        player.openInventory(inv);
    }

    /**
     * @return the group a clicked button leads to, or null when the item is not one of ours
     */
    public static @Nullable String readGroup(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        if (!BedWars.nms.isCustomBedWarsItem(item)) return null;

        String data = BedWars.nms.getCustomData(item);
        if (data == null || !data.startsWith(BUTTON_IDENTIFIER)) return null;
        return data.substring(BUTTON_IDENTIFIER.length());
    }

    private static ItemStack button(@NotNull Player player, @NotNull String group) {
        ItemStack item = BedWars.nms.createItemStack(
                BedWars.getForCurrentVersion("BED", "BED", "RED_BED"), 1, (short) 0);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    "&c&l" + displayGroup(player, group).toUpperCase()));
            meta.setLore(new ArrayList<>(Arrays.asList(
                    ChatColor.translateAlternateColorCodes('&',
                            "&f" + Arena.getPlayers(group) + " jogando"),
                    "",
                    ChatColor.translateAlternateColorCodes('&', "&eClique aqui para jogar"))));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return BedWars.nms.addCustomData(item, BUTTON_IDENTIFIER + group);
    }

    /**
     * The group name BedWars already shows, falling back to the raw key when it was never named.
     */
    private static String displayGroup(@NotNull Player player, @NotNull String group) {
        String path = Messages.ARENA_DISPLAY_GROUP_PATH + group.toLowerCase();
        Language language = Language.getPlayerLanguage(player);

        if (language == null || language.getYml().get(path) == null) return group;
        return language.m(path);
    }

    private static String title(@NotNull Player player) {
        Language language = Language.getPlayerLanguage(player);
        String path = "npc-modes-menu-name";

        if (language != null && language.getYml().get(path) != null) return language.m(path);
        return "&8Escolha o modo";
    }

    /** Marks an inventory as this menu. */
    public static class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
