/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.jdbc.management;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.isPostgreSql;
import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.isSqlServer;

import io.gravitee.repository.exceptions.StartupLockTimeoutException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.StartupLock;
import io.gravitee.repository.management.api.StartupLockRepository;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@CustomLog
@Repository
public class JdbcStartupLockRepository implements StartupLockRepository {

    private static final String LOCK_NAME = "gravitee_startup";
    private static final int MYSQL_LOCK_NAME_MAX_LENGTH = 64;
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration KEEP_ALIVE_INTERVAL = Duration.ofSeconds(30);

    private final DataSource dataSource;
    private final String lockName;

    JdbcStartupLockRepository(DataSource dataSource, @Value("${management.jdbc.prefix:}") String tablePrefix) {
        this.dataSource = dataSource;
        this.lockName = tablePrefix + LOCK_NAME;
    }

    @Override
    public StartupLock acquire(Duration timeout) throws TechnicalException {
        Connection connection = openConnection();
        NamedLock namedLock;
        try {
            namedLock = namedLock(connection);
            waitUntilAcquired(namedLock, timeout);
        } catch (TechnicalException | RuntimeException e) {
            closeConnection(connection);
            throw e;
        }
        try {
            log.debug("Startup lock [{}] acquired", lockName);
            return new JdbcStartupLock(connection, namedLock);
        } catch (RuntimeException | Error e) {
            releaseAndClose(connection, namedLock);
            throw e;
        }
    }

    private Connection openConnection() throws TechnicalException {
        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(true);
            return connection;
        } catch (SQLException e) {
            throw new TechnicalException("Failed to open a database connection for startup lock [" + lockName + "]", e);
        }
    }

    private NamedLock namedLock(Connection connection) throws TechnicalException {
        if (isSqlServer()) {
            return new SqlServerNamedLock(connection, lockName);
        }
        if (isPostgreSql()) {
            return new PostgreSqlNamedLock(connection, ByteBuffer.wrap(sha256(lockName)).getLong());
        }
        try {
            return new MySqlNamedLock(connection, mySqlLockName(connection));
        } catch (SQLException e) {
            throw new TechnicalException("Failed to read the current database for startup lock [" + lockName + "]", e);
        }
    }

    private String mySqlLockName(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT DATABASE()")) {
            resultSet.next();
            String serverWideName = resultSet.getString(1) + "." + lockName;
            if (serverWideName.length() <= MYSQL_LOCK_NAME_MAX_LENGTH) {
                return serverWideName;
            }
            return HexFormat.of().formatHex(sha256(serverWideName));
        }
    }

    private void waitUntilAcquired(NamedLock namedLock, Duration timeout) throws TechnicalException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!tryAcquire(namedLock)) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw new StartupLockTimeoutException("Startup lock [" + lockName + "] not acquired within " + timeout);
            }
            try {
                TimeUnit.NANOSECONDS.sleep(Math.min(remaining, POLL_INTERVAL.toNanos()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TechnicalException("Interrupted while waiting for startup lock [" + lockName + "]", e);
            }
        }
    }

    private boolean tryAcquire(NamedLock namedLock) throws TechnicalException {
        try {
            return namedLock.tryAcquire();
        } catch (SQLException e) {
            throw new TechnicalException("Failed to request startup lock [" + lockName + "]", e);
        }
    }

    private void releaseAndClose(Connection connection, NamedLock namedLock) {
        try {
            namedLock.release();
            log.debug("Startup lock [{}] released", lockName);
        } catch (SQLException e) {
            log.warn("Failed to release startup lock [{}], ending its database session instead", lockName, e);
            abort(connection);
        } finally {
            closeConnection(connection);
        }
    }

    private void abort(Connection connection) {
        try {
            connection.abort(Runnable::run);
        } catch (SQLException e) {
            log.error("Failed to end the database session of startup lock [{}]", lockName, e);
        }
    }

    private void closeConnection(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            log.warn("Failed to close the database connection of startup lock [{}]", lockName, e);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private interface NamedLock {
        boolean tryAcquire() throws SQLException;

        void release() throws SQLException;
    }

    private record PostgreSqlNamedLock(Connection connection, long key) implements NamedLock {
        @Override
        public boolean tryAcquire() throws SQLException {
            return queryBoolean("SELECT pg_try_advisory_lock(?)");
        }

        @Override
        public void release() throws SQLException {
            if (!queryBoolean("SELECT pg_advisory_unlock(?)")) {
                throw new SQLException("Advisory lock " + key + " was not held by this session");
            }
        }

        private boolean queryBoolean(String sql) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, key);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getBoolean(1);
                }
            }
        }
    }

    private record MySqlNamedLock(Connection connection, String name) implements NamedLock {
        @Override
        public boolean tryAcquire() throws SQLException {
            Integer result = queryInteger("SELECT GET_LOCK(?, 0)");
            if (result == null) {
                throw new SQLException("GET_LOCK failed for lock " + name);
            }
            return result == 1;
        }

        @Override
        public void release() throws SQLException {
            Integer result = queryInteger("SELECT RELEASE_LOCK(?)");
            if (result == null || result != 1) {
                throw new SQLException("Lock " + name + " was not held by this session");
            }
        }

        private Integer queryInteger(String sql) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    int value = resultSet.getInt(1);
                    return resultSet.wasNull() ? null : value;
                }
            }
        }
    }

    private record SqlServerNamedLock(Connection connection, String name) implements NamedLock {
        private static final int LOCK_REQUEST_TIMED_OUT = -1;

        @Override
        public boolean tryAcquire() throws SQLException {
            try (CallableStatement statement = connection.prepareCall("{? = call sp_getapplock(?, ?, ?, ?)}")) {
                statement.registerOutParameter(1, Types.INTEGER);
                statement.setString(2, name);
                statement.setString(3, "Exclusive");
                statement.setString(4, "Session");
                statement.setInt(5, 0);
                statement.execute();
                int result = statement.getInt(1);
                if (result == LOCK_REQUEST_TIMED_OUT) {
                    return false;
                }
                if (result < 0) {
                    throw new SQLException("sp_getapplock returned " + result + " for lock " + name);
                }
                return true;
            }
        }

        @Override
        public void release() throws SQLException {
            try (CallableStatement statement = connection.prepareCall("{? = call sp_releaseapplock(?, ?)}")) {
                statement.registerOutParameter(1, Types.INTEGER);
                statement.setString(2, name);
                statement.setString(3, "Session");
                statement.execute();
                int result = statement.getInt(1);
                if (result < 0) {
                    throw new SQLException("sp_releaseapplock returned " + result + " for lock " + name);
                }
            }
        }
    }

    private final class JdbcStartupLock implements StartupLock {

        private final Connection connection;
        private final NamedLock namedLock;
        private final AtomicBoolean closed = new AtomicBoolean();
        private final ScheduledExecutorService keepAlive = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("gravitee-startup-lock-keep-alive").factory()
        );

        private JdbcStartupLock(Connection connection, NamedLock namedLock) {
            this.connection = connection;
            this.namedLock = namedLock;
            keepAlive.scheduleWithFixedDelay(
                this::keepAlive,
                KEEP_ALIVE_INTERVAL.toMillis(),
                KEEP_ALIVE_INTERVAL.toMillis(),
                TimeUnit.MILLISECONDS
            );
        }

        private void keepAlive() {
            synchronized (connection) {
                if (closed.get()) {
                    return;
                }
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SELECT 1");
                } catch (SQLException e) {
                    log.error("Startup lock [{}] is lost: its database connection failed the keep-alive query", lockName, e);
                    keepAlive.shutdown();
                }
            }
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            keepAlive.shutdown();
            synchronized (connection) {
                releaseAndClose(connection, namedLock);
            }
        }
    }
}
