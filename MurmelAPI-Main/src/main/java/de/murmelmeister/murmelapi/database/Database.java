package de.murmelmeister.murmelapi.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.murmelmeister.murmelapi.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Database class to manage the database.
 * (Thread-safe)
 */
public final class Database {
    private final Logger logger = LoggerFactory.getLogger(Database.class);
    private volatile HikariDataSource dataSource;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Lock writeLock = lock.writeLock();

    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("Database-Thread-" + thread.threadId());
        thread.setDaemon(true);
        return thread;
    });

    public void shutdownExecutor() {
        executor.shutdown();
    }

    private HikariConfig getHikariConfig(String driverClassName, String url, String user, String password) {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(1800000);

        return config;
    }


    /**
     * Connects to the database using the provided URL, username and password.
     * Note: It is not checked whether it is really connected!
     *
     * @param url      The JDBC URL for the database.
     * @param user     The username for the database.
     * @param password The password for the database.
     */
    public void connect(String url, String user, String password) {
        writeLock.lock();
        try {
            if (dataSource != null && !dataSource.isClosed())
                dataSource.close();
            HikariConfig config = getHikariConfig("com.mysql.cj.jdbc.Driver", url, user, password);
            this.dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            logger.error("Error connecting to database", e);
            throw new DatabaseException("Database connecting error", e);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Establishes a connection to a database using the specified parameters.
     *
     * @param driver   The database driver to be used (e.g., "mysql", "postgresql").
     * @param hostname The hostname or IP address of the database server.
     * @param port     The port number on which the database server is listening.
     * @param database The name of the database to connect to.
     * @param username The username to use for authentication.
     * @param password The password to use for authentication.
     */
    public void connect(String driver, String hostname, String port, String database, String username, String password) {
        connect(String.format("jdbc:%s://%s:%s/%s", driver, hostname, port, database), username, password);
    }

    /**
     * Closes the database connection safely by attempting to close the
     * underlying data source if it is not already closed. Ensures that
     * the operation is thread-safe by acquiring a write lock before
     * performing the closure. If an exception occurs during the closure
     * process, a runtime exception is thrown with the error details.
     * The lock is always released after the operation, regardless of its success.
     */
    public void disconnect() {
        writeLock.lock();
        try {
            if (dataSource != null && !dataSource.isClosed())
                dataSource.close();
        } catch (Exception e) {
            logger.error("Error closing the database", e);
            throw new DatabaseException("Database closing error", e);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Executes an update on the database using the provided SQL query and parameters.
     * This method acquires a write lock to ensure thread safety while performing the operation.
     *
     * @param sql     The SQL query to be executed. It may contain placeholders for parameters.
     * @param objects The arguments to be used as parameters in the SQL query. These will replace
     *                the placeholders in the provided query.
     * @throws DatabaseException if an error occurs during database updating, wrapping the SQLException.
     */
    public void update(String sql, Object... objects) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = getPreparedStatement(connection, sql, objects)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error executing update: {}", sql, e);
            throw new DatabaseException("Database updating error", e);
        }
    }

    /**
     * Executes a database update call using a CallableStatement with the given name and parameters.
     * The method acquires a write lock before performing the operation to ensure thread-safety.
     *
     * @param name    The name of the database procedure or function to be called.
     * @param objects The parameters to be passed to the CallableStatement.
     *                These can be any number of objects to match the required procedure or function signature.
     */
    public void callUpdate(String name, Object... objects) {
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = getCallableStatement(connection, name, objects)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error executing callUpdate: {}", name, e);
            throw new DatabaseException("Database calling update error", e);
        }
    }

    public CompletableFuture<Void> asyncUpdate(String name, Object... objects) {
        return CompletableFuture.runAsync(() -> callUpdate(name, objects), executor);
    }

    /**
     * Creates a new database table if it does not already exist.
     *
     * @param tableName The name of the table to be created
     * @param value     The column definitions for the table
     */
    public void createTable(String tableName, String value) {
        update("CREATE TABLE IF NOT EXISTS " + tableName + " (" + value + ")");
    }

    /**
     * Executes a database query using a prepared statement and retrieves a result of the specified type and label.
     *
     * @param <T>          The type of the result to be retrieved from the database query.
     * @param defaultValue The default value to return in case the query does not produce a result.
     * @param label        The label of the column in the result set to extract the value from.
     * @param type         The class type of the expected result.
     * @param name         The name of the stored procedure or query to execute.
     * @param objects      The parameters to be set in the prepared statement for the query.
     * @return The value retrieved from the database query result set, or the default value if no result is found.
     * @throws DatabaseException If there is an error while executing the database query or processing the result.
     */
    public <T> T query(T defaultValue, String label, Class<T> type, String name, Object... objects) {
        T value = defaultValue;
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = getCallableStatement(connection, name, objects);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) value = resultSet.getObject(label, type);
        } catch (SQLException e) {
            logger.error("Error executing query: {}", name, e);
            throw new DatabaseException("Database query error", e);
        }
        return value;
    }

    public <T> CompletableFuture<T> asyncQuery(T defaultValue, String label, Class<T> type, String name, Object... objects) {
        return CompletableFuture.supplyAsync(() -> query(defaultValue, label, type, name, objects), executor);
    }

    public void queryProcess(ResultSetProcessor processor, String name, Object... objects) {
        try (Connection connection = dataSource.getConnection();
        CallableStatement statement = getCallableStatement(connection, name, objects);
        ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) processor.process(resultSet);
        } catch (SQLException e) {
            logger.error("Error executing query: {}", name, e);
            throw new DatabaseException("Database query error", e);
        }
    }

    public CompletableFuture<Void> queryAsync(ResultSetProcessor processor, String name, Object... objects) {
        return CompletableFuture.runAsync(() -> queryProcess(processor, name, objects), executor);
    }

    /**
     * Executes a database query using a callable statement and populates the provided list
     * with the results. The method operates under a read lock to ensure thread safety during
     * data retrieval. The query is executed using the given stored procedure name and parameters.
     *
     * @param <T>         The type of elements to be retrieved and added to the list.
     * @param defaultList A list to populate with the query results. The elements are cast
     *                    to the specified type.
     * @param label       The label or column name from the query result set to retrieve values from.
     * @param type        The class type of the elements to be added to the list.
     * @param name        The name of the stored procedure to be executed.
     * @param objects     A variable number of parameters to be passed to the stored procedure.
     * @return The list provided as the input, populated with elements extracted from the query result set.
     * @throws DatabaseException If a database access error occurs while querying or processing the results.
     */
    public <T> List<T> queryList(List<T> defaultList, String label, Class<T> type, String name, Object... objects) {
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = getCallableStatement(connection, name, objects);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) defaultList.add(resultSet.getObject(label, type));
        } catch (SQLException e) {
            logger.error("Error executing queryList: {}", name, e);
            throw new DatabaseException("Database query error", e);
        }
        return defaultList;
    }

    public <T> CompletableFuture<List<T>> asyncQueryList(List<T> defaultList, String label, Class<T> type, String name, Object... objects) {
        return CompletableFuture.supplyAsync(() -> queryList(defaultList, label, type, name, objects), executor);
    }

    /**
     * Executes a database query using a callable statement and populates the provided map
     * with the results. The method operates under a read lock to ensure thread safety
     * during data retrieval. The query is executed using the given stored procedure name
     * and parameters.
     *
     * @param defaultMap A map to populate with the query results. The keys are column labels
     *                   from the query result set, and the values are cast to the specified type.
     * @param valueType  The class type of the values to be stored in the map.
     * @param name       The name of the stored procedure to be executed.
     * @param objects    A variable number of parameters to be passed to the stored procedure.
     * @param <V>        The type of values to be stored in the map.
     * @return The map populated with key-value pairs extracted from the query result set.
     * @throws DatabaseException If a database access error occurs while querying or processing the results.
     */
    public <V> Map<String, V> queryMap(Map<String, V> defaultMap, Class<V> valueType, String name, Object... objects) {
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = getCallableStatement(connection, name, objects);
             ResultSet resultSet = statement.executeQuery()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            while (resultSet.next())
                for (int i = 1; i <= metaData.getColumnCount(); i++)
                    defaultMap.put(metaData.getColumnLabel(i), resultSet.getObject(i, valueType));
        } catch (SQLException e) {
            logger.error("Error executing queryMap: {}", name, e);
            throw new DatabaseException("Database query error", e);
        }
        return defaultMap;
    }

    public <V> CompletableFuture<Map<String, V>> asyncQueryMap(Map<String, V> defaultMap, Class<V> valueType, String name, Object... objects) {
        return CompletableFuture.supplyAsync(() -> queryMap(defaultMap, valueType, name, objects), executor);
    }

    /**
     * Checks whether a record exists in the database for a given stored procedure name
     * and the provided parameters.
     *
     * @param name    The name of the stored procedure to be executed for checking existence.
     * @param objects A variable number of objects representing the parameters to be
     *                passed to the stored procedure.
     * @return {@code true} if a record exists in the database for the specified procedure
     * and parameters, {@code false} otherwise.
     */
    public boolean exists(String name, Object... objects) {
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = getCallableStatement(connection, name, objects);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        } catch (SQLException e) {
            logger.error("Error executing exists: {}", name, e);
            throw new DatabaseException("Database retrieval error", e);
        }
    }

    public CompletableFuture<Boolean> asyncExists(String name, Object... objects) {
        return CompletableFuture.supplyAsync(() -> exists(name, objects), executor);
    }

    /**
     * Generates a unique identifier of type {@link UUID} that does not conflict with existing entries
     * associated with the specified name.
     * The method ensures uniqueness by repeatedly generating random UUIDs and checking
     * if they already exist in the database through the {@code exists} method.
     *
     * @param name The name used to identify the context in which the unique identifier is generated.
     *             Typically refers to a database table or similar scope for uniqueness checks.
     * @return A universally unique identifier (UUID) that is guaranteed to be unique
     * in the specified context.
     */
    public UUID generateUniqueIdentifier(String name) {
        UUID uuid;
        do {
            uuid = UUID.randomUUID();
        } while (exists(name, uuid.toString()));
        return uuid;
    }

    /**
     * Generates a SQL query string for creating a stored procedure in the database.
     *
     * @param name  The name of the stored procedure.
     * @param input The input parameters for the stored procedure, defined as a comma-separated list of
     *              parameter names and types (e.g., "param1 INT, param2 VARCHAR(100)").
     * @param query The SQL statement(s) to be executed within the body of the stored procedure.
     * @return A string containing the complete SQL query for creating the stored procedure,
     * including the name, input parameters, and body.
     */
    public static String getProcedureQuery(String name, String input, String query) {
        return "CREATE PROCEDURE IF NOT EXISTS " + name + '(' + input + ")\n" +
               "BEGIN\n    " + query + "\nEND;";
    }

    /**
     * Prepares a {@link CallableStatement} for executing a stored procedure call,
     * setting any provided parameters in the statement.
     *
     * @param connection The database connection to be used to prepare the callable statement.
     * @param name       The name of the stored procedure to be called.
     * @param objects    A variable number of objects representing the parameters to be
     *                   passed to the stored procedure.
     * @return A prepared {@link CallableStatement} with the specified stored procedure
     * and parameters.
     * @throws SQLException If an error occurs while preparing the statement or setting
     *                      its parameters.
     */
    private CallableStatement getCallableStatement(Connection connection, String name, Object... objects) throws SQLException {
        String placeholder = (objects.length > 0) ? String.join(",", Collections.nCopies(objects.length, "?")) : "";
        CallableStatement statement = connection.prepareCall("{CALL " + name + "(" + placeholder + ")}");
        setParameters(statement, objects);
        return statement;
    }

    /**
     * Prepares a {@link PreparedStatement} with the specified query and parameters.
     * Parameters are set in the PreparedStatement using the provided objects.
     *
     * @param connection The database connection to be used for preparing the statement.
     * @param query      The SQL query to be prepared.
     * @param objects    The parameters to be set in the query.
     * @return A prepared {@link PreparedStatement} with parameters set.
     * @throws SQLException If there is an error while preparing the statement or setting parameters.
     */
    private PreparedStatement getPreparedStatement(Connection connection, String query, Object... objects) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        setParameters(statement, objects);
        return statement;
    }

    /**
     * Sets the parameters for the given PreparedStatement using the provided objects.
     * Each object is mapped to a specific SQL type, and the appropriate setter method
     * is called on the PreparedStatement. If a parameter is null, it is set to SQL NULL.
     *
     * @param statement The PreparedStatement to which the parameters will be set.
     * @param objects   A variable number of objects representing the parameters to be
     *                  set in the PreparedStatement.
     * @throws SQLException If setting a parameter fails or if a database access error occurs.
     */
    private void setParameters(PreparedStatement statement, Object... objects) throws SQLException {
        for (int i = 0; i < objects.length; i++) {
            int parameterIndex = i + 1;
            Object object = objects[i];

            if (object == null) {
                statement.setNull(parameterIndex, Types.NULL);
                continue;
            }

            switch (object) {
                case Boolean value -> statement.setBoolean(parameterIndex, value);
                case Byte value -> statement.setByte(parameterIndex, value);
                case Short value -> statement.setShort(parameterIndex, value);
                case Integer value -> statement.setInt(parameterIndex, value);
                case Long value -> statement.setLong(parameterIndex, value);
                case Float value -> statement.setFloat(parameterIndex, value);
                case Double value -> statement.setDouble(parameterIndex, value);
                case BigDecimal value -> statement.setBigDecimal(parameterIndex, value);
                case String value -> statement.setString(parameterIndex, value);
                case byte[] value -> statement.setBytes(parameterIndex, value);
                case Date value -> statement.setDate(parameterIndex, value);
                case Time value -> statement.setTime(parameterIndex, value);
                case Timestamp value -> statement.setTimestamp(parameterIndex, value);
                case Array value -> statement.setArray(parameterIndex, value);
                case URL value -> statement.setURL(parameterIndex, value);
                case UUID value -> statement.setString(parameterIndex, value.toString());
                default -> statement.setObject(parameterIndex, object);
            }
        }
    }
}
