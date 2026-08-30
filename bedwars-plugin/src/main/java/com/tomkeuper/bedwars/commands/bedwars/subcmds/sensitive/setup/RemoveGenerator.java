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

package com.tomkeuper.bedwars.commands.bedwars.subcmds.sensitive.setup;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.server.SetupType;
import com.tomkeuper.bedwars.arena.SetupSession;
import com.tomkeuper.bedwars.configuration.Permissions;
import com.tomkeuper.bedwars.configuration.Sounds;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoveGenerator extends SubCommand {
    /**
     * Create a sub-command for a bedWars command
     * Make sure you return true or it will say command not found
     *
     * @param parent parent command
     * @param name   sub-command name
     */
    public RemoveGenerator(ParentCommand parent, String name) {
        super(parent, name);
        setArenaSetupCommand(true);
        setPermission(Permissions.PERMISSION_SETUP_ARENA);
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (!(s instanceof Player)) return false;
        Player p = (Player) s;
        SetupSession ss = SetupSession.getSession(p.getUniqueId());
        if (ss == null) return false;

        // Case 1: "/bw removeGenerator" (find nearest generator and remove it)
        if (args.length == 0) {
            String[] toRemove = new String[]{"", "", ""}; // {GeneratorType, Location String, Team Name}
            Location nearest = null;

            // Search team-based generators (Iron, Gold, Emerald)
            if (ss.getConfig().getYml().get("Team") != null) {
                for (String team : ss.getConfig().getYml().getConfigurationSection("Team").getKeys(false)) {
                    for (String type : new String[]{"Iron", "Gold", "Emerald"}) {
                        if (ss.getConfig().getYml().get("Team." + team + "." + type) != null) {
                            for (String loc : ss.getConfig().getList("Team." + team + "." + type)) {
                                Location loc2 = ss.getConfig().convertStringToArenaLocation(loc);
                                if (loc2 != null && p.getLocation().distance(loc2) <= 2) {
                                    if (nearest == null || p.getLocation().distance(nearest) > p.getLocation().distance(loc2)) {
                                        nearest = loc2;
                                        toRemove[0] = type; // Type
                                        toRemove[1] = loc;  // Location string
                                        toRemove[2] = team; // Team
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search standalone generators (Diamond, Emerald)
            if (ss.getConfig().getYml().get("generator") != null) {
                for (String type : new String[]{"Emerald", "Diamond"}) {
                    if (ss.getConfig().getYml().get("generator." + type) != null) {
                        for (String loc : ss.getConfig().getList("generator." + type)) {
                            Location loc2 = ss.getConfig().convertStringToArenaLocation(loc);
                            if (loc2 != null && p.getLocation().distance(loc2) <= 2) {
                                if (nearest == null || p.getLocation().distance(nearest) > p.getLocation().distance(loc2)) {
                                    nearest = loc2;
                                    toRemove[0] = type; // Type
                                    toRemove[1] = loc;  // Location string
                                    toRemove[2] = "";   // No team
                                }
                            }
                        }
                    }
                }
            }

            // No generator found
            if (nearest == null) {
                p.sendMessage(ss.getPrefix() + "Nenhum gerador encontrado por perto (alcance 2x2).");
                p.sendMessage(ss.getPrefix() + "Você precisa estar perto do holograma do gerador que deseja remover.");
                BedWars.nms.sendTitle(p, " ", ChatColor.RED + "Nenhum gerador encontrado por perto.", 5, 40, 5);
                Sounds.playSound(ConfigPath.SOUNDS_INSUFF_MONEY, p);
                return true;
            }

            // Remove standalone generator
            if (toRemove[2].isEmpty()) {
                List<String> list = ss.getConfig().getList("generator." + toRemove[0]);
                list.remove(toRemove[1]);
                ss.getConfig().set("generator." + toRemove[0], list);

                p.sendMessage(ss.getPrefix() + "Gerador de " + toRemove[0] + " removido na localização: X:" + nearest.getBlockX() + " Y:" + nearest.getBlockY() + " Z:" + nearest.getBlockZ());
                BedWars.nms.sendTitle(p, " ", ChatColor.GREEN + "Gerador de " + toRemove[0] + " removido.", 5, 40, 5);
                Sounds.playSound(ConfigPath.SOUNDS_BOUGHT, p);
                ss.removeGeneratorHologram(nearest);
                return true;
            }

            // Remove team-based generator
            if (ss.getSetupType() == SetupType.ASSISTED) {
                // Remove all generators for this team
                ss.getConfig().set("Team." + toRemove[2] + ".Emerald", new ArrayList<>());
                ss.getConfig().set("Team." + toRemove[2] + ".Iron", new ArrayList<>());
                ss.getConfig().set("Team." + toRemove[2] + ".Gold", new ArrayList<>());

                BedWars.nms.sendTitle(p, " ", "Gerador do time " + ss.getTeamColor(toRemove[2]) + toRemove[2] + ChatColor.GREEN + " removido.", 5, 40, 5);
                Sounds.playSound(ConfigPath.SOUNDS_BOUGHT, p);
                ss.removeGeneratorHologram(nearest);

                p.sendMessage(ss.getPrefix() + "Geradores do time " + ss.getTeamColor(toRemove[2]) + toRemove[2] + ChatColor.getLastColors(ss.getPrefix()) + " foram removidos!");
                return true;
            } else {
                // Remove only the specified generator type for this team
                List<String> list = ss.getConfig().getList("Team." + toRemove[2] + "." + toRemove[0]);
                list.remove(toRemove[1]);
                ss.getConfig().set("Team." + toRemove[2] + "." + toRemove[0], list);

                p.sendMessage(ss.getPrefix() + "Gerador de " + toRemove[0] + " do time " + ss.getTeamColor(toRemove[2]) + toRemove[2] + ChatColor.getLastColors(ss.getPrefix()) + " removido na localização: X:" + nearest.getBlockX() + " Y:" + nearest.getBlockY() + " Z:" + nearest.getBlockZ());
                BedWars.nms.sendTitle(p, " ", ChatColor.GREEN + "Gerador de " + toRemove[0] + " do time " + ss.getTeamColor(toRemove[2]) + toRemove[2] + ChatColor.GREEN + " removido.", 5, 40, 5);
                Sounds.playSound(ConfigPath.SOUNDS_BOUGHT, p);
                ss.removeGeneratorHologramLineContainingType(nearest, toRemove[0]);
                return true;
            }
        }

        // Case 2: "/bw removeGenerator <type>" (remove specific generator type)

        if (args.length == 1) {
            String type = args[0];

            // Validate generator type
            List<String> validTypes = Arrays.asList("Iron", "Gold", "Emerald", "Diamond");
            if (!validTypes.contains(type)) {
                p.sendMessage(ss.getPrefix() + "Tipo de gerador inválido: " + type);
                return true;
            }

            Location nearest = null;
            String locString = ""; // Found location string
            String teamName = "";  // Found team name (if any)

            // Search normal/global generators (e.g., "generator.<type>")
            if (ss.getConfig().getYml().get("generator." + type) != null) {
                for (String loc : ss.getConfig().getList("generator." + type)) {
                    Location loc2 = ss.getConfig().convertStringToArenaLocation(loc);
                    if (loc2 != null && p.getLocation().distance(loc2) <= 2) {
                        nearest = loc2;
                        locString = loc;
                        break;
                    }
                }
            }

            // Search team-based generators (e.g., "Team.<team>.<type>")
            if (ss.getConfig().getYml().get("Team") != null) {
                for (String team : ss.getConfig().getYml().getConfigurationSection("Team").getKeys(false)) {
                    if (ss.getConfig().getYml().get("Team." + team + "." + type) != null) {
                        for (String loc : ss.getConfig().getList("Team." + team + "." + type)) {
                            Location loc2 = ss.getConfig().convertStringToArenaLocation(loc);
                            if (loc2 != null && p.getLocation().distance(loc2) <= 2) {
                                nearest = loc2;
                                locString = loc;
                                teamName = team;
                                break;
                            }
                        }
                    }
                }
            }

            // If a nearest generator was found, remove it
            if (nearest != null) {
                if (teamName.isEmpty()) {
                    // Normal generator
                    List<String> list = ss.getConfig().getList("generator." + type);
                    list.remove(locString);
                    ss.getConfig().set("generator." + type, list);

                    p.sendMessage(ss.getPrefix() + "Gerador global de " + type + " removido na localização: " +
                            "X:" + nearest.getBlockX() + " Y:" + nearest.getBlockY() + " Z:" + nearest.getBlockZ());
                    ss.removeGeneratorHologram(nearest);
                } else {
                    // Team-based generator
                    List<String> list = ss.getConfig().getList("Team." + teamName + "." + type);
                    list.remove(locString);
                    ss.getConfig().set("Team." + teamName + "." + type, list);

                    p.sendMessage(ss.getPrefix() + "Gerador de " + type + " removido do time " +
                            ss.getTeamColor(teamName) + teamName +
                            ChatColor.getLastColors(ss.getPrefix()) + " na localização: " +
                            "X:" + nearest.getBlockX() + " Y:" + nearest.getBlockY() + " Z:" + nearest.getBlockZ());
                    ss.removeGeneratorHologramLineContainingType(nearest, type);
                }

                // Save configuration after changes
                ss.getConfig().save();
                return true;
            }

            // No generator found
            p.sendMessage(ss.getPrefix() + "Nenhum gerador de " + type + " próximo para remover.");
            return true;
        }

        // If neither scenario matched, return true (command accepted but nothing executed)
        BedWars.debug("Comando de remoção de gerador não executado.");
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return new ArrayList<>();
    }
}
