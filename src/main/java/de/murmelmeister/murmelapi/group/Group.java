package de.murmelmeister.murmelapi.group;

import java.time.LocalDateTime;

public record Group(int id, String groupName, int priority, boolean isDefault, int createdBy,
                    LocalDateTime createdAt, Integer changedBy, LocalDateTime changedAt) {
    public Group withUpdateMeta(String groupName, Integer priority,
                                Integer changedBy, LocalDateTime changedAt) {
        return new Group(id,
                groupName != null ? groupName : this.groupName,
                priority != null ? priority : this.priority,
                isDefault,
                createdBy, createdAt,
                changedBy != null ? changedBy : this.changedBy,
                changedAt != null ? changedAt : this.changedAt);
    }
}
