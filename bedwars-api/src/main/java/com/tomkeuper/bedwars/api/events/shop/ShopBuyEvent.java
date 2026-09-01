package com.tomkeuper.bedwars.api.events.shop;

import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.shop.ICategoryContent;
import com.tomkeuper.bedwars.api.shop.IShopCache;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event triggered when a player buys items from the shop.
 */
@Getter
public class ShopBuyEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player buyer;
    private final IArena arena;
    private final ICategoryContent categoryContent;
    private final int slot;

    @Setter
    private IShopCache shopCache;

    @Setter
    private boolean cancelled = false;

    /**
     * Creates a new ShopBuyEvent.
     *
     * @param buyer            The player who made the purchase.
     * @param arena            The arena where the purchase occurred.
     * @param categoryContent  The category content from the shop where the purchase was made.
     * @param shopCache         The cache that contains the items bought by the player.
     */
    public ShopBuyEvent(Player buyer, IArena arena, ICategoryContent categoryContent, IShopCache shopCache, int slot) {
        this.categoryContent = categoryContent;
        this.buyer = buyer;
        this.arena = arena;
        this.shopCache = shopCache;
        this.slot = slot;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the handler list for this event.
     *
     * @return The handler list.
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
