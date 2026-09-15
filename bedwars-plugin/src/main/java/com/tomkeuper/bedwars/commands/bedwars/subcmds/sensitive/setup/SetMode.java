package com.tomkeuper.bedwars.commands.bedwars.subcmds.sensitive.setup;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.arena.ArenaMode;
import com.tomkeuper.bedwars.arena.SetupSession;
import com.tomkeuper.bedwars.configuration.Permissions;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SetMode extends SubCommand {

    public SetMode(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        SetupSession ss = SetupSession.getSession(p.getUniqueId());
        if (ss == null) {
            p.sendMessage("§c ▪ §7Você não está em uma sessão de setup!");
            return true;
        }

        if (ss.getConfig() == null) {
            p.sendMessage("§c ▪ §7Escolha o tipo de setup antes de definir o modo!");
            return true;
        }

        if (args.length == 0) {
            sendUsage(p);
            return true;
        }

        ArenaMode mode = ArenaMode.getByGroup(args[0]);
        if (mode == null) {
            p.sendMessage("§c▪ §7Modo inválido: §f" + args[0]);
            sendUsage(p);
            return true;
        }

        ss.getConfig().set("group", mode.getGroup());
        ss.getConfig().set("maxInTeam", mode.getMaxInTeam());
        ss.getConfig().set("minPlayers", mode.getMinPlayers());
        registerGroup(mode);

        p.sendMessage("§6 ▪ §7Modo definido: §f" + mode.getGroup());
        p.sendMessage("§6 ▪ §7Máximo por time: §f" + mode.getMaxInTeam() + " §7| Mínimo para começar: §f" + mode.getMinPlayers());
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return ArenaMode.getGroups();
    }

    @Override
    public boolean canSee(CommandSender s, com.tomkeuper.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (!SetupSession.isInSetupSession(p.getUniqueId())) return false;

        return hasPermission(s);
    }

    private void sendUsage(Player p) {
        p.sendMessage("§c▪ §7Uso: /" + BedWars.mainCmd + " " + getSubCommandName() + " <modo>");
        p.sendMessage("§c▪ §7Modos: §f" + String.join("§7, §f", ArenaMode.getGroups()));
    }

    private void registerGroup(ArenaMode mode) {
        List<String> groups = BedWars.config.getYml().getStringList(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS);
        if (groups == null) groups = new ArrayList<>();
        for (String group : groups) {
            if (group.equalsIgnoreCase(mode.getGroup())) return;
        }
        groups.add(mode.getGroup());
        BedWars.config.set(ConfigPath.GENERAL_CONFIGURATION_ARENA_GROUPS, groups);
    }
}
