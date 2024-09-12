package de.murmelmeister.murmelapi.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MurmelMemory class provides a thread-safe storage mechanism for key-value pairs.
 */
public abstract class MurmelMemory {
    protected final Map<String, Object> data = new ConcurrentHashMap<>();

    /**
     * Stores a key-value pair in the data map.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    public void set(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the specified key, or null if no value is found
     */
    public Object get(String key) {
        return data.get(key);
    }

    /**
     * Retrieves an Optional containing the value associated with the specified key if it is an instance of the specified type.
     *
     * @param key the key whose associated value is to be returned
     * @param type the class of the type to which the value should be cast
     * @return an Optional containing the value if it exists and is of the specified type, otherwise an empty Optional
     */
    public <T> Optional<T> getOptional(String key, Class<T> type) {
        Object value = data.get(key);
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Retrieves the value associated with the specified key if it is an instance of the specified type.
     *
     * @param key the key whose associated value is to be returned
     * @param type the class of the type to which the value should be cast
     * @return the value associated with the specified key if it exists and is of the specified type, otherwise null
     */
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    /**
     * Retrieves the Boolean value associated with the specified key.
     *
     * @param key the key whose associated Boolean value is to be returned
     * @return the Boolean value associated with the specified key if it exists and is of type Boolean, otherwise null
     */
    public Boolean getBoolean(String key) {
        return get(key, Boolean.class);
    }


    /**
     * Retrieves the Byte value associated with the specified key.
     *
     * @param key the key whose associated Byte value is to be returned
     * @return the Byte value associated with the specified key if it exists and is of type Byte, otherwise null
     */
    public Byte getByte(String key) {
        return get(key, Byte.class);
    }

    /**
     * Retrieves the Short value associated with the specified key.
     *
     * @param key the key whose associated Short value is to be returned
     * @return the Short value associated with the specified key if it exists and is of type Short, otherwise null
     */
    public Short getShort(String key) {
        return get(key, Short.class);
    }

    /**
     * Retrieves the Integer value associated with the specified key if it exists and is of type Integer.
     *
     * @param key the key whose associated Integer value is to be returned
     * @return the Integer value associated with the specified key if it exists and is of type Integer, otherwise null
     */
    public Integer getInteger(String key) {
        return get(key, Integer.class);
    }

    /**
     * Retrieves the Long value associated with the specified key.
     *
     * @param key the key whose associated Long value is to be returned
     * @return the Long value associated with the specified key if it exists and is of type Long, otherwise null
     */
    public Long getLong(String key) {
        return get(key, Long.class);
    }

    /**
     * Retrieves the Float value associated with the specified key if it exists and is of type Float.
     *
     * @param key the key whose associated Float value is to be returned
     * @return the Float value associated with the specified key if it exists and is of type Float, otherwise null
     */
    public Float getFloat(String key) {
        return get(key, Float.class);
    }

    /**
     * Retrieves the Double value associated with the specified key.
     *
     * @param key the key whose associated Double value is to be returned
     * @return the Double value associated with the specified key if it exists and is of type Double, otherwise null
     */
    public Double getDouble(String key) {
        return get(key, Double.class);
    }

    /**
     * Retrieves the Character value associated with the specified key.
     *
     * @param key the key whose associated Character value is to be returned
     * @return the Character value associated with the specified key if it exists and is of type Character, otherwise null
     */
    public Character getCharacter(String key) {
        return get(key, Character.class);
    }

    /**
     * Retrieves the String value associated with the specified key.
     *
     * @param key the key whose associated String value is to be returned
     * @return the String value associated with the specified key if it exists and is of type String, otherwise null
     */
    public String getString(String key) {
        return get(key, String.class);
    }

    /**
     * Retrieves a list associated with the specified key.
     *
     * @param key the key whose associated list is to be returned
     * @return the list associated with the specified key if it exists and is of type List, otherwise null
     */
    public List<?> getList(String key) {
        return get(key, List.class);
    }

    /**
     * Retrieves a list of Boolean values associated with the specified key.
     *
     * @param key the key whose associated list of Boolean values is to be returned
     * @return a list containing Boolean values, or an empty list if no values are found or if the list contains non-Boolean values
     */
    public List<Boolean> getBooleanList(String key) {
        List<?> list = getList(key);
        if (list == null) return new ArrayList<>();
        List<Boolean> result = new ArrayList<>();
        for (Object object : list)
            if (object instanceof Boolean)
                result.add((Boolean) object);
        return result;
    }

    /**
     * Retrieves a list of Byte values associated with the specified key.
     *
     * @param key the key whose associated list of Byte values is to be returned
     * @return a list containing Byte values, or an empty list if no values are found or if the list contains non-Byte values
     */
    public List<Byte> getByteList(String key) {
        List<?> list = getList(key);
        if (list == null) return new ArrayList<>();
        List<Byte> result = new ArrayList<>();
        for (Object object : list)
            if (object instanceof Byte)
                result.add((Byte) object);
        return result;
    }

    /**
     * Retrieves a list of Short values associated with the specified key.
     *
     * @param key the key whose associated list of Short values is to be returned
     * @return a list containing Short values, or an empty list if no values are found or if the list contains non-Short values
     */
    public List<Short> getShortList(String key) {
        List<?> list = getList(key);
        if (list == null) return new ArrayList<>();
        List<Short> result = new ArrayList<>();
        for (Object object : list)
            if (object instanceof Short)
                result.add((Short) object);
        return result;
    }

    /**
     * Retrieves a list of Integer values associated with the specified key.
     *
     * @param key the key whose associated list of Integer values is to be returned
     * @return a list containing Integer values, or an empty list if no values are found or if the list contains non-Integer values
     */
    public List<Integer> getIntegerList(String key) {
        List<?> list = getList(key);
        if (list == null) return new ArrayList<>();
        List<Integer> result = new ArrayList<>();
        for (Object object : list)
            if (object instanceof Integer)
                result.add((Integer) object);
        return result;
    }

    /**
     * Retrieves a list of Long values associated with the specified key.
     *
     * @param key the key whose associated list of Long values is to be returned
     * @return a list containing Long values, or an empty list if no values are found or if the list contains non-Long values
     */
    public List<Long> getLongList(String key) {
        List<?> list = getList(key);
        if (list == null) return new ArrayList<>();
        List<Long> result = new ArrayList<>();
        for (Object object : list)
            if (object instanceof Long)
                result.add((Long) object);
        return result;
    }

    /**
     * Retrieves a list of Float values associated with the specified key.
     *
     * @param key the key whose associated list of Float values is to be returned
     * @return a list containing Float values, or an empty list if no values are found or if the list contains non-Float values
     */
    public List<Float> getFloatList(String key) {
        List<?> list = getList(key);
        if (list == null) return new ArrayList<>();
        List<Float> result = new ArrayList<>();
        for (Object object : list)
            if (object instanceof Float)
                result.add((Float) object);
        return result;
    }

    /**
     * Retrieves a list of Double values associated with the specified key.
     *
     * @param key the key whose associated list of Double values is to be returned
     * @return a list containing Double values, or an empty list if no values are found or if the list contains non-Double values
     */
    public List<Double> getDoubleList(String key) {
        List<?> list = getList(key);
        if (list == null) return new ArrayList<>();
        List<Double> result = new ArrayList<>();
        for (Object object : list)
            if (object instanceof Double)
                result.add((Double) object);
        return result;
    }

    /**
     * Retrieves a list of Character values associated with the specified key.
     *
     * @param key the key whose associated list of Character values is to be returned
     * @return a list containing Character values, or an empty list if no values are found or if the list contains non-Character values
     */
    public List<Character> getCharacterList(String key) {
        List<?> list = getList(key);
        if (list == null) return new ArrayList<>();
        List<Character> result = new ArrayList<>();
        for (Object object : list)
            if (object instanceof Character)
                result.add((Character) object);
        return result;
    }

    /**
     * Retrieves a list of String values associated with the specified key.
     *
     * @param key the key whose associated list of String values is to be returned
     * @return a list containing String values, or an empty list if no values are found or if the list contains non-String values
     */
    public List<String> getStringList(String key) {
        List<?> list = getList(key);
        if (list == null) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (Object object : list)
            if (object instanceof String)
                result.add(String.valueOf(object));
        return result;
    }
}
