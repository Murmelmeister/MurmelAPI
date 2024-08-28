package de.murmelmeister.murmelapi.bansystem;

import java.util.List;

public final class LogProvider implements Log {
    private final String tableName;
    private final Reason reason;

    public LogProvider(String tableName, Reason reason) {
        this.tableName = tableName;
        this.reason = reason;
    }

    public void crateTable() {

    }

    @Override
    public boolean existsLog(int logId) {
        return false;
    }

    @Override
    public int addLog(int userId, int creatorId, long time, int reasonId) {
        return 0;
    }

    @Override
    public void removeLog(int logId) {

    }

    @Override
    public void deleteLog(int userId) {

    }

    @Override
    public List<Integer> getLogs(int userId) {
        return List.of();
    }

    @Override
    public int getUserId(int logId) {
        return 0;
    }

    @Override
    public int getCreatorId(int logId) {
        return 0;
    }

    @Override
    public long getCreatedTime(int logId) {
        return 0;
    }

    @Override
    public String getCreatedDate(int logId) {
        return "";
    }

    @Override
    public long getExpiredTime(int logId) {
        return 0;
    }

    @Override
    public String getExpiredDate(int logId) {
        return "";
    }

    @Override
    public String setExpiredTime(int logId, long time) {
        return "";
    }

    @Override
    public String addExpiredTime(int logId, long time) {
        return "";
    }

    @Override
    public String removeExpiredTime(int logId, long time) {
        return "";
    }

    @Override
    public int getReasonId(int logId) {
        return 0;
    }

    @Override
    public void setReasonId(int logId, int reasonId) {

    }

    @Override
    public String getReason(int logId) {
        return "";
    }

    private enum Procedure {
        ;
        private static final Procedure[] VALUES = values();
    }
}
