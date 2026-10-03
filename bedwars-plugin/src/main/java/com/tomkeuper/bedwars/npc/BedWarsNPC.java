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
import com.tomkeuper.bedwars.api.npc.IPlayerNPC;
import com.tomkeuper.bedwars.arena.Arena;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A join NPC standing in the lobby.
 * <p>
 * The body is a real player drawn with packets, so it looks like a player rather than a statue. A packet
 * entity raises no Bukkit events, so an invisible armour stand sits on the same spot purely to catch clicks.
 * Versions with no packet NPC fall back to a visible armour stand wearing the skin as a head.
 * <p>
 * The text above it is a stack of marker stands, one per line: those are real entities, so everyone in the
 * world sees them without any per viewer bookkeeping.
 */
public class BedWarsNPC {

    /** Vertical gap between hologram lines. */
    private static final double LINE_SPACING = 0.28;
    /** Height of the first line above the NPC's feet, so the text clears the head. */
    private static final double FIRST_LINE_OFFSET = 2.375;

    private final String id;
    private NPCType type;
    /** Arena group for a MODE npc, or the comma separated groups a MULTI npc offers. */
    private String group;
    private String skin;
    private Location location;
    private List<String> lines;

    /** The player everyone sees, or null on a version that cannot draw one. */
    private IPlayerNPC npc;
    /** Invisible when there is a packet NPC, visible when it is standing in for one. */
    private ArmorStand body;
    private final List<ArmorStand> holograms = new ArrayList<>();

    public BedWarsNPC(@NotNull String id, @NotNull NPCType type, @NotNull String group,
                      @NotNull String skin, @NotNull Location location, @NotNull List<String> lines) {
        this.id = id;
        this.type = type;
        this.group = group;
        this.skin = skin;
        this.location = location;
        this.lines = new ArrayList<>(lines);
    }

    /* ------------------------------------------------------------------ data */

    public String getId() {
        return id;
    }

    public NPCType getType() {
        return type;
    }

    public void setType(@NotNull NPCType type) {
        this.type = type;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(@NotNull String group) {
        this.group = group;
    }

    /**
     * @return the groups a MULTI npc offers, or the single group of a MODE one
     */
    public @NotNull List<String> getGroups() {
        return Arrays.asList(group.split(","));
    }

    public String getSkin() {
        return skin;
    }

    public void setSkin(@NotNull String skin) {
        this.skin = skin;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(@NotNull Location location) {
        this.location = location;
    }

    public @NotNull List<String> getLines() {
        return lines;
    }

    public void setLines(@NotNull List<String> lines) {
        this.lines = new ArrayList<>(lines);
    }

    /* ------------------------------------------------------------------ world */

    /**
     * @return the id the clients know the body by, or -1 when there is no packet NPC
     */
    public int getEntityId() {
        return npc == null ? -1 : npc.getEntityId();
    }

    public boolean isSpawned() {
        return body != null && body.isValid();
    }

    /**
     * @return true if the entity belongs to this npc, which is how a click is traced back to it
     */
    public boolean owns(@Nullable Entity entity) {
        if (entity == null) return false;
        if (body != null && body.equals(entity)) return true;
        return holograms.contains(entity);
    }

    /**
     * Build the body and the text.
     * <p>
     * Leftovers from a previous run are swept first: armour stands are real entities, so a server that was
     * killed without a clean shutdown would otherwise stack a second copy on top of the first.
     */
    public void spawn() {
        despawn();
        if (location.getWorld() == null) return;

        clearLeftovers();
        renderLines();

        // The hitbox always exists: it is what turns a click into an event, packet NPC or not.
        body = create(location.clone(), false);

        SkinTexture texture = NPCSkins.texture(skin);
        npc = texture == null ? null
                : BedWars.nms.createPlayerNPC(location.clone(), texture.getValue(), texture.getSignature());

        if (npc != null) {
            body.setVisible(false);
            showToWorld();
            return;
        }

        // No packet NPC on this version, so the stand has to be the body itself.
        body.setVisible(true);
        body.setArms(true);
        body.setBasePlate(false);

        ItemStack head = NPCSkins.head(skin);
        if (head != null) body.getEquipment().setHelmet(head);
    }

    /**
     * Send the NPC to everyone already standing in its world.
     */
    public void showToWorld() {
        if (npc == null || location.getWorld() == null) return;
        for (Player player : location.getWorld().getPlayers()) npc.show(player);
    }

    /**
     * A packet NPC has to be sent to each player, so anyone arriving in the world needs it too.
     */
    public void show(@NotNull Player player) {
        if (npc == null) return;
        if (location.getWorld() == null) return;
        if (!location.getWorld().equals(player.getWorld())) return;

        npc.show(player);
    }

    public void hide(@NotNull Player player) {
        if (npc != null) npc.hide(player);
    }

    public void despawn() {
        if (npc != null) {
            npc.destroy();
            npc = null;
        }
        if (body != null) {
            body.remove();
            body = null;
        }
        for (ArmorStand hologram : holograms) hologram.remove();
        holograms.clear();
    }

    /**
     * Rewrite the text, which is how the player count stays current.
     */
    public void updateLines() {
        if (!isSpawned()) return;

        // The line count can change when an admin edits it, so the stands are rebuilt rather than reused.
        if (holograms.size() != lines.size()) {
            for (ArmorStand hologram : holograms) hologram.remove();
            holograms.clear();
            renderLines();
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            holograms.get(i).setCustomName(render(lines.get(i)));
        }
    }

    private void renderLines() {
        for (int i = 0; i < lines.size(); i++) {
            // First line on top: the list reads downwards, the stands go downwards from the offset.
            double y = FIRST_LINE_OFFSET - (i * LINE_SPACING);

            ArmorStand hologram = create(location.clone().add(0, y, 0), true);
            hologram.setVisible(false);
            hologram.setCustomName(render(lines.get(i)));
            hologram.setCustomNameVisible(true);
            holograms.add(hologram);
        }
    }

    /**
     * Colour codes, the live player count and the names of this npc's groups.
     * <p>
     * A MULTI npc holds several groups, so both placeholders are worked out across all of them: the count is
     * their sum, and the label is the list. Reading either straight off {@code group} would give the raw
     * "1v1,2v2,3v3,4v4" and a count of zero, since no single arena is in a group by that name.
     */
    private String render(@NotNull String line) {
        return ChatColor.translateAlternateColorCodes('&', line
                .replace("%bw_players%", String.valueOf(countPlayers()))
                .replace("%bw_modes%", listGroups()));
    }

    private int countPlayers() {
        int total = 0;
        for (String each : getGroups()) total += Arena.getPlayers(each.trim());
        return total;
    }

    /**
     * @return the groups written out for a player to read, e.g. "1v1, 2v2, 3v3 e 4v4"
     */
    private String listGroups() {
        List<String> groups = getGroups();
        if (groups.size() == 1) return groups.get(0).trim();

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) text.append(i == groups.size() - 1 ? " e " : ", ");
            text.append(groups.get(i).trim());
        }
        return text.toString();
    }

    private ArmorStand create(@NotNull Location at, boolean marker) {
        ArmorStand stand = at.getWorld().spawn(at, ArmorStand.class);
        stand.setGravity(false);
        stand.setMarker(marker);
        stand.setVisible(false);
        stand.setCustomNameVisible(false);
        stand.setRemoveWhenFarAway(false);
        return stand;
    }

    /**
     * Drop armour stands left around this spot by a previous run of the server.
     */
    private void clearLeftovers() {
        for (Entity nearby : location.getWorld().getNearbyEntities(location, 1.5, 3.5, 1.5)) {
            if (nearby.getType() != EntityType.ARMOR_STAND) continue;
            nearby.remove();
        }
    }
}
