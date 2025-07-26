package de.murmelmeister.murmelapi.utils;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.database.ResultSetProcessor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public final class CacheUtil {
    public static <K, V> LoadingCache<K, V> buildCache(CacheLoader<K, V> loader, long maxSize) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .recordStats()
                .build(loader);
    }

    public static <K, V> LoadingCache<K, V> buildCacheExpired(CacheLoader<K, V> loader, long maxSize, Duration ttl) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttl)
                .recordStats()
                .build(loader);
    }

    public static <K, V> LoadingCache<K, V> buildCacheRefresh(CacheLoader<K, V> loader, long maxSize, Duration ttl) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .refreshAfterWrite(ttl)
                .recordStats()
                .build(loader);
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

    public static <V> V loadSingle(Database database, String sql, Long limit, ResultSetProcessor<V> resultSet, Object... args) {
        String limitSql = sql + (limit != null && limit > 0 ? " LIMIT ?" : "");
        return limit != null && limit > 0
                ? database.query(limitSql, null, resultSet, args, limit)
                : database.query(limitSql, null, resultSet, args);
    }

    public static <V> List<V> loadList(Database database, String sql, Long limit, ResultSetProcessor<V> resultSet, Object... args) {
        String limitSql = sql + (limit != null && limit > 0 ? " LIMIT ?" : "");
        return limit != null && limit > 0
                ? database.queryList(limitSql, resultSet, args, limit)
                : database.queryList(limitSql, resultSet, args);
    }
}
