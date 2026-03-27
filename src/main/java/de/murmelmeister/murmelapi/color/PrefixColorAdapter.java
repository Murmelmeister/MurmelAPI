package de.murmelmeister.murmelapi.color;

import com.google.gson.*;
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

public final class PrefixColorAdapter implements TypeAdapterFactory {
    @Override
    public <T> @Nullable TypeAdapter<T> create(Gson gson, @NotNull TypeToken<T> type) {
        if (!PrefixColor.class.isAssignableFrom(type.getRawType())) return null;

        final TypeAdapter<LocalDateTime> ldtAdapter = gson.getAdapter(LocalDateTime.class);

        TypeAdapter<PrefixColor> adapter = new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, PrefixColor value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }

                out.beginObject();
                out.name("id");
                out.value(value.id());
                out.name("color");
                out.value(value.color());
                out.name("animated");
                out.value(value.animated());

                out.name("createdAt");
                ldtAdapter.write(out, value.createdAt());
                out.name("createdBy");
                out.value(value.createdBy());

                out.name("changedAt");
                if (value.changedAt() != null) ldtAdapter.write(out, value.changedAt());
                else out.nullValue();

                out.name("changedBy");
                if (value.changedBy() != null) out.value(value.changedBy());
                else out.nullValue();

                out.endObject();
            }

            @Override
            public @Nullable PrefixColor read(@NotNull JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }

                String id = null;
                String color = null;
                Boolean animated = null;
                LocalDateTime createdAt = null;
                Integer createdBy = null;
                LocalDateTime changedAt = null;
                Integer changedBy = null;

                in.beginObject();
                while (in.hasNext()) {
                    String name = in.nextName();
                    switch (name) {
                        case "id" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            id = in.nextString();
                        }
                        case "color" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            color = in.nextString();
                        }
                        case "animated" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            animated = in.nextBoolean();
                        }
                        case "createdAt" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            createdAt = ldtAdapter.read(in);
                        }
                        case "createdBy" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            createdBy = in.nextInt();
                        }
                        case "changedAt" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            changedAt = ldtAdapter.read(in);
                        }
                        case "changedBy" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            changedBy = in.nextInt();
                        }
                        default -> in.skipValue();
                    }
                }
                in.endObject();

                if (id == null) throw new JsonParseException("Missing required field 'id' for PrefixColor");
                if (color == null) throw new JsonParseException("Missing required field 'color' for PrefixColor");
                if (animated == null) throw new JsonParseException("Missing required field 'animated' for PrefixColor");
                if (createdAt == null)
                    throw new JsonParseException("Missing required field 'createdAt' for PrefixColor");
                if (createdBy == null)
                    throw new JsonParseException("Missing required field 'createdBy' for PrefixColor");

                return new PrefixColorImpl(id, color, animated, createdAt, createdBy, changedAt, changedBy);
            }
        };

        @SuppressWarnings("unchecked")
        TypeAdapter<T> result = (TypeAdapter<T>) adapter.nullSafe();
        return result;
    }

    @ApiStatus.Internal
    static PrefixColor resultSet(@NotNull ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");
        String color = resultSet.getString("color");
        boolean animated = resultSet.getBoolean("animated");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        return new PrefixColorImpl(id, color, animated, createdAt, createdBy, changedAt, changedBy);
    }
}
