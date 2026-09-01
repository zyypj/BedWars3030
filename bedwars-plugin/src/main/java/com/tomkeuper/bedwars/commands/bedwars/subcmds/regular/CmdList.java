package com.tomkeuper.bedwars.commands.bedwars.subcmds.regular;

import com.tomkeuper.bedwars.api.BedWars;
import com.tomkeuper.bedwars.api.arena.team.TeamColor;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.api.server.SetupType;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.arena.Misc;
import com.tomkeuper.bedwars.arena.SetupSession;
import com.tomkeuper.bedwars.commands.bedwars.MainCommand;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

import static com.tomkeuper.bedwars.BedWars.mainCmd;
import static com.tomkeuper.bedwars.BedWars.plugin;
import static com.tomkeuper.bedwars.api.language.Language.getList;

public class CmdList extends SubCommand {

    public CmdList(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(11);
        showInList(true);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §f/" + MainCommand.getInstance().getName() + " " + getSubCommandName() + "         §8 - §e ver cmds de jogador", "§fVer os comandos de jogador.", "/" + getParent().getName() + " " + getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        if (SetupSession.isInSetupSession(p.getUniqueId())) {
            SetupSession ss = SetupSession.getSession(p.getUniqueId());
            Objects.requireNonNull(ss).getConfig().reload();

            boolean waitingSpawn = ss.getConfig().getYml().get("waiting.Loc") != null,
                    pos1 = ss.getConfig().getYml().get("waiting.Pos1") != null,
                    pos2 = ss.getConfig().getYml().get("waiting.Pos2") != null,
                    pos = pos1 && pos2;
            StringBuilder spawnNotSetNames = new StringBuilder();
            StringBuilder bedNotSet = new StringBuilder();
            StringBuilder shopNotSet = new StringBuilder();
            StringBuilder killDropsNotSet = new StringBuilder();
            StringBuilder upgradeNotSet = new StringBuilder();
            StringBuilder spawnNotSet = new StringBuilder();
            StringBuilder generatorNotSet = new StringBuilder();
            int teams = 0;

            if (ss.getConfig().getYml().get("Team") != null) {
                for (String team : Objects.requireNonNull(ss.getConfig().getYml().getConfigurationSection("Team")).getKeys(true)) {
                    if (ss.getConfig().getYml().get("Team." + team + ".Color") == null) continue;
                    ChatColor color = TeamColor.getChatColor(Objects.requireNonNull(ss.getConfig().getYml().getString("Team." + team + ".Color")));
                    if (ss.getConfig().getYml().get("Team." + team + ".Spawn") == null) {
                        spawnNotSet.append(color).append("▋");
                        spawnNotSetNames.append(color).append(team).append(" ");
                    }
                    if (ss.getConfig().getYml().get("Team." + team + ".Bed") == null) {
                        bedNotSet.append(color).append("▋");
                    }
                    if (ss.getConfig().getYml().get("Team." + team + ".Shop") == null) {
                        shopNotSet.append(color).append("▋");
                    }
                    if (ss.getConfig().getYml().get("Team." + team + "." + ConfigPath.ARENA_TEAM_KILL_DROPS_LOC) == null) {
                        killDropsNotSet.append(color).append("▋");
                    }
                    if (ss.getConfig().getYml().get("Team." + team + ".Upgrade") == null) {
                        upgradeNotSet.append(color).append("▋");
                    }
                    if (ss.getConfig().getYml().get("Team." + team + ".Iron") == null || ss.getConfig().getYml().get("Team." + team + ".Gold") == null) {
                        generatorNotSet.append(color).append("▋");
                    }
                    teams++;
                }
            }
            int emGen = 0, dmGen = 0;
            if (ss.getConfig().getYml().get("generator.Emerald") != null) {
                emGen = ss.getConfig().getYml().getStringList("generator.Emerald").size();
            }
            if (ss.getConfig().getYml().get("generator.Diamond") != null) {
                dmGen = ss.getConfig().getYml().getStringList("generator.Diamond").size();
            }

            String posMsg, group = ChatColor.RED + "(NÃO DEFINIDO)";
            if (pos1 && !pos2) {
                posMsg = ChatColor.RED + "(POS 2 NÃO DEFINIDA)";
            } else if (!pos1 && pos2) {
                posMsg = ChatColor.RED + "(POS 1 NÃO DEFINIDA)";
            } else if (pos1) {
                posMsg = ChatColor.GREEN + "(DEFINIDO)";
            } else {
                posMsg = ChatColor.GRAY + "(NÃO DEFINIDO) " + ChatColor.ITALIC + "OPCIONAL";
            }

            String g2 = ss.getConfig().getYml().getString("group");
            if (g2 != null) {
                if (!g2.equalsIgnoreCase("default")) {
                    group = ChatColor.GREEN + "(" + g2 + ")";
                }
            }

            int maxInTeam = ss.getConfig().getInt("maxInTeam");

            String setWaitingSpawn = ss.dot() + (waitingSpawn ? ChatColor.STRIKETHROUGH : "") + "setWaitingSpawn" + ChatColor.RESET + " " + (waitingSpawn ? ChatColor.GREEN + "(SET)" : ChatColor.RED + "(NÃO DEFINIDO)");
            String waitingPos = ss.dot() + (pos ? ChatColor.STRIKETHROUGH : "") + "waitingPos 1/2" + ChatColor.RESET + " " + posMsg;
            String setSpawn = ss.dot() + ((spawnNotSet.length() == 0) ? ChatColor.STRIKETHROUGH : "") + "setSpawn <teamName>" + ChatColor.RESET + " " + ((spawnNotSet.length() == 0) ? ChatColor.GREEN + "(TUDO DEFINIDO)" : ChatColor.RED + "(Restantes: " + spawnNotSet + ChatColor.RED + ")");
            String setBed = ss.dot() + ((bedNotSet.toString().isEmpty()) ? ChatColor.STRIKETHROUGH : "") + "setBed" + ChatColor.RESET + " " + ((bedNotSet.length() == 0) ? ChatColor.GREEN + "(TUDO DEFINIDO)" : ChatColor.RED + "(Restantes: " + bedNotSet + ChatColor.RED + ")");
            String setShop = ss.dot() + ((shopNotSet.toString().isEmpty()) ? ChatColor.STRIKETHROUGH : "") + "setShop" + ChatColor.RESET + " " + ((shopNotSet.length() == 0) ? ChatColor.GREEN + "(TUDO DEFINIDO)" : ChatColor.RED + "(Restantes: " + shopNotSet + ChatColor.RED + ")");
            String setKillDrops = ss.dot() + ((killDropsNotSet.toString().isEmpty()) ? ChatColor.STRIKETHROUGH : "") + "setKillDrops" + ChatColor.RESET + " " + ((shopNotSet.length() == 0) ? ChatColor.GREEN + "(TUDO DEFINIDO)" : ChatColor.RED + "(Restantes: " + killDropsNotSet + ChatColor.RED + ")");
            String setUpgrade = ss.dot() + ((upgradeNotSet.toString().isEmpty()) ? ChatColor.STRIKETHROUGH : "") + "setUpgrade" + ChatColor.RESET + " " + ((upgradeNotSet.length() == 0) ? ChatColor.GREEN + "(TUDO DEFINIDO)" : ChatColor.RED + "(Restantes: " + upgradeNotSet + ChatColor.RED + ")");
            String addGenerator = ss.dot() + "addGenerator " + ((generatorNotSet.toString().isEmpty()) ? "" : ChatColor.RED + "(Restantes: " + generatorNotSet + ChatColor.RED + ") ") + ChatColor.YELLOW + "(" + ChatColor.DARK_GREEN + "E" + emGen + " " + ChatColor.AQUA + "D" + dmGen + ChatColor.YELLOW + ")";
            String setSpectatorSpawn = ss.dot() + (ss.getConfig().getYml().get(ConfigPath.ARENA_SPEC_LOC) == null ? "" : ChatColor.STRIKETHROUGH) + "setSpectSpawn" + ChatColor.RESET + " " + (ss.getConfig().getYml().get(ConfigPath.ARENA_SPEC_LOC) == null ? ChatColor.RED + "(NÃO DEFINIDO)" : ChatColor.GRAY + "(SET)");

            s.sendMessage("");
            s.sendMessage(ChatColor.GRAY + "" + ChatColor.BOLD + MainCommand.getDot() + ChatColor.GOLD + plugin.getDescription().getName() + " v" + plugin.getDescription().getVersion() + ChatColor.GRAY + '-' + " " + ChatColor.GREEN + ss.getWorldName() + " commands");
            p.spigot().sendMessage(Misc.msgHoverClick(setWaitingSpawn, ChatColor.WHITE + "Define o local onde os jogadores vão\n" + ChatColor.WHITE + "esperar antes da partida começar.", "/" + getParent().getName() + " setWaitingSpawn", ss.getSetupType() == SetupType.ASSISTED ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(waitingPos, ChatColor.WHITE + "Faz o lobby de espera sumir quando a partida começar.\n" + ChatColor.WHITE + "Selecione como uma região do world edit.", "/" + getParent().getName() + " waitingPos ", ClickEvent.Action.SUGGEST_COMMAND));
            if (ss.getSetupType() == SetupType.ADVANCED) {
                p.spigot().sendMessage(Misc.msgHoverClick(setSpectatorSpawn, ChatColor.WHITE + "Define onde os espectadores vão nascer.", "/" + getParent().getName() + " setSpectSpawn", ClickEvent.Action.RUN_COMMAND));
            }
            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "autoCreateTeams " + ChatColor.YELLOW + "(detecção automática)", ChatColor.WHITE + "Cria os times com base nas cores das ilhas.", "/" + getParent().getName() + " autoCreateTeams", ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "createTeam <nome> <cor> " + ChatColor.YELLOW + "(" + teams + " CREATED)", ChatColor.WHITE + "Cria um time.", "/" + getParent().getName() + " createTeam ", ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "removeTeam <name>", ChatColor.WHITE + "Remove um time pelo nome.", "/" + mainCmd + " removeTeam ", ClickEvent.Action.SUGGEST_COMMAND));


            p.spigot().sendMessage(Misc.msgHoverClick(setSpawn, ChatColor.WHITE + "Define o spawn de um time.\n" + ChatColor.WHITE + "Times sem spawn definido:\n" + spawnNotSetNames.toString(), "/" + getParent().getName() + " setSpawn ", ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(setBed, ChatColor.WHITE + "Define a localização da cama de um time.\n" + ChatColor.WHITE + "Você não precisa especificar o nome do time.", "/" + getParent().getName() + " setBed", ss.getSetupType() == SetupType.ASSISTED ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(setShop, ChatColor.WHITE + "Define o NPC de um time.\n" + ChatColor.WHITE + "Você não precisa especificar o nome do time.\n" + ChatColor.WHITE + "Ele só será gerado quando a partida começar.", "/" + getParent().getName() + " setShop", ss.getSetupType() == SetupType.ASSISTED ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(setUpgrade, ChatColor.WHITE + "Define o NPC de melhorias de um time.\n" + ChatColor.WHITE + "Você não precisa especificar o nome do time.\n" + ChatColor.WHITE + "Ele só será gerado quando a partida começar.", "/" + getParent().getName() + " setUpgrade", ss.getSetupType() == SetupType.ASSISTED ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));
            if (ss.getSetupType() == SetupType.ADVANCED) {
                p.spigot().sendMessage(Misc.msgHoverClick(setKillDrops, ChatColor.WHITE + "Define o local onde serão dropados\n" + ChatColor.WHITE + "os itens do inimigo após você matá-lo.", "/" + getParent().getName() + " setKillDrops ", ClickEvent.Action.SUGGEST_COMMAND));
            }
            String genHover = (ss.getSetupType() == SetupType.ADVANCED ? ChatColor.WHITE + "Adiciona um ponto de spawn de gerador.\n" + ChatColor.YELLOW + "/" + getParent().getName() + " addGenerator <Iron/ Gold/ Emerald, Diamond>" :
                    ChatColor.WHITE + "Adiciona um ponto de spawn de gerador.\n" + ChatColor.YELLOW + "Fique na ilha de um time para definir um gerador de time") + "\n" + ChatColor.WHITE + "Fique sobre um bloco de diamante para definir o gerador de diamante.\n" + ChatColor.WHITE + "Fique sobre um bloco de esmeralda para definir um gerador de esmeralda.";

            p.spigot().sendMessage(Misc.msgHoverClick(addGenerator, genHover, "/" + getParent().getName() + " addGenerator ", ss.getSetupType() == SetupType.ASSISTED ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));
            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "removeGenerator", genHover, "/" + getParent().getName() + " removeGenerator", ss.getSetupType() == SetupType.ASSISTED ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND));

            if (ss.getSetupType() == SetupType.ADVANCED) {
                p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "setMaxInTeam <int> (DEFINIDO COMO " + maxInTeam + ")", ChatColor.WHITE + "Define o tamanho máximo do time.", "/" + mainCmd + " setMaxInTeam ", ClickEvent.Action.SUGGEST_COMMAND));
                p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "arenaGroup " + group, ChatColor.WHITE + "Define o grupo da arena.", "/" + mainCmd + " arenaGroup ", ClickEvent.Action.SUGGEST_COMMAND));
            } else {
                p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "setType <type> " + group, ChatColor.WHITE + "Adiciona a arena a um grupo.", "/" + getParent().getName() + " setType", ClickEvent.Action.RUN_COMMAND));
            }

            p.spigot().sendMessage(Misc.msgHoverClick(ss.dot() + "save", ChatColor.WHITE + "Salva a arena e volta para o lobby", "/" + getParent().getName() + " save", ClickEvent.Action.SUGGEST_COMMAND));
        } else {
            for (String string : getList((Player) s, Messages.COMMAND_MAIN)) {
                s.sendMessage(string);
            }
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
        if (s instanceof Player) {
            Player p = (Player) s;
            if (Arena.isInArena(p)) return false;

            if (SetupSession.isInSetupSession(p.getUniqueId())) return false;
        }

        return hasPermission(s);
    }
}
