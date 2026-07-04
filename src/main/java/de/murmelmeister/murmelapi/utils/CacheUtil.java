package de.murmelmeister.murmelapi.utils;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.database.ParameterProcessor;
import de.murmelmeister.library.database.ResultSetProcessor;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public final class CacheUtil {
    public static <K, V> @NotNull LoadingCache<K, V> buildCache(CacheLoader<K, V> loader, long maxSize) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .recordStats();
        if (maxSize > 0) builder.maximumSize(maxSize);
        return builder.build(loader);
    }

    public static <K, V> @NotNull LoadingCache<K, V> buildCacheExpired(CacheLoader<K, V> loader, long maxSize, Duration ttl) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .recordStats();
        if (maxSize > 0) builder.maximumSize(maxSize);
        return builder.build(loader);
    }

    public static <K, V> @NotNull LoadingCache<K, V> buildCacheRefresh(CacheLoader<K, V> loader, long maxSize, Duration ttl) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .recordStats()
                .refreshAfterWrite(ttl)
                .expireAfterAccess(ttl.multipliedBy(3));
        if (maxSize > 0) builder.maximumSize(maxSize);
        return builder.build(loader);
    }

    public static <K, V> void remove(LoadingCache<K, List<V>> cache, K key, Predicate<V> removeIf) {
        if (key == null) return;
        cache.asMap().compute(key, (k, values) -> {
            if (values == null) return null;
            List<V> copy = new ArrayList<>(values);
            copy.removeIf(removeIf);
            return copy.isEmpty() ? null : Collections.unmodifiableList(copy);
        });
    }

    public static <K, V> void put(LoadingCache<K, List<V>> cache, K key, V value, Predicate<V> removeIf) {
        if (key == null) return;
        cache.asMap().compute(key, (k, values) -> {
            List<V> copy = (values == null) ? new ArrayList<>() : new ArrayList<>(values);
            copy.removeIf(removeIf);
            copy.add(value);
            return Collections.unmodifiableList(copy);
        });
    }

    public static <V> V loadSingle(Database database, String sql, Long limit, ResultSetProcessor<V> resultSet, ParameterProcessor args) {
        LimitBinding binding = prepareLimit(sql, limit);
        ParameterProcessor processor = ParameterProcessor.of(args).andThen(binding.processor());
        return database.query(binding.sql(), null, resultSet, processor);
    }

    public static <V> V loadCallSingle(Database database, String sql, Long limit, ResultSetProcessor<V> resultSet, ParameterProcessor args) {
        LimitBinding binding = prepareLimit(sql, limit);
        ParameterProcessor processor = ParameterProcessor.of(args).andThen(binding.processor());
        return database.queryCallable(binding.sql(), null, resultSet, processor);
    }

    public static <V> V loadSingle(Database database, String sql, Long limit, ResultSetProcessor<V> resultSet) {
        LimitBinding binding = prepareLimit(sql, limit);
        return database.query(binding.sql(), null, resultSet, binding.processor());
    }

    public static <V> List<V> loadList(Database database, String sql, Long limit, ResultSetProcessor<V> resultSet, ParameterProcessor args) {
        LimitBinding binding = prepareLimit(sql, limit);
        ParameterProcessor processor = ParameterProcessor.of(args).andThen(binding.processor());
        return database.queryList(binding.sql(), resultSet, processor);
    }

    public static <V> List<V> loadList(Database database, String sql, Long limit, ResultSetProcessor<V> resultSet) {
        LimitBinding binding = prepareLimit(sql, limit);
        return database.queryList(binding.sql(), resultSet, binding.processor());
    }

    private static int countPlaceholders(String sql) {
        if (sql == null || sql.isEmpty())
            return 0;

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int count = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                if (inSingleQuote && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    i++; // skip escaped a single quote represented by ''
                    continue;
                }
                boolean escaped = i > 0 && sql.charAt(i - 1) == '\\';
                if (!escaped)
                    inSingleQuote = !inSingleQuote;
                continue;
            }

            if (c == '"' && !inSingleQuote) {
                boolean escaped = i > 0 && sql.charAt(i - 1) == '\\';
                if (!escaped)
                    inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (c == '?' && !inSingleQuote && !inDoubleQuote)
                count++;
        }

        return count;
    }

    private static LimitBinding prepareLimit(String sql, Long limit) {
        if (limit == null || limit <= 0)
            return new LimitBinding(sql, ParameterProcessor.noop());

        int limitIndex = countPlaceholders(sql) + 1;
        String limitSql = sql + " LIMIT ?";
        ParameterProcessor limitProcessor = stmt -> stmt.setLong(limitIndex, limit);
        return new LimitBinding(limitSql, limitProcessor);
    }

    private record LimitBinding(String sql, ParameterProcessor processor) {
    }
}
