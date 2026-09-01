package com.tomkeuper.bedwars.mapselector.menu;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * A menu whose content is split across pages, with the page buttons only drawn when there is somewhere to go.
 */
public abstract class PaginatedMapSelectorMenu extends MapSelectorMenu {

    private int page = 1;

    protected PaginatedMapSelectorMenu(@NotNull Player player, int rows) {
        super(player, rows);
    }

    public int getPage() {
        return page;
    }

    @Override
    public void update() {
        if (getInventory() == null) {
            open();
            return;
        }
        clearSlots();

        List<Integer> slots = getPaginatedSlots();
        List<MenuItem> all = getAllPageItems();

        int perPage = slots.size();
        int maxPage = perPage == 0 ? 1 : (int) Math.ceil(all.size() / (double) perPage);
        if (maxPage < 1) maxPage = 1;
        // Removing the last map of a page must not strand the player on a page that no longer exists.
        if (page > maxPage) page = maxPage;

        int start = (page - 1) * perPage;
        for (int i = 0; i < perPage; i++) {
            int index = start + i;
            if (index >= all.size()) break;

            MenuItem item = all.get(index);
            item.slotOverride(slots.get(i));
            setItem(item);
        }

        for (MenuItem item : getGlobalItems()) {
            setItem(item);
        }

        if (page > 1) {
            MenuItem previous = getPreviousPageItem();
            if (previous != null) {
                previous.event(event -> {
                    page = Math.max(1, page - 1);
                    update();
                });
                setItem(previous);
            }
        }

        if (page < maxPage) {
            MenuItem next = getNextPageItem();
            if (next != null) {
                int lastPage = maxPage;
                next.event(event -> {
                    page = Math.min(lastPage, page + 1);
                    update();
                });
                setItem(next);
            }
        }

        player.updateInventory();
    }

    @Override
    public List<MenuItem> getItems() {
        return Collections.emptyList();
    }

    @Override
    public String getTitle() {
        return getPaginationTitle();
    }

    public abstract List<Integer> getPaginatedSlots();

    public abstract String getPaginationTitle();

    /**
     * Every item that takes part in pagination, in display order.
     */
    public abstract List<MenuItem> getAllPageItems();

    /**
     * Items that stay put on every page.
     */
    public abstract List<MenuItem> getGlobalItems();

    public abstract @Nullable MenuItem getNextPageItem();

    public abstract @Nullable MenuItem getPreviousPageItem();
}
