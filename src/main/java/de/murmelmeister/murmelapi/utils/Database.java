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
        update(String.format(sql, objects));
    }

    /**
     * Updates the database with the provided SQL statement and objects.
     *
     * @param sql the SQL statement to be executed
     */
    public static void update(String sql) {
        WRITE_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("An error occurred while updating the database.", e);
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    /**
     * Creates a new table in the database if it does not already exist.
     *
     * @param value The schema definition of the columns in the table.
     * @param tableName The name of the table to be created.
     */
    public static void createTable(String value, String tableName) {
        update("CREATE TABLE IF NOT EXISTS [TABLE] ([VALUES])".replace("[TABLE]", tableName).replace("[VALUES]", value));
    }

    /**
     * Updates a database call based on the provided name and objects.
     *
     * @param name    The name of the database call to be updated.
     * @param objects The variable number of objects to be included in the database call.
     */
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

    /**
     * Retrieves values from the database and populates the given list.
     *
     * @param <T>     the type of the elements in the list
     * @param values  the list that will be populated with the retrieved values
     * @param type    the class type of the elements to retrieve from the database
     * @param value   the column name from which to retrieve the values
     * @param name    the name of the query to use in the database call
     * @param objects additional objects that may be needed for the database call
     * @throws SQLException if a database access error occurs
     */
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

    /**
     * Retrieves values from a database call and populates the provided list.
     *
     * @param list    the list to populate with retrieved values
     * @param type    the class type of the elements in the list
     * @param value   a string value used in the database query
     * @param name    a string name used in the database query
     * @param objects additional parameters used in the database query
     * @return a synchronized list containing the retrieved values, or the original list if no values were retrieved
     * @throws RuntimeException if there is an error during database retrieval
     */
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

    /**
     * Retrieves a list of values based on the specified type and parameters.
     *
     * @param type    the class type of the elements in the list
     * @param value   a string value used in the retrieval process
     * @param name    a name associated with the retrieval
     * @param objects an array of additional parameters
     * @return a list of values of the specified type
     */
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

    /**
     * Retrieves a value from the database based on a specified SQL query.
     *
     * @param <T>          The type of value expected to be retrieved from the database.
     * @param defaultValue The default value to return if the query does not yield any results.
     * @param type         The Class object corresponding to the type of value to be retrieved.
     * @param value        The name of the column from which to retrieve the value.
     * @param name         The name of the database call or procedure.
     * @param objects      Additional objects needed for the database call.
     * @return The value retrieved from the database, or defaultValue if no result is found.
     */
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

    /**
     * Checks for the existence of a call in the database based on the provided name and parameters.
     *
     * @param name    the name of the call to check for existence
     * @param objects the parameters associated with the call
     * @return {@code true} if the call exists in the database, {@code false} otherwise
     */
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

    /**
     * Retrieves a string based on the provided parameters, falling back to a default value if necessary.
     *
     * @param defaultValue specifies the default value to return if no other conditions are met.
     * @param value        the current value to be evaluated.
     * @param name         the name associated with the value, potentially for logging or debugging purposes.
     * @param objects      additional objects that may be relevant in determining the final value.
     * @return the determined string value based on the provided parameters, or the default value if no other suitable value is found.
     */
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

    /**
     * Retrieves an integer value based on the provided parameters using the
     * getValueCall method.
     *
     * @param defaultValue the default integer value to return if the retrieval fails
     * @param value        the string representation of the value to be retrieved
     * @param name         the name associated with the value
     * @param objects      additional objects that may be needed for retrieval
     * @return the retrieved integer value or the default value if retrieval fails
     */
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

    /**
     * Retrieves a long value based on the provided parameters.
     *
     * @param defaultValue the default value to return if the conversion is not possible
     * @param value        the string representation of the value to convert
     * @param name         the name associated with the value
     * @param objects      additional parameters used in the conversion process
     * @return the converted long value or the default value if conversion fails
     */
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

    /**
     * Converts the specified string to a float and returns the value.
     * If the string is null or cannot be converted, the defaultValue is returned.
     *
     * @param defaultValue the default float value to return if conversion fails
     * @param value        the string representation of the float value
     * @param name         a descriptor for the value, used in logging or error messages
     * @param objects      additional parameters for value conversion or processing
     * @return the float value obtained from the string, or defaultValue if conversion fails
     */
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

    /**
     * Retrieves a double value by invoking a method call with the provided parameters.
     *
     * @param defaultValue the default double value to return if the method call does not succeed
     * @param value        a string representation of the value to be retrieved
     * @param name         the name of the method to be called
     * @param objects      the parameters to be passed to the method call
     * @return the double value obtained from the method call, or the defaultValue if the method call fails
     */
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

    /**
     * Retrieves a unique identifier (UUID) based on the provided parameters.
     *
     * @param defaultValue The default UUID value to be used if no other value is found.
     * @param value        A string representation that can be used to derive a UUID.
     * @param name         A name used in the identification process to derive the UUID.
     * @param objects      Additional parameters that might influence the UUID generation.
     * @return A UUID object based on the provided parameters or the default value if none is derived.
     */
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

    /**
     * Creates a List<String> by invoking the getValuesCall method with specified parameters.
     *
     * @param value   the value to be processed.
     * @param name    the name to be associated with the call.
     * @param objects additional objects that may influence the call.
     * @return a List of Strings as a result of the getValuesCall method.
     */
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

    /**
     * Retrieves a list of integers based on the provided parameters.
     *
     * @param value   The column name or expression specifying the value to retrieve.
     * @param name    the name associated with the values
     * @param objects additional objects or parameters to be considered in the lookup
     * @return a list of integers corresponding to the provided parameters
     */
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

    /**
     * Generates a list of unique identifiers based on the provided parameters.
     *
     * @param value   A string representing the value to be used.
     * @param name    A string representing the name to be used.
     * @param objects Additional parameters that might be required for generating the UUID list.
     * @return A list of UUIDs based on the provided parameters.
     */
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

    /**
     * Generates a SQL procedure creation query as a single string, without including any objects.
     *
     * @param name the name of the procedure
     * @param input the input parameters for the procedure
     * @param query the SQL query to be executed within the procedure
     * @return the complete SQL procedure creation statement as a string
     */
    public static String getProcedureQueryWithoutObjects(String name, String input, String query) {
        return "CREATE PROCEDURE IF NOT EXISTS " +
               name +
               '(' +
               input +
               ")\nBEGIN\n    " +
               query +
               "\nEND;";
    }

    /**
     * Constructs a SQL query for a stored procedure call.
     *
     * @param name the name of the stored procedure to call.
     * @param objects the parameters to pass to the stored procedure.
     * @return a string representing the constructed SQL query with the provided parameters.
     */
    private static String getQueryWithCall(String name, Object... objects) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < objects.length; i++) {
            if (i != 0) builder.append(",");
            builder.append("'").append(objects[i]).append("'");
        }

        String finalSql = builder.toString();
        return "CALL " +
               name +
               "(" +
               finalSql +
               ")";
    }
}
