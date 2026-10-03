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
import com.tomkeuper.bedwars.api.configuration.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the join NPCs: where they are, what they do and keeping their text current.
 */
public class NPCManager {

    private static final String PATH = "npcs";
    /** How often the player counts on the holograms are rewritten. */
    private static final long REFRESH_TICKS = 40L;

    private static NPCManager instance;

    private final Map<String, BedWarsNPC> npcs = new LinkedHashMap<>();
    /** Ids whose world was not loaded yet, and the world each one is waiting for. */
    private final Map<String, String> waitingForWorld = new LinkedHashMap<>();
    private ConfigManager config;
    private int refreshTask = -1;

    public static NPCManager getInstance() {
        return instance;
    }

    public static void init() {
        if (instance != null) return;
        instance = new NPCManager();
        instance.load();
    }

    /* ------------------------------------------------------------------ storage */

    private void load() {
        NPCSkins.init();
        config = new ConfigManager(BedWars.plugin, "npcs", BedWars.plugin.getDataFolder().getPath());
        config.getYml().options().header("NPCs de entrada do BedWars.\n"
                + "Use /bw npc no jogo em vez de editar isto na mao.\n");

        ConfigurationSection section = config.getYml().getConfigurationSection(PATH);
        if (section != null) {
            for (String id : section.getKeys(false)) {
                BedWarsNPC npc = read(id);
                if (npc == null) continue;

                npcs.put(id, npc);
                if (migrateLines(npc)) save(npc);
            }
        }

        if (!waitingForWorld.isEmpty()) {
            BedWars.plugin.getLogger().info("NPCs aguardando o mundo carregar: " + waitingForWorld.keySet());
        }

        // Worlds are not loaded yet while the plugin enables, so the bodies go up on the first tick.
        Bukkit.getScheduler().runTaskLater(BedWars.plugin, this::spawnAll, 20L);

        refreshTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(BedWars.plugin,
                () -> npcs.values().forEach(BedWarsNPC::updateLines), REFRESH_TICKS, REFRESH_TICKS);
    }

    private @Nullable BedWarsNPC read(@NotNull String id) {
        String base = PATH + "." + id + ".";

        NPCType type = NPCType.byName(config.getYml().getString(base + "type"));
        String group = config.getYml().getString(base + "group");
        String skin = config.getYml().getString(base + "skin", "Steve");
        String world = config.getYml().getString(base + "world");
        List<String> lines = config.getYml().getStringList(base + "lines");

        if (type == null || group == null || world == null) {
            // Naming the missing field turns "my npcs vanished" into something readable in the log.
            BedWars.plugin.getLogger().warning("NPC invalido no npcs.yml: " + id
                    + " (type=" + type + ", group=" + group + ", world=" + world + ")");
            return null;
        }

        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            // A world managed by Multiverse and friends opens in their onEnable, which can come after this one.
            // Dropping the npc here is what made it vanish on restart while its holograms stayed behind, so it
            // is parked instead and read again once the world shows up.
            waitingForWorld.put(id, world);
            return null;
        }

        waitingForWorld.remove(id);

        Location location = new Location(bukkitWorld,
                config.getYml().getDouble(base + "x"),
                config.getYml().getDouble(base + "y"),
                config.getYml().getDouble(base + "z"),
                (float) config.getYml().getDouble(base + "yaw"),
                (float) config.getYml().getDouble(base + "pitch"));

        return new BedWarsNPC(id, type, group, skin == null ? "Steve" : skin, location, lines);
    }

    public void save(@NotNull BedWarsNPC npc) {
        String base = PATH + "." + npc.getId() + ".";
        Location at = npc.getLocation();

        config.getYml().set(base + "type", npc.getType().name());
        config.getYml().set(base + "group", npc.getGroup());
        config.getYml().set(base + "skin", npc.getSkin());
        config.getYml().set(base + "world", at.getWorld() == null ? null : at.getWorld().getName());
        config.getYml().set(base + "x", at.getX());
        config.getYml().set(base + "y", at.getY());
        config.getYml().set(base + "z", at.getZ());
        config.getYml().set(base + "yaw", at.getYaw());
        config.getYml().set(base + "pitch", at.getPitch());
        config.getYml().set(base + "lines", npc.getLines());
        config.save();
    }

    /* ------------------------------------------------------------------ lifecycle */

    /**
     * Put every npc in the world.
     * <p>
     * A skin that was never saved is looked up at Mojang, so the resolution runs off the main thread and each
     * npc is then built back on it.
     */
    public void spawnAll() {
        loadWaiting();
        for (BedWarsNPC npc : npcs.values()) spawnLater(npc);
    }

    /**
     * Try again on the npcs whose world was missing, and build the ones that can now be read.
     *
     * @return how many were recovered
     */
    public int loadWaiting() {
        if (waitingForWorld.isEmpty()) return 0;

        int recovered = 0;
        for (String id : new ArrayList<>(waitingForWorld.keySet())) {
            BedWarsNPC npc = read(id);
            if (npc == null) continue;

            npcs.put(id, npc);
            if (migrateLines(npc)) save(npc);
            spawnLater(npc);
            recovered++;
        }

        if (recovered > 0) {
            BedWars.plugin.getLogger().info("NPCs carregados depois que o mundo abriu: " + recovered);
        }
        return recovered;
    }

    /**
     * @return the ids still waiting on a world, so {@code /bw npc list} does not claim there are none
     */
    public @NotNull Map<String, String> getWaitingForWorld() {
        return waitingForWorld;
    }

    /**
     * Warm the skin cache off the main thread, then build the npc on it.
     */
    public void spawnLater(@NotNull BedWarsNPC npc) {
        Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, () -> {
            NPCSkins.texture(npc.getSkin());
            Bukkit.getScheduler().runTask(BedWars.plugin, npc::spawn);
        });
    }

    public void despawnAll() {
        npcs.values().forEach(BedWarsNPC::despawn);
    }

    public void shutdown() {
        if (refreshTask != -1) Bukkit.getScheduler().cancelTask(refreshTask);
        despawnAll();
    }

    /* ------------------------------------------------------------------ editing */

    public @NotNull Collection<BedWarsNPC> getNPCs() {
        return npcs.values();
    }

    public @Nullable BedWarsNPC get(@NotNull String id) {
        return npcs.get(id);
    }

    /**
     * @return the npc the entity belongs to, which is how a click finds its owner
     */
    public @Nullable BedWarsNPC byEntity(@Nullable Entity entity) {
        if (entity == null) return null;
        for (BedWarsNPC npc : npcs.values()) {
            if (npc.owns(entity)) return npc;
        }
        return null;
    }

    /**
     * @return the npc drawn with this entity id, which is what a click packet carries
     */
    public @Nullable BedWarsNPC byEntityId(int entityId) {
        if (entityId < 0) return null;
        for (BedWarsNPC npc : npcs.values()) {
            if (npc.getEntityId() == entityId) return npc;
        }
        return null;
    }

    /**
     * Create an npc and put it in the world straight away.
     *
     * @return the npc, or null when the id is already taken
     */
    public @Nullable BedWarsNPC create(@NotNull String id, @NotNull NPCType type, @NotNull String group,
                                       @NotNull String skin, @NotNull Location location) {
        if (npcs.containsKey(id)) return null;

        BedWarsNPC npc = new BedWarsNPC(id, type, group, skin, location, defaultLines(type, group));
        npcs.put(id, npc);
        save(npc);
        spawnLater(npc);
        return npc;
    }

    public boolean remove(@NotNull String id) {
        BedWarsNPC npc = npcs.remove(id);
        if (npc == null) return false;

        npc.despawn();
        config.getYml().set(PATH + "." + id, null);
        config.save();
        return true;
    }

    /**
     * Move an npc and rebuild it where it now stands.
     */
    public void move(@NotNull BedWarsNPC npc, @NotNull Location location) {
        npc.setLocation(location);
        save(npc);
        spawnLater(npc);
    }

    /**
     * Apply a change that needs the body rebuilt, such as a new skin or new text.
     */
    public void refresh(@NotNull BedWarsNPC npc) {
        save(npc);
        spawnLater(npc);
    }

    /**
     * The two lines the user asked for: the mode in bold, and the live count under it.
     * <p>
     * A MULTI npc names its groups through the placeholder rather than a fixed word, so changing which modes it
     * offers also changes what it says, with nothing to retype.
     */
    private List<String> defaultLines(@NotNull NPCType type, @NotNull String group) {
        String label = type == NPCType.MULTI ? "%bw_modes%" : group.toUpperCase();
        return new ArrayList<>(Arrays.asList("&c&l" + label, "&f%bw_players% jogando"));
    }

    /**
     * Older MULTI npcs were created with the word "MODOS" baked into the first line. They are switched to the
     * placeholder as they load, so an npc already standing in the lobby starts naming its modes without having
     * to be removed and placed again. A line that was edited by hand is left alone.
     */
    private boolean migrateLines(@NotNull BedWarsNPC npc) {
        if (npc.getType() != NPCType.MULTI) return false;

        List<String> lines = new ArrayList<>(npc.getLines());
        boolean changed = false;

        for (int i = 0; i < lines.size(); i++) {
            if (!"&c&lMODOS".equals(lines.get(i))) continue;
            lines.set(i, "&c&l%bw_modes%");
            changed = true;
        }

        if (changed) npc.setLines(lines);
        return changed;
    }
}
