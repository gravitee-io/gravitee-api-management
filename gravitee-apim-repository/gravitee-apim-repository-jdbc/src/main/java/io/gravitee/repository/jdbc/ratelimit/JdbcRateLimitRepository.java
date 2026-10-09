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
package io.gravitee.repository.jdbc.ratelimit;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.isSqlServer;

import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.reactivex.rxjava3.core.Single;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC-backed {@link RateLimitRepository}. Each increment locks the row of its key with
 * {@code SELECT ... FOR UPDATE} (a {@code WITH (UPDLOCK, HOLDLOCK)} hint on SQL Server), resets the
 * window when it has expired, adds the weight and writes the row back, in one transaction on a connection
 * it owns. The row lock serialises concurrent requests on a key, so no increment is lost and no two
 * requests read the same counter.
 *
 * <p>The connection and transaction are managed explicitly, as in the JDBC token-bucket repository:
 * a {@code SELECT ... FOR UPDATE} executed in auto-commit mode releases its lock as soon as the statement
 * returns.
 *
 * <p>The first request for a key has no row to lock, so the row is <em>seeded</em> first with the supplied
 * rate limit, in auto-commit and outside any locking transaction, then the locking transaction increments
 * it. A concurrent seeder loses with a duplicate-key error, which is ignored: either way the row exists
 * afterwards. Seeding inside the locking transaction would deadlock on MySQL/MariaDB and SQL Server, where an
 * empty locking SELECT takes a range lock that every concurrent INSERT then waits on.
 *
 * <p>Concurrent requests on a key wait for each other on the row lock. A whole call, retries included,
 * gets the same 5 seconds budget as the previous transaction timeout. The supplier may be called more than
 * once per call: to seed the row, then to reset its window if the seeded one has already expired, and again
 * on a retry.
 *
 * @author njt
 */
@CustomLog
@Repository
public class JdbcRateLimitRepository implements RateLimitRepository<RateLimit> {

    /** Safety net only: the seed/lock handshake converges by design, retries cover deadlocks and a purge racing the seed. */
    private static final int MAX_RETRIES = 5;

    /** Budget of a whole call, retries included, as the previous 5 seconds transaction timeout, so a request cannot block indefinitely on the row lock. */
    private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);

    /** SQLState class 23 == integrity constraint violation; a primary-key clash on the seed lands here. */
    private static final String INTEGRITY_VIOLATION_SQLSTATE_CLASS = "23";

    /** SQLState class 40 == transaction rollback; MySQL/MariaDB/SQL Server report 40001 on a deadlock, PostgreSQL 40P01. */
    private static final String TRANSIENT_ROLLBACK_SQLSTATE_CLASS = "40";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String selectForUpdateSql;
    private final String selectSqlServerSql;
    private final String insertSql;
    private final String updateSql;

    public JdbcRateLimitRepository(@Value("${ratelimit.jdbc.prefix:}") String tablePrefix) {
        String table = tablePrefix + "ratelimit";
        String key = escapeReservedWord("key");
        String limit = escapeReservedWord("limit");
        String columns = key + ", counter, " + limit + ", subscription, reset_time";
        this.selectForUpdateSql = "select " + columns + " from " + table + " where " + key + " = ? for update";
        this.selectSqlServerSql = "select " + columns + " from " + table + " with (updlock, holdlock) where " + key + " = ?";
        this.insertSql = "insert into " + table + " (" + columns + ") values (?, ?, ?, ?, ?)";
        this.updateSql = "update " + table + " set counter = ?, " + limit + " = ?, reset_time = ? where " + key + " = ?";
    }

    @Override
    public Single<RateLimit> incrementAndGet(String key, long weight, Supplier<RateLimit> supplier) {
        log.debug("JdbcRateLimitRepository.incrementAndGet({}, {}, {})", key, weight, supplier);

        try {
            return Single.just(incrementWithRetries(key, weight, supplier, System.nanoTime() + TIMEOUT_NANOS));
        } catch (Exception ex) {
            return Single.error(ex);
        }
    }

    private RateLimit incrementWithRetries(String key, long weight, Supplier<RateLimit> supplier, long deadline) throws SQLException {
        for (int attempt = 0; ; attempt++) {
            try {
                RateLimit rateLimit = incrementInTransaction(key, weight, supplier, deadline);
                if (rateLimit == null) {
                    // First request for this key: seed the row (see the class javadoc), then lock and increment it.
                    seed(key, supplier.get(), deadline);
                    rateLimit = incrementInTransaction(key, weight, supplier, deadline);
                }
                if (rateLimit != null) {
                    return rateLimit;
                }
                // The row vanished between the seed and the locking pass (external purge racing us): retry.
            } catch (SQLException e) {
                // A transient rollback is safe to replay: the transaction rolled back entirely.
                if (!isTransientRollback(e) || attempt >= MAX_RETRIES) {
                    throw e;
                }
                log.debug("Retrying the increment of rate limit {} after a transient rollback (attempt {})", key, attempt + 1, e);
            }
            if (attempt >= MAX_RETRIES) {
                throw new SQLException("Rate limit '" + key + "' could not be incremented after " + (attempt + 1) + " attempts");
            }
        }
    }

    /**
     * Lock the row for {@code key}, reset it if its window has expired, increment it and write it back, all in
     * one transaction. Returns {@code null}, after releasing the (range) lock via rollback, when no row exists
     * yet, so the caller can seed it lock-free.
     */
    private RateLimit incrementInTransaction(String key, long weight, Supplier<RateLimit> supplier, long deadline) throws SQLException {
        final DataSource dataSource = jdbcTemplate.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            final boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                RateLimit rateLimit = selectForUpdate(connection, key, deadline);
                if (rateLimit == null) {
                    // Nothing read, nothing written: roll back to release the range lock the empty
                    // locking SELECT may hold, so the caller's seed INSERT cannot deadlock on it.
                    connection.rollback();
                    return null;
                }

                if (rateLimit.getResetTime() < System.currentTimeMillis()) {
                    rateLimit = supplier.get();
                }
                rateLimit.setCounter(rateLimit.getCounter() + weight);

                update(connection, key, rateLimit, deadline);
                connection.commit();
                return rateLimit;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    e.addSuppressed(rollbackFailure);
                }
                throw e;
            } finally {
                restoreAutoCommit(connection, previousAutoCommit);
            }
        }
    }

    /**
     * Insert the supplied rate limit for {@code key} in auto-commit, tolerating the duplicate key a concurrent
     * seeder loses with: either way the row exists (and is committed) afterwards.
     */
    private void seed(String key, RateLimit rateLimit, long deadline) throws SQLException {
        final DataSource dataSource = jdbcTemplate.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            final boolean previousAutoCommit = connection.getAutoCommit();
            // The seed must be committed and visible before the locking pass, whatever the pool's default.
            connection.setAutoCommit(true);
            try {
                insert(connection, key, rateLimit, deadline);
            } catch (SQLException e) {
                if (!isIntegrityViolation(e)) {
                    throw e;
                }
                // Another request seeded the row first; the locking pass will increment it.
            } finally {
                restoreAutoCommit(connection, previousAutoCommit);
            }
        }
    }

    /**
     * Best effort: the pool resets the connection anyway, and a failure here must neither hide the error being
     * thrown nor fail an increment that is already committed.
     */
    private static void restoreAutoCommit(Connection connection, boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            log.debug("Unable to restore the auto-commit mode of a rate limit connection", e);
        }
    }

    private RateLimit selectForUpdate(Connection connection, String key, long deadline) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(isSqlServer() ? selectSqlServerSql : selectForUpdateSql)) {
            ps.setQueryTimeout(remainingSeconds(key, deadline));
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                RateLimit rateLimit = new RateLimit(rs.getString(1));
                rateLimit.setCounter(rs.getLong(2));
                rateLimit.setLimit(rs.getLong(3));
                rateLimit.setSubscription(rs.getString(4));
                rateLimit.setResetTime(rs.getLong(5));
                return rateLimit;
            }
        }
    }

    private void insert(Connection connection, String key, RateLimit rateLimit, long deadline) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            ps.setQueryTimeout(remainingSeconds(key, deadline));
            ps.setString(1, key);
            ps.setLong(2, rateLimit.getCounter());
            ps.setLong(3, rateLimit.getLimit());
            ps.setString(4, rateLimit.getSubscription());
            ps.setLong(5, rateLimit.getResetTime());
            ps.executeUpdate();
        }
    }

    private void update(Connection connection, String key, RateLimit rateLimit, long deadline) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setQueryTimeout(remainingSeconds(key, deadline));
            ps.setLong(1, rateLimit.getCounter());
            ps.setLong(2, rateLimit.getLimit());
            ps.setLong(3, rateLimit.getResetTime());
            ps.setString(4, key);
            ps.executeUpdate();
        }
    }

    /** What is left of the call's budget, rounded up to the second as JDBC query timeouts are; fails once it is spent. */
    private static int remainingSeconds(String key, long deadline) throws SQLTimeoutException {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
            throw new SQLTimeoutException("Rate limit '" + key + "' could not be incremented within the 5 seconds budget");
        }
        return (int) Math.max(1, (remaining + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1));
    }

    private static boolean isIntegrityViolation(SQLException e) {
        for (SQLException current = e; current != null; current = current.getNextException()) {
            String sqlState = current.getSQLState();
            if (sqlState != null && sqlState.startsWith(INTEGRITY_VIOLATION_SQLSTATE_CLASS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTransientRollback(SQLException e) {
        for (SQLException current = e; current != null; current = current.getNextException()) {
            String sqlState = current.getSQLState();
            if (sqlState != null && sqlState.startsWith(TRANSIENT_ROLLBACK_SQLSTATE_CLASS)) {
                return true;
            }
        }
        return false;
    }
}
