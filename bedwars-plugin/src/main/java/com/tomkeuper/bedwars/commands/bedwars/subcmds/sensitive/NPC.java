/*
 * BedWars2023 - A bed wars mini-game.
 * Copyright (C) 2024 Tomas Keuper
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: contact@fyreblox.com
 */

package com.tomkeuper.bedwars.commands.bedwars.subcmds.sensitive;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.arena.ArenaMode;
import com.tomkeuper.bedwars.arena.Misc;
import com.tomkeuper.bedwars.configuration.Permissions;
import com.tomkeuper.bedwars.npc.BedWarsNPC;
import com.tomkeuper.bedwars.npc.NPCManager;
import com.tomkeuper.bedwars.npc.NPCSkins;
import com.tomkeuper.bedwars.npc.NPCType;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Manage the join NPCs from in game.
 * <p>
 * Everything an NPC needs is either short enough to type or picked up from where the admin is standing, so no
 * part of this asks for a texture value in chat: a skin is a player name or a nickname saved in skins.yml.
 */
public class NPC extends SubCommand {

    public NPC(ParentCommand parent, String name) {
        super(parent, name);
        showInList(true);
        setPriority(12);
        setPermission(Permissions.PERMISSION_NPC);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " " + getSubCommandName()
                        + "         §8   - §egerenciar NPCs de entrada",
                "§fCriar, mover, remover e editar NPCs.\n§fClique para ver os comandos.",
                "/" + getParent().getName() + " " + getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage("§cEste comando é apenas para jogadores!");
            return true;
        }

        Player player = (Player) sender;
        NPCManager manager = NPCManager.getInstance();
        if (manager == null) {
            player.sendMessage("§cO sistema de NPCs não foi inicializado.");
            return true;
        }

        if (args.length == 0) {
            usage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                return create(player, manager, args);
            case "remove":
                return remove(player, manager, args);
            case "move":
                return move(player, manager, args);
            case "skin":
                return skin(player, manager, args);
            case "line":
                return line(player, manager, args);
            case "saveskin":
                return saveSkin(player, args);
            case "list":
                return list(player, manager);
            default:
                usage(player);
                return true;
        }
    }

    /* ------------------------------------------------------------------ sub actions */

    private boolean create(@NotNull Player player, @NotNull NPCManager manager, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§c▪ §7Uso: §e/" + BedWars.mainCmd + " npc create <id> <mode|multi> <grupo[,grupo2]> [skin]");
            player.sendMessage("§7O NPC nasce onde você está, virado para onde você olha.");
            return true;
        }

        String id = args[1];
        NPCType type = NPCType.byName(args[2]);
        if (type == null) {
            player.sendMessage("§cTipo inválido. Use §fmode §cou §fmulti§c.");
            return true;
        }

        String group = args[3];
        for (String part : group.split(",")) {
            String trimmed = part.trim();
            boolean known = ArenaMode.getByGroup(trimmed) != null
                    || Arena.getArenas().stream().anyMatch(a -> a.getGroup().equalsIgnoreCase(trimmed));

            if (!known) {
                player.sendMessage("§cO grupo §f" + trimmed + " §cnão existe.");
                return true;
            }
            // A mode with no arena yet is fine: the menu opens and says there is no map when someone tries.
            if (Arena.getArenas().stream().noneMatch(a -> a.getGroup().equalsIgnoreCase(trimmed))) {
                player.sendMessage("§eAviso: §f" + trimmed + " §eainda não tem nenhuma arena.");
            }
        }

        String skin = args.length > 4 ? args[4] : player.getName();

        BedWarsNPC npc = manager.create(id, type, group, skin, player.getLocation());
        if (npc == null) {
            player.sendMessage("§cJá existe um NPC com o id §f" + id + "§c.");
            return true;
        }

        player.sendMessage("§aNPC §f" + id + " §acriado. Edite o texto com §f/" + BedWars.mainCmd + " npc line " + id + " 1 <texto>§a.");
        return true;
    }

    private boolean remove(@NotNull Player player, @NotNull NPCManager manager, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c▪ §7Uso: §e/" + BedWars.mainCmd + " npc remove <id>");
            return true;
        }

        if (!manager.remove(args[1])) {
            player.sendMessage("§cNenhum NPC com o id §f" + args[1] + "§c.");
            return true;
        }

        player.sendMessage("§aNPC §f" + args[1] + " §aremovido.");
        return true;
    }

    private boolean move(@NotNull Player player, @NotNull NPCManager manager, String[] args) {
        BedWarsNPC npc = require(player, manager, args);
        if (npc == null) return true;

        manager.move(npc, player.getLocation());
        player.sendMessage("§aNPC §f" + npc.getId() + " §amovido para onde você está.");
        return true;
    }

    private boolean skin(@NotNull Player player, @NotNull NPCManager manager, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c▪ §7Uso: §e/" + BedWars.mainCmd + " npc skin <id> <nick|apelido>");
            if (!NPCSkins.getPresets().isEmpty()) {
                player.sendMessage("§7Apelidos salvos: §f" + String.join("§7, §f", NPCSkins.getPresets()));
            }
            return true;
        }

        BedWarsNPC npc = require(player, manager, args);
        if (npc == null) return true;

        npc.setSkin(args[2]);
        manager.refresh(npc);
        player.sendMessage("§aSkin do NPC §f" + npc.getId() + " §aalterada para §f" + args[2] + "§a.");
        return true;
    }

    private boolean line(@NotNull Player player, @NotNull NPCManager manager, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§c▪ §7Uso: §e/" + BedWars.mainCmd + " npc line <id> <numero> <texto>");
            player.sendMessage("§7Use §e%bw_players% §7para a contagem de jogadores do grupo.");
            player.sendMessage("§7Exemplo: §f&c&lSOLO §7e §f&f%bw_players% jogando");
            return true;
        }

        BedWarsNPC npc = require(player, manager, args);
        if (npc == null) return true;

        int index;
        try {
            index = Integer.parseInt(args[2]) - 1;
        } catch (NumberFormatException error) {
            player.sendMessage("§cO número da linha precisa ser um número.");
            return true;
        }
        if (index < 0) {
            player.sendMessage("§cA primeira linha é a número 1.");
            return true;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        List<String> lines = new ArrayList<>(npc.getLines());
        // Writing past the end just appends, so lines can be added without a separate command.
        while (lines.size() <= index) lines.add("");
        lines.set(index, text);

        npc.setLines(lines);
        manager.refresh(npc);
        player.sendMessage("§aLinha §f" + (index + 1) + " §ado NPC §f" + npc.getId() + " §aatualizada.");
        return true;
    }

    /**
     * The one place a texture value is ever typed, and it is typed once: afterwards the nickname is enough.
     */
    private boolean saveSkin(@NotNull Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c▪ §7Uso: §e/" + BedWars.mainCmd + " npc saveskin <apelido> <texture value>");
            player.sendMessage("§7Cole o value uma vez só. Depois use o apelido em §f/" + BedWars.mainCmd + " npc skin§7.");
            return true;
        }

        String value = String.join("", Arrays.copyOfRange(args, 2, args.length));
        NPCSkins.savePreset(args[1], value);
        player.sendMessage("§aSkin §f" + args[1] + " §asalva. Use §f/" + BedWars.mainCmd + " npc skin <id> " + args[1] + "§a.");
        return true;
    }

    private boolean list(@NotNull Player player, @NotNull NPCManager manager) {
        Map<String, String> waiting = manager.getWaitingForWorld();

        if (manager.getNPCs().isEmpty() && waiting.isEmpty()) {
            player.sendMessage("§7Nenhum NPC criado ainda.");
            return true;
        }

        player.sendMessage("§6NPCs de entrada:");
        for (BedWarsNPC npc : manager.getNPCs()) {
            player.sendMessage("§7 ▪ §f" + npc.getId() + " §8· §7" + npc.getType().name().toLowerCase()
                    + " §8· §7" + npc.getGroup() + " §8· §7skin: §f" + npc.getSkin());
        }

        // Listed separately so a npc held up by a world is never mistaken for one that was lost.
        for (Map.Entry<String, String> entry : waiting.entrySet()) {
            player.sendMessage("§7 ▪ §f" + entry.getKey() + " §8· §caguardando o mundo §f" + entry.getValue());
        }
        return true;
    }

    /* ------------------------------------------------------------------ helpers */

    private BedWarsNPC require(@NotNull Player player, @NotNull NPCManager manager, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cInforme o id do NPC.");
            return null;
        }

        BedWarsNPC npc = manager.get(args[1]);
        if (npc == null) player.sendMessage("§cNenhum NPC com o id §f" + args[1] + "§c.");
        return npc;
    }

    private void usage(@NotNull Player player) {
        String base = "§c▪ §7/" + BedWars.mainCmd + " npc ";
        player.sendMessage("§6NPCs de entrada:");
        player.sendMessage(base + "§ecreate <id> <mode|multi> <grupo[,grupo2]> [skin]");
        player.sendMessage(base + "§eremove <id>");
        player.sendMessage(base + "§emove <id>");
        player.sendMessage(base + "§eskin <id> <nick|apelido>");
        player.sendMessage(base + "§eline <id> <numero> <texto>");
        player.sendMessage(base + "§esaveskin <apelido> <texture value>");
        player.sendMessage(base + "§elist");
    }

    @Override
    public List<String> getTabComplete() {
        return Arrays.asList("create", "remove", "move", "skin", "line", "saveskin", "list");
    }
}
