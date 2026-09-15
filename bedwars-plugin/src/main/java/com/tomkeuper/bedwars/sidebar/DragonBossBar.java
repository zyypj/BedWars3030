package com.tomkeuper.bedwars.sidebar;

import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.sidebar.IBossBar;
import org.bukkit.entity.EnderDragon;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DragonBossBar {

    private final IBossBar bossBar;
    private final ITeam team;
    private final int dragonNumber;

    public DragonBossBar(@NotNull IBossBar bossBar, @NotNull ITeam team, int dragonNumber) {
        this.bossBar = bossBar;
        this.team = team;
        this.dragonNumber = dragonNumber;
    }

    @NotNull
    public IBossBar getBossBar() {
        return bossBar;
    }

    public void refresh() {
        bossBar.setProgress(getDragonHealthPercentage());
    }

    public void remove() {
        bossBar.remove();
    }

    private double getDragonHealthPercentage() {
        List<EnderDragon> dragons = team.getDragons();
        if (dragons == null || dragonNumber >= dragons.size()) return 0.0D;

        EnderDragon dragon = dragons.get(dragonNumber);
        if (dragon == null || dragon.isDead()) return 0.0D;

        double maxHealth = dragon.getMaxHealth();
        if (maxHealth <= 0.0D) return 0.0D;
        return dragon.getHealth() / maxHealth;
    }
}
