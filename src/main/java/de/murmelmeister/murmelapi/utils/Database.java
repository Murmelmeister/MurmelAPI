package de.murmelmeister.murmelapi.utils;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;
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
        } catch (Exception e) {
            throw new RuntimeException("Database connecting error", e);
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    /**
     * Connects to an environment-based configuration using the provided parameters.
     *
     * @param url      The environment variable key for the URL to connect to
     * @param user     The environment variable key for the username to use for the connection
     * @param password The environment variable key for the password to use for the connection
     */
    public static void connectEnv(String url, String user, String password) {
        connect(System.getenv(url), System.getenv(user), System.getenv(password));
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
     * Establishes a connection to an environment-specific database using the provided parameters.
     *
     * @param driver   The name of the environment variable containing the database driver
     * @param hostname The name of the environment variable containing the database host name
     * @param port     The name of the environment variable containing the database port number
     * @param database The name of the environment variable containing the database name
     * @param username The name of the environment variable containing the database username
     * @param password The name of the environment variable containing the database password
     */
    public static void connectEnv(String driver, String hostname, String port, String database, String username, String password) {
        connect(System.getenv(driver), System.getenv(hostname), System.getenv(port), System.getenv(database), System.getenv(username), System.getenv(password));
    }

    /**
     * Disconnects from the database and close the connection pool.
     */
    public static void disconnect() {
        WRITE_LOCK.lock();
        try {
            if (!DATA_SOURCE.isClosed())
                DATA_SOURCE.close();
        } catch (Exception e) {
            throw new RuntimeException("Database closing error", e);
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    /**
     * Executes an update operation on the database using the provided SQL statement and parameters.
     *
     * @param sql     The SQL statement to be executed
     * @param objects The parameters to be set in the SQL statement
     */
    public static void update(String sql, Object... objects) {
        WRITE_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, objects);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database updating error", e);
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    /**
     * Executes an update operation in the database using a callable query constructed
     * from the provided name and objects.
     *
     * @param name    The name identifying the callable query to be executed
     * @param objects A variable number of objects to be passed as parameters to the query
     */
    public static void callUpdate(String name, Object... objects) {
        WRITE_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection();
             PreparedStatement statement = getCallableStatement(connection, name, objects)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database calling update error", e);
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    /**
     * Creates a new table in the database if it does not already exist.
     *
     * @param tableName The name of the table to be created
     * @param value     The column definitions and other SQL specifications for the table
     */
    public static void createTable(String tableName, String value) {
        update("CREATE TABLE IF NOT EXISTS [TABLE] ([VALUES])".replace("[TABLE]", tableName).replace("[VALUES]", value));
    }

    /**
     * Executes a SQL query and returns a result of type T.
     *
     * @param defaultValue The default value to return if the query result is empty
     * @param label        The column label of the result to retrieve
     * @param type         The type of the result to be returned
     * @param sql          The SQL query string to execute
     * @param objects      The parameters for the SQL query
     * @return the result of the query of type T, or the default value if the query result is empty
     */
    public static <T> T query(T defaultValue, String label, Class<T> type, String sql, Object... objects) {
        READ_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, objects);
            T value = defaultValue;
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) value = resultSet.getObject(label, type);
            }
            return value;
        } catch (SQLException e) {
            throw new RuntimeException("Database query error", e);
        } finally {
            READ_LOCK.unlock();
        }
    }

    /**
     * Executes the provided SQL query and returns a list of results extracted from the specified column label.
     *
     * @param <T>     The type of the list items to be returned
     * @param label   The label of the column from which to extract the results
     * @param type    The class type of the items to be returned
     * @param sql     The SQL query to be executed
     * @param objects The parameters to be set in the SQL query
     * @return a list of results extracted from the specified column label
     * @throws RuntimeException if there is a database access error
     */
    public static <T> List<T> queryList(String label, Class<T> type, String sql, Object... objects) {
        READ_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, objects);
            List<T> value = Collections.synchronizedList(new ArrayList<>());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) value.add(resultSet.getObject(label, type));
            }
            return value;
        } catch (SQLException e) {
            throw new RuntimeException("Database query error", e);
        } finally {
            READ_LOCK.unlock();
        }
    }

    /**
     * Executes a database stored procedure query and retrieves a value from the result set based on the provided label and type.
     *
     * @param defaultValue The default value to return if no result is found
     * @param label        The label of the column to retrieve the value from
     * @param type         The class type of the expected result
     * @param name         The name of the query or stored procedure to be executed
     * @param objects      Additional objects to be included in the query
     * @return the value retrieved from the result set based on the provided label and type, or the default value if no result is found
     * @throws RuntimeException if a database query error occurs
     */
    public static <T> T callQuery(T defaultValue, String label, Class<T> type, String name, Object... objects) {
        READ_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection();
             PreparedStatement statement = getCallableStatement(connection, name, objects)) {
            T value = defaultValue;
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) value = resultSet.getObject(label, type);
            }
            return value;
        } catch (SQLException e) {
            throw new RuntimeException("Database query error", e);
        } finally {
            READ_LOCK.unlock();
        }
    }

    /**
     * Executes a database stored procedure query and retrieves a list of result objects.
     *
     * @param label   The label of the column from which to retrieve the result objects
     * @param type    The class type of the objects to retrieve
     * @param name    The name of the callable query to execute
     * @param objects The parameters to be applied to the callable query
     * @return a synchronized list of result objects fetched from the specified column
     */
    public static <T> List<T> callQueryList(String label, Class<T> type, String name, Object... objects) {
        READ_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection();
             PreparedStatement statement = getCallableStatement(connection, name, objects)) {
            List<T> value = Collections.synchronizedList(new ArrayList<>());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) value.add(resultSet.getObject(label, type));
            }
            return value;
        } catch (SQLException e) {
            throw new RuntimeException("Database query error", e);
        } finally {
            READ_LOCK.unlock();
        }
    }

    /**
     * Checks if any records exist in the database for the provided SQL query and parameters.
     *
     * @param sql     The SQL query to execute
     * @param objects The parameters to set in the SQL query
     * @return {@code true} if records exist, {@code false} otherwise
     */
    public static boolean exists(String sql, Object... objects) {
        READ_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, objects);
            boolean exist = false;
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) exist = true;
            }
            return exist;
        } catch (SQLException e) {
            throw new RuntimeException("Database retrieval error", e);
        } finally {
            READ_LOCK.unlock();
        }
    }

    /**
     * Checks if a call with the specified name exists in the database, considering the provided objects.
     *
     * @param name    The name of the call to check for existence
     * @param objects A variable number of objects that are considered in the call lookup process
     * @return {@code true} if records exist, {@code false} otherwise
     */
    public static boolean callExists(String name, Object... objects) {
        READ_LOCK.lock();
        try (Connection connection = DATA_SOURCE.getConnection();
             CallableStatement statement = getCallableStatement(connection, name, objects)) {
            boolean exist = false;
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) exist = true;
            }
            return exist;
        } catch (SQLException e) {
            throw new RuntimeException("Database retrieval error", e);
        } finally {
            READ_LOCK.unlock();
        }
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
     * @param name  the name of the procedure
     * @param input the input parameters for the procedure
     * @param query the SQL query to be executed within the procedure
     * @return the complete SQL procedure creation statement as a string
     */
    public static String getProcedureQueryWithoutObjects(String name, String input, String query) {
        return "CREATE PROCEDURE IF NOT EXISTS " + name + '(' + input + ")\n" +
               "BEGIN\n    " + query + "\nEND;";
    }

    /**
     * Constructs a SQL query for a stored procedure call.
     *
     * @param name    the name of the stored procedure to call.
     * @param objects the parameters to pass to the stored procedure.
     * @return a string representing the constructed SQL query with the provided parameters.
     */
    private static String getQueryWithCall(String name, Object... objects) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < objects.length; i++) {
            if (i != 0) builder.append(",");
            builder.append("'").append(objects[i]).append("'");
        }
        return "CALL " + name + "(" + builder + ")";
    }

    /**
     * Creates a CallableStatement for a stored procedure call with the given name and parameters.
     *
     * @param connection The database connection to be used for creating the CallableStatement.
     * @param name       The name of the stored procedure to be called.
     * @param objects    The parameters to be passed to the stored procedure.
     * @return The created CallableStatement with parameters set.
     * @throws SQLException If a database access error occurs or this method is called on a closed connection.
     */
    private static CallableStatement getCallableStatement(Connection connection, String name, Object... objects) throws SQLException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < objects.length; i++) {
            if (i != 0) builder.append(",");
            builder.append("?");
        }
        CallableStatement statement = connection.prepareCall("{CALL " + name + "(" + builder + ")}");
        setParameters(statement, objects);
        return statement;
    }

    /**
     * Sets the parameters for a PreparedStatement.
     *
     * @param statement The PreparedStatement to which the parameters are to be set.
     * @param objects   The parameters to set in the PreparedStatement.
     * @throws SQLException If an SQL exception occurs while setting the parameters.
     */
    private static void setParameters(PreparedStatement statement, Object... objects) throws SQLException {
        for (int i = 0; i < objects.length; i++)
            statement.setObject(i + 1, objects[i]);
    }
}
