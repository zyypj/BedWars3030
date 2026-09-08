package com.tomkeuper.bedwars.api.sidebar;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IBossBar {

    void addPlayer(@NotNull Player player);

    void removePlayer(@NotNull Player player);

    @NotNull
    List<Player> getPlayers();

    void setTitle(@NotNull String title);

    void setProgress(double progress);

    void remove();
}
