package de.murmelmeister.murmelapi.utils;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Database class to manage the database.
 * (Thread-safe)
 */
public final class Database {
    private static final HikariDataSource DATA_SOURCE = new HikariDataSource();
    private static final ReadWriteLock LOCK = new ReentrantReadWriteLock(true);
    private static final Lock READ_LOCK = LOCK.readLock();
    private static final Lock WRITE_LOCK = LOCK.writeLock();

    /**
     * Connects to the database using the provided URL, username and password.
     * Note: It is not checked whether it is really connected!
     *
     * @param url      The JDBC URL for the database.
     * @param user     The username for the database.
     * @param password The password for the database.
     */
    public static void connect(String url, String user, String password) {
        WRITE_LOCK.lock();
        try {
            DATA_SOURCE.setJdbcUrl(url);
            DATA_SOURCE.setUsername(user);
            DATA_SOURCE.setPassword(password);
        } finally {
            WRITE_LOCK.unlock();
        }
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
        WRITE_LOCK.lock();
        try {
            DATA_SOURCE.close();
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    /**
     * Updates the database with the provided SQL statement and objects.
     *
     * @param sql     the SQL statement to be executed
     * @param objects the objects to be used in the SQL statement
     * @throws RuntimeException if an error occurs while updating the database
     */
    public static void update(String sql, Object... objects) {
        WRITE_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(String.format(sql, objects));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("An error occurred while updating the database.", e);
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    public static void updateCall(String name, Object... objects) {
        WRITE_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(getQueryWithCall(name, objects));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("An error occurred while updating the database.", e);
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    /**
     * Retrieves values from the database based on the provided parameters.
     *
     * @param values  The list to store the retrieved values.
     * @param type    The class type of the values to be retrieved.
     * @param value   The column name of the value to retrieve from the database.
     * @param sql     The SQL statement to execute for retrieving the values.
     * @param objects The optional objects to be used in the SQL statement.
     * @throws SQLException If there is an error executing the SQL statement.
     */
    private static <T> void retrieveValuesFromDatabase(List<T> values, Class<T> type, String value, String sql, Object... objects) throws SQLException {
        READ_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(String.format(sql, StringUtil.checkAllObjects(objects)))) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) values.add(resultSet.getObject(value, type));
            }
        } finally {
            READ_LOCK.unlock();
        }
    }

    private static <T> void retrieveValuesFromDatabaseCall(List<T> values, Class<T> type, String value, String name, Object... objects) throws SQLException {
        READ_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(getQueryWithCall(name, StringUtil.checkAllObjects(objects)))) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) values.add(resultSet.getObject(value, type));
            }
        } finally {
            READ_LOCK.unlock();
        }
    }

    /**
     * Retrieves values from a database and populates the given list with default values if the database retrieval fails.
     *
     * @param list    the list to populate with values
     * @param type    the class type of the elements in the list
     * @param value   the value to retrieve from the database
     * @param sql     the SQL query to retrieve values from the database
     * @param objects the optional query parameters
     * @param <T>     the type parameter for the elements in the list
     * @return the list of values retrieved from the database, or the default list if retrieval fails
     * @throws RuntimeException if there is an error retrieving values from the database
     */
    public static <T> List<T> getValuesWithDefaultList(List<T> list, Class<T> type, String value, String sql, Object... objects) {
        List<T> values = Collections.synchronizedList(list);
        try {
            retrieveValuesFromDatabase(values, type, value, sql, objects);
        } catch (SQLException e) {
            throw new RuntimeException("Database retrieval error", e);
        }
        return values;
    }

    public static <T> List<T> getValuesWithDefaultListCall(List<T> list, Class<T> type, String value, String name, Object... objects) {
        List<T> values = Collections.synchronizedList(list);
        try {
            retrieveValuesFromDatabaseCall(values, type, value, name, objects);
        } catch (SQLException e) {
            throw new RuntimeException("Database retrieval error", e);
        }
        return values;
    }

    /**
     * Retrieves values of a specified type from a database based on the provided parameters.
     * Default list is a ArrayList.
     *
     * @param type    The class object representing the type of values to retrieve.
     * @param value   The column name or expression specifying the values to retrieve.
     * @param sql     The SQL query statement to execute for retrieving the values.
     * @param objects The optional array of parameters to be passed to the SQL query.
     * @param <T>     The generic type of values to retrieve.
     * @return A list of values of the specified type retrieved from the database.
     */
    public static <T> List<T> getValues(Class<T> type, String value, String sql, Object... objects) {
        return getValuesWithDefaultList(new ArrayList<>(), type, value, sql, objects);
    }

    public static <T> List<T> getValuesCall(Class<T> type, String value, String name, Object... objects) {
        return getValuesWithDefaultListCall(new ArrayList<>(), type, value, name, objects);
    }

    /**
     * Retrieves a value from a database using the specified SQL query and parameters.
     *
     * @param defaultValue the default value to return if the database retrieval fails or no value is found
     * @param type         the class representing the type of the value to retrieve
     * @param value        the name of the column or alias from which to retrieve the value
     * @param sql          the SQL query with placeholders for the parameter values
     * @param objects      the parameters to replace the placeholders in the SQL query
     * @param <T>          the type of the value to retrieve
     * @return the retrieved value from the database or the default value if retrieval fails or no value is found
     * @throws RuntimeException if there is an error during database retrieval
     */
    public static <T> T getValue(T defaultValue, Class<T> type, String value, String sql, Object... objects) {
        READ_LOCK.lock();
        T val = defaultValue;
        try (Connection connection = DATA_SOURCE.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(String.format(sql, StringUtil.checkAllObjects(objects)))) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) val = resultSet.getObject(value, type);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database retrieval error", e);
        } finally {
            READ_LOCK.unlock();
        }
        return val;
    }

    public static <T> T getValueCall(T defaultValue, Class<T> type, String value, String name, Object... objects) {
        READ_LOCK.lock();
        T val = defaultValue;
        try (Connection connection = DATA_SOURCE.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(getQueryWithCall(name, StringUtil.checkAllObjects(objects)))) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) val = resultSet.getObject(value, type);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database retrieval error", e);
        } finally {
            READ_LOCK.unlock();
        }
        return val;
    }

    /**
     * Checks if a record exists in the database based on the provided SQL statement and objects.
     *
     * @param sql     the SQL statement to be executed
     * @param objects the objects to be used in the SQL statement
     * @return true if a record exists, false otherwise
     */
    public static boolean exists(String sql, Object... objects) {
        READ_LOCK.lock();
        boolean b = false;
        try (Connection connection = DATA_SOURCE.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(String.format(sql, StringUtil.checkAllObjects(objects)))) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) b = true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database retrieval error", e);
        } finally {
            READ_LOCK.unlock();
        }
        return b;
    }

    public static boolean existsCall(String name, Object... objects) {
        READ_LOCK.lock();
        boolean b = false;
        try (Connection connection = DATA_SOURCE.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(getQueryWithCall(name, StringUtil.checkAllObjects(objects)))) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) b = true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database retrieval error", e);
        } finally {
            READ_LOCK.unlock();
        }
        return b;
    }

    /**
     * Retrieve a string value from a database using the provided SQL query, with a default value in case the query returns null or no results.
     *
     * @param defaultValue a string value to be returned if the query returns null or no results
     * @param value        the value to be used for the query
     * @param sql          an SQL query string
     * @param objects      optional parameters to be used in the SQL query
     * @return a string value retrieved from the database, or the defaultValue if the query returns null or no results
     */
    public static String getString(String defaultValue, String value, String sql, Object... objects) {
        return getValue(defaultValue, String.class, value, sql, objects);
    }

    public static String getStringCall(String defaultValue, String value, String name, Object... objects) {
        return getValueCall(defaultValue, String.class, value, name, objects);
    }

    /**
     * Retrieves an integer value from the database using the given SQL statement and optional parameters.
     *
     * @param defaultValue the default value to return if the value is null or cannot be retrieved from the database
     * @param value        the name of the column or field from which to retrieve the integer value
     * @param sql          the SQL statement used to retrieve the integer value
     * @param objects      optional parameters to be used in the SQL statement
     * @return the integer value retrieved from the database, or the defaultValue if the value is null or cannot be retrieved
     */
    public static int getInt(int defaultValue, String value, String sql, Object... objects) {
        return getValue(defaultValue, int.class, value, sql, objects);
    }

    public static int getIntCall(int defaultValue, String value, String name, Object... objects) {
        return getValueCall(defaultValue, int.class, value, name, objects);
    }

    /**
     * Retrieves a long value based on the given parameters.
     *
     * @param defaultValue the default value to be returned if the value is not found or cannot be converted to a long
     * @param value        the value to be checked and converted to a long
     * @param sql          the SQL statement used for retrieving the value
     * @param objects      the optional arguments used in the SQL statement
     * @return the long value if found and can be converted, otherwise the default value
     */
    public static long getLong(long defaultValue, String value, String sql, Object... objects) {
        return getValue(defaultValue, long.class, value, sql, objects);
    }

    public static long getLongCall(long defaultValue, String value, String name, Object... objects) {
        return getValueCall(defaultValue, long.class, value, name, objects);
    }

    /**
     * Returns a float value obtained from the specified input parameters. If the value cannot be obtained,
     * the method returns the specified default value.
     *
     * @param defaultValue the default value to be returned if the float value cannot be obtained
     * @param value        the value to be converted to float
     * @param sql          the SQL statement used to obtain the value
     * @param objects      the objects to be included in the SQL statement
     * @return a float value converted from the specified input parameters, or the default value if the conversion fails
     */
    public static float getFloat(float defaultValue, String value, String sql, Object... objects) {
        return getValue(defaultValue, float.class, value, sql, objects);
    }

    public static float getFloatCall(float defaultValue, String value, String name, Object... objects) {
        return getValueCall(defaultValue, float.class, value, name, objects);
    }

    /**
     * Returns the value of the specified SQL query as a double. If the query does not
     * return a value, the method will return the provided defaultValue.
     *
     * @param defaultValue the default value to be returned if the SQL query does not return a value
     * @param value        the value to be passed to the SQL query
     * @param sql          the SQL query to execute
     * @param objects      the optional objects to be passed as parameters to the SQL query
     * @return the value of the SQL query as a double, or the defaultValue if no value is returned
     */
    public static double getDouble(double defaultValue, String value, String sql, Object... objects) {
        return getValue(defaultValue, double.class, value, sql, objects);
    }

    public static double getDoubleCall(double defaultValue, String value, String name, Object... objects) {
        return getValueCall(defaultValue, double.class, value, name, objects);
    }

    /**
     * Retrieves a unique ID from the database based on the provided value and SQL query.
     *
     * @param defaultValue The default unique ID to return if the database retrieval fails or no value is found.
     * @param value        The column name or expression specifying the value to retrieve from the database.
     * @param sql          The SQL query statement to execute for retrieving the unique ID.
     * @param objects      The optional objects to be used in the SQL query.
     * @return The retrieved unique ID from the database or the default unique ID if retrieval fails or no value is found.
     */
    public static UUID getUniqueId(UUID defaultValue, String value, String sql, Object... objects) {
        return getValue(defaultValue, UUID.class, value, sql, objects);
    }

    public static UUID getUniqueIdCall(UUID defaultValue, String value, String name, Object... objects) {
        return getValueCall(defaultValue, UUID.class, value, name, objects);
    }

    /**
     * Retrieves a list of strings from the database based on the provided value and SQL query.
     *
     * @param value   The column name or expression specifying the value to retrieve.
     * @param sql     The SQL query statement to execute for retrieving the strings.
     * @param objects The optional objects to be used in the SQL query.
     * @return A list of strings retrieved from the database.
     */
    public static List<String> getStringList(String value, String sql, Object... objects) {
        return getValues(String.class, value, sql, objects);
    }

    public static List<String> getStringListCall(String value, String name, Object... objects) {
        return getValuesCall(String.class, value, name, objects);
    }

    /**
     * Retrieves a list of integers from the database based on the provided value and SQL query.
     *
     * @param value   The column name or expression specifying the value to retrieve.
     * @param sql     The SQL query statement to execute for retrieving the integers.
     * @param objects The optional objects to be used in the SQL query.
     * @return A list of integers retrieved from the database.
     */
    public static List<Integer> getIntList(String value, String sql, Object... objects) {
        return getValues(int.class, value, sql, objects);
    }

    public static List<Integer> getIntListCall(String value, String name, Object... objects) {
        return getValuesCall(int.class, value, name, objects);
    }

    /**
     * Retrieves a list of unique IDs from the database based on the provided value and SQL query.
     *
     * @param value   The column name or expression specifying the value to retrieve.
     * @param sql     The SQL query statement to execute for retrieving the unique IDs.
     * @param objects The optional objects to be used in the SQL query.
     * @return A list of unique IDs retrieved from the database.
     */
    public static List<UUID> getUniqueIdList(String value, String sql, Object... objects) {
        return getValues(UUID.class, value, sql, objects);
    }

    public static List<UUID> getUniqueIdListCall(String value, String name, Object... objects) {
        return getValuesCall(UUID.class, value, name, objects);
    }

    /**
     * Returns the SQL query for creating a stored procedure.
     *
     * @param name    the name of the stored procedure
     * @param input   the input parameters of the stored procedure in the format "parameter1 type1, parameter2 type2, ..."
     * @param query   the body of the stored procedure
     * @param objects the objects to be formatted into the query string
     * @return the SQL query string for creating the stored procedure
     */
    public static String getProcedureQuery(String name, String input, String query, Object... objects) {
        return String.format("""
                CREATE PROCEDURE IF NOT EXISTS %s(%s)
                BEGIN
                    %s
                END;""", name, input, String.format(query, objects));
    }

    public static String getProcedureQueryWithoutObjects(String name, String input, String query) {
        return "CREATE PROCEDURE IF NOT EXISTS " +
               name +
               '(' +
               input +
               ")\nBEGIN\n    " +
               query +
               "\nEND;";
    }

    private static String getQueryWithCall(String name, Object... objects) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < objects.length; i++) {
            if (i != 0) builder.append(",");
            builder.append("'").append(objects[i]).append("'");
        }

        String finalSql = builder.toString();
        return String.format("CALL %s(%s)", name, finalSql);
    }
}
