package com.tomkeuper.bedwars.mapselector.menu;

/**
 * What a menu slot does when clicked.
 */
@FunctionalInterface
public interface MenuClickHandler {
    void onClick(MenuClickEvent event);
}
