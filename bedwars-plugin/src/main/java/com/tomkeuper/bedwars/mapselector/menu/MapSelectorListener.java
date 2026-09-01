package com.tomkeuper.bedwars.mapselector.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * Routes clicks in a map selector screen to the handler the menu registered for that slot.
 */
public class MapSelectorListener implements Listener {

    @EventHandler
    public void onMapSelectorClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof MapSelectorMenu)) return;

        event.setCancelled(true);

        // Clicks in the player's own inventory are blocked but never routed.
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) return;

        MenuClickHandler handler = ((MapSelectorMenu) top.getHolder()).getHandler(event.getSlot());
        if (handler != null) handler.onClick(new MenuClickEvent(event));
    }

    @EventHandler
    public void onMapSelectorDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof MapSelectorMenu) {
            event.setCancelled(true);
        }
    }
}
