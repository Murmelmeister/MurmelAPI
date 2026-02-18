package de.murmelmeister.murmelapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.clan.ClanProvider;
import de.murmelmeister.murmelapi.clan.ClanProviderImpl;
import de.murmelmeister.murmelapi.clan.group.ClanGroupProvider;
import de.murmelmeister.murmelapi.clan.group.ClanGroupProviderImpl;
import de.murmelmeister.murmelapi.clan.member.ClanMemberProvider;
import de.murmelmeister.murmelapi.clan.member.ClanMemberProviderImpl;
import de.murmelmeister.murmelapi.clan.parent.ClanParentProvider;
import de.murmelmeister.murmelapi.clan.parent.ClanParentProviderImpl;
import de.murmelmeister.murmelapi.clan.permission.ClanPermissionProvider;
import de.murmelmeister.murmelapi.clan.permission.ClanPermissionProviderImpl;
import de.murmelmeister.murmelapi.color.PrefixColorProvider;
import de.murmelmeister.murmelapi.color.PrefixColorProviderImpl;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.GroupProviderImpl;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorProviderImpl;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.parent.GroupParentProviderImpl;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProviderImpl;
import de.murmelmeister.murmelapi.inventory.InventoryTypeProvider;
import de.murmelmeister.murmelapi.inventory.InventoryTypeProviderImpl;
import de.murmelmeister.murmelapi.language.Language;
import de.murmelmeister.murmelapi.language.LanguageProvider;
import de.murmelmeister.murmelapi.language.LanguageProviderImpl;
import de.murmelmeister.murmelapi.language.message.MessageProvider;
import de.murmelmeister.murmelapi.language.message.MessageProviderImpl;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.language.message.MurmelMessage;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.permission.PermissionProvider;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProvider;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProviderImpl;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIpProvider;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIpProviderImpl;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProvider;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProviderImpl;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProvider;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProviderImpl;
import de.murmelmeister.murmelapi.settings.SettingsProvider;
import de.murmelmeister.murmelapi.settings.SettingsProviderImpl;
import de.murmelmeister.murmelapi.settings.SettingsService;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserProviderImpl;
import de.murmelmeister.murmelapi.user.UserService;
import de.murmelmeister.murmelapi.user.color.UserPrefixColorProvider;
import de.murmelmeister.murmelapi.user.color.UserPrefixColorProviderImpl;
import de.murmelmeister.murmelapi.user.inventory.UserInventoryProvider;
import de.murmelmeister.murmelapi.user.inventory.UserInventoryProviderImpl;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.login.UserLoginProviderImpl;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.parent.UserParentProviderImpl;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProvider;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProviderImpl;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTimeProvider;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTimeProviderImpl;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import de.murmelmeister.murmelapi.user.session.UserSessionProviderImpl;
import de.murmelmeister.murmelapi.user.stats.UserStatsProvider;
import de.murmelmeister.murmelapi.user.stats.UserStatsProviderImpl;
import de.murmelmeister.murmelapi.utils.adapter.LocalDateTimeAdapter;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshProviderImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

/**
 * The MurmelAPI main class.
 */
public final class MurmelAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(MurmelAPI.class);

    public static final String ENGLISH_CODE = "en-US";
    public static final String GERMAN_CODE = "de-DE";
    public static final int DEFAULT_GROUP_ID = 1;
    public static final int CONSOLE_USER_ID = -1;

    private final Database database;
    private final Gson gson;
    private final Long fetchLimit;
    private final long cacheCapacity;
    private final Duration refreshInterval;

    private final RefreshProvider refreshProvider;

    private final SettingsProvider settingsProvider;
    private final SettingsService settingsService;

    private final LanguageProvider languageProvider;
    private final MessageProvider messageProvider;
    private final MessageService messageService;

    private final UserProvider userProvider;
    private final UserStatsProvider userStatsProvider;
    private final UserPlayTimeProvider userPlayTimeProvider;
    private final UserLoginProvider userLoginProvider;
    private final UserSessionProvider userSessionProvider;
    private final UserService userService;

    private final GroupProvider groupProvider;
    private final GroupColorProvider groupColorProvider;

    private final UserPermissionProvider userPermissionProvider;
    private final UserParentProvider userParentProvider;
    private final GroupPermissionProvider groupPermissionProvider;
    private final GroupParentProvider groupParentProvider;
    private final Permission permission;

    private final PunishmentReasonProvider punishReasonProvider;
    private final PunishmentLogProvider punishLogProvider;
    private final PunishmentCurrentIpProvider punishIpProvider;
    private final PunishmentCurrentUserProvider punishUserProvider;
    private final PunishmentService punishmentService;

    private final ClanProvider clanProvider;
    private final ClanMemberProvider clanMemberProvider;
    private final ClanGroupProvider clanGroupProvider;
    private final ClanParentProvider clanParentProvider;
    private final ClanPermissionProvider clanPermissionProvider;

    private final PrefixColorProvider prefixColorProvider;
    private final UserPrefixColorProvider userPrefixColorProvider;

    private final InventoryTypeProvider inventoryTypeProvider;
    private final UserInventoryProvider userInventoryProvider;

    public MurmelAPI() {
        this(null, 10_000, Duration.ofMinutes(30));
    }

    public MurmelAPI(Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this(new Database(), fetchLimit, cacheCapacity, refreshInterval);
    }

    public MurmelAPI(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).disableHtmlEscaping().create();
        this.fetchLimit = fetchLimit;
        this.cacheCapacity = cacheCapacity;
        this.refreshInterval = refreshInterval;

        this.refreshProvider = new RefreshProviderImpl();

        this.settingsProvider = new SettingsProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.settingsService = new SettingsService(settingsProvider);
        this.languageProvider = new LanguageProviderImpl(database, gson, refreshProvider, cacheCapacity);
        this.messageProvider = new MessageProviderImpl(database, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.messageService = new MessageService(languageProvider, messageProvider);
        this.userProvider = new UserProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.userStatsProvider = new UserStatsProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.userPlayTimeProvider = new UserPlayTimeProviderImpl(database, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.userLoginProvider = new UserLoginProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.userSessionProvider = new UserSessionProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.userService = new UserService(userProvider, userStatsProvider, userPlayTimeProvider, userLoginProvider, userSessionProvider);
        this.groupProvider = new GroupProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.groupColorProvider = new GroupColorProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.userPermissionProvider = new UserPermissionProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.userParentProvider = new UserParentProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.groupPermissionProvider = new GroupPermissionProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.groupParentProvider = new GroupParentProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.permission = new PermissionProvider(database, refreshProvider, userProvider, groupParentProvider, groupPermissionProvider, userParentProvider, userPermissionProvider,
                cacheCapacity, refreshInterval);
        this.punishReasonProvider = new PunishmentReasonProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.punishLogProvider = new PunishmentLogProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.punishIpProvider = new PunishmentCurrentIpProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.punishUserProvider = new PunishmentCurrentUserProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.punishmentService = new PunishmentService(punishReasonProvider, punishLogProvider, punishIpProvider, punishUserProvider);
        this.clanProvider = new ClanProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.clanMemberProvider = new ClanMemberProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.clanGroupProvider = new ClanGroupProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.clanParentProvider = new ClanParentProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.clanPermissionProvider = new ClanPermissionProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.prefixColorProvider = new PrefixColorProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.userPrefixColorProvider = new UserPrefixColorProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.inventoryTypeProvider = new InventoryTypeProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
        this.userInventoryProvider = new UserInventoryProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }

    public void connect(HikariConfig config) {
        database.connect(config);
    }

    public void connect(String propertyFileName) {
        database.connect(propertyFileName);
    }

    public void connect(Properties properties) {
        database.connect(properties);
    }

    public void connect(String url, String user, String password) {
        database.connect(url, user, password);
    }

    public void connectToMariadb(String hostname, int port, String databaseName, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + hostname + ":" + port + "/" + databaseName + "?useUnicode=true&characterEncoding=UTF-8");
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        database.connect(config);
    }

    public void disconnect() {
        try {
            refreshProvider.close();
        } catch (Exception e) {
            LOGGER.warn("Failed to close refresh provider", e);
            throw new RuntimeException("Failed to close refresh provider", e);
        }
        database.disconnect();
    }

    public void setupTables() {
        runSqlScript("schema.sql");
        runSqlScript("data.sql");
        PermissionProvider.setup(database);
    }

    private void runSqlScript(String script) {
        try (InputStream in = MurmelAPI.class.getClassLoader().getResourceAsStream(script)) {
            if (in == null) throw new IllegalStateException("Missing schema.sql");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                database.runSqlScript(reader);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load sql script {}", script, e);
            throw new RuntimeException("Failed to run schema.sql", e);
        }
    }

    public void loadMessages() {
        MurmelMessage.loadMessages(messageProvider);
    }

    /*public static int deleteUserSoft(int userId) {
        if (userId < 1) return 0;
        int sessionRow = activeSession.closeSession(userId);
        int loginRows = loginHistory.deleteUserLogins(userId);
        int permissionRow = userProvider.getPermission().clearPermission(userId);
        int parentRow = userProvider.getParent().clearParent(userId);
        int playTimeRow = playTime.deleteUser(userId);
        return sessionRow + loginRows + permissionRow + parentRow + playTimeRow;
    }

    public static int deleteUserHard(int userId) {
        if (userId < 1) return 0;
        int softDeleteRow = deleteUserSoft(userId);
        //int punishmentRow = punishmentUser.unpunish(userId);
        //int logsRow = punishmentLog.deleteUserLogs(userId);
        int userRow = userProvider.deleteUser(userId);
        //return softDeleteRow + punishmentRow + logsRow + userRow;
        return softDeleteRow + userRow;
    }*/

    public Database getDatabase() {
        return database;
    }

    public Gson getGson() {
        return gson;
    }

    public @NotNull DateTimeFormatter getDateTimeFormatter(int languageId) {
        String pattern = messageService.getMessage(MurmelMessage.DATE_TIME_FORMAT.getTag(), languageId);
        if (pattern == null)
            throw new IllegalArgumentException("No pattern for language " + languageId + " found!");
        return DateTimeFormatter.ofPattern(pattern);
    }

    public @NotNull DecimalFormat getDecimalFormat(int languageId, @NotNull String pattern) {
        Language language = getLanguageProvider().findById(languageId);
        if (language == null)
            throw new IllegalArgumentException("No language for id " + languageId + " found!");
        Locale locale = Locale.of(language.code());
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        return new DecimalFormat(pattern, symbols);
    }

    public Long getFetchLimit() {
        return fetchLimit;
    }

    public long getCacheCapacity() {
        return cacheCapacity;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public RefreshProvider getRefreshProvider() {
        return refreshProvider;
    }

    public SettingsProvider getSettingsProvider() {
        return settingsProvider;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public LanguageProvider getLanguageProvider() {
        return languageProvider;
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public UserProvider getUserProvider() {
        return userProvider;
    }

    public UserStatsProvider getUserStatsProvider() {
        return userStatsProvider;
    }

    public UserPlayTimeProvider getUserPlayTimeProvider() {
        return userPlayTimeProvider;
    }

    public UserLoginProvider getUserLoginProvider() {
        return userLoginProvider;
    }

    public UserSessionProvider getUserSessionProvider() {
        return userSessionProvider;
    }

    public UserService getUserService() {
        return userService;
    }

    public GroupProvider getGroupProvider() {
        return groupProvider;
    }

    public GroupColorProvider getGroupColorProvider() {
        return groupColorProvider;
    }

    public UserPermissionProvider getUserPermissionProvider() {
        return userPermissionProvider;
    }

    public UserParentProvider getUserParentProvider() {
        return userParentProvider;
    }

    public GroupPermissionProvider getGroupPermissionProvider() {
        return groupPermissionProvider;
    }

    public GroupParentProvider getGroupParentProvider() {
        return groupParentProvider;
    }

    public Permission getPermission() {
        return permission;
    }

    public PunishmentReasonProvider getPunishReasonProvider() {
        return punishReasonProvider;
    }

    public PunishmentLogProvider getPunishLogProvider() {
        return punishLogProvider;
    }

    public PunishmentCurrentIpProvider getPunishIpProvider() {
        return punishIpProvider;
    }

    public PunishmentCurrentUserProvider getPunishUserProvider() {
        return punishUserProvider;
    }

    public PunishmentService getPunishmentService() {
        return punishmentService;
    }

    public ClanProvider getClanProvider() {
        return clanProvider;
    }

    public ClanMemberProvider getClanMemberProvider() {
        return clanMemberProvider;
    }

    public ClanGroupProvider getClanGroupProvider() {
        return clanGroupProvider;
    }

    public ClanParentProvider getClanParentProvider() {
        return clanParentProvider;
    }

    public ClanPermissionProvider getClanPermissionProvider() {
        return clanPermissionProvider;
    }

    public PrefixColorProvider getPrefixColorProvider() {
        return prefixColorProvider;
    }

    public UserPrefixColorProvider getUserPrefixColorProvider() {
        return userPrefixColorProvider;
    }

    public InventoryTypeProvider getInventoryTypeProvider() {
        return inventoryTypeProvider;
    }

    public UserInventoryProvider getUserInventoryProvider() {
        return userInventoryProvider;
    }
}
