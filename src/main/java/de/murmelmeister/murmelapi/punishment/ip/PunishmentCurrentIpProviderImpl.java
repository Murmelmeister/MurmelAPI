package de.murmelmeister.murmelapi.punishment.ip;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PunishmentCurrentIpProviderImpl implements PunishmentCurrentIpProvider {
    public static final String TABLE_NAME = "punishment_current_ip";

    private final Database database;
    private final PunishmentCurrentIpCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_IPS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_IP;

    public PunishmentCurrentIpProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new PunishmentCurrentIpCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public List<InetAddress> getAllPunishedIps(int typeId) {
        return cache.getCachedPunishIPs().stream()
                .filter(punish -> punish.typeId() == typeId)
                .map(PunishmentCurrentIp::inetAddress)
                .toList();
    }

    @Override
    public PunishmentCurrentIp getPunishedIp(InetAddress inetAddress, int typeId) {
        return cache.get(inetAddress, typeId);
    }

    @Override
    public PunishmentCurrentIp create(InetAddress inetAddress, int typeId, UUID logId) {
        if (inetAddress == null || typeId < 1 || logId == null)
            return null;

        //ipAddress = ipAddress.strip();
        //if (ipAddress.isEmpty()) return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (ip_address, type_id, log_id) VALUES (?, ?, ?)";
        //String finalIpAddress = ipAddress;
        int row = database.update(sql, stmt -> {
            stmt.setString(1, inetAddress.getHostAddress());
            stmt.setInt(2, typeId);
            stmt.setString(3, logId.toString());
        });
        if (row < 1) return null;

        PunishmentCurrentIp punish = new PunishmentCurrentIp(inetAddress, typeId, logId);
        RefreshUtil.fireSingle(single, new PunishmentCurrentIpCache.IpTypeKey(inetAddress, typeId));
        return punish;
    }

    @Override
    public int delete(InetAddress inetAddress, int typeId) {
        if (inetAddress == null || typeId < 1)
            return 0;

        //ipAddress = ipAddress.strip();
        //if (ipAddress.isEmpty()) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE ip_address = ? AND type_id = ?";
        //String finalIpAddress = ipAddress;
        int row = database.update(sql, stmt -> {
            stmt.setString(1, inetAddress.getHostAddress());
            stmt.setInt(2, typeId);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new PunishmentCurrentIpCache.IpTypeKey(inetAddress, typeId));
        return row;
    }

    @Override
    public PunishmentCurrentIp update(InetAddress inetAddress, int typeId, UUID logId) {
        //String normalizedIpAddress = StringUtil.normalize(ipAddress);
        if (inetAddress == null || typeId < 1 || logId == null)
            return null;

        PunishmentCurrentIp existing = cache.get(inetAddress, typeId);
        if (existing == null) return null;

        if (Objects.equals(logId, existing.logId()))
            return existing; // No update needed if the log ID is the same

        String sql = "UPDATE " + TABLE_NAME + " SET log_id = ? WHERE ip_address = ? AND type_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, logId.toString());
            stmt.setString(2, inetAddress.getHostAddress());
            stmt.setInt(3, typeId);
        });
        if (row < 1) return null;

        PunishmentCurrentIp punish = existing.withUpdateLog(logId);
        RefreshUtil.fireSingle(single, new PunishmentCurrentIpCache.IpTypeKey(inetAddress, typeId));
        return punish;
    }
}
