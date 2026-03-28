package de.murmelmeister.murmelapi.inventory;

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

public final class InventoryTypeAdapter implements TypeAdapterFactory {
    @Override
    public <T> @Nullable TypeAdapter<T> create(Gson gson, @NotNull TypeToken<T> type) {
        if (!InventoryType.class.isAssignableFrom(type.getRawType())) return null;

        TypeAdapter<InventoryType> adapter = new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, InventoryType value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }

                out.beginObject();
                out.name("id").value(value.id());
                out.name("name").value(value.name());
                out.endObject();
            }

            @Override
            public @Nullable InventoryType read(@NotNull JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }

                Integer id = null;
                String name = null;

                in.beginObject();
                while (in.hasNext()) {
                    String name1 = in.nextName();
                    switch (name1) {
                        case "id" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            id = in.nextInt();
                        }
                        case "name" -> {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                break;
                            }
                            name = in.nextString();
                        }
                        default -> in.skipValue();
                    }
                }
                in.endObject();

                if (id == null) throw new JsonParseException("Missing required field 'id' for InventoryType");
                if (name == null) throw new JsonParseException("Missing required field 'name' for InventoryType");

                return new InventoryTypeImpl(id, name);
            }
        };

        @SuppressWarnings("unchecked")
        TypeAdapter<T> result = (TypeAdapter<T>) adapter.nullSafe();
        return result;
    }

    @ApiStatus.Internal
    static InventoryType resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String name = resultSet.getString("inventory_name");
        return new InventoryTypeImpl(id, name);
    }
}
