package de.murmelmeister.murmelapi.utils;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Database class to manage the database.
 */
public final class Database {
    private final static HikariDataSource dataSource;

    static {
        dataSource = new HikariDataSource();
    }

    /**
     * Connects to the database using the provided URL, username and password.
     * Note: It is not checked whether it is really connected!
     *
     * @param url      The JDBC URL for the database.
     * @param user     The username for the database.
     * @param password The password for the database.
     */
    public static void connect(String url, String user, String password) {
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
    }

    /**
     * Connects to the database using the provided driver, host, port, database, username, and password.
     * Note: It is not checked whether it is really connected!
     *
     * @param driver   The JDBC driver for the database.
     * @param hostname The hostname of the database.
     * @param port     The port of the database.
     * @param database The name of the database.
     * @param username The username for the database.
     * @param password The password for the database.
     */
    public static void connect(String driver, String hostname, String port, String database, String username, String password) {
        connect(String.format("jdbc:%s://%s:%s/%s", driver, hostname, port, database), username, password);
    }

    /**
     * Disconnects from the database and close the connection pool.
     */
    public static void disconnect() {
        dataSource.close();
    }


    /**
     * Send an update in the database.
     *
     * @param sql SQL command
     * @throws SQLException If an SQL error occurs.
     */
    public static void update(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.executeUpdate();
            }
        }
    }

    /**
     * Update the sql which the {@link String#format(String, Object...)}.
     *
     * @param sql     SQL command
     * @param objects {@link String#format(String, Object...)}
     * @throws SQLException If an SQL error occurs.
     */
    public static void update(String sql, Object... objects) throws SQLException {
        update(String.format(sql, objects));
    }

    /**
     * Obtain a value.
     *
     * @param defaultValue Set a default value
     * @param type         Type what value have
     * @param value        Value what you get
     * @param sql          SQL command
     * @param <T>          Which type return
     * @return A type what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static <T> T getValue(T defaultValue, Class<T> type, String value, String sql) throws SQLException {
        T val = defaultValue;
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) val = resultSet.getObject(value, type);
            }
        }
        return val;
    }

    /**
     * Obtain a value list.
     *
     * @param list  Set a default list
     * @param type  Type what value have
     * @param value Value what you get
     * @param sql   SQL command
     * @param <T>   Which type return
     * @return A type list what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static <T> List<T> getValues(List<T> list, Class<T> type, String value, String sql) throws SQLException {
        AtomicReference<List<T>> values = new AtomicReference<>(list);
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) values.get().add(resultSet.getObject(value, type));
            }
        }
        return values.get();
    }

    /**
     * Exists the value in the database.
     *
     * @param sql SQL command
     * @return True if exists the value
     * @throws SQLException If an SQL error occurs.
     */
    public static boolean exists(String sql) throws SQLException {
        boolean b = false;
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) b = true;
            }
        }
        return b;
    }

    /**
     * Exists the value in the database.
     *
     * @param sql     SQL command
     * @param objects {@link String#format(String, Object...)}
     * @return True if exists the value
     * @throws SQLException If an SQL error occurs.
     */
    public static boolean exists(String sql, Object... objects) throws SQLException {
        return exists(String.format(sql, objects));
    }

    /**
     * Obtain the string value.
     *
     * @param defaultValue Set a default string
     * @param value        Value what you get
     * @param sql          SQL command
     * @return A String what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static String getString(String defaultValue, String value, String sql) throws SQLException {
        return getValue(defaultValue, String.class, value, sql);
    }

    /**
     * Obtain the string value.
     *
     * @param defaultValue Set a default string
     * @param value        Value what you get
     * @param sql          SQL command
     * @param objects      {@link String#format(String, Object...)}
     * @return A String what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static String getString(String defaultValue, String value, String sql, Object... objects) throws SQLException {
        return getString(defaultValue, value, String.format(sql, objects));
    }

    /**
     * Obtain the int value.
     *
     * @param defaultValue Set a default int
     * @param value        Value what you get
     * @param sql          SQL command
     * @return An int what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static int getInt(int defaultValue, String value, String sql) throws SQLException {
        return getValue(defaultValue, int.class, value, sql);
    }

    /**
     * Obtain the int value.
     *
     * @param defaultValue Set a default int
     * @param value        Value what you get
     * @param sql          SQL command
     * @param objects      {@link String#format(String, Object...)}
     * @return An int what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static int getInt(int defaultValue, String value, String sql, Object... objects) throws SQLException {
        return getInt(defaultValue, value, String.format(sql, objects));
    }

    /**
     * Obtain the long value.
     *
     * @param defaultValue Set a default long
     * @param value        Value what you get
     * @param sql          SQL command
     * @return A long what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static long getLong(long defaultValue, String value, String sql) throws SQLException {
        return getValue(defaultValue, long.class, value, sql);
    }

    /**
     * Obtain the long value.
     *
     * @param defaultValue Set a default long
     * @param value        Value what you get
     * @param sql          SQL command
     * @param objects      {@link String#format(String, Object...)}
     * @return A long what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static long getLong(long defaultValue, String value, String sql, Object... objects) throws SQLException {
        return getLong(defaultValue, value, String.format(sql, objects));
    }

    /**
     * Obtain the float value.
     *
     * @param defaultValue Set a default float
     * @param value        Value what you get
     * @param sql          SQL command
     * @return A float what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static float getFloat(float defaultValue, String value, String sql) throws SQLException {
        return getValue(defaultValue, float.class, value, sql);
    }

    /**
     * Obtain the float value.
     *
     * @param defaultValue Set a default float
     * @param value        Value what you get
     * @param sql          SQL command
     * @param objects      {@link String#format(String, Object...)}
     * @return A float what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static float getFloat(float defaultValue, String value, String sql, Object... objects) throws SQLException {
        return getFloat(defaultValue, value, String.format(sql, objects));
    }

    /**
     * Obtain the double value.
     *
     * @param defaultValue Set a default double
     * @param value        Value what you get
     * @param sql          SQL command
     * @return A double what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static double getDouble(double defaultValue, String value, String sql) throws SQLException {
        return getValue(defaultValue, double.class, value, sql);
    }

    /**
     * Obtain the double value.
     *
     * @param defaultValue Set a default double
     * @param value        Value what you get
     * @param sql          SQL command
     * @param objects      {@link String#format(String, Object...)}
     * @return A double what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static double getDouble(double defaultValue, String value, String sql, Object... objects) throws SQLException {
        return getDouble(defaultValue, value, String.format(sql, objects));
    }

    /**
     * Obtain the id.
     *
     * @param defaultValue Set a default id
     * @param value        Value what you get
     * @param sql          SQL command
     * @return A UniqueId what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static UUID getUniqueId(UUID defaultValue, String value, String sql) throws SQLException {
        return getValue(defaultValue, UUID.class, value, sql);
    }

    /**
     * Obtain the id.
     *
     * @param defaultValue Set a default id
     * @param value        Value what you get
     * @param sql          SQL command
     * @param objects      {@link String#format(String, Object...)}
     * @return A UniqueId what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static UUID getUniqueId(UUID defaultValue, String value, String sql, Object... objects) throws SQLException {
        return getUniqueId(defaultValue, value, String.format(sql, objects));
    }

    /**
     * Obtain the string list.
     *
     * @param list  Set a default list
     * @param value Value what you get
     * @param sql   SQL command
     * @return A String list what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static List<String> getStringList(List<String> list, String value, String sql) throws SQLException {
        return getValues(list, String.class, value, sql);
    }

    /**
     * Obtain the string list.
     *
     * @param list    Set a default list
     * @param value   Value what you get
     * @param sql     SQL command
     * @param objects {@link String#format(String, Object...)}
     * @return A String list what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static List<String> getStringList(List<String> list, String value, String sql, Object... objects) throws SQLException {
        return getStringList(list, value, String.format(sql, objects));
    }

    /**
     * Obtain the int list.
     *
     * @param list  Set a default list
     * @param value Value what you get
     * @param sql   SQL command
     * @return An int list what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static List<Integer> getIntList(List<Integer> list, String value, String sql) throws SQLException {
        return getValues(list, int.class, value, sql);
    }

    /**
     * Obtain the int list.
     *
     * @param list    Set a default list
     * @param value   Value what you get
     * @param sql     SQL command
     * @param objects {@link String#format(String, Object...)}
     * @return An int list what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static List<Integer> getIntList(List<Integer> list, String value, String sql, Object... objects) throws SQLException {
        return getIntList(list, value, String.format(sql, objects));
    }

    /**
     * Obtain the id list.
     *
     * @param list  Set a default list
     * @param value Value what you get
     * @param sql   SQL command
     * @return A UUID list what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static List<UUID> getUniqueIdList(List<UUID> list, String value, String sql) throws SQLException {
        return getValues(list, UUID.class, value, sql);
    }

    /**
     * Obtain the id list.
     *
     * @param list    Set a default list
     * @param value   Value what you get
     * @param sql     SQL command
     * @param objects {@link String#format(String, Object...)}
     * @return A UUID list what do you will to get
     * @throws SQLException If an SQL error occurs.
     */
    public static List<UUID> getUniqueIdList(List<UUID> list, String value, String sql, Object... objects) throws SQLException {
        return getUniqueIdList(list, value, String.format(sql, objects));
    }

    /**
     * Obtain the query.
     *
     * @param name    The name of the procedure.
     * @param input   The input of the procedure.
     * @param query   The query of the procedure.
     * @param objects The objects of the query.
     * @return The query.
     */
    public static String getProcedureQuery(String name, String input, String query, Object... objects) {
        return String.format("""
                CREATE PROCEDURE IF NOT EXISTS %s(%s)
                BEGIN
                    %s
                END;""", name, input, String.format(query, objects));
    }

    public static HikariDataSource getDataSource() {
        return dataSource;
    }
}
