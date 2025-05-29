package de.murmelmeister.murmelapi.punishment.reason;

import java.sql.Timestamp;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

public class Reason {
    private final int id;
    private int typeId;
    private String reason;
    private long duration;
    private boolean isAutoIpFlag;
    private boolean isAutoPunish;
    private int createdBy;
    private Timestamp createdAt;
    private int updatedBy;
    private Timestamp updatedAt;

    public Reason(int id, int typeId, String reason, long duration, boolean isAutoIpFlag, boolean isAutoPunish, int createdBy, Timestamp createdAt, int updatedBy, Timestamp updatedAt) {
        this.id = id;
        this.typeId = typeId;
        this.reason = reason;
        this.duration = duration;
        this.isAutoIpFlag = isAutoIpFlag;
        this.isAutoPunish = isAutoPunish;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isAutoIpFlag() {
        return isAutoIpFlag;
    }

    public void setAutoIpFlag(boolean autoIpFlag) {
        isAutoIpFlag = autoIpFlag;
    }

    public boolean isAutoPunish() {
        return isAutoPunish;
    }

    public void setAutoPunish(boolean autoPunish) {
        isAutoPunish = autoPunish;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public String getCreatedDate() {
        return createdAt == null ? null : getDateFormat().format(createdAt);
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(int updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedDate() {
        return updatedAt == null ? null : getDateFormat().format(updatedAt);
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
