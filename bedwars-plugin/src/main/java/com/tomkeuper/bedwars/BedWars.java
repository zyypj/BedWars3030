package com.tomkeuper.bedwars;

import com.andrei1058.vipfeatures.api.IVipFeatures;
import com.andrei1058.vipfeatures.api.MiniGameAlreadyRegistered;
import com.tomkeuper.bedwars.addon.AddonManager;
import com.tomkeuper.bedwars.api.addon.Addon;
import com.tomkeuper.bedwars.api.addon.IAddonManager;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.chat.IChat;
import com.tomkeuper.bedwars.api.configuration.ConfigManager;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.database.IDatabase;
import com.tomkeuper.bedwars.api.economy.IEconomy;
import com.tomkeuper.bedwars.api.hologram.IHologramManager;
import com.tomkeuper.bedwars.api.items.handlers.IPermanentItem;
import com.tomkeuper.bedwars.api.items.handlers.IPermanentItemHandler;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.levels.Level;
import com.tomkeuper.bedwars.api.party.Party;
import com.tomkeuper.bedwars.api.server.RestoreAdapter;
import com.tomkeuper.bedwars.api.server.ServerType;
import com.tomkeuper.bedwars.api.server.VersionSupport;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.arena.ArenaManager;
import com.tomkeuper.bedwars.arena.VoidChunkGenerator;
import com.tomkeuper.bedwars.arena.despawnables.TargetListener;
import com.tomkeuper.bedwars.arena.feature.AntiDropFeature;
import com.tomkeuper.bedwars.arena.feature.GenSplitFeature;
import com.tomkeuper.bedwars.arena.feature.ResourceChestFeature;
import com.tomkeuper.bedwars.arena.feature.SpoilPlayerTNTFeature;
import com.tomkeuper.bedwars.arena.spectator.SpectatorListeners;
import com.tomkeuper.bedwars.arena.tasks.HologramTask;
import com.tomkeuper.bedwars.arena.tasks.OneTick;
import com.tomkeuper.bedwars.arena.tasks.Refresh;
import com.tomkeuper.bedwars.arena.upgrades.BaseListener;
import com.tomkeuper.bedwars.arena.upgrades.HealPoolListener;
import com.tomkeuper.bedwars.commands.bedwars.MainCommand;
import com.tomkeuper.bedwars.commands.join.JoinCommand;
import com.tomkeuper.bedwars.commands.leave.LeaveCommand;
import com.tomkeuper.bedwars.commands.mapselector.MapSelectorCommand;
import com.tomkeuper.bedwars.commands.party.PartyCommand;
import com.tomkeuper.bedwars.commands.rejoin.RejoinCommand;
import com.tomkeuper.bedwars.commands.shout.ShoutCommand;
import com.tomkeuper.bedwars.commands.start.StartCommand;
import com.tomkeuper.bedwars.configuration.*;
import com.tomkeuper.bedwars.connectionmanager.LoadedUsersCleaner;
import com.tomkeuper.bedwars.connectionmanager.redis.RedisArenaListeners;
import com.tomkeuper.bedwars.connectionmanager.redis.RedisConnection;
import com.tomkeuper.bedwars.database.H2;
import com.tomkeuper.bedwars.database.MySQL;
import com.tomkeuper.bedwars.database.SQLite;
import com.tomkeuper.bedwars.halloween.HalloweenSpecial;
import com.tomkeuper.bedwars.handlers.items.LobbyItem;
import com.tomkeuper.bedwars.handlers.items.PreGameItem;
import com.tomkeuper.bedwars.handlers.items.SpectatorItem;
import com.tomkeuper.bedwars.handlers.main.CommandItemHandler;
import com.tomkeuper.bedwars.handlers.main.LeaveItemHandler;
import com.tomkeuper.bedwars.handlers.main.StatsItemHandler;
import com.tomkeuper.bedwars.hologram.HologramManager;
import com.tomkeuper.bedwars.language.English;
import com.tomkeuper.bedwars.language.LangListener;
import com.tomkeuper.bedwars.language.Portuguese;
import com.tomkeuper.bedwars.levels.internal.InternalLevel;
import com.tomkeuper.bedwars.levels.internal.LevelListeners;
import com.tomkeuper.bedwars.listeners.*;
import com.tomkeuper.bedwars.listeners.arenaselector.ArenaSelectorListener;
import com.tomkeuper.bedwars.listeners.blockstatus.BlockStatusListener;
import com.tomkeuper.bedwars.listeners.chat.ChatAFK;
import com.tomkeuper.bedwars.listeners.chat.ChatFormatting;
import com.tomkeuper.bedwars.listeners.joinhandler.*;
import com.tomkeuper.bedwars.maprestore.internal.InternalAdapter;
import com.tomkeuper.bedwars.mapselector.MapSelectorCache;
import com.tomkeuper.bedwars.mapselector.MapSelectorConfig;
import com.tomkeuper.bedwars.mapselector.menu.MapSelectorListener;
import com.tomkeuper.bedwars.money.internal.MoneyListeners;
import com.tomkeuper.bedwars.shop.ShopCache;
import com.tomkeuper.bedwars.shop.ShopManager;
import com.tomkeuper.bedwars.shop.quickbuy.PlayerQuickBuyCache;
import com.tomkeuper.bedwars.sidebar.BoardManager;
import com.tomkeuper.bedwars.stats.StatsManager;
import com.tomkeuper.bedwars.support.citizens.CitizensListener;
import com.tomkeuper.bedwars.support.citizens.JoinNPC;
import com.tomkeuper.bedwars.support.papi.PAPISupport;
import com.tomkeuper.bedwars.support.papi.SupportPAPI;
import com.tomkeuper.bedwars.support.party.*;
import com.tomkeuper.bedwars.support.vault.NoChat;
import com.tomkeuper.bedwars.support.vault.NoEconomy;
import com.tomkeuper.bedwars.support.vault.WithChat;
import com.tomkeuper.bedwars.support.vault.WithEconomy;
import com.tomkeuper.bedwars.support.vipfeatures.VipFeatures;
import com.tomkeuper.bedwars.support.vipfeatures.VipListeners;
import com.tomkeuper.bedwars.upgrades.UpgradesManager;
import com.tomkeuper.bedwars.utils.ItemBuilder;
import com.tomkeuper.bedwars.utils.SlimLogger;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.github.slimjar.app.builder.ApplicationBuilder;
import me.neznamy.tab.api.TabAPI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.BiFunction;

@SuppressWarnings("WeakerAccess")
public class BedWars extends JavaPlugin {

    private static final String minecraftVersion = Bukkit.getServer().getBukkitVersion().split("-")[0];
    public static boolean debug = true, autoscale = false, isPaper = false;
    public static int hologramUpdateDistance = 50; // DEFAULT DISTANCE (update distance measured in blocks)
    public static String mainCmd = "bw", link = "https://polymart.org/resource/bedwars2023.5702";
    public static ConfigManager signs, generators;
    public static MainConfig config;
    public static ShopManager shop;
    public static PlayerQuickBuyCache playerQuickBuyCache;
    public static ShopCache shopCache;
    public static StatsManager statsManager;
    public static BedWars plugin;
    public static VersionSupport nms;
    public static ArenaManager arenaManager = new ArenaManager();
    public static IAddonManager addonManager = new AddonManager();
    public static IHologramManager hologramManager = new HologramManager();
    protected static Level level;
    private static ServerType serverType = ServerType.MULTIARENA;
    private static MapSelectorConfig mapSelectorConfig;
    private static MapSelectorCache mapSelectorCache;
    private static UpgradesManager upgradesManager;
    private static Party partyManager = new NoParty();
    private static IChat chat = new NoChat();
    private static IEconomy economy;
    private static String nmsVersion = Bukkit.getServer().getClass().getName().split("\\.")[3];
    private static String lobbyWorld = "";
    private static boolean shuttingDown = false;
    // BedWars Items;
    private static Collection<IPermanentItem> lobbyItems = new ArrayList<>();
    private static Collection<IPermanentItem> spectatorItems = new ArrayList<>();
    private static Collection<IPermanentItem> preGameItems = new ArrayList<>();
    private static Map<String, IPermanentItemHandler> itemHandlers = new HashMap<>();
    //remote database
    private static IDatabase remoteDatabase;
    private static RedisConnection redisConnection;
    private static com.tomkeuper.bedwars.api.BedWars api;
    private BukkitAudiences adventure;
    private boolean serverSoftwareSupport = true, papiSupportLoaded = false, vaultEconomyLoaded = false, vaultChatLoaded = false;

    public static void registerEvents(Listener... listeners) {
        Arrays.stream(listeners).forEach(l -> plugin.getServer().getPluginManager().registerEvents(l, plugin));
    }

    public static void setDebug(boolean value) {
        debug = value;
    }

    public static void setAutoscale(boolean autoscale) {
        BedWars.autoscale = autoscale;
    }

    public static void debug(String message) {
        if (debug) {
            plugin.getLogger().info("DEBUG: " + message);
        }
    }

    public static String getForCurrentVersion(String v18, String v12, String v13) {
        switch (getServerVersion()) {
            case "v1_8_R3":
                return v18;
            case "v1_12_R1":
                return v12;
        }
        return v13;
    }

    public static ServerType getServerType() {
        return serverType;
    }

    public static void setServerType(ServerType serverType) {
        BedWars.serverType = serverType;
        // bungee has always required auto scale to work; the other modes opt in through the config
        autoscale = serverType == ServerType.BUNGEE
                || config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_AUTO_SCALE_ENABLED);
    }

    public static Party getPartyManager() {
        return partyManager;
    }

    public static void setPartyManager(Party partyManager) {
        BedWars.partyManager = partyManager;
    }

    public static IChat getChatSupport() {
        return chat;
    }

    /**
     * Get current levels manager.
     */
    public static Level getLevelSupport() {
        return level;
    }

    /**
     * Set the levels manager.
     * You can use this to add your own levels manager just implement
     * the Level interface so the plugin will be able to display
     * the level internally.
     */

    public static void setLevelAdapter(Level levelsManager) {
        if (levelsManager instanceof InternalLevel) {
            if (LevelListeners.instance == null) {
                Bukkit.getPluginManager().registerEvents(new LevelListeners(), BedWars.plugin);
            }
        } else {
            if (LevelListeners.instance != null) {
                PlayerJoinEvent.getHandlerList().unregister(LevelListeners.instance);
                PlayerQuitEvent.getHandlerList().unregister(LevelListeners.instance);
                LevelListeners.instance = null;
            }
        }
        level = levelsManager;
    }

    public static IEconomy getEconomy() {
        return economy;
    }

    public static void setEconomy(IEconomy economy) {
        BedWars.economy = economy;
    }

    public static ConfigManager getGeneratorsCfg() {
        return generators;
    }

    /**
     * Configuration of the /bwmenu map selector.
     */
    public static MapSelectorConfig getMapSelectorConfig() {
        return mapSelectorConfig;
    }

    /**
     * Favourite maps and per map join counts of the /bwmenu map selector.
     */
    public static MapSelectorCache getMapSelectorCache() {
        return mapSelectorCache;
    }

    /**
     * Get the server version
     * Ex: v1_8_R3
     *
     * @since v0.6.5beta
     */
    public static String getServerVersion() {
        return nmsVersion;
    }

    public static String getLobbyWorld() {
        return lobbyWorld;
    }

    public static void setLobbyWorld(String lobbyWorld) {
        BedWars.lobbyWorld = lobbyWorld;
    }

    /**
     * Get remote database.
     */
    public static IDatabase getRemoteDatabase() {
        return remoteDatabase;
    }

    public static void setRemoteDatabase(IDatabase database) {
        remoteDatabase = database;
    }

    /**
     * Get redis connection.
     */
    public static RedisConnection getRedisConnection() {
        return redisConnection;
    }

    public static StatsManager getStatsManager() {
        return statsManager;
    }

    public static UpgradesManager getUpgradeManager() {
        return upgradesManager;
    }

    public static com.tomkeuper.bedwars.api.BedWars getAPI() {
        return api;
    }

    public static boolean isShuttingDown() {
        return shuttingDown;
    }

    public static Collection<IPermanentItem> getLobbyItems() {
        return lobbyItems;
    }

    public static Collection<IPermanentItem> getSpectatorItems() {
        return spectatorItems;
    }

    public static Collection<IPermanentItem> getPreGameItems() {
        return preGameItems;
    }

    public static boolean registerItemHandler(IPermanentItemHandler handler) {
        if (itemHandlers.containsKey(handler.getId())) {
            return false;
        }
        itemHandlers.put(handler.getId(), handler);
        return true;
    }

    public static Map<String, IPermanentItemHandler> getItemHandlers() {
        return itemHandlers;
    }

    @Override
    public void onLoad() {

        //Spigot support
        try {
            Class.forName("org.spigotmc.SpigotConfig");
        } catch (Exception ignored) {
            this.getLogger().severe("Não consigo rodar no software do seu servidor. Verifique:");
            this.getLogger().severe("https://wiki.tomkeuper.com/docs/BedWars2023/compatibility");
            serverSoftwareSupport = false;
            return;
        }

        try {
            Path downloadPath = Paths.get(getDataFolder().getPath() + File.separator + "libs");
            ApplicationBuilder.appending("BedWars2030")
                    .logger(new SlimLogger(this))
                    .downloadDirectoryPath(downloadPath)
                    .mirrorSelector((a, b) -> a)
                    .build();

        } catch (IOException | ReflectiveOperationException | URISyntaxException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            isPaper = true;
        } catch (ClassNotFoundException e) {
            isPaper = false;
        }

        plugin = this;

        /* Load version support */
        //noinspection rawtypes
        switch (minecraftVersion) {
            case "1.20.4":
                nmsVersion = "v1_20_R3";
                break;
            case "1.20.5":
                nmsVersion = "v1_20_R4";
                break;
            case "1.20.6":
                nmsVersion = "v1_20_6";
                break;
            case "1.21":
            case "1.21.1":
                nmsVersion = "v1_21_R1";
                break;
            case "1.21.2":
            case "1.21.3":
                nmsVersion = "v1_21_R2";
                break;
            case "1.21.4":
                nmsVersion = "v1_21_R3";
                break;
            case "1.21.6":
            case "1.21.7":
            case "1.21.8":
                nmsVersion = "v1_21_R5";
                break;
            case "1.21.11":
                nmsVersion = "v1_21_R7";
            default:
                break;
        }
        Class supp;

        try {
            supp = Class.forName("com.tomkeuper.bedwars.support.version." + nmsVersion + "." + nmsVersion);
        } catch (ClassNotFoundException e) {
            serverSoftwareSupport = false;
            this.getLogger().severe("Não consigo rodar na sua versão: " + minecraftVersion);
            return;
        }

        api = new API();
        Bukkit.getServicesManager().register(com.tomkeuper.bedwars.api.BedWars.class, api, this, ServicePriority.Highest);

        try {
            //noinspection unchecked
            nms = (VersionSupport) supp.getConstructor(Class.forName("org.bukkit.plugin.Plugin"), String.class).newInstance(this, nmsVersion);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            e.printStackTrace();
            serverSoftwareSupport = false;
            this.getLogger().severe("Não foi possível carregar o suporte para a versão do servidor: " + minecraftVersion);
            return;
        }

        this.getLogger().info("Carregando suporte para paper/spigot: " + minecraftVersion);

        // Setup languages
        new English();
        new Portuguese();

        config = new MainConfig(this, "config");
        hologramUpdateDistance = config.getInt(ConfigPath.GENERAL_CONFIGURATION_HOLOGRAM_UPDATE_DISTANCE);

        generators = new GeneratorsConfig(this, "generators", this.getDataFolder().getPath());

        /* /bwmenu map selector */
        mapSelectorConfig = new MapSelectorConfig(this, "map-selector", this.getDataFolder().getPath());
        mapSelectorCache = new MapSelectorCache(this, "map-selector-cache", this.getDataFolder().getPath());
        // Initialize signs config after the main config
        if (getServerType() != ServerType.BUNGEE) {
            signs = new SignsConfig(this, "signs", this.getDataFolder().getPath());
        }
    }

    @Override
    public void onEnable() {
        if (!serverSoftwareSupport) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.adventure = BukkitAudiences.create(this);

        nms.registerVersionListeners();

        if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null) {
            plugin.getLogger().warning("-=-=-=-=-=-=-=- Multiverse foi encontrado! -=-=-=-=-=-=-=-");
            plugin.getLogger().warning("");
            plugin.getLogger().warning(" Se não for configurado corretamente, o Multiverse vai causar problemas!");
            plugin.getLogger().warning("");
            plugin.getLogger().warning("      Garanta que o MV NÃO mexa em nenhum mapa do BW.");
            plugin.getLogger().warning("");
            plugin.getLogger().warning("_________________________________________________________");
        }

        if (!this.handleWorldAdapter()) {
            api.setRestoreAdapter(new InternalAdapter(this));
        }

        /* Register commands */
        nms.registerCommand(mainCmd, new MainCommand(mainCmd));

        // newer versions do not seem to like delayed registration of commands
        if (nms.getVersion() >= 9) {
            this.registerDelayedCommands();
        } else {
            Bukkit.getScheduler().runTaskLater(this, this::registerDelayedCommands, 20L);
        }

        /* Setup plugin messaging channel */
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        /* Check if lobby location is set. Required for non Bungee servers */
        if (config.getLobbyWorldName().isEmpty() && serverType != ServerType.BUNGEE) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "A localização do lobby não está definida!");
        }

        /* Check if CloudNet support is requested (replaces server-id name the CloudNet service ID) */
        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_CLOUDNET_SUPPORT) && serverType == ServerType.BUNGEE) {
            plugin.getLogger().log(java.util.logging.Level.INFO, "CloudNet Service ID = " + Wrapper.getInstance().getServiceId().getName());
            config.set(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID, Wrapper.getInstance().getServiceId().getName());
        }

        /* Load lobby world if not main level
         * when the server finishes loading. */
        if (getServerType() == ServerType.MULTIARENA)
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!config.getLobbyWorldName().isEmpty()) {
                    if (Bukkit.getWorld(config.getLobbyWorldName()) == null && new File(Bukkit.getWorldContainer(), config.getLobbyWorldName() + "/level.dat").exists()) {
                        if (!config.getLobbyWorldName().equalsIgnoreCase(Bukkit.getServer().getWorlds().get(0).getName())) {
                            Bukkit.getScheduler().runTaskLater(this, () -> {
                                Bukkit.createWorld(new WorldCreator(config.getLobbyWorldName()));

                                if (Bukkit.getWorld(config.getLobbyWorldName()) != null) {
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> Objects.requireNonNull(Bukkit.getWorld(config.getLobbyWorldName()))
                                            .getEntities().stream().filter(e -> e instanceof Monster).forEach(Entity::remove), 20L);
                                }
                            }, 100L);
                        }
                    }
                    Location l = config.getConfigLoc("lobbyLoc");
                    if (l != null) {
                        World w = Bukkit.getWorld(config.getLobbyWorldName());
                        if (w != null) {
                            w.setSpawnLocation(l.getBlockX(), l.getBlockY(), l.getBlockZ());
                        }
                    }
                }
            }, 1L);

        // Register events
        registerEvents(new EnderPearlLanded(), new QuitAndTeleportListener(), new BreakPlace(), new DamageDeathMove(), new Inventory(), new Interact(), new RefreshGUI(), new HungerWeatherSpawn(), new CmdProcess(),
                new FireballListener(), new EggBridge(), new SpectatorListeners(), new BaseListener(), new TargetListener(), new LangListener(), new Warnings(this), new ChatAFK(), new GameEndListener(),
                new MapSelectorListener());

        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_HEAL_POOL_ENABLE)) {
            registerEvents(new HealPoolListener());
        }

        if (getServerType() == ServerType.BUNGEE) {
            if (autoscale) {
                redisConnection = new RedisConnection();
                registerEvents(new RedisArenaListeners(redisConnection));
                if (!redisConnection.connect()) {
                    getLogger().severe("Não foi possível conectar ao servidor redis! Verifique a configuração do redis e certifique-se de que o servidor redis está rodando! Desativando o plugin...");
                    setEnabled(false);
                    return;
                }
                registerEvents(new AutoscaleListener(), new JoinListenerBungee());
                Bukkit.getScheduler().runTaskTimerAsynchronously(this, new LoadedUsersCleaner(), 60L, 60L);
            } else {
                registerEvents(new ServerPingListener(), new JoinListenerBungeeLegacy());
            }
        } else if (getServerType() == ServerType.MULTIARENA || getServerType() == ServerType.SHARED) {
            registerEvents(new ArenaSelectorListener(), new BlockStatusListener());
            if (getServerType() == ServerType.MULTIARENA) {
                registerEvents(new JoinListenerMultiArena());
            } else {
                registerEvents(new JoinListenerShared());
            }
            if (autoscale) registerEvents(new AutoscaleListener());
        }

        registerEvents(new WorldLoadListener());

        if (!(getServerType() == ServerType.BUNGEE && autoscale)) {
            registerEvents(new JoinHandlerCommon());
        }

        // Register setup-holograms fix
        registerEvents(new ChunkLoad());

        registerEvents(new InvisibilityPotionListener());

        statsManager = new StatsManager();

        /* Party support */
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (config.getYml().getBoolean(ConfigPath.GENERAL_CONFIGURATION_ALLOW_PARTIES)) {

                if (getServer().getPluginManager().isPluginEnabled("Parties")) {
                    getLogger().info("Integração com o Parties (por AlessioDP) ativada!");
                    partyManager = new PartiesAdapter();
                } else if (Bukkit.getServer().getPluginManager().isPluginEnabled("PartyAndFriends")) {
                    getLogger().info("Integração com o Party and Friends for Spigot (por Simonsator) ativada!");
                    partyManager = new PAF();
                } else if (Bukkit.getServer().getPluginManager().isPluginEnabled("Spigot-Party-API-PAF")) {
                    getLogger().info("Integração com a Spigot Party API do Party and Friends Extended (por Simonsator) ativada!");
                    partyManager = new PAFBungeecordRedisApi();
                }

                if (partyManager instanceof NoParty) {
                    partyManager = new Internal();
                    getLogger().info("Loading internal Party system. /party");
                }
            } else {
                partyManager = new NoParty();
            }
        }, 10L);

        /* Levels support */
        setLevelAdapter(new InternalLevel());

        /* Register tasks */
        Bukkit.getScheduler().runTaskTimer(this, new Refresh(), 20L, 20L);
        //new Refresh().runTaskTimer(this, 20L, 20L);

        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_PERFORMANCE_ROTATE_GEN)) {
            //new OneTick().runTaskTimer(this, 120, 1);
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, new OneTick(), 120, 1);
        }

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new HologramTask(), 20L, config.getInt(ConfigPath.GENERAL_CONFIGURATION_PERFORMANCE_HOLOGRAM_UPDATE_RATE));

        /* Register NMS entities */
        nms.registerEntities();

        /* Database support */
        if (config.getString(ConfigPath.GENERAL_CONFIGURATION_DATABASE_TYPE).equalsIgnoreCase("mysql")) {
            MySQL mySQL = new MySQL();
            long time = System.currentTimeMillis();
            if (!mySQL.connect()) {
                this.getLogger().severe("Não foi possível conectar ao banco de dados! Verifique suas credenciais e certifique-se de que o IP do servidor está liberado no MySQL.");
                remoteDatabase = new SQLite();
            } else {
                remoteDatabase = mySQL;
            }
            if (System.currentTimeMillis() - time >= 5000) {
                this.getLogger().severe("It took " + ((System.currentTimeMillis() - time) / 1000) + " ms para estabelecer a conexão com o banco de dados!\n" +
                        "Não é recomendável usar esta conexão remota!");
            }
            remoteDatabase.init();
        } else if (config.getString(ConfigPath.GENERAL_CONFIGURATION_DATABASE_TYPE).equalsIgnoreCase("sqlite")) {
            remoteDatabase = new SQLite();
            remoteDatabase.init();
        } else if (config.getString(ConfigPath.GENERAL_CONFIGURATION_DATABASE_TYPE).equalsIgnoreCase("h2")) {
            remoteDatabase = new H2();
            remoteDatabase.init();
        }

        /* Citizens support */
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (this.getServer().getPluginManager().getPlugin("Citizens") != null) {
                JoinNPC.setCitizensSupport(true);
                getLogger().info("Integração com o Citizens ativada. /bw npc");
                registerEvents(new CitizensListener());
            }

            //spawn NPCs
            try {
                JoinNPC.spawnNPCs();
            } catch (Exception e) {
                this.getLogger().severe("Não foi possível criar os NPCs de entrada. Certifique-se de ter a versão correta do Citizens para o seu servidor!");
                JoinNPC.setCitizensSupport(false);
            }
        }, 40L);

        /* Save messages for stats gui items if custom items added, for each language */
        Language.setupCustomStatsMessages();


        /* PlaceholderAPI Support */
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPISupport().register();
            SupportPAPI.setSupportPAPI(new SupportPAPI.withPAPI());
            papiSupportLoaded = true;
        }
        /*
         * Vault support
         * The task is to initialize after all plugins have loaded,
         *  to make sure any economy/chat plugins have been loaded and registered.
         */
        Bukkit.getScheduler().runTask(this, () -> {
            if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
                try {
                    //noinspection rawtypes
                    RegisteredServiceProvider rsp = this.getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
                    if (rsp != null) {
                        WithChat.setChat((net.milkbowl.vault.chat.Chat) rsp.getProvider());
                        vaultChatLoaded = true;
                        chat = new WithChat();
                    } else {
                        plugin.getLogger().info("Vault encontrado, mas nenhum provedor de chat!");
                        chat = new NoChat();
                    }
                } catch (Exception var2_2) {
                    chat = new NoChat();
                }
                try {
                    registerEvents(new MoneyListeners());
                    RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = this.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (rsp != null) {
                        WithEconomy.setEconomy(rsp.getProvider());
                        vaultEconomyLoaded = true;
                        economy = new WithEconomy();
                    } else {
                        plugin.getLogger().info("Vault encontrado, mas nenhum provedor de economia!");
                        economy = new NoEconomy();
                    }
                } catch (Exception var2_2) {
                    economy = new NoEconomy();
                }
            } else {
                chat = new NoChat();
                economy = new NoEconomy();
            }
        });

        /* Chat support */
        if (config.getBoolean(ConfigPath.GENERAL_CHAT_FORMATTING)) {
            registerEvents(new ChatFormatting());
        }

        /* Protect glass walls from tnt explosion */
        nms.registerTntWhitelist(
                (float) config.getDouble(ConfigPath.GENERAL_TNT_PROTECTION_END_STONE_BLAST),
                (float) config.getDouble(ConfigPath.GENERAL_TNT_PROTECTION_GLASS_BLAST)
        );

        /* Prevent issues on reload */
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kickPlayer("O BedWars2030 foi RECARREGADO! (não recarregue plugins)");
        }

        /* Load sounds configuration */
        Sounds.init();

        /* Initialize shop */
        shop = new ShopManager();
        shop.loadShop();

        /* Load shop overrides */
        shop.loadOverrides();

        // Startup migration: convert legacy Quick Buy identifiers to scoped format
        com.tomkeuper.bedwars.shop.ShopDataMigrator.runIfNeeded();

        registerItemHandlers(new StatsItemHandler("stats", this, api), new CommandItemHandler("command", this, api), new LeaveItemHandler("leave", this, api));

        /* Initialize instances */
        shopCache = new ShopCache();
        playerQuickBuyCache = new PlayerQuickBuyCache();

        //Leave this code at the end of the enable method
        for (Language l : Language.getLanguages()) {
            l.setupUnSetCategories();
            Language.addDefaultMessagesCommandItems(l);
        }

        LevelsConfig.init();

        /* Load Money Configuration */
        MoneyConfig.init();

        // bStats metrics
        Metrics metrics = new Metrics(this, 18317);
        metrics.addCustomChart(new SimplePie("server_type", () -> getServerType().toString()));
        metrics.addCustomChart(new SimplePie("default_language", () -> Language.getDefaultLanguage().getIso()));
        metrics.addCustomChart(new SimplePie("auto_scale", () -> String.valueOf(autoscale)));
        metrics.addCustomChart(new SimplePie("party_adapter", () -> partyManager.getClass().getSimpleName()));
        metrics.addCustomChart(new SimplePie("chat_adapter", () -> chat.getClass().getSimpleName()));
        metrics.addCustomChart(new SimplePie("level_adapter", () -> getLevelSupport().getClass().getSimpleName()));
        metrics.addCustomChart(new SimplePie("db_adapter", () -> getRemoteDatabase().getClass().getSimpleName()));
        metrics.addCustomChart(new SimplePie("map_adapter", () -> String.valueOf(getAPI().getRestoreAdapter().getOwner().getName())));

        if (Bukkit.getPluginManager().getPlugin("VipFeatures") != null) {
            try {
                IVipFeatures vf = Bukkit.getServicesManager().getRegistration(IVipFeatures.class).getProvider();
                vf.registerMiniGame(new VipFeatures(this));
                registerEvents(new VipListeners(vf));
                getLogger().log(java.util.logging.Level.INFO, "Integração com o VipFeatures ativada.");
            } catch (Exception e) {
                getLogger().warning("Não foi possível carregar o suporte ao VipFeatures.");
            } catch (MiniGameAlreadyRegistered miniGameAlreadyRegistered) {
                miniGameAlreadyRegistered.printStackTrace();
            }
        }

        // Initialize team upgrades
        upgradesManager = new UpgradesManager();
        upgradesManager.init();

        // Initialize sidebar manager
        Bukkit.getScheduler().runTask(this, () -> {
            if (Bukkit.getPluginManager().getPlugin("TAB") != null) {
                getLogger().info("Ativando a integração com o TAB!");
                if (!checkTABVersion(Bukkit.getPluginManager().getPlugin("TAB").getDescription().getVersion())) {
                    this.getLogger().severe("Versão do TAB inválida, você está usando a v" + Bukkit.getPluginManager().getPlugin("TAB").getDescription().getVersion() + " mas é necessária a v5.0.0 ou superior!");
                    Bukkit.getPluginManager().disablePlugin(this);
                    return;
                }
                if (BoardManager.init()) {
                    getLogger().info("O suporte ao TAB foi carregado");

                    /* Drop game worlds a previous crash left behind before creating new ones. */
                    arenaManager.cleanupOrphanWorlds();

                    /* Load join signs. */
                    loadArenasAndSigns();

                    /* Keep one joinable game per arena, self healing if one fails to load. */
                    startAutoScaleTopUpTask();

                } else {
                    this.getLogger().severe("A scoreboard do TAB não está ativada! Aplicando a configuração do TAB automaticamente...");

                    // Execute the command programmatically
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bw applyTabConfig");
                    getLogger().info("O comando de configuração do TAB foi executado.");

                    this.getLogger().warning("\n\nReiniciando o servidor para aplicar as alterações...\n\n");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");


                }
            } else {
                this.getLogger().severe("Não foi possível integrar com o TAB do NEZNAMY!");
                Bukkit.getPluginManager().disablePlugin(this);
            }
        });

        // Load items after plugin is initialized, this gives addons a chance to register handlers.
        Bukkit.getScheduler().runTask(this, () -> {
            debug("Loading item + handlers");
            /* Load permanent join items */
            loadLobbyItems();
            loadSpectatorItems();
            loadPreGameItems();
        });

//        registerEvents(new ScoreboardListener()); #Disabled for now

        // Halloween Special
        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_ENABLE_HALLOWEEN)) HalloweenSpecial.init();

        // Resource Chest
        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_RESOURCE_CHEST_ENABLED)) ResourceChestFeature.init();

        // Register features
        SpoilPlayerTNTFeature.init();
        GenSplitFeature.init();
        AntiDropFeature.init();

        // Initialize the addons
        Bukkit.getScheduler().runTaskLater(this, () -> addonManager.loadAddons(), 60L);

        // Check config settings
        if (redisConnection != null) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (redisConnection.checkSettings("default_rankup_cost", String.valueOf(LevelsConfig.getNextCost(1)))) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "As configurações do redis correspondem aos valores padrão.");
                } else {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "As configurações de rede não correspondem aos valores definidos! Verifique a configuração!");
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Quer definir o valor como padrão? Use '" + ChatColor.WHITE + "/bw redisUpdate default_rankup_cost" + ChatColor.RED + "' pelo console!");
                }
            }, 70L);
        }

        // Send startup message, delayed to make sure everything is loaded and registered.
        Bukkit.getScheduler().runTaskLater(this, () -> {
            this.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            this.getLogger().info("BedWars2030 v" + plugin.getDescription().getVersion() + " foi ativado!");
            this.getLogger().info("");
            this.getLogger().info("Tipo de servidor: " + getServerType().toString() + (getServerType() == ServerType.BUNGEE ? " (ServerID: " + config.getString(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_SERVER_ID) + ")" : ""));
            this.getLogger().info("Auto Scale: " + autoscale);
            this.getLogger().info("Datasource: " + remoteDatabase.getClass().getSimpleName());
            this.getLogger().info("Restore Adapter: " + api.getRestoreAdapter().getDisplayName());
            this.getLogger().info("NMS version: " + nms.getClass().getSimpleName());
            this.getLogger().info("");

            StringJoiner arenaString = new StringJoiner(", ");
            arenaString.setEmptyValue("Nenhuma");
            for (IArena arena : api.getArenaUtil().getArenas()) {
                arenaString.add(arena.getArenaName());
            }

            this.getLogger().info("Arena" + (api.getArenaUtil().getArenas().isEmpty() || api.getArenaUtil().getArenas().size() > 1 ? "s" : "") + " (" + api.getArenaUtil().getArenas().size() + "): " + arenaString);

            StringJoiner addonString = new StringJoiner(", ");
            addonString.setEmptyValue("Nenhuma");
            for (Addon addon : api.getAddonsUtil().getAddons()) {
                addonString.add(addon.getName());
            }

            this.getLogger().info("Addon" + (addonManager.getAddons().isEmpty() || addonManager.getAddons().size() > 1 ? "s" : "") + " (" + addonManager.getAddons().size() + "): " + addonString);
            this.getLogger().info("");
            this.getLogger().info("PAPI Support: " + papiSupportLoaded);
            this.getLogger().info("Vault Chat Hook: " + vaultChatLoaded);
            this.getLogger().info("Vault Economy Hook: " + vaultEconomyLoaded);
            this.getLogger().info("");
            this.getLogger().info("TAB Version: " + Bukkit.getPluginManager().getPlugin("TAB").getDescription().getVersion());
            this.getLogger().info("TAB Features: ");
            this.getLogger().info("  - Scoreboard: " + (TabAPI.getInstance().getScoreboardManager() == null));
            this.getLogger().info("  - BossBar: " + (TabAPI.getInstance().getBossBarManager() == null));
            this.getLogger().info("  - TablistNameFormatting: " + (TabAPI.getInstance().getTabListFormatManager() == null));
            this.getLogger().info("  - HeaderFooterFormatting: " + (TabAPI.getInstance().getHeaderFooterManager() == null));
            this.getLogger().info("");
            this.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }, 80L);
    }

    private void registerDelayedCommands() {
        if (!nms.isBukkitCommandRegistered("shout")) {
            nms.registerCommand("shout", new ShoutCommand("shout"));
        }
        nms.registerCommand("rejoin", new RejoinCommand("rejoin"));
        if (!(nms.isBukkitCommandRegistered("leave") && getServerType() == ServerType.BUNGEE)) {
            nms.registerCommand("leave", new LeaveCommand("leave"));
        }
        if (getServerType() != ServerType.BUNGEE && config.getBoolean(ConfigPath.GENERAL_ENABLE_PARTY_CMD)) {
            Bukkit.getLogger().info("Registrando o comando /party..");
            nms.registerCommand("party", new PartyCommand("party"));
        }
        registerShortcut("bwmenu", Collections.singletonList("bedwarsmenu"), MapSelectorCommand::new);
        registerShortcut("iniciar", Collections.singletonList("start"), StartCommand::new);
        registerShortcut("entrar", Arrays.asList("join", "jogar"), JoinCommand::new);
    }

    /**
     * Register a standalone shortcut for a /{@link #mainCmd} sub command, skipping any
     * alias another plugin already owns so we never steal an existing command.
     */
    private void registerShortcut(String name, List<String> aliases, BiFunction<String, List<String>, Command> factory) {
        if (nms.isBukkitCommandRegistered(name)) {
            getLogger().warning("O comando /" + name + " já está registrado por outro plugin, atalho ignorado.");
            return;
        }
        List<String> free = new ArrayList<>(aliases.size());
        for (String alias : aliases) {
            if (!nms.isBukkitCommandRegistered(alias)) free.add(alias);
        }
        nms.registerCommand(name, factory.apply(name, free));
    }

    public void onDisable() {
        shuttingDown = true;
        addonManager.unloadAddons();
        if (!serverSoftwareSupport) return;
        if (getServerType() == ServerType.BUNGEE) {
            redisConnection.close();
        }
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        for (IArena a : new LinkedList<>(Arena.getArenas())) {
            try {
                a.disable();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    /**
     * Safety net for the auto scale system.
     * <p>
     * Games are normally topped up the moment one starts playing or ends, so this pass usually
     * finds nothing to do. It exists for the cases events cannot cover: a world that failed to
     * load, an arena disabled and re-enabled by hand, or a limit that freed up. It creates at
     * most one game per pass and does no I/O, so it stays cheap even with many maps.
     */
    private void startAutoScaleTopUpTask() {
        if (!autoscale) return;
        int seconds = config.getInt(ConfigPath.GENERAL_CONFIGURATION_AUTO_SCALE_TOP_UP_INTERVAL);
        if (seconds <= 0) return;
        Bukkit.getScheduler().runTaskTimer(this, () -> arenaManager.topUpArenas(), 200L, seconds * 20L);
    }

    private void loadArenasAndSigns() {

        api.getRestoreAdapter().convertWorlds();

        File dir = new File(plugin.getDataFolder(), "/Arenas");
        if (dir.exists()) {
            List<File> files = new ArrayList<>();
            File[] fls = dir.listFiles();
            for (File fl : Objects.requireNonNull(fls)) {
                if (fl.isFile()) {
                    if (fl.getName().endsWith(".yml")) {
                        files.add(fl);
                    }
                }
            }

            if (serverType == ServerType.BUNGEE && !autoscale) {
                if (files.isEmpty()) {
                    this.getLogger().log(java.util.logging.Level.WARNING, "Nenhuma arena encontrada!");
                    return;
                }
                Random r = new Random();
                int x = r.nextInt(files.size());
                String name = files.get(x).getName().replace(".yml", "");
                new Arena(name, null);
            } else {
                for (File file : files) {
                    new Arena(file.getName().replace(".yml", ""), null);
                }
            }
        }
    }

    /**
     * Try loading custom adapter support.
     *
     * @return true when custom adapter was registered.
     */
    private boolean handleWorldAdapter() { //todo fix version check because current check is limited
        String adapterPath;
        if (nms.getVersion() <= 12) {
            Plugin swmPlugin = Bukkit.getPluginManager().getPlugin("SlimeWorldManager");

            if (null == swmPlugin) {
                return false;
            }
            PluginDescriptionFile pluginDescription = swmPlugin.getDescription();
            if (null == pluginDescription) {
                return false;
            }

            String[] versionString = pluginDescription.getVersion().split("\\.");

            int major = Integer.parseInt(versionString[0]);
            int minor = Integer.parseInt(versionString[1]);
            int release = versionString.length >= 3 ? Integer.parseInt(versionString[2]) : 0;

            if (((major == 2 && minor == 2 && release == 1) || swmPlugin.getDescription().getVersion().equals("2.3.0-SNAPSHOT")) && (nms.getVersion() == 0 || nms.getVersion() == 5)) {
                adapterPath = "com.tomkeuper.bedwars.arena.mapreset.slime.SlimeAdapter";
            } else if ((major == 2 && (minor >= 8 && minor <= 10) && (release >= 0 && release <= 9)) && (nms.getVersion() == 8)) {
                adapterPath = "com.tomkeuper.bedwars.arena.mapreset.slime.AdvancedSlimeAdapter";
            } else if ((major > 2 || major == 2 && minor >= 10) && (nms.getVersion() >= 9 && nms.getVersion() <= 12)) {
                adapterPath = "com.tomkeuper.bedwars.arena.mapreset.slime.SlimePaperAdapter";
            } else {
                this.getLogger().warning("Não foi possível encontrar o caminho do adaptador para a versão do SWM, ela não é suportada?");
                return false;
            }
        } else {
            if (Bukkit.getServer().getName().equalsIgnoreCase("AdvancedSlimePaper")) {
                adapterPath = "com.tomkeuper.bedwars.arena.mapreset.slime.AdvancedSlimePaperAdapter";
            } else {
                this.getLogger().warning("Não foi possível encontrar o caminho do adaptador para a versão do ASP, ela não é suportada?");
                return false;
            }
        }

        try {
            Constructor<?> constructor = Class.forName(adapterPath).getConstructor(Plugin.class);
            getLogger().info("Loading restore adapter: " + adapterPath + " ...");

            RestoreAdapter candidate = (RestoreAdapter) constructor.newInstance(this);
            api.setRestoreAdapter(candidate);
            getLogger().info("Hook into " + candidate.getDisplayName() + " como adaptador de restauração.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            this.getLogger().warning("Algo deu errado! Usando o adaptador de reset interno...");
        }
        return false;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new VoidChunkGenerator();
    }

    private boolean checkTABVersion(String version) {
        String targetVersion = "5.0.0";

        String[] currentParts = version.split("\\.");
        String[] targetParts = targetVersion.split("\\.");

        // Compare major version
        int currentMajor = Integer.parseInt(currentParts[0]);
        int targetMajor = Integer.parseInt(targetParts[0]);
        if (currentMajor < targetMajor) {
            return false;
        } else if (currentMajor > targetMajor) {
            return true;
        }

        // Compare minor version
        int currentMinor = Integer.parseInt(currentParts[1]);
        int targetMinor = Integer.parseInt(targetParts[1]);
        if (currentMinor < targetMinor) {
            return false;
        } else if (currentMinor > targetMinor) {
            return true;
        }

        // Compare patch version
        int currentPatch = Integer.parseInt(currentParts[2]);
        int targetPatch = Integer.parseInt(targetParts[2]);

        return currentPatch >= targetPatch;
    }

    private void loadPreGameItems() {
        if (config.getYml().get(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_PATH) == null) return;

        for (String item : config.getYml()
                .getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_PATH)
                .getKeys(false)) {

            if (!checkConfigEntries(item,
                    ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_MATERIAL,
                    ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_DATA,
                    ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_SLOT,
                    ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_ENCHANTED)) {
                continue;
            }

            String materialStr = config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_MATERIAL.replace("%path%", item));
            Material material = Material.valueOf(materialStr);
            int data = config.getInt(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_DATA.replace("%path%", item));
            boolean enchanted = config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_ENCHANTED.replace("%path%", item));
            int slot = config.getInt(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_SLOT.replace("%path%", item));

            ItemBuilder builder = new ItemBuilder(material).setDurability((short) data);
            if (enchanted) builder.setGlow(true);

            // Default Head
            if (material.name().equals("PLAYER_HEAD") || material.name().equals("SKULL_ITEM") && data == 3) {
                builder.setSkull("MrCeasar");
            }

            ItemStack itemStack = builder.build();
            ItemStack finalItemStack = nms.addCustomData(itemStack, "preGameItem");

            PreGameItem preGameItem;
            IPermanentItemHandler handler = itemHandlers.get(item);
            if (handler != null) {
                preGameItem = new PreGameItem(handler, finalItemStack, slot, item);
            } else {
                if (config.getYml().getString(
                        ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_COMMAND.replace("%path%", item)) != null) {
                    preGameItem = new PreGameItem(itemHandlers.get("command"), finalItemStack, slot, item);
                } else {
                    this.getLogger().severe("Nenhum handler ou comando encontrado para o item de pré-jogo: " + item);
                    continue;
                }
            }

            debug("Item de pré-jogo carregado: " + preGameItem.getIdentifier());
            preGameItems.add(preGameItem);
        }
    }

    void loadSpectatorItems() {
        if (config.getYml().get(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_PATH) == null) return;

        for (String item : config.getYml()
                .getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_PATH)
                .getKeys(false)) {

            if (!checkConfigEntries(item,
                    ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_MATERIAL,
                    ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_DATA,
                    ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_SLOT,
                    ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_ENCHANTED)) {
                continue;
            }

            String materialStr = config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_MATERIAL.replace("%path%", item));
            Material material = Material.valueOf(materialStr);
            int data = config.getInt(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_DATA.replace("%path%", item));
            boolean enchanted = config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_ENCHANTED.replace("%path%", item));
            int slot = config.getInt(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_SLOT.replace("%path%", item));

            ItemBuilder builder = new ItemBuilder(material).setDurability((short) data);
            if (enchanted) builder.setGlow(true);

            // Default Head
            if ((material.name().equals("SKULL_ITEM") && data == 3) || material.name().equals("PLAYER_HEAD")) {
                builder.setSkull("MrCeasar");
            }

            ItemStack itemStack = builder.build();
            ItemStack finalItemStack = nms.addCustomData(itemStack, "spectatorItem");

            SpectatorItem spectatorItem;
            IPermanentItemHandler handler = itemHandlers.get(item);
            if (handler != null) {
                spectatorItem = new SpectatorItem(handler, finalItemStack, slot, item);
            } else {
                if (config.getYml().getString(
                        ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_COMMAND.replace("%path%", item)) != null) {
                    spectatorItem = new SpectatorItem(itemHandlers.get("command"), finalItemStack, slot, item);
                } else {
                    this.getLogger().severe("Nenhum handler ou comando encontrado para o item de espectador: " + item);
                    continue;
                }
            }

            debug("Item de espectador carregado: " + spectatorItem.getIdentifier());
            spectatorItems.add(spectatorItem);
        }
    }

    void loadLobbyItems() {
        if (config.getYml().get(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH) == null) return;

        for (String item : config.getYml()
                .getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH)
                .getKeys(false)) {

            if (!checkConfigEntries(item,
                    ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_MATERIAL,
                    ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_DATA,
                    ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_SLOT,
                    ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_ENCHANTED)) {
                continue;
            }

            String materialStr = config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_MATERIAL.replace("%path%", item));
            Material material = Material.valueOf(materialStr);
            int data = config.getInt(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_DATA.replace("%path%", item));
            boolean enchanted = config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_ENCHANTED.replace("%path%", item));
            int slot = config.getInt(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_SLOT.replace("%path%", item));

            ItemBuilder builder = new ItemBuilder(material).setDurability((short) data);
            if (enchanted) builder.setGlow(true);

            // Default Head
            if (material.name().equals("PLAYER_HEAD") || material.name().equals("SKULL_ITEM") && data == 3) {
                builder.setSkull("MrCeasar");
            }

            ItemStack itemStack = builder.build();
            ItemStack finalItemStack = nms.addCustomData(itemStack, "lobbyItem");

            LobbyItem lobbyItem;
            IPermanentItemHandler handler = itemHandlers.get(item);
            if (handler != null) {
                lobbyItem = new LobbyItem(handler, finalItemStack, slot, item);
            } else {
                if (config.getYml().getString(
                        ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_COMMAND.replace("%path%", item)) != null) {
                    lobbyItem = new LobbyItem(itemHandlers.get("command"), finalItemStack, slot, item);
                } else {
                    this.getLogger().severe("Nenhum handler ou comando encontrado para o item de lobby: " + item);
                    continue;
                }
            }

            debug("Item de lobby carregado: " + lobbyItem.getIdentifier());
            lobbyItems.add(lobbyItem);
        }
    }

    private boolean checkConfigEntries(String item, String... paths) {
        boolean valid = true;
        for (String path : paths) {
            if (config.getYml().get(path.replace("%path%", item)) == null) {
                BedWars.plugin.getLogger().severe(path.replace("%path%", item) + " não está definido!");
                valid = false;
            }
        }
        return valid;
    }

    private void registerItemHandlers(IPermanentItemHandler... handlers) {
        for (IPermanentItemHandler handler : handlers) {
            if (registerItemHandler(handler)) {
                getLogger().info("Registered item handler: " + handler.getId());
            } else {
                getLogger().warning("Não foi possível registrar o handler de item: " + handler.getId());
            }
        }
    }

    public BukkitAudiences adventure() {
        return this.adventure;
    }
}
