package com.tomkeuper.bedwars.mapselector;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.configuration.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Configuration of the map selector opened by {@code /bwmenu}.
 * <p>
 * Materials are written with {@link BedWars#getForCurrentVersion(String, String, String)} so a freshly
 * generated file already names items the running server knows about, instead of leaning on a material
 * translation layer at read time.
 */
public class MapSelectorConfig extends ConfigManager {

    public static final String PATH = "map-selector";

    public static final String MENU_TITLE = PATH + ".menus.bedwars-menu.title";
    public static final String MENU_SLOTS = PATH + ".menus.bedwars-menu.slots";
    public static final String MENU_ITEMS = PATH + ".menus.bedwars-menu.items";
    public static final String MAPS_TITLE = PATH + ".menus.maps-menu.title";
    public static final String MAPS_SLOTS = PATH + ".menus.maps-menu.slots";
    public static final String MAPS_MAP_SLOTS = PATH + ".menus.maps-menu.maps-slots";
    public static final String MAPS_ITEMS = PATH + ".menus.maps-menu.items";

    public static final String MSG_GROUP_MISSING = PATH + ".messages.open.missing";
    public static final String MSG_GROUP_UNKNOWN = PATH + ".messages.open.group-doesnt-exists";
    public static final String MSG_NO_PERMISSION = PATH + ".messages.no-permission";
    public static final String MSG_NO_MAPS = PATH + ".messages.no-maps";
    public static final String MSG_NO_FAVORITES = PATH + ".messages.no-favorites-maps";
    public static final String MSG_NOT_PARTY_LEADER = PATH + ".messages.not-party-leader";

    public static final String SELECTION_PERMISSION = PATH + ".selections.permission";
    public static final String SELECTION_UNLIMITED = PATH + ".selections.unlimited-message";

    public MapSelectorConfig(Plugin plugin, String name, String dir) {
        super(plugin, name, dir);

        YamlConfiguration yml = getYml();
        yml.options().header("Menu de seleção de mapas do /bwmenu <modo>.\n"
                + "Os itens aceitam material, data, slot, name e lore.\n");

        addDefault(MSG_GROUP_UNKNOWN, "&4&lERRO! &cEste grupo não existe.");
        addDefault(MSG_GROUP_MISSING, "&4&lERRO! &cUse: /bwmenu <modo>");
        addDefault(MSG_NO_PERMISSION, "&4&lERRO! &cVocê não tem permissão para isso.");
        addDefault(MSG_NO_FAVORITES, "&4&lERRO! &cVocê não tem nenhum mapa favorito.");
        addDefault(MSG_NO_MAPS, "&4&lERRO! &cNão há nenhuma arena disponível para esse modo.");
        addDefault(MSG_NOT_PARTY_LEADER, "&4&lERRO! &cVocê não pode entrar aqui, pois não é o líder da party.");

        addDefault(SELECTION_PERMISSION, "bw.mapselector");
        addDefault(SELECTION_UNLIMITED, "Ilimitado");

        addDefault(MENU_TITLE, "&7Bed Wars {groupName}");
        addDefault(MENU_SLOTS, 27);
        addDefault(MAPS_TITLE, "&8Bed Wars {groupName}");
        addDefault(MAPS_SLOTS, 45);
        addDefault(MAPS_MAP_SLOTS, "10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34");

        /* Main menu */
        addItem(MENU_ITEMS + ".join-random", 12, BedWars.getForCurrentVersion("BED", "BED", "RED_BED"), 0,
                "&cBed Wars ({groupName})",
                "&7%bw2023_group_count_{groupName}% jogando agora!", "", "&eClique para jogar!");
        addItem(MENU_ITEMS + ".map-selector", 14, BedWars.getForCurrentVersion("SIGN", "SIGN", "OAK_SIGN"), 0,
                "&cSeletor de Mapas ({groupName})",
                "&7Escolha o mapa que você quer jogar", "&7entre os disponíveis.", "", "&eClique para ver!");
        addItem(MENU_ITEMS + ".rejoin", 15, "ENDER_PEARL", 0,
                "&cVoltar para a partida",
                "&7Clique para voltar à sua partida", "&7caso tenha se desconectado.");
        addItem(MENU_ITEMS + ".close", 22, "BARRIER", 0, "&cFechar");

        /* Maps menu */
        addItem(MAPS_ITEMS + ".map", 0, "PAPER", 0,
                "&c{mapName}",
                "", "&7Salas disponíveis: &c{availableGames}", "&7Salas totais: &c{totalGames}", "",
                "&bClique direito para favoritar", "", "&eClique para jogar");
        addItem(MAPS_ITEMS + ".map-favorite", 0, BedWars.getForCurrentVersion("MAP", "MAP", "FILLED_MAP"), 0,
                "&6✫ &c{mapName}",
                "", "&7Salas disponíveis: &c{availableGames}", "&7Salas totais: &c{totalGames}", "",
                "&bClique direito para desfavoritar", "", "&eClique para jogar");
        /* Bottom row of the 45 slot inventory: 36 to 44. */
        addItem(MAPS_ITEMS + ".back", 36, "ARROW", 0, "&cVoltar");
        addItem(MAPS_ITEMS + ".previous-page", 38, "ARROW", 0, "&cPágina anterior", "&ePágina {previousPage}");
        addItem(MAPS_ITEMS + ".random-favourite", 40, "DIAMOND", 0,
                "&cFavorito aleatório", "", "&eClique para jogar");
        addItem(MAPS_ITEMS + ".next-page", 42, "ARROW", 0, "&cPróxima página", "&ePágina {nextPage}");
        addItem(MAPS_ITEMS + ".random-map", 44, BedWars.getForCurrentVersion("FIREWORK", "FIREWORK", "FIREWORK_ROCKET"), 0,
                "&aMapa aleatório",
                "&8{groupName}", "", "&7Seleções: &a{selectionsType}", "", "&a▸ Clique para jogar");

        yml.options().copyDefaults(true);
        save();
    }

    private void addDefault(String path, Object value) {
        getYml().addDefault(path, value);
    }

    private void addItem(String path, int slot, String material, int data, String name, String... lore) {
        addDefault(path + ".slot", slot);
        addDefault(path + ".material", material);
        addDefault(path + ".data", data);
        addDefault(path + ".name", name);
        addDefault(path + ".lore", Arrays.asList(lore));
    }

    /**
     * @return the message with colour codes translated, or an empty string when it is missing
     */
    public String getMessage(String path) {
        String value = getYml().getString(path);
        return value == null ? "" : ChatColor.translateAlternateColorCodes('&', value);
    }

    public Set<String> getKeys(String path) {
        ConfigurationSection section = getYml().getConfigurationSection(path);
        return section == null ? Collections.emptySet() : section.getKeys(false);
    }

    /**
     * Read a comma separated slot list, skipping anything that is not a number so one typo does not break the
     * whole menu.
     */
    public List<Integer> getSlots(String path) {
        String raw = getYml().getString(path);
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();

        List<Integer> slots = new ArrayList<>();
        for (String part : raw.split(",")) {
            try {
                slots.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // a bad entry should not cost the admin the entire menu
            }
        }
        return slots;
    }
}
