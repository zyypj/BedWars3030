package com.tomkeuper.bedwars.mapselector.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * The part of an inventory click a menu item cares about.
 */
public class MenuClickEvent {

    private final InventoryClickEvent event;

    public MenuClickEvent(InventoryClickEvent event) {
        this.event = event;
    }

    public boolean isRightClick() {
        return event.isRightClick();
    }

    public boolean isLeftClick() {
        return event.isLeftClick();
    }

    public ClickType getClick() {
        return event.getClick();
    }

    public Player getPlayer() {
        return (Player) event.getWhoClicked();
    }

    public InventoryClickEvent getEvent() {
        return event;
    }
}
