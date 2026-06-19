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

import io.gravitee.repository.ratelimit.api.TokenBucketCalculator;
import io.gravitee.repository.ratelimit.api.TokenBucketConsumeResult;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import io.reactivex.rxjava3.core.Single;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC-backed {@link TokenBucketRateLimitRepository}. Per request it opens a connection, turns
 * <strong>auto-commit off</strong>, takes a row lock with {@code SELECT ... FOR UPDATE} (a
 * {@code WITH (UPDLOCK, HOLDLOCK)} hint on SQL Server, which lacks ANSI {@code FOR UPDATE}),
 * refills/consumes via {@link TokenBucketCalculator}, writes the row back and commits — all on that
 * same connection. The row lock serialises concurrent requests on a key so the bucket cannot be
 * over-consumed.
 *
 * <p>The connection and transaction are managed explicitly rather than relying on an ambient Spring
 * transaction: a {@code SELECT ... FOR UPDATE} executed in auto-commit mode releases its lock the
 * instant the statement returns, which provides no serialisation at all and silently over-admits
 * under concurrency. Owning the connection here guarantees the lock is held across the whole
 * read-modify-write regardless of how the surrounding context wires its transaction manager.
 *
 * <p>The first request for a key has no row to lock, so a concurrent insert can lose the race with a
 * duplicate-key error; that is retried, and the retry locks the now-existing row.
 */
@CustomLog
@Repository("tokenBucketRateLimitRepository")
public class JdbcTokenBucketRateLimitRepository implements TokenBucketRateLimitRepository<TokenBucket> {

    private static final int MAX_INSERT_RETRIES = 5;

    /** Upper bound on each statement, preserved from the previous transaction-template timeout so a request cannot block indefinitely on the row lock. */
    private static final int STATEMENT_TIMEOUT_SECONDS = 5;

    /** SQLState class 23 == integrity constraint violation; a primary-key clash on first insert lands here. */
    private static final String INTEGRITY_VIOLATION_SQLSTATE_CLASS = "23";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String selectForUpdateSql;
    private final String selectSqlServerSql;
    private final String insertSql;
    private final String updateSql;

    public JdbcTokenBucketRateLimitRepository(@Value("${ratelimit.jdbc.prefix:}") String tablePrefix) {
        String table = tablePrefix + "tokenbucket";
        String key = escapeReservedWord("key");
        String columns = key + ", tokens, last_refill, subscription";
        this.selectForUpdateSql = "select " + columns + " from " + table + " where " + key + " = ? for update";
        this.selectSqlServerSql = "select " + columns + " from " + table + " with (updlock, holdlock) where " + key + " = ?";
        this.insertSql = "insert into " + table + " (" + key + ", tokens, last_refill, subscription) values (?, ?, ?, ?)";
        this.updateSql = "update " + table + " set tokens = ?, last_refill = ?, subscription = ? where " + key + " = ?";
    }

    @Override
    public Single<TokenBucketConsumeResult> refillAndTryConsume(
        String key,
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long nowMillis,
        Supplier<TokenBucket> supplier
    ) {
        return Single.fromCallable(() ->
            consumeWithRetry(key, tokensRequested, refillRate, refillPeriodMillis, capacity, nowMillis, supplier)
        );
    }

    private TokenBucketConsumeResult consumeWithRetry(
        String key,
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long nowMillis,
        Supplier<TokenBucket> supplier
    ) throws SQLException {
        for (int attempt = 0; ; attempt++) {
            try {
                return consumeInTransaction(key, tokensRequested, refillRate, refillPeriodMillis, capacity, nowMillis, supplier);
            } catch (SQLException e) {
                // Another request inserted this key first; retry — the row now exists and the locking
                // SELECT will serialise on it.
                if (isIntegrityViolation(e) && attempt < MAX_INSERT_RETRIES) {
                    continue;
                }
                throw e;
            }
        }
    }

    private TokenBucketConsumeResult consumeInTransaction(
        String key,
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long nowMillis,
        Supplier<TokenBucket> supplier
    ) throws SQLException {
        final DataSource dataSource = jdbcTemplate.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            final boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                TokenBucket bucket = selectForUpdate(connection, key);
                boolean isNew = bucket == null;
                if (isNew) {
                    bucket = TokenBucketCalculator.newFullBucket(supplier.get(), capacity, nowMillis);
                }

                TokenBucketConsumeResult result = TokenBucketCalculator.refillAndTryConsume(
                    bucket,
                    tokensRequested,
                    refillRate,
                    refillPeriodMillis,
                    capacity,
                    nowMillis
                );

                if (isNew) {
                    insert(connection, key, bucket);
                } else {
                    update(connection, key, bucket);
                }
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private TokenBucket selectForUpdate(Connection connection, String key) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(isSqlServer() ? selectSqlServerSql : selectForUpdateSql)) {
            ps.setQueryTimeout(STATEMENT_TIMEOUT_SECONDS);
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                TokenBucket bucket = new TokenBucket(rs.getString(1));
                bucket.setTokens(rs.getLong(2));
                bucket.setLastRefillTime(rs.getLong(3));
                bucket.setSubscription(rs.getString(4));
                return bucket;
            }
        }
    }

    private void insert(Connection connection, String key, TokenBucket bucket) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            ps.setQueryTimeout(STATEMENT_TIMEOUT_SECONDS);
            ps.setString(1, key);
            ps.setLong(2, bucket.getTokens());
            ps.setLong(3, bucket.getLastRefillTime());
            ps.setString(4, bucket.getSubscription());
            ps.executeUpdate();
        }
    }

    private void update(Connection connection, String key, TokenBucket bucket) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setQueryTimeout(STATEMENT_TIMEOUT_SECONDS);
            ps.setLong(1, bucket.getTokens());
            ps.setLong(2, bucket.getLastRefillTime());
            ps.setString(3, bucket.getSubscription());
            ps.setString(4, key);
            ps.executeUpdate();
        }
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
}
