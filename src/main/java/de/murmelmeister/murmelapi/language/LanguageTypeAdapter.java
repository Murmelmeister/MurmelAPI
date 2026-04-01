package de.murmelmeister.murmelapi.language;

import com.google.gson.Gson;
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

import static de.murmelmeister.murmelapi.MurmelAPI.ENGLISH_CODE;

public final class LanguageTypeAdapter implements TypeAdapterFactory {
    @Override
    public <T> @Nullable TypeAdapter<T> create(Gson gson, @NotNull TypeToken<T> type) {
        if (!LanguageType.class.isAssignableFrom(type.getRawType())) return null;

        final TypeAdapter<LanguageType> adapter = new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, LanguageType value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }

                out.beginObject();
                out.name("id").value(value.id());
                out.name("code").value(value.code());
                out.endObject();
            }

            @Override
            public @Nullable LanguageType read(@NotNull JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }

                Integer id = null;
                String code = null;

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
                        case "code" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            code = in.nextString();
                        }
                        default -> in.skipValue();
                    }
                }
                in.endObject();

                if (id == null) throw new IOException("Missing required field 'id' for LanguageType");
                if (code == null) throw new IOException("Missing required field 'code' for LanguageType");

                return new LanguageTypeImpl(id, code);
            }
        };

        @SuppressWarnings("unchecked")
        TypeAdapter<T> result = (TypeAdapter<T>) adapter.nullSafe();
        return result;
    }

    @ApiStatus.Internal
    static LanguageType resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String code = resultSet.getString("code");
        return new LanguageTypeImpl(id, code != null && !code.isBlank() ? code : ENGLISH_CODE);
    }
}
