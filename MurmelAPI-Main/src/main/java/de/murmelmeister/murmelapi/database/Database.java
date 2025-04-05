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

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return config;
    }


    /**
     * Connects to the database using the provided URL, username and password.
     * Note: It is not checked whether it is really connected!
     *
     * @param driverClassName The class name of the JDBC driver to be used.
     * @param url             The JDBC URL for the database.
     * @param user            The username for the database.
     * @param password        The password for the database.
     */
    public void connect(String driverClassName, String url, String user, String password) {
        writeLock.lock();
        try {
            if (dataSource != null && !dataSource.isClosed())
                dataSource.close();
            HikariConfig config = getHikariConfig(driverClassName, url, user, password);
            this.dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            logger.error("Error connecting to database", e);
            throw new DatabaseException("Database connecting error", e);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Connects to a MySQL database using the provided URL, username and password.
     * Note: It is not checked whether it is really connected!
     *
     * @param url      The JDBC URL for the MySQL database.
     * @param user     The username for the MySQL database.
     * @param password The password for the MySQL database.
     */
    public void connectMySQL(String url, String user, String password) {
        connect("com.mysql.cj.jdbc.Driver", url, user, password);
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
        connectMySQL(String.format("jdbc:%s://%s:%s/%s", driver, hostname, port, database), username, password);
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
     * Executes a database update using a prepared statement with the given SQL query and parameters.
     * The method acquires a write lock before performing the operation to ensure thread-safety.
     *
     * @param sql     The SQL query to be executed. It can contain placeholders for parameters (e.g., "?").
     * @param objects The parameters to be set in the prepared statement. These can be any number of objects
     *                to match the placeholders in the SQL query.
     */
    public void update(String sql, Object... objects) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            try (PreparedStatement statement = getPreparedStatement(connection, sql, objects)) {
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            if (connection != null)
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    logger.error("Error rolling back transaction", rollbackException);
                }
            logger.error("Error executing update: {}", sql, e);
            throw new DatabaseException("Database updating error", e);
        } finally {
            if (connection != null)
                try {
                    connection.close();
                } catch (SQLException closeException) {
                    logger.error("Error closing connection", closeException);
                }
        }
    }

    /**
     * Executes a database update asynchronously using a CompletableFuture.
     * The method runs the update operation in a separate thread to avoid blocking the main thread.
     *
     * @param sql     The SQL query to be executed. It can contain placeholders for parameters (e.g., "?").
     * @param objects The parameters to be set in the prepared statement. These can be any number of objects
     *                to match the placeholders in the SQL query.
     */
    public void updateAsync(String sql, Object... objects) {
        CompletableFuture.runAsync(() -> update(sql, objects), executor);
    }

    /**
     * Executes a database update using a CallableStatement with the given name and parameters.
     * The method acquires a write lock before performing the operation to ensure thread-safety.
     *
     * @param name    The name of the database procedure or function to be called.
     * @param objects The parameters to be passed to the CallableStatement.
     *                These can be any number of objects to match the required procedure or function signature.
     * @return The number of rows affected by the update operation.
     */
    public int updateCallable(String name, Object... objects) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            try (CallableStatement statement = getCallableStatement(connection, name, objects)) {
                int updateCount = statement.executeUpdate();
                connection.commit();
                return updateCount;
            }
        } catch (SQLException e) {
            if (connection != null)
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    logger.error("Error rolling back transaction", rollbackException);
                }
            logger.error("Error executing update: {}", name, e);
            throw new DatabaseException("Database updating error", e);
        } finally {
            if (connection != null)
                try {
                    connection.close();
                } catch (SQLException closeException) {
                    logger.error("Error closing connection", closeException);
                }
        }
    }

    /**
     * Executes a database update asynchronously using a CompletableFuture.
     * The method runs the update operation in a separate thread to avoid blocking the main thread.
     *
     * @param name    The name of the database procedure or function to be called.
     * @param objects The parameters to be passed to the CallableStatement.
     *                These can be any number of objects to match the required procedure or function signature.
     * @return A CompletableFuture that will complete with the number of rows affected by the update operation.
     */
    public CompletableFuture<Integer> updateCallableAsync(String name, Object... objects) {
        return CompletableFuture.supplyAsync(() -> updateCallable(name, objects), executor);
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
     * Creates a new database table asynchronously if it does not already exist.
     *
     * @param tableName The name of the table to be created
     * @param value     The column definitions for the table
     */
    public void createTableAsync(String tableName, String value) {
        updateAsync("CREATE TABLE IF NOT EXISTS " + tableName + " (" + value + ")");
    }

    /**
     * Executes a database query using a prepared statement and retrieves a result of the specified type and label.
     *
     * @param <T>          The type of the result to be retrieved from the database query.
     * @param sql          The SQL query to be executed. It can contain placeholders for parameters (e.g., "?").
     * @param defaultValue The default value to return in case the query does not produce a result.
     * @param processor    A ResultSetProcessor that processes the result set and extracts the desired value.
     * @param objects      The parameters to be set in the prepared statement for the query.
     * @return The value retrieved from the database query result set, or the default value if no result is found.
     * @throws DatabaseException If there is an error while executing the database query or processing the result.
     */
    public <T> T query(String sql, T defaultValue, ResultSetProcessor<T> processor, Object... objects) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = getPreparedStatement(connection, sql, objects);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) return processor.process(resultSet);
            else return defaultValue;
        } catch (SQLException e) {
            logger.error("Error executing query: {}", sql, e);
            throw new DatabaseException("Database query error", e);
        }
    }

    /**
     * Asynchronously executes a database query using a prepared statement and retrieves a result of the specified type and label.
     *
     * @param <T>          The type of the result to be retrieved from the database query.
     * @param sql          The SQL query to be executed. It can contain placeholders for parameters (e.g., "?").
     * @param defaultValue The default value to return in case the query does not produce a result.
     * @param processor    A ResultSetProcessor that processes the result set and extracts the desired value.
     * @param objects      The parameters to be set in the prepared statement for the query.
     * @return A CompletableFuture that will complete with the retrieved value when the operation is finished.
     */
    public <T> CompletableFuture<T> queryAsync(String sql, T defaultValue, ResultSetProcessor<T> processor, Object... objects) {
        return CompletableFuture.supplyAsync(() -> query(sql, defaultValue, processor, objects), executor);
    }

    /**
     * Executes a database query using a callable statement and retrieves a result of the specified type and label.
     *
     * @param <T>          The type of the result to be retrieved from the database query.
     * @param name         The name of the stored procedure or query to execute.
     * @param defaultValue The default value to return in case the query does not produce a result.
     * @param processor    A ResultSetProcessor that processes the result set and extracts the desired value.
     * @param objects      The parameters to be set in the prepared statement for the query.
     * @return The value retrieved from the database query result set, or the default value if no result is found.
     * @throws DatabaseException If there is an error while executing the database query or processing the result.
     */
    public <T> T queryCallable(String name, T defaultValue, ResultSetProcessor<T> processor, Object... objects) {
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = getCallableStatement(connection, name, objects);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) return processor.process(resultSet);
            else return defaultValue;
        } catch (SQLException e) {
            logger.error("Error executing query: {}", name, e);
            throw new DatabaseException("Database query error", e);
        }
    }

    /**
     * Asynchronously executes a database query using a callable statement and retrieves a result of the specified type and label.
     *
     * @param <T>          The type of the result to be retrieved from the database query.
     * @param name         The name of the stored procedure or query to execute.
     * @param defaultValue The default value to return in case the query does not produce a result.
     * @param processor    A ResultSetProcessor that processes the result set and extracts the desired value.
     * @param objects      The parameters to be set in the prepared statement for the query.
     * @return A CompletableFuture that will complete with the retrieved value when the operation is finished.
     */
    public <T> CompletableFuture<T> queryCallableAsync(String name, T defaultValue, ResultSetProcessor<T> processor, Object... objects) {
        return CompletableFuture.supplyAsync(() -> queryCallable(name, defaultValue, processor, objects), executor);
    }

    /**
     * Executes a database query using a prepared statement and populates the provided list
     * with the results. The method operates under a read lock to ensure thread safety during
     * data retrieval. The query is executed using the given SQL statement and parameters.
     *
     * @param <T>         The type of elements to be retrieved and added to the list.
     * @param sql         The SQL query to be executed. It can contain placeholders for parameters (e.g., "?").
     * @param defaultList A list to populate with the query results. The elements are cast
     *                    to the specified type.
     * @param processor   A ResultSetProcessor that processes each row of the result set.
     * @param objects     A variable number of parameters to be passed to the SQL query.
     * @return The list provided as the input, populated with elements extracted from the query result set.
     * @throws DatabaseException If a database access error occurs while querying or processing the results.
     */
    public <T> List<T> queryList(String sql, List<T> defaultList, ResultSetProcessor<T> processor, Object... objects) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = getPreparedStatement(connection, sql, objects);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) defaultList.add(processor.process(resultSet));
        } catch (SQLException e) {
            logger.error("Error executing queryList: {}", sql, e);
            throw new DatabaseException("Database query error", e);
        }
        return defaultList;
    }

    /**
     * Asynchronously executes a database query using a prepared statement and populates the provided list
     * with the results. The method runs the operation in a separate thread to avoid blocking the main thread.
     *
     * @param <T>         The type of elements to be retrieved and added to the list.
     * @param sql         The SQL query to be executed. It can contain placeholders for parameters (e.g., "?").
     * @param defaultList A list to populate with the query results. The elements are cast
     *                    to the specified type.
     * @param processor   A ResultSetProcessor that processes each row of the result set.
     * @param objects     A variable number of parameters to be passed to the SQL query.
     * @return A CompletableFuture that will complete with the populated list when the operation is finished.
     */
    public <T> CompletableFuture<List<T>> queryListAsync(String sql, List<T> defaultList, ResultSetProcessor<T> processor, Object... objects) {
        return CompletableFuture.supplyAsync(() -> queryList(sql, defaultList, processor, objects), executor);
    }

    /**
     * Executes a database query using a callable statement and populates the provided list
     * with the results. The method operates under a read lock to ensure thread safety during
     * data retrieval. The query is executed using the given stored procedure name and parameters.
     *
     * @param <T>         The type of elements to be retrieved and added to the list.
     * @param name        The name of the stored procedure to be executed.
     * @param defaultList A list to populate with the query results. The elements are cast
     *                    to the specified type.
     * @param processor   A ResultSetProcessor that processes each row of the result set.
     * @param objects     A variable number of parameters to be passed to the stored procedure.
     * @return The list provided as the input, populated with elements extracted from the query result set.
     * @throws DatabaseException If a database access error occurs while querying or processing the results.
     */
    public <T> List<T> queryListCallable(String name, List<T> defaultList, ResultSetProcessor<T> processor, Object... objects) {
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = getCallableStatement(connection, name, objects);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) defaultList.add(processor.process(resultSet));
        } catch (SQLException e) {
            logger.error("Error executing queryList: {}", name, e);
            throw new DatabaseException("Database query error", e);
        }
        return defaultList;
    }

    /**
     * Asynchronously executes a database query using a callable statement and populates the provided list
     * with the results. The method runs the operation in a separate thread to avoid blocking the main thread.
     *
     * @param <T>         The type of elements to be retrieved and added to the list.
     * @param name        The name of the stored procedure to be executed.
     * @param defaultList A list to populate with the query results. The elements are cast
     *                    to the specified type.
     * @param processor   A ResultSetProcessor that processes each row of the result set.
     * @param objects     A variable number of parameters to be passed to the stored procedure.
     * @return A CompletableFuture that will complete with the populated list when the operation is finished.
     */
    public <T> CompletableFuture<List<T>> queryListCallableAsync(String name, List<T> defaultList, ResultSetProcessor<T> processor, Object... objects) {
        return CompletableFuture.supplyAsync(() -> queryListCallable(name, defaultList, processor, objects), executor);
    }

    /**
     * Checks whether a record exists in the database for a given SQL query
     * and the provided parameters.
     *
     * @param sql     The SQL query to be executed for checking existence.
     * @param objects A variable number of objects representing the parameters to be
     *                passed to the SQL query.
     * @return {@code true} if a record exists in the database for the specified query
     * and parameters, {@code false} otherwise.
     */
    public boolean exists(String sql, Object... objects) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = getPreparedStatement(connection, sql, objects);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        } catch (SQLException e) {
            logger.error("Error executing exists: {}", sql, e);
            throw new DatabaseException("Database retrieval error", e);
        }
    }

    /**
     * Asynchronously checks whether a record exists in the database for a given SQL query
     * and the provided parameters.
     *
     * @param sql     The SQL query to be executed for checking existence.
     * @param objects A variable number of objects representing the parameters to be
     *                passed to the SQL query.
     * @return A CompletableFuture that will complete with {@code true} if a record exists,
     * or {@code false} otherwise.
     */
    public CompletableFuture<Boolean> existsAsync(String sql, Object... objects) {
        return CompletableFuture.supplyAsync(() -> exists(sql, objects), executor);
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
    public boolean existsCallable(String name, Object... objects) {
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = getCallableStatement(connection, name, objects);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        } catch (SQLException e) {
            logger.error("Error executing exists: {}", name, e);
            throw new DatabaseException("Database retrieval error", e);
        }
    }

    /**
     * Asynchronously checks whether a record exists in the database for a given stored procedure name
     * and the provided parameters.
     *
     * @param name    The name of the stored procedure to be executed for checking existence.
     * @param objects A variable number of objects representing the parameters to be
     *                passed to the stored procedure.
     * @return A CompletableFuture that will complete with {@code true} if a record exists,
     * or {@code false} otherwise.
     */
    public CompletableFuture<Boolean> existsCallableAsync(String name, Object... objects) {
        return CompletableFuture.supplyAsync(() -> existsCallable(name, objects), executor);
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
    public UUID generateUniqueIdentifierCallable(String name) {
        UUID uuid;
        do {
            uuid = UUID.randomUUID();
        } while (existsCallable(name, uuid.toString()));
        return uuid;
    }

    /**
     * Retrieves the auto-increment value for a specified table in the database.
     * The method executes a SQL query to obtain the auto-increment value for the given table name.
     *
     * @param tableName The name of the table for which to retrieve the auto-increment value.
     * @return A CompletableFuture containing the auto-increment value for the specified table.
     */
    public CompletableFuture<Long> getAutoIncrement(String tableName) {
        String sql = "SHOW TABLE STATUS LIKE ?"; // TODO: Maybe use a stored procedure for this
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = getPreparedStatement(connection, sql, tableName);
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    return resultSet.getLong("Auto_increment");
            } catch (SQLException e) {
                logger.error("Error retrieving auto-increment value for table: {}", tableName, e);
                throw new DatabaseException("Database retrieval error", e);
            }
            return null;
        }, executor);
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
