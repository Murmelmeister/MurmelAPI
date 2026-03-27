package de.murmelmeister.murmelapi.group.color;

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

public final class GroupColorAdapter implements TypeAdapterFactory {
    @Override
    public <T> @Nullable TypeAdapter<T> create(Gson gson, @NotNull TypeToken<T> type) {
        if (!GroupColor.class.isAssignableFrom(type.getRawType())) return null;

        final TypeAdapter<LocalDateTime> ldtAdapter = gson.getAdapter(LocalDateTime.class);

        TypeAdapter<GroupColor> adapter = new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, GroupColor value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }

                out.beginObject();
                out.name("groupId").value(value.groupId());
                out.name("typeId").value(value.typeId());
                out.name("value").value(value.value());

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
            public @Nullable GroupColor read(@NotNull JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }

                int groupId = -1;
                int typeId = -1;
                String value = null;
                int createdBy = -1;
                LocalDateTime createdAt = null;
                Integer changedBy = null;
                LocalDateTime changedAt = null;

                in.beginObject();
                while (in.hasNext()) {
                    switch (in.nextName()) {
                        case "groupId" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            groupId = in.nextInt();
                        }
                        case "typeId" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            typeId = in.nextInt();
                        }
                        case "value" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            value = in.nextString();
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
                    }
                }
                in.endObject();

                if (groupId == -1) throw new JsonParseException("Missing required field 'groupId' for GroupColor");
                if (typeId == -1) throw new JsonParseException("Missing required field 'typeId' for GroupColor");
                if (value == null) throw new JsonParseException("Missing required field 'value' for GroupColor");
                if (createdBy == -1) throw new JsonParseException("Missing required field 'createdBy' for GroupColor");
                if (createdAt == null)
                    throw new JsonParseException("Missing required field 'createdAt' for GroupColor");
                if (changedBy == null)
                    throw new JsonParseException("Missing required field 'changedBy' for GroupColor");
                if (changedAt == null)
                    throw new JsonParseException("Missing required field 'changedAt' for GroupColor");
                return new GroupColorImpl(groupId, typeId, value, createdBy, createdAt, changedBy, changedAt);
            }
        };

        @SuppressWarnings("unchecked")
        TypeAdapter<T> result = (TypeAdapter<T>) adapter.nullSafe();
        return result;
    }

    @ApiStatus.Internal
    static GroupColor resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int groupId = resultSet.getInt("group_id");
        int typeId = resultSet.getInt("type_id");
        String value = resultSet.getString("value");
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        return new GroupColorImpl(groupId, typeId, value, createdBy, createdAt, changedBy, changedAt);
    }
}
