/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
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
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.tomkeuper.bedwars;

import com.tomkeuper.bedwars.addon.AddonManager;
import com.tomkeuper.bedwars.api.addon.IAddonManager;
import com.andrei1058.vipfeatures.api.IVipFeatures;
import com.andrei1058.vipfeatures.api.MiniGameAlreadyRegistered;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.chat.IChat;
import com.tomkeuper.bedwars.api.configuration.ConfigManager;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.economy.IEconomy;
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
import com.tomkeuper.bedwars.arena.feature.SpoilPlayerTNTFeature;
import com.tomkeuper.bedwars.arena.spectator.SpectatorListeners;
import com.tomkeuper.bedwars.arena.tasks.OneTick;
import com.tomkeuper.bedwars.arena.tasks.Refresh;
import com.tomkeuper.bedwars.arena.upgrades.BaseListener;
import com.tomkeuper.bedwars.arena.upgrades.HealPoolListener;
import com.tomkeuper.bedwars.commands.bedwars.MainCommand;
import com.tomkeuper.bedwars.commands.leave.LeaveCommand;
import com.tomkeuper.bedwars.commands.party.PartyCommand;
import com.tomkeuper.bedwars.commands.rejoin.RejoinCommand;
import com.tomkeuper.bedwars.commands.shout.ShoutCommand;
import com.tomkeuper.bedwars.configuration.*;
import com.tomkeuper.bedwars.api.database.IDatabase;
import com.tomkeuper.bedwars.database.H2;
import com.tomkeuper.bedwars.database.MySQL;
import com.tomkeuper.bedwars.database.SQLite;
import com.tomkeuper.bedwars.halloween.HalloweenSpecial;
import com.tomkeuper.bedwars.language.*;
import com.tomkeuper.bedwars.levels.internal.InternalLevel;
import com.tomkeuper.bedwars.levels.internal.LevelListeners;
import com.tomkeuper.bedwars.listeners.*;
import com.tomkeuper.bedwars.listeners.arenaselector.ArenaSelectorListener;
import com.tomkeuper.bedwars.listeners.blockstatus.BlockStatusListener;
import com.tomkeuper.bedwars.listeners.chat.ChatAFK;
import com.tomkeuper.bedwars.listeners.chat.ChatFormatting;
import com.tomkeuper.bedwars.listeners.joinhandler.*;
import com.tomkeuper.bedwars.lobbysocket.ArenaSocket;
import com.tomkeuper.bedwars.lobbysocket.LoadedUsersCleaner;
import com.tomkeuper.bedwars.lobbysocket.SendTask;
import com.tomkeuper.bedwars.maprestore.internal.InternalAdapter;
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
import com.tomkeuper.bedwars.support.vault.*;
import com.tomkeuper.bedwars.support.vipfeatures.VipFeatures;
import com.tomkeuper.bedwars.support.vipfeatures.VipListeners;
import com.tomkeuper.bedwars.upgrades.UpgradesManager;
import de.dytanic.cloudnet.wrapper.Wrapper;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.nametag.UnlimitedNameTagManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@SuppressWarnings("WeakerAccess")
public class BedWars extends JavaPlugin {

    private static ServerType serverType = ServerType.MULTIARENA;
    public static boolean debug = true, autoscale = false, isPaper = false;
    public static String mainCmd = "bw", link = "https://www.spigotmc.org/resources/50942/";
    public static ConfigManager signs, generators;
    public static MainConfig config;
    public static ShopManager shop;
    private static UpgradesManager upgradesManager;
    public static PlayerQuickBuyCache playerQuickBuyCache;
    public static ShopCache shopCache;
    public static StatsManager statsManager;
    public static BedWars plugin;
    public static VersionSupport nms;

    private static Party party = new NoParty();
    private static IChat chat = new NoChat();
    protected static Level level;
    private static IEconomy economy;
    private static final String version = Bukkit.getServer().getClass().getName().split("\\.")[3];
    private static String lobbyWorld = "";
    private static boolean shuttingDown = false;

    public static ArenaManager arenaManager = new ArenaManager();
    public static IAddonManager addonManager = new AddonManager();

    //remote database
    private static IDatabase remoteDatabase;

    private boolean serverSoftwareSupport = true, papiSupportLoaded = false, vaultEconomyLoaded = false, vaultChatLoaded = false;

    private static com.tomkeuper.bedwars.api.BedWars api;

    @Override
    public void onLoad() {

        //Spigot support
        try {
            Class.forName("org.spigotmc.SpigotConfig");
        } catch (Exception ignored) {
            this.getLogger().severe("I can't run on your server software. Please check:");
            this.getLogger().severe("https://gitlab.com/tomkeuper/BedWars2023/wikis/compatibility");
            serverSoftwareSupport = false;
            return;
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
        Class supp;

        try {
            supp = Class.forName("com.tomkeuper.bedwars.support.version." + version + "." + version);
        } catch (ClassNotFoundException e) {
            serverSoftwareSupport = false;
            this.getLogger().severe("I can't run on your version: " + version);
            return;
        }

        api = new API();
        Bukkit.getServicesManager().register(com.tomkeuper.bedwars.api.BedWars.class, api, this, ServicePriority.Highest);

        try {
            //noinspection unchecked
            nms = (VersionSupport) supp.getConstructor(Class.forName("org.bukkit.plugin.Plugin"), String.class).newInstance(this, version);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException e) {
            e.printStackTrace();
            serverSoftwareSupport = false;
            this.getLogger().severe("Could not load support for server version: " + version);
            return;
        }

        this.getLogger().info("Loading support for paper/spigot: " + version);

        // Setup languages
        new English();
        new Romanian();
        new Italian();
        new Polish();
        new Spanish();
        new Russian();
        new Bangla();
        new Persian();
        new Hindi();
        new Indonesia();
        new Portuguese();
        new SimplifiedChinese();
        new Turkish();

        config = new MainConfig(this, "config");

        generators = new GeneratorsConfig(this, "generators", this.getDataFolder().getPath());
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

        nms.registerVersionListeners();

        if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null){
            plugin.getLogger().warning("-=-=-=-=-=-=-=- Multiverse has been found! -=-=-=-=-=-=-=-");
            plugin.getLogger().warning("");
            plugin.getLogger().warning(" Unless properly configured, multiverse will cause issues!");
            plugin.getLogger().warning("");
            plugin.getLogger().warning("      Make sure that MV does NOT touch any BW maps.");
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
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Lobby location is not set!");
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
                new FireballListener(), new EggBridge(), new SpectatorListeners(), new BaseListener(), new TargetListener(), new LangListener(), new Warnings(this), new ChatAFK(), new GameEndListener());

        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_HEAL_POOL_ENABLE)) {
            registerEvents(new HealPoolListener());
        }

        if (getServerType() == ServerType.BUNGEE) {
            if (autoscale) {
                //registerEvents(new ArenaListeners());
                ArenaSocket.lobbies.addAll(config.getList(ConfigPath.GENERAL_CONFIGURATION_BUNGEE_OPTION_LOBBY_SERVERS));
                new SendTask();
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
                    getLogger().info("Hook into Parties (by AlessioDP) support!");
                    party = new PartiesAdapter();
                } else if (Bukkit.getServer().getPluginManager().isPluginEnabled("PartyAndFriends")) {
                    getLogger().info("Hook into Party and Friends for Spigot (by Simonsator) support!");
                    party = new PAF();
                } else if (Bukkit.getServer().getPluginManager().isPluginEnabled("Spigot-Party-API-PAF")) {
                    getLogger().info("Hook into Spigot Party API for Party and Friends Extended (by Simonsator) support!");
                    party = new PAFBungeecordRedisApi();
                }

                if (party instanceof NoParty) {
                    party = new Internal();
                    getLogger().info("Loading internal Party system. /party");
                }
            } else {
                party = new NoParty();
            }
        }, 10L);

        /* Levels support */
        setLevelAdapter(new InternalLevel());

        /* Register tasks */
        Bukkit.getScheduler().runTaskTimer(this, new Refresh(), 20L, 20L);
        //new Refresh().runTaskTimer(this, 20L, 20L);

        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_PERFORMANCE_ROTATE_GEN)) {
            //new OneTick().runTaskTimer(this, 120, 1);
            Bukkit.getScheduler().runTaskTimer(this, new OneTick(), 120, 1);
        }

        /* Register NMS entities */
        nms.registerEntities();

        /* Database support */
        if (config.getString(ConfigPath.GENERAL_CONFIGURATION_DATABASE_TYPE).equalsIgnoreCase("mysql")) {
            MySQL mySQL = new MySQL();
            long time = System.currentTimeMillis();
            if (!mySQL.connect()) {
                this.getLogger().severe("Could not connect to database! Please verify your credentials and make sure that the server IP is whitelisted in MySQL.");
                remoteDatabase = new SQLite();
            } else {
                remoteDatabase = mySQL;
            }
            if (System.currentTimeMillis() - time >= 5000) {
                this.getLogger().severe("It took " + ((System.currentTimeMillis() - time) / 1000) + " ms to establish a database connection!\n" +
                        "Using this remote connection is not recommended!");
            }
            remoteDatabase.init();
        } else if (config.getString(ConfigPath.GENERAL_CONFIGURATION_DATABASE_TYPE).equalsIgnoreCase("sqlite")){
            remoteDatabase = new SQLite();
            remoteDatabase.init();
        } else if (config.getString(ConfigPath.GENERAL_CONFIGURATION_DATABASE_TYPE).equalsIgnoreCase("h2")){
            remoteDatabase = new H2();
            remoteDatabase.init();
        }

        /* Citizens support */
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (this.getServer().getPluginManager().getPlugin("Citizens") != null) {
                JoinNPC.setCitizensSupport(true);
                getLogger().info("Hook into Citizens support. /bw npc");
                registerEvents(new CitizensListener());
            }

            //spawn NPCs
            try {
                JoinNPC.spawnNPCs();
            } catch (Exception e) {
                this.getLogger().severe("Could not spawn CmdJoin NPCs. Make sure you have right version of Citizens for your server!");
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
                        plugin.getLogger().info("Vault found, but no chat provider!");
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
                        plugin.getLogger().info("Vault found, but no economy provider!");
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
            p.kickPlayer("BedWars2023 was RELOADED! (do not reload plugins)");
        }

        /* Load sounds configuration */
        Sounds.init();

        /* Initialize shop */
        shop = new ShopManager();
        shop.loadShop();

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
        metrics.addCustomChart(new SimplePie("party_adapter", () -> party.getClass().getSimpleName()));
        metrics.addCustomChart(new SimplePie("chat_adapter", () -> chat.getClass().getSimpleName()));
        metrics.addCustomChart(new SimplePie("level_adapter", () -> getLevelSupport().getClass().getSimpleName()));
        metrics.addCustomChart(new SimplePie("db_adapter", () -> getRemoteDatabase().getClass().getSimpleName()));
        metrics.addCustomChart(new SimplePie("map_adapter", () -> String.valueOf(getAPI().getRestoreAdapter().getOwner().getName())));

        if (Bukkit.getPluginManager().getPlugin("VipFeatures") != null) {
            try {
                IVipFeatures vf = Bukkit.getServicesManager().getRegistration(IVipFeatures.class).getProvider();
                vf.registerMiniGame(new VipFeatures(this));
                registerEvents(new VipListeners(vf));
                getLogger().log(java.util.logging.Level.INFO, "Hook into VipFeatures support.");
            } catch (Exception e) {
                getLogger().warning("Could not load support for VipFeatures.");
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
                getLogger().info("Hooking into TAB support!");
                if (!checkTABVersion(Bukkit.getPluginManager().getPlugin("TAB").getDescription().getVersion())){
                    this.getLogger().severe("Invalid TAB version, you are using v" + Bukkit.getPluginManager().getPlugin("TAB").getDescription().getVersion() + " but v4.0.2 or higher is required!" );
                    Bukkit.getPluginManager().disablePlugin(this);
                    return;
                }
                if (BoardManager.init()) {
                    getLogger().info("TAB support has been loaded");

                    /* Load join signs. */
                    loadArenasAndSigns();

                } else {
                    this.getLogger().severe("Tab scoreboard is not enabled! please enable this in the tab configuration file!");
                }
            } else {
                this.getLogger().severe("TAB by NEZNAMY could not be hooked!");
                Bukkit.getPluginManager().disablePlugin(this);
            }
        });

//        registerEvents(new ScoreboardListener()); #Disabled for now

        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_ENABLE_HALLOWEEN)) {
            // Halloween Special
            HalloweenSpecial.init();
        }

        // Register features
        SpoilPlayerTNTFeature.init();
        GenSplitFeature.init();
        AntiDropFeature.init();

        // Initialize the addons
        Bukkit.getScheduler().runTaskLater(this, () -> addonManager.loadAddons(), 60L);

        // Send startup message, delayed to make sure everything is loaded and registered.
        Bukkit.getScheduler().runTaskLater(this, () -> {
            this.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            this.getLogger().info("BedWars2023 v"+ plugin.getDescription().getVersion()+" has been enabled!");
            this.getLogger().info("");
            this.getLogger().info("ServerType: " + getServerType().toString());
            this.getLogger().info("Auto Scale enabled: " + autoscale);
            this.getLogger().info("Restore adapter: " + api.getRestoreAdapter().getDisplayName());
            this.getLogger().info("");
            this.getLogger().info("Arena's enabled: " + api.getArenaUtil().getArenas().size());

            StringJoiner stringJoiner = new StringJoiner(", ");
            if (api.getArenaUtil().getArenas().isEmpty()) stringJoiner.add("none");
            for (IArena arena : api.getArenaUtil().getArenas()){
                stringJoiner.add(arena.getArenaName());
            }

            this.getLogger().info("being: " + stringJoiner);
            this.getLogger().info("");
            this.getLogger().info("Datasource: " + remoteDatabase.getClass().getSimpleName());
            this.getLogger().info("Addons loaded: " + addonManager.getAddons().size());
            this.getLogger().info("");
            this.getLogger().info("PAPI support: " + papiSupportLoaded);
            this.getLogger().info("Vault Chat hook enabled: " + vaultChatLoaded);
            this.getLogger().info("Vault Economy hook enabled: " + vaultEconomyLoaded);
            this.getLogger().info("");
            this.getLogger().info("TAB version: " + Bukkit.getPluginManager().getPlugin("TAB").getDescription().getVersion());
            this.getLogger().info("TAB Features enabled; Scoreboard: " + (TabAPI.getInstance().getScoreboardManager() == null ? "false" : "true") + ", UnlimitedNameTag: " + ((TabAPI.getInstance().getNameTagManager() instanceof UnlimitedNameTagManager)  ? "true" : "false") + ", BossBar: " + ((TabAPI.getInstance().getBossBarManager() == null)  ? "false" : "true") + ", TablistNameFormatting: " + ((TabAPI.getInstance().getTabListFormatManager() == null)  ? "false" : "true"));
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
            Bukkit.getLogger().info("Registering /party command..");
            nms.registerCommand("party", new PartyCommand("party"));
        }
    }

    public void onDisable() {
        shuttingDown = true;
        addonManager.unloadAddons();
        if (!serverSoftwareSupport) return;
        if (getServerType() == ServerType.BUNGEE) {
            ArenaSocket.disable();
        }
        for (IArena a : new LinkedList<>(Arena.getArenas())) {
            try {
                a.disable();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

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
                    this.getLogger().log(java.util.logging.Level.WARNING, "Could not find any arena!");
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

    public static void registerEvents(Listener... listeners) {
        Arrays.stream(listeners).forEach(l -> plugin.getServer().getPluginManager().registerEvents(l, plugin));
    }

    public static void setDebug(boolean value) {
        debug = value;
    }

    public static void setServerType(ServerType serverType) {
        BedWars.serverType = serverType;
        if (serverType == ServerType.BUNGEE) autoscale = true;
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

    public static Party getParty() {
        return party;
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

    public static ConfigManager getGeneratorsCfg() {
        return generators;
    }

    public static void setLobbyWorld(String lobbyWorld) {
        BedWars.lobbyWorld = lobbyWorld;
    }

    /**
     * Get the server version
     * Ex: v1_8_R3
     *
     * @since v0.6.5beta
     */
    public static String getServerVersion() {
        return version;
    }

    public static String getLobbyWorld() {
        return lobbyWorld;
    }

    /**
     * Get remote database.
     */
    public static IDatabase getRemoteDatabase() {
        return remoteDatabase;
    }

    public static StatsManager getStatsManager() {
        return statsManager;
    }

    public static UpgradesManager getUpgradeManager(){
        return upgradesManager;
    }

    public static com.tomkeuper.bedwars.api.BedWars getAPI() {
        return api;
    }

    /**
     * Try loading custom adapter support.
     * @return true when custom adapter was registered.
     */
    private boolean handleWorldAdapter() { //todo fix version check because current check is limited
        Plugin swmPlugin = Bukkit.getPluginManager().getPlugin("SlimeWorldManager");

        if (null == swmPlugin){
            return false;
        }
        PluginDescriptionFile pluginDescription = swmPlugin.getDescription();
        if (null == pluginDescription) {
            return false;
        }

        String[] versionString = pluginDescription.getVersion().split("\\.");


        try {
            int major = Integer.parseInt(versionString[0]);
            int minor = Integer.parseInt(versionString[1]);
            int release = versionString.length >= 3 ? Integer.parseInt(versionString[2]) : 0;

            String adapterPath;
            if (((major == 2 && minor == 2 && release == 1) || swmPlugin.getDescription().getVersion().equals("2.3.0-SNAPSHOT")) && (nms.getVersion() == 0 || nms.getVersion() == 5)) {
                adapterPath = "com.tomkeuper.bedwars.arena.mapreset.slime.SlimeAdapter";
            } else if ((major == 2 && (minor >= 8 && minor <= 10) && (release >= 0 && release <= 9)) && (nms.getVersion() == 8)) {
                adapterPath = "com.tomkeuper.bedwars.arena.mapreset.slime.AdvancedSlimeAdapter";
            } else if ((major > 2 || major == 2 && minor >= 10) && nms.getVersion() == 9) {
                adapterPath = "com.tomkeuper.bedwars.arena.mapreset.slime.SlimePaperAdapter";
            } else {
                this.getLogger().warning("Could not find adapter path for SWM version, is it unsupported?");
                return false;
            }

            Constructor<?> constructor = Class.forName(adapterPath).getConstructor(Plugin.class);
            getLogger().info("Loading restore adapter: "+adapterPath+" ...");

            RestoreAdapter candidate = (RestoreAdapter) constructor.newInstance(this);
            api.setRestoreAdapter(candidate);
            getLogger().info("Hook into "+candidate.getDisplayName()+" as restore adapter.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            this.getLogger().warning("Something went wrong! Using internal reset adapter...");
        }
        return false;
    }

    public static boolean isShuttingDown() {
        return shuttingDown;
    }

    public static void setParty(Party party) {
        BedWars.party = party;
    }

    public static void setEconomy(IEconomy economy) {
        BedWars.economy = economy;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new VoidChunkGenerator();
    }

    public static void setRemoteDatabase(IDatabase database){
        remoteDatabase = database;
    }


    private boolean checkTABVersion(String version) {
        String targetVersion = "4.0.2";

        String[] currentParts = version.split("\\.");
        String[] targetParts = targetVersion.split("\\.");

        // Compare major version
        int currentMajor = Integer.parseInt(currentParts[0]);
        int targetMajor = Integer.parseInt(targetParts[0]);
        if (currentMajor < targetMajor) {
            return true;
        } else if (currentMajor > targetMajor) {
            return false;
        }

        // Compare minor version
        int currentMinor = Integer.parseInt(currentParts[1]);
        int targetMinor = Integer.parseInt(targetParts[1]);
        if (currentMinor < targetMinor) {
            return true;
        } else if (currentMinor > targetMinor) {
            return false;
        }

        // Compare patch version
        int currentPatch = Integer.parseInt(currentParts[2]);
        int targetPatch = Integer.parseInt(targetParts[2]);

        // Check for equality
        if (currentPatch == targetPatch) {
            return true; // Versions are equal
        }

        return currentPatch < targetPatch;
    }
}
