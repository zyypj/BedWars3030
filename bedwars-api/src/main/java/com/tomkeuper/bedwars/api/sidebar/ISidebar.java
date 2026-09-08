package com.tomkeuper.bedwars.api.sidebar;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ISidebar {

    @NotNull
    Player getPlayer();

    @NotNull
    String getName();

    @NotNull
    List<String> getLines();

    void setContent(@NotNull String title, @NotNull List<String> lines);

    void refreshTitle();

    void refreshLines();

    void remove();
}
