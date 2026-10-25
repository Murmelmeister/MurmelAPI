package de.murmelmeister.murmelapi.redis;

import com.google.gson.Gson;
import de.murmelmeister.murmelapi.utils.update.*;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.slf4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.Objects;

/**
 * Player-independent transport. Database work runs on one worker, never on Lettuce's IO threads.
 */
public final class RedisRefreshBridge implements AutoCloseable {
    private final RedisConfig config;
    private final RefreshProvider provider;
    private final Logger logger;
    private final Gson gson;
    private final Consumer<RefreshMessage> receiver;
    private final Supplier<RedisClient> clientFactory;
    private final RefreshListener listener = this::publish;
    private final AtomicBoolean dirty = new AtomicBoolean();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "murmel-redis-refresh");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean running;
    private boolean closed;
    private RedisClient client;
    private StatefulRedisConnection<String, String> publisher;
    private StatefulRedisPubSubConnection<String, String> subscriber;

    public RedisRefreshBridge(RedisConfig config, RefreshProvider provider, Logger logger,
                              Gson gson, Consumer<RefreshMessage> receiver) {
        this(config, provider, logger, gson, receiver, () -> RedisClient.create(config.uri()));
    }

    RedisRefreshBridge(RedisConfig config, RefreshProvider provider, Logger logger,
                       Gson gson, Consumer<RefreshMessage> receiver, Supplier<RedisClient> clientFactory) {
        this.config = Objects.requireNonNull(config);
        this.provider = Objects.requireNonNull(provider);
        this.logger = Objects.requireNonNull(logger);
        this.gson = Objects.requireNonNull(gson);
        this.receiver = Objects.requireNonNull(receiver);
        this.clientFactory = Objects.requireNonNull(clientFactory);
    }

    public synchronized void register() {
        if (closed) throw new IllegalStateException("Redis refresh bridge is closed.");
        if (running || !config.enabled()) return;
        running = true;
        provider.register(listener);
        worker.scheduleWithFixedDelay(this::maintain, 0, 5, TimeUnit.SECONDS);
    }

    private void submit(Runnable action) {
        if (!running) return;
        try {
            worker.execute(() -> {
                try {
                    action.run();
                } catch (Exception exception) {
                    logger.error("Redis refresh processing failed.", exception);
                }
            });
        } catch (RejectedExecutionException ignored) {
            // Shutdown raced with a Redis callback.
        }
    }

    private void maintain() {
        if (!running) return;
        try {
            if (client == null) connect();
            flushPendingRefresh();
        } catch (Exception exception) {
            dirty.set(true);
            logger.warn("Redis refresh connection unavailable; retrying in 5 seconds ({}).", exception.getClass().getSimpleName());
            disconnect();
        }
    }

    private void connect() {
        client = clientFactory.get();
        ClientOptions.Builder options = ClientOptions.builder()
                .autoReconnect(true)
                .requestQueueSize(1024)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);

        if (config.ssl()) options.sslOptions(config.sslOptions());
        client.setOptions(options.build());

        publisher = client.connect();
        subscriber = client.connectPubSub();

        subscriber.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String payload) {
                if (!config.channel().equals(channel)) return;
                submit(() -> {
                    RefreshMessage message = RefreshMessage.decode(payload, gson);
                    if (config.serverId().equals(message.sender())) return;
                    message.apply(provider, gson);
                    receiver.accept(message);
                });
            }

            @Override
            public void subscribed(String channel, long count) {
                if (!config.channel().equals(channel)) return;
                submit(() -> {
                    provider.fireAll(RefreshOrigin.REMOTE);
                    logger.info("Redis refresh subscribed to {}; local caches refreshed.", channel);
                });
            }
        });

        subscriber.sync().subscribe(config.channel());
    }

    private void publish(RefreshEvent<?> event) {
        if (event.origin() == RefreshOrigin.REMOTE || !running) return;
        RefreshMessage message = RefreshMessage.fromEvent(config.serverId(), event, gson);

        submit(() -> {
            if (dirty.get()) return; // A pending full refresh already covers this event.
            try {
                send(message);
            } catch (Exception exception) {
                dirty.set(true);
                logger.warn("Redis refresh publish failed; a full refresh will be retried ({}).", exception.getClass().getSimpleName());
            }
        });
    }

    private void send(RefreshMessage message) {
        if (publisher == null) throw new IllegalStateException("Redis is not connected.");
        publisher.sync().publish(config.channel(), gson.toJson(message));
    }

    private void flushPendingRefresh() {
        if (!dirty.getAndSet(false)) return;
        try {
            send(new RefreshMessage(config.serverId(), RefreshType.ALL.getName(), null));
        } catch (RuntimeException exception) {
            dirty.set(true);
            throw exception;
        }
    }

    private void disconnect() {
        try {
            if (subscriber != null) subscriber.close();
        } catch (RuntimeException exception) {
            logger.warn("Could not close Redis subscriber ({}).", exception.getClass().getSimpleName());
        } finally {
            subscriber = null;
        }

        try {
            if (publisher != null) publisher.close();
        } catch (RuntimeException exception) {
            logger.warn("Could not close Redis publisher ({}).", exception.getClass().getSimpleName());
        } finally {
            publisher = null;
        }

        try {
            if (client != null) client.shutdown();
        } catch (RuntimeException exception) {
            logger.warn("Could not shut down Redis client ({}).", exception.getClass().getSimpleName());
        } finally {
            client = null;
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        running = false;
        provider.unregister(listener);

        // Drain accepted events before disconnecting Redis or the database.
        worker.execute(() -> {
            try {
                flushPendingRefresh();
            } catch (RuntimeException exception) {
                logger.warn("Redis unavailable during shutdown; a refresh remains undelivered.");
            }
        });

        worker.shutdown();
        boolean interrupted = Thread.interrupted();

        try {
            if (!worker.awaitTermination(15, TimeUnit.SECONDS)) {
                int discarded = worker.shutdownNow().size();
                logger.warn("Redis shutdown timed out; discarded {} queued tasks.", discarded);

                if (!worker.awaitTermination(5, TimeUnit.SECONDS))
                    throw new IllegalStateException("Redis refresh worker did not stop before database shutdown.");
            }
        } catch (InterruptedException exception) {
            worker.shutdownNow();
            interrupted = true;

            try {
                if (!worker.awaitTermination(5, TimeUnit.SECONDS))
                    throw new IllegalStateException("Redis refresh worker did not stop after interruption.", exception);
            } catch (InterruptedException repeated) {
                throw new IllegalStateException("Interrupted again while stopping Redis refresh worker.", repeated);
            }
        } finally {
            disconnect();
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
}
