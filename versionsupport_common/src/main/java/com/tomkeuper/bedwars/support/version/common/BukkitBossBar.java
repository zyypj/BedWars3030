package com.tomkeuper.bedwars.support.version.common;

import com.tomkeuper.bedwars.api.sidebar.IBossBar;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BukkitBossBar implements IBossBar {

    private final BossBar handle;

    public BukkitBossBar(@NotNull String title, @NotNull String color) {
        handle = Bukkit.createBossBar(title, toBarColor(color), BarStyle.SOLID);
        handle.setProgress(1.0D);
        handle.setVisible(true);
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        handle.addPlayer(player);
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        handle.removePlayer(player);
    }

    @Override
    public @NotNull List<Player> getPlayers() {
        return new ArrayList<>(handle.getPlayers());
    }

    @Override
    public void setTitle(@NotNull String title) {
        handle.setTitle(title);
    }

    @Override
    public void setProgress(double progress) {
        handle.setProgress(Math.max(0.0D, Math.min(1.0D, progress)));
    }

    @Override
    public void remove() {
        handle.removeAll();
        handle.setVisible(false);
    }

    private static BarColor toBarColor(String color) {
        switch (color.toUpperCase()) {
            case "RED":
            case "DARK_RED":
                return BarColor.RED;
            case "BLUE":
            case "AQUA":
            case "DARK_AQUA":
            case "DARK_BLUE":
                return BarColor.BLUE;
            case "GREEN":
            case "DARK_GREEN":
                return BarColor.GREEN;
            case "YELLOW":
            case "GOLD":
                return BarColor.YELLOW;
            case "PINK":
            case "LIGHT_PURPLE":
                return BarColor.PINK;
            case "PURPLE":
            case "DARK_PURPLE":
                return BarColor.PURPLE;
            default:
                return BarColor.WHITE;
        }
    }
}
