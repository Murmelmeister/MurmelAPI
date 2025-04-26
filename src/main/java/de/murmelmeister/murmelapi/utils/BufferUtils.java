package de.murmelmeister.murmelapi.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for reading and writing UTF-8 strings to/from ByteBuffer.
 */
public final class BufferUtils {
    /**
     * Encodes the given string into a UTF-8 byte array prefixed with its length.
     * <p>
     * The resulting byte array begins with a 4-byte integer representing the length
     * of the UTF-8 encoded string, followed by the UTF-8 encoded bytes of the string.
     *
     * @param value The string to encode
     * @return A byte array containing the UTF-8 encoded string prefixed with its length
     */
    public static byte[] encodeUTF(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + bytes.length);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        return buffer.array();
    }

    /**
     * Decodes a UTF-8 encoded string from the given byte array.
     * <p>
     * The byte array is expected to start with a 4-byte integer that specifies the
     * length of the UTF-8 encoded string, followed by the string's byte representation.
     *
     * @param bytes The byte array containing the UTF-8 encoded string, prefixed with its length
     * @return The decoded string
     */
    public static String decodeUTF(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int length = buffer.getInt();
        byte[] stringBytes = new byte[length];
        buffer.get(stringBytes);
        return new String(stringBytes, StandardCharsets.UTF_8);
    }
}
