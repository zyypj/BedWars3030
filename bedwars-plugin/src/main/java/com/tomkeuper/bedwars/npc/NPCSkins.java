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

import com.saicone.rtag.util.SkullTexture;
import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.configuration.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where an NPC skin comes from.
 * <p>
 * A texture value is a few hundred characters of base64, which nobody is going to paste into chat. So there
 * are two ways in and neither of them asks for that: name a player and the skin is fetched from their account,
 * or save the long value once in {@code skins.yml} under a short nickname and pick it from a menu afterwards.
 */
public final class NPCSkins {

    public static final String PATH = "skins";

    private static ConfigManager config;

    private NPCSkins() {
    }

    public static void init() {
        if (config != null) return;
        config = new ConfigManager(BedWars.plugin, "skins", BedWars.plugin.getDataFolder().getPath());

        config.getYml().options().header("Skins dos NPCs.\n"
                + "Cada entrada guarda o texture value, que so precisa ser colado aqui uma vez.\n"
                + "No jogo basta escolher pelo apelido, ou usar o nome de um jogador direto no comando.\n"
                + "\n"
                + "skins:\n"
                + "  meu-npc:\n"
                + "    value: <texture value em base64>\n");

        config.getYml().options().copyDefaults(true);
        config.save();
    }

    public static ConfigManager getConfig() {
        return config;
    }

    /**
     * @return the nicknames saved in skins.yml
     */
    public static @NotNull List<String> getPresets() {
        if (config == null) return Collections.emptyList();

        ConfigurationSection section = config.getYml().getConfigurationSection(PATH);
        return section == null ? Collections.emptyList() : new ArrayList<>(section.getKeys(false));
    }

    public static boolean isPreset(@Nullable String name) {
        if (name == null || config == null) return false;
        return config.getYml().get(PATH + "." + name + ".value") != null;
    }

    /**
     * Remember a texture value under a nickname, so it never has to be typed again.
     */
    public static void savePreset(@NotNull String name, @NotNull String value) {
        init();
        config.getYml().set(PATH + "." + name + ".value", value);
        config.save();
    }

    /**
     * The signed texture of a skin, which is what a packet NPC needs.
     * <p>
     * Saved skins answer straight away. A player name is looked up at Mojang once and then written into
     * skins.yml under that name, so the lookup never happens twice and the server can work offline afterwards.
     *
     * @return the texture, or null when the skin is unknown and Mojang could not be reached
     */
    public static @Nullable SkinTexture texture(@Nullable String skin) {
        if (skin == null || skin.trim().isEmpty() || config == null) return null;

        String value = config.getYml().getString(PATH + "." + skin + ".value");
        if (value != null) {
            return new SkinTexture(value, config.getYml().getString(PATH + "." + skin + ".signature"));
        }

        // Not saved yet, so treat it as a player name. This touches the network and must stay off the main thread.
        SkinTexture fetched = SkinTexture.fetch(skin);
        if (fetched == null) return null;

        savePreset(skin, fetched.getValue(), fetched.getSignature());
        return fetched;
    }

    /**
     * Remember a texture under a nickname, signature included when there is one.
     */
    public static void savePreset(@NotNull String name, @NotNull String value, @Nullable String signature) {
        init();
        config.getYml().set(PATH + "." + name + ".value", value);
        if (signature != null) config.getYml().set(PATH + "." + name + ".signature", signature);
        config.save();
    }

    /**
     * Build the head an NPC wears, used as the body on versions with no packet NPC.
     *
     * @param skin a nickname from skins.yml, a player name, or a raw texture value
     * @return the head, or null when the skin could not be resolved
     */
    public static @Nullable ItemStack head(@Nullable String skin) {
        if (skin == null || skin.trim().isEmpty()) return null;

        String resolved = resolve(skin);
        try {
            // RTag takes a player name, a texture value or a texture url and works out which it was given.
            return SkullTexture.getTexturedHead(resolved);
        } catch (Throwable error) {
            BedWars.plugin.getLogger().warning("Nao foi possivel carregar a skin do NPC: " + skin
                    + " (" + error.getMessage() + ")");
            return null;
        }
    }

    /**
     * @return the texture value behind a nickname, or the input untouched when it is not one
     */
    public static @NotNull String resolve(@NotNull String skin) {
        if (config == null) return skin;

        String value = config.getYml().getString(PATH + "." + skin + ".value");
        return value == null ? skin : value;
    }
}
