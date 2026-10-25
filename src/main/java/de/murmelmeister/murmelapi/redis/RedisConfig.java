package de.murmelmeister.murmelapi.redis;

import io.lettuce.core.RedisURI;
import io.lettuce.core.SslOptions;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

/**
 * Shared configuration, stored in each plugin's data directory.
 */
public record RedisConfig(
        boolean enabled,
        String host,
        int port,
        String username,
        String password,
        boolean ssl,
        String channel,
        String serverId,
        TlsSettings tls
) {

    public RedisConfig(boolean enabled, String host, int port, String username,
                       String password, boolean ssl, String channel, String serverId) {
        this(enabled, host, port, username, password, ssl, channel, serverId, TlsSettings.defaults());
    }

    public RedisConfig {
        if (host == null || host.isBlank() || channel == null || channel.isBlank()
                || serverId == null || serverId.isBlank())
            throw new IllegalArgumentException("Redis host, channel and server-id must not be blank.");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid Redis port.");
        if (tls == null) throw new IllegalArgumentException("TLS settings must not be null.");
        host = host.trim();
        channel = channel.trim();
        serverId = serverId.trim();
        username = username == null ? "" : username.trim();
        password = password == null ? "" : password;
        if (ssl) tls.validate();
    }

    public static RedisConfig load(Path directory) {
        Path file = directory.resolve("redis.properties");
        Properties properties = new Properties();
        try {
            Files.createDirectories(directory);
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            }

            boolean changed = false;
            String[][] defaults = {
                    {"enabled", "true"},
                    {"host", "127.0.0.1"},
                    {"port", "6379"},
                    {"username", "default"},
                    {"password", ""},
                    {"ssl", "false"},
                    {"channel", "murmel:refresh"},
                    {"server-id", UUID.randomUUID().toString()},
                    {"tls.ca-cert", ""},
                    {"tls.client-cert", ""},
                    {"tls.client-key", ""},
                    {"tls.client-key-password", ""},
                    {"tls.truststore", ""},
                    {"tls.truststore-password", ""},
                    {"tls.keystore", ""},
                    {"tls.keystore-password", ""},
                    {"tls.store-type", "PKCS12"}
            };

            for (String[] entry : defaults) {
                if (!properties.containsKey(entry[0])) {
                    properties.setProperty(entry[0], entry[1]);
                    changed = true;
                }
            }

            if (changed) {
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Redis refresh transport. Use a unique server-id per instance. Restart after changes.");
                }
            }

            return new RedisConfig(
                    booleanValue(properties, "enabled"),
                    properties.getProperty("host").trim(),
                    Integer.parseInt(properties.getProperty("port").trim()),
                    properties.getProperty("username"),
                    properties.getProperty("password"),
                    booleanValue(properties, "ssl"),
                    properties.getProperty("channel").trim(),
                    properties.getProperty("server-id").trim(),
                    new TlsSettings(
                            resolve(directory, properties.getProperty("tls.ca-cert")),
                            resolve(directory, properties.getProperty("tls.client-cert")),
                            resolve(directory, properties.getProperty("tls.client-key")),
                            properties.getProperty("tls.client-key-password"),
                            resolve(directory, properties.getProperty("tls.truststore")),
                            properties.getProperty("tls.truststore-password"),
                            resolve(directory, properties.getProperty("tls.keystore")),
                            properties.getProperty("tls.keystore-password"),
                            properties.getProperty("tls.store-type").trim()
                    )
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Redis configuration: " + file, exception);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Redis configuration in " + file + ": " + exception.getMessage(), exception);
        }
    }

    private static boolean booleanValue(Properties properties, String key) {
        String value = properties.getProperty(key).trim();
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
            throw new IllegalArgumentException(key + " must be true or false.");
        return Boolean.parseBoolean(value);
    }

    public RedisURI uri() {
        RedisURI.Builder builder = RedisURI.Builder.redis(host, port)
                .withSsl(ssl)
                .withVerifyPeer(true)
                .withTimeout(Duration.ofSeconds(5));

        if (password != null && !password.isEmpty()) {
            if (username == null || username.isBlank()) builder.withPassword(password.toCharArray());
            else builder.withAuthentication(username, password.toCharArray());
        }

        return builder.build();
    }

    private static Path resolve(Path directory, String value) {
        return value == null || value.isBlank() ? null : directory.resolve(value.trim()).toAbsolutePath().normalize();
    }

    public SslOptions sslOptions() {
        tls.validate();
        SslOptions.Builder builder = SslOptions.builder()
                .jdkSslProvider()
                .protocols("TLSv1.3", "TLSv1.2")
                .handshakeTimeout(Duration.ofSeconds(5))
                .keyStoreType(tls.storeType());

        if (tls.caCert() != null) builder.trustManager(tls.caCert().toFile());
        if (tls.truststore() != null) builder.truststore(tls.truststore().toFile(), tls.truststorePassword());
        if (tls.clientCert() != null)
            builder.keyManager(
                    tls.clientCert().toFile(),
                    tls.clientKey().toFile(),
                    tls.clientKeyPassword().isEmpty() ? null : tls.clientKeyPassword().toCharArray()
            );
        if (tls.keystore() != null) builder.keystore(tls.keystore().toFile(), tls.keystorePassword().toCharArray());

        return builder.build();
    }

    /**
     * PEM files or Java stores; an empty trust configuration uses the JVM's trusted CAs.
     */
    public record TlsSettings(
            Path caCert,
            Path clientCert,
            Path clientKey,
            String clientKeyPassword,
            Path truststore,
            String truststorePassword,
            Path keystore,
            String keystorePassword,
            String storeType
    ) {

        public TlsSettings {
            clientKeyPassword = clientKeyPassword == null ? "" : clientKeyPassword;
            truststorePassword = truststorePassword == null ? "" : truststorePassword;
            keystorePassword = keystorePassword == null ? "" : keystorePassword;
            storeType = storeType == null ? "PKCS12" : storeType.trim().toUpperCase();
        }

        public static TlsSettings defaults() {
            return new TlsSettings(null, null, null, "", null, "", null, "", "PKCS12");
        }

        public void validate() {
            if (!"PKCS12".equalsIgnoreCase(storeType) && !"JKS".equalsIgnoreCase(storeType))
                throw new IllegalArgumentException("tls.store-type must be PKCS12 or JKS.");
            if (caCert != null && truststore != null)
                throw new IllegalArgumentException("Choose tls.ca-cert OR tls.truststore.");
            if ((clientCert == null) != (clientKey == null))
                throw new IllegalArgumentException("tls.client-cert and tls.client-key must be configured together.");
            if (keystore != null && clientCert != null)
                throw new IllegalArgumentException("Choose a PEM client certificate/key OR tls.keystore.");
            for (Path path : new Path[]{caCert, clientCert, clientKey, truststore, keystore}) {
                if (path != null && (!Files.isRegularFile(path) || !Files.isReadable(path)))
                    throw new IllegalArgumentException("TLS file does not exist or is not readable: " + path);
            }
        }

        @Override
        public @NonNull String toString() {
            return "TlsSettings[caCert=" + caCert + ", clientCert=" + clientCert + ", clientKey=" + clientKey
                    + ", truststore=" + truststore + ", keystore=" + keystore + ", storeType=" + storeType
                    + ", passwords=<redacted>]";
        }
    }

    @Override
    public @NonNull String toString() {
        return "RedisConfig[enabled=" + enabled + ", host=" + host + ", port=" + port
                + ", password=<redacted>, ssl=" + ssl + ", channel=" + channel + ", serverId=" + serverId + "]";
    }
}
