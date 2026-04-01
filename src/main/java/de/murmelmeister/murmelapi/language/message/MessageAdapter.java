package de.murmelmeister.murmelapi.language.message;

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

public final class MessageAdapter implements TypeAdapterFactory {
    @Override
    public <T> @Nullable TypeAdapter<T> create(Gson gson, @NotNull TypeToken<T> type) {
        if (!Message.class.isAssignableFrom(type.getRawType())) return null;

        TypeAdapter<Message> adapter = new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, Message value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }

                out.beginObject();
                out.name("id").value(value.id());
                out.name("tagId").value(value.tagId());
                out.name("languageId").value(value.languageId());
                out.name("message").value(value.message());
                out.endObject();
            }

            @Override
            public @Nullable Message read(@NotNull JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }

                Integer id = null;
                String tagId = null;
                Integer languageId = null;
                String message = null;

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
                        case "tagId" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            tagId = in.nextString();
                        }
                        case "languageId" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            languageId = in.nextInt();
                        }
                        case "message" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            message = in.nextString();
                        }
                        default -> in.skipValue();
                    }
                }
                in.endObject();

                if (id == null) throw new JsonParseException("Missing required field 'id' for Message");
                if (tagId == null) throw new JsonParseException("Missing required field 'tag' for Message");
                if (languageId == null) throw new JsonParseException("Missing required field 'languageId' for Message");
                if (message == null) throw new JsonParseException("Missing required field 'message' for Message");

                return new MessageImpl(id, tagId, languageId, message);
            }
        };

        @SuppressWarnings("unchecked")
        TypeAdapter<T> result = (TypeAdapter<T>) adapter.nullSafe();
        return result;
    }

    @ApiStatus.Internal
    static Message resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String tag = resultSet.getString("tag_id");
        int languageId = resultSet.getInt("language_id");
        String message = resultSet.getString("message");
        return new MessageImpl(id, tag, languageId, message);
    }
}
