package com.tomkeuper.bedwars.api.arena.shop;

/**
 * How a shop item's purchase limit is counted.
 * <p>
 * A limit only exists when the content declares one; without it an item can be bought as often as the player
 * can afford it, which is the normal BedWars behaviour.
 */
public enum ShopLimitType {

    /**
     * Counted per player for the whole match: buy three and that player is done, whatever their team does.
     */
    PER_PLAYER,

    /**
     * Counted across the whole team for the whole match, so a team shares one allowance.
     */
    PER_TEAM,

    /**
     * Counted against what the player is carrying right now, so dropping or using one frees the slot again.
     */
    IN_INVENTORY;

    /**
     * @return the type with this name, ignoring case, or {@link #PER_PLAYER} when the name is unknown
     */
    public static ShopLimitType byName(String name) {
        if (name == null) return PER_PLAYER;
        for (ShopLimitType type : values()) {
            if (type.name().equalsIgnoreCase(name.replace('-', '_').replace(' ', '_'))) return type;
        }
        return PER_PLAYER;
    }
}
