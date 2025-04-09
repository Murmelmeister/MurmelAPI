package de.murmelmeister.murmelapi.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Database class managing connection pool configuration, connection handling,
 * and various SQL operations (query, update, batch update, asynchronous access).
 */
public final class Database {
    private final Logger logger = LoggerFactory.getLogger(Database.class);

    private volatile HikariDataSource dataSource;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Lock writeLock = lock.writeLock();

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    // Threshold in milliseconds for slow query logging
    private static final long SLOW_QUERY_THRESHOLD_MS = 500;

    /**
     * Creates a configured HikariConfig instance.
     *
     * @param driverClassName The JDBC driver class name (e.g., "com.mysql.cj.jdbc.Driver").
     * @param url             The JDBC database URL.
     * @param user            The database username.
     * @param password        The database user's password.
     * @return Configured HikariConfig instance.
     */
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
     * Connects to the database using the provided JDBC driver, URL, username, and password.
     * If an existing connection exists, it will be closed before establishing a new one.
     *
     * @param driverClassName The JDBC driver class name.
     * @param url             The JDBC database URL.
     * @param user            The database username.
     * @param password        The database user's password.
     */
    public void connect(String driverClassName, String url, String user, String password) {
        try {
            if (!writeLock.tryLock(10, TimeUnit.SECONDS))
                throw new DatabaseException("Could not acquire lock for establishing connection.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DatabaseException("Thread interrupted while waiting for lock.", e);
        }

        try {
            if (dataSource != null && !dataSource.isClosed())
                dataSource.close();
            HikariConfig config = getHikariConfig(driverClassName, url, user, password);
            this.dataSource = new HikariDataSource(config);
            logger.info("Database connection successfully established: {}", url);
        } catch (Exception e) {
            logger.error("Failed to establish database connection", e);
            throw new DatabaseException("Failed to establish database connection.", e);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Connects to a MySQL database.
     *
     * @param url      The JDBC URL, e.g., "jdbc:mysql://localhost:3306/database".
     * @param user     The database username.
     * @param password The database user's password.
     */
    public void connectMySQL(String url, String user, String password) {
        connect("com.mysql.cj.jdbc.Driver", url, user, password);
    }

    /**
     * Connects to the database using external configuration.
     *
     * @param properties A Properties object containing keys "db.driver", "db.url", "db.user", and "db.password".
     */
    public void connectFromProperties(Properties properties) {
        String driverClassName = properties.getProperty("db.driver");
        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");

        if (driverClassName == null || url == null || user == null || password == null) {
            throw new DatabaseException("Missing database connection properties.");
        }

        connect(driverClassName, url, user, password);
    }

    /**
     * Disconnects from the database.
     */
    public void disconnect() {
        try {
            if (!writeLock.tryLock(10, TimeUnit.SECONDS))
                throw new DatabaseException("Could not acquire lock for disconnecting from database.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DatabaseException("Thread interrupted while waiting for lock.", e);
        }

        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                dataSource = null;
                logger.info("Database connection successfully closed.");
            }
        } catch (Exception e) {
            logger.error("Failed to close database connection", e);
            throw new DatabaseException("Failed to close database connection.", e);
        } finally {
            writeLock.unlock();
        }

        if (!executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS))
                    executor.shutdownNow();
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Returns the ExecutorService used for asynchronous operations.
     *
     * @return The ExecutorService instance.
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Functional interface for executing operations using a Connection.
     *
     * @param <T> The return type of the operation.
     */
    @FunctionalInterface
    private interface SQLConnectionOperation<T> {
        T execute(Connection connection) throws SQLException;
    }

    /**
     * Executes an operation within a database transaction.
     * Commits the transaction upon success, opr rolls back upon failure.
     *
     * @param operation The database operation to execute.
     * @param <T>       The return type of the operation.
     * @return The result of the operation.
     */
    private <T> T executeInTransaction(SQLConnectionOperation<T> operation) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            long startTime = System.nanoTime();
            T result = operation.execute(connection);
            connection.commit();
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            if (durationMs > SLOW_QUERY_THRESHOLD_MS)
                logger.warn("Slow query detected: {} ms", durationMs);
            return result;
        } catch (SQLException e) {
            if (connection != null)
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    logger.error("Failed to roll back transaction", rollbackException);
                }
            logger.error("Failed to execute transaction", e);
            throw new DatabaseException("Failed to execute transaction.", e);
        } finally {
            if (connection != null)
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Failed to close connection", e);
                }
        }
    }

    /**
     * Executes a SQL update statement.
     *
     * @param sql        The SQL update statement to execute.
     * @param parameters The parameters to set in the PreparedStatement.
     */
    public void update(String sql, Object... parameters) {
        executeInTransaction(connection -> {
            long startTime = System.nanoTime();
            try (PreparedStatement statement = getPreparedStatement(connection, sql, parameters)) {
                statement.executeUpdate();
            }
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            if (durationMs > SLOW_QUERY_THRESHOLD_MS)
                logger.warn("Update statement [{}] took {} ms", sql, durationMs);
            return null;
        });
    }

    /**
     * Executes a callable update statement and returns the number of affected rows.
     *
     * @param name       The name of the stored procedure to call.
     * @param parameters The parameters to set in the CallableStatement.
     * @return The number of affected rows.
     */
    public int updateCallable(String name, Object... parameters) {
        return executeInTransaction(connection -> {
            long startTime = System.nanoTime();
            try (CallableStatement statement = getCallableStatement(connection, name, parameters)) {
                int affectedRows = statement.executeUpdate();
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                if (durationMs > SLOW_QUERY_THRESHOLD_MS)
                    logger.warn("Callable update [{}] took {} ms", name, durationMs);
                return affectedRows;
            }
        });
    }

    /**
     * Executes a batch update for a given SQL statement.
     *
     * @param sql             The SQL update statement to execute.
     * @param batchParameters The list of parameters for each batch.
     * @return An array of integers representing the number of affected rows for each batch.
     */
    public int[] updateBatch(String sql, List<Object[]> batchParameters) {
        return executeInTransaction(connection -> {
            long startTime = System.nanoTime();
            try (PreparedStatement statement = getPreparedStatement(connection, sql)) {
                for (Object[] parameters : batchParameters) {
                    setParameters(statement, parameters);
                    statement.addBatch();
                }
                int[] affectedRows = statement.executeBatch();
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                if (durationMs > SLOW_QUERY_THRESHOLD_MS)
                    logger.warn("Batch update statement [{}] took {} ms", sql, durationMs);
                return affectedRows;
            }
        });
    }

    /**
     * Executes a SQL query and processes the first result row.
     *
     * @param sql          The SQL query to execute.
     * @param defaultValue The default value to return if no result is found.
     * @param processor    The processor to handle the ResultSet.
     * @param parameters   The parameters to set in the PreparedStatement.
     * @param <T>          The type of the result.
     * @return The processed result from the ResultSet, or the default value if no result is found.
     */
    public <T> T query(String sql, T defaultValue, ResultSetProcessor<T> processor, Object... parameters) {
        long startTime = System.nanoTime();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = getPreparedStatement(connection, sql, parameters);
             ResultSet resultSet = statement.executeQuery()) {

            T result = defaultValue;
            if (resultSet.next())
                result = processor.process(resultSet);

            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            if (durationMs > SLOW_QUERY_THRESHOLD_MS)
                logger.warn("Query statement [{}] took {} ms", sql, durationMs);

            return result;
        } catch (SQLException e) {
            logger.error("Error executing query: {} with parameters {}", sql, Arrays.toString(parameters), e);
            throw new DatabaseException("Failed to execute query.", e);
        }
    }

    /**
     * Executes a callable query and processes the first result row.
     *
     * @param name         The name of the stored procedure to call.
     * @param defaultValue The default value to return if no result is found.
     * @param processor    The processor to handle the ResultSet.
     * @param parameters   The parameters to set in the CallableStatement.
     * @param <T>          The type of the result.
     * @return The processed result from the ResultSet, or the default value if no result is found.
     */
    public <T> T queryCallable(String name, T defaultValue, ResultSetProcessor<T> processor, Object... parameters) {
        long startTime = System.nanoTime();
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = getCallableStatement(connection, name, parameters);
             ResultSet resultSet = statement.executeQuery()) {

            T result = defaultValue;
            if (resultSet.next())
                result = processor.process(resultSet);

            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            if (durationMs > SLOW_QUERY_THRESHOLD_MS)
                logger.warn("Callable query [{}] took {} ms", name, durationMs);

            return result;
        } catch (SQLException e) {
            logger.error("Error executing callable query: {} with parameters {}", name, Arrays.toString(parameters), e);
            throw new DatabaseException("Failed to execute callable query.", e);
        }
    }

    /**
     * Executes a SQL query and returns a list of processed result rows.
     *
     * @param sql        The SQL query to execute.
     * @param processor  The processor to handle the ResultSet.
     * @param parameters The parameters to set in the PreparedStatement.
     * @param <T>        The type of the result.
     * @return A list of processed results from the ResultSet.
     */
    public <T> List<T> queryList(String sql, ResultSetProcessor<T> processor, Object... parameters) {
        long startTime = System.nanoTime();
        List<T> resultList = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = getPreparedStatement(connection, sql, parameters);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next())
                resultList.add(processor.process(resultSet));

            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            if (durationMs > SLOW_QUERY_THRESHOLD_MS)
                logger.warn("Query list [{}] took {} ms", sql, durationMs);

        } catch (SQLException e) {
            logger.error("Error executing queryList: {} with parameters {}", sql, Arrays.toString(parameters), e);
            throw new DatabaseException("Failed to execute query.", e);

        }
        return resultList;
    }

    /**
     * Executes a callable query and returns a list of processed result rows.
     *
     * @param name       The name of the stored procedure to call.
     * @param processor  The processor to handle the ResultSet.
     * @param parameters The parameters to set in the CallableStatement.
     * @param <T>        The type of the result.
     * @return A list of processed results from the ResultSet.
     */
    public <T> List<T> queryListCallable(String name, ResultSetProcessor<T> processor, Object... parameters) {
        long startTime = System.nanoTime();
        List<T> resultList = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = getCallableStatement(connection, name, parameters);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next())
                resultList.add(processor.process(resultSet));

            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            if (durationMs > SLOW_QUERY_THRESHOLD_MS)
                logger.warn("Callable query list [{}] took {} ms", name, durationMs);

        } catch (SQLException e) {
            logger.error("Error executing queryList: {} with parameters {}", name, Arrays.toString(parameters), e);
            throw new DatabaseException("Failed to execute query.", e);

        }
        return resultList;
    }

    /**
     * Executes a SQL query to check if a result exists.
     *
     * @param sql        The SQL query to execute.
     * @param parameters The parameters to set in the PreparedStatement.
     * @return True if a result exists, false otherwise.
     */
    public boolean exists(String sql, Object... parameters) {
        long startTime = System.nanoTime();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = getPreparedStatement(connection, sql, parameters);
             ResultSet resultSet = statement.executeQuery()) {

            boolean exists = resultSet.next();
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            if (durationMs > SLOW_QUERY_THRESHOLD_MS)
                logger.warn("Exists query [{}] took {} ms", sql, durationMs);

            return exists;
        } catch (SQLException e) {
            logger.error("Error executing exists query: {} with parameters {}", sql, Arrays.toString(parameters), e);
            throw new DatabaseException("Failed to execute exists query.", e);
        }
    }

    /**
     * Executes a callable query to check if a result exists.
     *
     * @param name       The name of the stored procedure to call.
     * @param parameters The parameters to set in the CallableStatement.
     * @return True if a result exists, false otherwise.
     */
    public boolean existsCallable(String name, Object... parameters) {
        long startTime = System.nanoTime();
        try (Connection connection = dataSource.getConnection();
             CallableStatement statement = getCallableStatement(connection, name, parameters);
             ResultSet resultSet = statement.executeQuery()) {

            boolean exists = resultSet.next();
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            if (durationMs > SLOW_QUERY_THRESHOLD_MS)
                logger.warn("Exists callable query [{}] took {} ms", name, durationMs);

            return exists;
        } catch (SQLException e) {
            logger.error("Error executing exists callable query: {} with parameters {}", name, Arrays.toString(parameters), e);
            throw new DatabaseException("Failed to execute exists callable query.", e);
        }
    }

    /**
     * Executes a SQL query asynchronously and processes the first result row.
     *
     * @param sql          The SQL query to execute.
     * @param defaultValue The default value to return if no result is found.
     * @param processor    The processor to handle the ResultSet.
     * @param parameters   The parameters to set in the PreparedStatement.
     * @param <T>          The type of the result.
     * @return A CompletableFuture containing the processed result from the ResultSet, or the default value if no result is found.
     */
    public <T> CompletableFuture<T> queryAsync(String sql, T defaultValue, ResultSetProcessor<T> processor, Object... parameters) {
        return CompletableFuture.supplyAsync(() -> query(sql, defaultValue, processor, parameters), executor);
    }

    /**
     * Executes a callable query asynchronously and processes the first result row.
     *
     * @param name         The name of the stored procedure to call.
     * @param defaultValue The default value to return if no result is found.
     * @param processor    The processor to handle the ResultSet.
     * @param parameters   The parameters to set in the CallableStatement.
     * @param <T>          The type of the result.
     * @return A CompletableFuture containing the processed result from the ResultSet, or the default value if no result is found.
     */
    public <T> CompletableFuture<T> queryCallableAsync(String name, T defaultValue, ResultSetProcessor<T> processor, Object... parameters) {
        return CompletableFuture.supplyAsync(() -> queryCallable(name, defaultValue, processor, parameters), executor);
    }

    /**
     * Creates a table in the database with the specified name and columns.
     * The table name is validated to ensure it contains only letters, digits, and underscores.
     *
     * @param tableName         The name of the table to create.
     * @param columnsDefinition The SQL definition of the columns (e.g., "id INT PRIMARY KEY, name VARCHAR(255)").
     */
    public void createTable(String tableName, String columnsDefinition) {
        // Validate table name: Only letters, digits, and underscores are allowed.
        if (!tableName.matches("[A-Za-z0-9_]+"))
            throw new IllegalArgumentException("Invalid table name: " + tableName);

        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnsDefinition + ")";
        update(sql);
    }

    /**
     * Generates a unique UUID by repeatedly creating random UUIDs until one is found
     * that does not already exist in the database (as determined by the provided callable check).
     *
     * @param checkProcedureName The name of the callable procedure used to check existence.
     * @return A unique UUID.
     */
    public UUID generateUniqueIdCallable(String checkProcedureName) {
        UUID uuid;
        do {
            uuid = UUID.randomUUID();
        } while (existsCallable(checkProcedureName, uuid));
        return uuid;
    }

    /**
     * Retrieves the next auto-increment value for the specified table.
     *
     * @param tableName The name of the table.
     * @return A CompletableFuture resolving to the next auto-increment value.
     */
    public CompletableFuture<Long> getAutoIncrement(String tableName) {
        // Validate table name: Only letters, digits, and underscores allowed.
        if (!tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
        String sql = "SHOW TABLE STATUS LIKE ?";
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = getPreparedStatement(connection, sql, tableName);
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    return resultSet.getLong("Auto_increment");
                else
                    throw new DatabaseException("Table not found: " + tableName);
            } catch (SQLException e) {
                logger.error("Error retrieving auto-increment value for table: {}", tableName, e);
                throw new DatabaseException("Database retrieval error", e);
            }
        }, executor);
    }

    /**
     * Generates a SQL query to create a stored procedure if it does not exist.
     *
     * @param procedureName   The name of the procedure.
     * @param inputParameters The input parameters as a comma-separated list (e.g., "IN param1 INT, IN param2 VARCHAR(255)").
     * @param procedureBody   The SQL query or statements that form the procedure body.
     * @return The SQL query string to create the procedure.
     */
    public static String getProcedureQuery(String procedureName, String inputParameters, String procedureBody) {
        // Validate procedure name: Only letters, digits, and underscores allowed.
        if (!procedureName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid procedure name: " + procedureName);
        }

        return "CREATE PROCEDURE IF NOT EXISTS `" + procedureName + "`(" + inputParameters + ")\n" +
               "BEGIN\n    " + procedureBody + "\nEND;";
    }


    /**
     * Creates a CallableStatement for the given stored procedure and sets the parameters.
     *
     * @param connection The database connection to use.
     * @param name       The name of the stored procedure.
     * @param parameters The parameters to set in the CallableStatement.
     * @return The prepared statement with the parameters set.
     * @throws SQLException If an error occurs while preparing the statement or setting the parameters.
     */
    private CallableStatement getCallableStatement(Connection connection, String name, Object... parameters) throws SQLException {
        String placeholder = (parameters.length > 0) ? String.join(",", Collections.nCopies(parameters.length, "?")) : "";
        CallableStatement statement = connection.prepareCall("{CALL " + name + "(" + placeholder + ")}");
        setParameters(statement, parameters);
        return statement;
    }

    /**
     * Creates a PreparedStatement for the given SQL query and sets the parameters.
     *
     * @param connection The database connection to use.
     * @param sql        The SQL query to prepare.
     * @param parameters The parameters to set in the PreparedStatement.
     * @return The prepared statement with the parameters set.
     * @throws SQLException If an error occurs while preparing the statement or setting the parameters.
     */
    private PreparedStatement getPreparedStatement(Connection connection, String sql, Object... parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        setParameters(statement, parameters);
        return statement;
    }

    /**
     * Sets the parameters on the given PreparedStatement.
     *
     * @param statement  The PreparedStatement to set parameters on.
     * @param parameters The parameters to set.
     * @throws SQLException If an error occurs while setting the parameters.
     */
    private void setParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            int parameterIndex = i + 1;
            Object parameter = parameters[i];

            if (parameter == null) {
                statement.setNull(parameterIndex, Types.NULL);
                continue;
            }

            switch (parameter) {
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
                default -> statement.setObject(parameterIndex, parameter);
            }
        }
    }
}
