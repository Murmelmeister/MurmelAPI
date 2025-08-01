package de.murmelmeister.murmelapi.group.color;

import java.time.LocalDateTime;

public record GroupColor(int groupId, int typeId, String value, int createdBy, LocalDateTime createdAt,
                         Integer changedBy, LocalDateTime changedAt) {

    public GroupColor withUpdateMeta(String value, Integer changedBy, LocalDateTime changedAt) {
        return new GroupColor(groupId, typeId,
                value != null ? value : this.value,
                createdBy, createdAt,
                changedBy != null ? changedBy : this.changedBy,
                changedAt != null ? changedAt : this.changedAt);
    }
}
