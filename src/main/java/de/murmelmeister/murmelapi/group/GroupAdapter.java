package de.murmelmeister.murmelapi.group;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public final class GroupAdapter implements TypeAdapterFactory {
    @Override
    public <T> @Nullable TypeAdapter<T> create(Gson gson, @NotNull TypeToken<T> type) {
        if (!Group.class.isAssignableFrom(type.getRawType())) return null;

        final TypeAdapter<LocalDateTime> ldtAdapter = gson.getAdapter(LocalDateTime.class);

        TypeAdapter<Group> adapter = new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, Group value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }

                out.beginObject();
                out.name("id").value(value.id());
                out.name("groupName").value(value.groupName());
                out.name("priority").value(value.priority());
                out.name("isDefault").value(value.isDefault());
                out.name("createdBy").value(value.createdBy());
                out.name("createdAt");
                ldtAdapter.write(out, value.createdAt());

                out.name("changedBy");
                if (value.changedBy() != null) out.value(value.changedBy());
                else out.nullValue();

                out.name("changedAt");
                if (value.changedAt() != null) ldtAdapter.write(out, value.changedAt());
                else out.nullValue();

                out.endObject();
            }

            @Override
            public @Nullable Group read(@NotNull JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }

                Integer id = null;
                String groupName = null;
                Integer priority = null;
                Boolean isDefault = null;
                Integer createdBy = null;
                LocalDateTime createdAt = null;
                Integer changedBy = null;
                LocalDateTime changedAt = null;

                in.beginObject();
                while (in.hasNext()) {
                    String name = in.nextName();
                    switch (name) {
                        case "id" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            id = in.nextInt();
                        }
                        case "groupName" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            groupName = in.nextString();
                        }
                        case "priority" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            priority = in.nextInt();
                        }
                        case "isDefault" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            isDefault = in.nextBoolean();
                        }
                        case "createdBy" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            createdBy = in.nextInt();
                        }
                        case "createdAt" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            createdAt = ldtAdapter.read(in);
                        }
                        case "changedBy" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            changedBy = in.nextInt();
                        }
                        case "changedAt" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            changedAt = ldtAdapter.read(in);
                        }
                        default -> in.skipValue();
                    }
                }
                in.endObject();

                if (id == null) throw new JsonParseException("Missing required field 'id' for Group");
                if (groupName == null) throw new JsonParseException("Missing required field 'groupName' for Group");
                if (priority == null) throw new JsonParseException("Missing required field 'priority' for Group");
                if (isDefault == null) throw new JsonParseException("Missing required field 'isDefault' for Group");
                if (createdBy == null) throw new JsonParseException("Missing required field 'createdBy' for Group");
                if (createdAt == null) throw new JsonParseException("Missing required field 'createdAt' for Group");

                return new GroupImpl(id, groupName, priority, isDefault, createdBy, createdAt, changedBy, changedAt);
            }
        };

        @SuppressWarnings("unchecked")
        TypeAdapter<T> result = (TypeAdapter<T>) adapter.nullSafe();
        return result;
    }

    @ApiStatus.Internal
    static Group resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String groupName = resultSet.getString("group_name");
        int priority = resultSet.getInt("priority");
        boolean isDefault = resultSet.getBoolean("is_default");
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        return new GroupImpl(id, groupName, priority, isDefault, createdBy, createdAt, changedBy, changedAt);
    }
}
