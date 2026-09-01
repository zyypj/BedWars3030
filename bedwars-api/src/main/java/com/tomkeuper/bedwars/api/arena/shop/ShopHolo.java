package com.tomkeuper.bedwars.api.arena.shop;

import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.hologram.containers.IHologram;
import lombok.Getter;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

public class ShopHolo {

    @Getter
    private final IHologram hologram;
    @Getter
    private final String iso;
    @Getter
    private final IArena arena;
    @Getter
    private final ITeam team;

    public ShopHolo(@Nonnull IHologram hologram, @Nonnull ITeam team, @Nonnull String iso) {
        this.hologram = hologram;
        this.team = team;
        this.arena = team.getArena();
        this.iso = iso;
        arena.addShopHologram(iso, this);
    }

    public void update() {
        hologram.update();
    }

    public void update(Player p) {
        hologram.update(p);
    }

    public void clear() {
        hologram.remove();
    }

    public void clearForPlayer(Player p) {
        hologram.removePlayer(p);
    }
}
