package de.murmelmeister.murmelapi.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for reading and writing UTF-8 strings to/from ByteBuffer.
 */
public final class BufferUtils {
    /**
     * Reads a UTF-8 string from the given ByteBuffer.
     * The string is prefixed with its length as an integer.
     *
     * @param buffer The ByteBuffer to read from
     * @return The read UTF-8 string
     */
    public static String readUTF(ByteBuffer buffer) {
        int length = buffer.getInt();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Writes a UTF-8 string to the given ByteBuffer.
     * The string is prefixed with its length as an integer.
     *
     * @param buffer The ByteBuffer to write to
     * @param value  The UTF-8 string to write
     */
    public static void writeUTF(ByteBuffer buffer, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }
}
