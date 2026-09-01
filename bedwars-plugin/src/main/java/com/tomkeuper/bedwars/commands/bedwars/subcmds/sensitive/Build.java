package com.tomkeuper.bedwars.commands.bedwars.subcmds.sensitive;

import com.tomkeuper.bedwars.api.BedWars;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.arena.Misc;
import com.tomkeuper.bedwars.arena.SetupSession;
import com.tomkeuper.bedwars.commands.bedwars.MainCommand;
import com.tomkeuper.bedwars.configuration.Permissions;
import com.tomkeuper.bedwars.listeners.BreakPlace;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class Build extends SubCommand {

    public Build(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(9);
        showInList(true);
        setPermission(Permissions.PERMISSION_BUILD);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " "+getSubCommandName()+ "         §8 - §epermissão de construção", "§fAtiva ou desativa o modo de construção \n§fpara você poder quebrar e colocar blocos.",
                "/" + getParent().getName() + " "+getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        if (!MainCommand.isLobbySet() && p != null) {
            p.sendMessage("§c▪ §7Você precisa definir a localização do lobby primeiro!");
            return true;
        }
        if (BreakPlace.isBuildSession(p)) {
            p.sendMessage("§6 ▪ §7Você não pode mais colocar e quebrar blocos!");
            BreakPlace.removeBuildSession(p);
        } else {
            p.sendMessage("§6 ▪ §7Agora você pode colocar e quebrar blocos.");
            BreakPlace.addBuildSession(p);
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return null;
    }

    @Override
    public boolean canSee(CommandSender s, BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (Arena.isInArena(p)) return false;

        if (SetupSession.isInSetupSession(p.getUniqueId())) return false;
        return hasPermission(s);
    }
}
