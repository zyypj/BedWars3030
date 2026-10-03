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

package com.tomkeuper.bedwars.utils;

import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Names an item stack for a message, in the language of the player who is reading it.
 * <p>
 * Every item is covered without asking the server owner to translate hundreds of materials:
 * <ol>
 *     <li>a custom display name on the stack wins, which already covers everything bought from the shop;</li>
 *     <li>then the language key for that exact material, e.g. {@code meaning-item-gold_ingot-plural};</li>
 *     <li>then a wildcard key for the material family, e.g. {@code meaning-item-*_wool-plural}, so one entry
 *     names all sixteen wool colours;</li>
 *     <li>and finally the material name itself, tidied up: {@code GOLD_INGOT} reads as {@code Gold Ingot}.</li>
 * </ol>
 * Anything in steps 2 and 3 is optional, so a language file only carries the names worth translating.
 */
public final class ItemNames {

    private ItemNames() {
    }

    /**
     * @param amount how many are being talked about, which picks the singular or the plural key
     * @return a display ready name, never null and never empty
     */
    public static @NotNull String of(@Nullable Player player, @NotNull ItemStack item, int amount) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String displayName = meta.getDisplayName();
            // The shop prefixes configured names with a colour reset, so strip formatting before deciding
            // whether there is really a name here.
            if (displayName != null && !ChatColor.stripColor(displayName).trim().isEmpty()) return displayName;
        }

        String material = item.getType().name().toLowerCase();
        boolean singular = amount == 1;

        String name = lookup(player, material, singular);
        if (name != null) return name;

        String family = family(material);
        if (family != null) {
            name = lookup(player, family, singular);
            if (name != null) return name;
        }

        return prettify(item.getType().name());
    }

    public static @NotNull String of(@Nullable Player player, @NotNull ItemStack item) {
        return of(player, item, item.getAmount());
    }

    /**
     * Try the form that matches the amount first, then accept the other one: a language that only bothered with
     * the plural should still name the item when a single one is deposited.
     */
    private static @Nullable String lookup(@Nullable Player player, @NotNull String key, boolean singular) {
        String first = singular ? Messages.MEANING_ITEM_SINGULAR : Messages.MEANING_ITEM_PLURAL;
        String second = singular ? Messages.MEANING_ITEM_PLURAL : Messages.MEANING_ITEM_SINGULAR;

        String name = read(player, first.replace("%bw_material%", key));
        return name != null ? name : read(player, second.replace("%bw_material%", key));
    }

    /**
     * The wildcard key of a material, built from the part after its last underscore.
     * <p>
     * That is where the meaningful noun sits in a Bukkit material name, so {@code light_blue_wool} and
     * {@code orange_wool} both land on {@code *_wool}, and {@code iron_pickaxe} on {@code *_pickaxe}.
     *
     * @return the wildcard key, or null for a single word material that has no family
     */
    private static @Nullable String family(@NotNull String material) {
        int separator = material.lastIndexOf('_');
        if (separator < 0 || separator == material.length() - 1) return null;
        return "*_" + material.substring(separator + 1);
    }

    /**
     * Read straight from the language file: an item name is not a message, so it gets no prefix and no
     * placeholder pass, and a key that is absent or blank counts as not set.
     */
    private static @Nullable String read(@Nullable Player player, @NotNull String path) {
        Language language = player == null ? Language.getDefaultLanguage() : Language.getPlayerLanguage(player);
        if (language == null || language.getYml() == null) return null;

        String value = language.getYml().getString(path);
        if (value == null || value.trim().isEmpty()) return null;
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    /**
     * {@code GOLD_INGOT} to {@code Gold Ingot}. The last resort, so that an item nobody translated still reads
     * like a name instead of an enum constant.
     */
    private static @NotNull String prettify(@NotNull String materialName) {
        StringBuilder out = new StringBuilder(materialName.length());
        for (String word : materialName.toLowerCase().split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.length() == 0 ? materialName : out.toString();
    }
}
