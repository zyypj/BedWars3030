package com.tomkeuper.bedwars.commands.bedwars.subcmds.sensitive;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ApplyTabConfig extends SubCommand {

    private static final String RESOURCE_FILE = "tab_config.yml"; // File inside BedWars JAR
    private static final String TAB_CONFIG_FILENAME = "config.yml"; // Target name in TAB folder
    private static final String TAB_PLUGIN_NAME = "TAB"; // TAB plugin name

    public ApplyTabConfig(ParentCommand parent, String name) {
        super(parent, name);
        showInList(false);
    }

    @Override
    public boolean execute(String[] args, CommandSender sender) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("§cEste comando só pode ser executado pelo console.");
            return true;
        }

        // Check if TAB plugin is installed
        Plugin tabPlugin = Bukkit.getPluginManager().getPlugin(TAB_PLUGIN_NAME);
        if (tabPlugin == null) {
            sender.sendMessage("§cErro: O plugin TAB não está instalado ou não está ativado.");
            return true;
        }

        // Get TAB plugin's config folder
        File tabConfigFolder = tabPlugin.getDataFolder();
        if (!tabConfigFolder.exists() && !tabConfigFolder.mkdirs()) {
            sender.sendMessage("§cErro: Não foi possível criar a pasta de config do plugin TAB.");
            return true;
        }

        File destinationFile = new File(tabConfigFolder, TAB_CONFIG_FILENAME);

        // Copy file from BedWars JAR to TAB config folder
        try (InputStream input = BedWars.plugin.getResource(RESOURCE_FILE)) {
            if (input == null) {
                sender.sendMessage("§cErro: Não foi possível encontrar " + RESOURCE_FILE + " dentro do JAR do BedWars.");
                return true;
            }

            Files.copy(input, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            sender.sendMessage("§aConfiguração do plugin TAB aplicada com sucesso em " + destinationFile.getPath());
        } catch (IOException e) {
            sender.sendMessage("§cErro ao copiar a config do TAB: " + e.getMessage());
        }

        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return null;
    }
}
