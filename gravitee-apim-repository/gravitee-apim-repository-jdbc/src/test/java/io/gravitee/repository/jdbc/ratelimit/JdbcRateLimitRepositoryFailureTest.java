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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.ratelimit.model.RateLimit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Covers how {@link JdbcRateLimitRepository} handles database failures: the seed/lock handshake on a first
 * request, the bounded retry around it and the connection clean-up.
 *
 * <p>These paths are driven by errors a container-backed test can only produce by racing (a duplicate key from
 * a concurrent seeder, an InnoDB deadlock), so they are injected deterministically against a stubbed JDBC
 * layer. Behaviour against real databases stays in {@link JdbcRateLimitRepositoryConcurrencyTest}.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class JdbcRateLimitRepositoryFailureTest {

    private static final String KEY = "k";
    private static final String SUBSCRIPTION = "sub";
    private static final long COUNTER = 7L;
    private static final long LIMIT = 10L;

    /** SQLState of an InnoDB/range-lock deadlock: transient, safe to replay. */
    private static final String DEADLOCK_SQLSTATE = "40001";

    /** SQLState of a primary-key clash: the concurrent seeder that lost. */
    private static final String DUPLICATE_KEY_SQLSTATE = "23505";

    /** SQLState of a connection failure: neither transient nor an integrity violation, so never replayed. */
    private static final String CONNECTION_FAILURE_SQLSTATE = "08006";

    private final DataSource dataSource = mock(DataSource.class);
    private final FakeDatabase database = new FakeDatabase();
    private final JdbcRateLimitRepository repository = new JdbcRateLimitRepository("");
    private final AtomicInteger supplierCalls = new AtomicInteger();

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenAnswer(database);
        ReflectionTestUtils.setField(repository, "jdbcTemplate", new JdbcTemplate(dataSource));
    }

    @Test
    void seeds_the_row_outside_the_transaction_then_increments_it_on_first_request() {
        RateLimit rateLimit = increment();

        assertThat(rateLimit.getCounter()).isEqualTo(COUNTER + 1);
        assertThat(database.insertCount).isEqualTo(1);
        // One locking SELECT that finds nothing, one that finds the seeded row.
        assertThat(database.selectCount).isEqualTo(2);
        assertThat(database.updateCount).isEqualTo(1);
        assertThat(supplierCalls).hasValue(1);
    }

    @Test
    void rolls_back_the_empty_locking_select_before_seeding() {
        increment();

        // The range lock the empty SELECT may hold must be released before the seed INSERT runs,
        // otherwise concurrent first requests deadlock on each other.
        assertThat(database.rollbackCount).isEqualTo(1);
        assertThat(database.rolledBackBeforeFirstInsert).isTrue();
    }

    @Test
    void increments_an_existing_row_without_calling_the_supplier() {
        database.rowPresent = true;

        RateLimit rateLimit = increment();

        assertThat(rateLimit.getCounter()).isEqualTo(COUNTER + 1);
        assertThat(rateLimit.getSubscription()).isEqualTo(SUBSCRIPTION);
        assertThat(database.insertCount).isZero();
        assertThat(supplierCalls).hasValue(0);
    }

    @Test
    void tolerates_a_concurrent_seeder_winning_the_insert() {
        database.insertFailures.add(sqlException(DUPLICATE_KEY_SQLSTATE));

        RateLimit rateLimit = increment();

        assertThat(rateLimit.getCounter()).isEqualTo(COUNTER + 1);
        // The duplicate key is swallowed: the row exists either way, so the locking pass increments it.
        assertThat(database.insertCount).isEqualTo(1);
        assertThat(database.updateCount).isEqualTo(1);
    }

    @Test
    void rethrows_a_seed_failure_that_is_not_a_duplicate_key() {
        database.insertFailures.add(sqlException(CONNECTION_FAILURE_SQLSTATE));

        assertThat(rootFailureOf(this::increment).getSQLState()).isEqualTo(CONNECTION_FAILURE_SQLSTATE);
        assertThat(database.insertCount).isEqualTo(1);
    }

    @Test
    void retries_a_deadlocked_transaction_and_succeeds() {
        database.rowPresent = true;
        database.selectFailures.add(sqlException(DEADLOCK_SQLSTATE));

        RateLimit rateLimit = increment();

        assertThat(rateLimit.getCounter()).isEqualTo(COUNTER + 1);
        assertThat(database.selectCount).isEqualTo(2);
        assertThat(database.rollbackCount).isEqualTo(1);
    }

    @Test
    void does_not_retry_a_failure_that_is_not_transient() {
        database.rowPresent = true;
        database.selectFailures.add(sqlException(CONNECTION_FAILURE_SQLSTATE));

        assertThat(rootFailureOf(this::increment).getSQLState()).isEqualTo(CONNECTION_FAILURE_SQLSTATE);
        assertThat(database.selectCount).isEqualTo(1);
    }

    @Test
    void gives_up_after_a_bounded_number_of_deadlocks() {
        database.rowPresent = true;
        database.alwaysDeadlock = true;

        assertThat(rootFailureOf(this::increment).getSQLState()).isEqualTo(DEADLOCK_SQLSTATE);
        // The initial attempt plus MAX_RETRIES, then the deadlock propagates rather than looping forever.
        assertThat(database.selectCount).isEqualTo(6);
    }

    @Test
    void gives_up_when_the_row_keeps_vanishing_between_the_seed_and_the_lock() {
        // Simulates an external purge deleting the row as fast as it is seeded.
        database.discardInserts = true;

        assertThat(rootFailureOf(this::increment).getMessage()).contains("could not be incremented after 6 attempts");
        assertThat(database.insertCount).isEqualTo(6);
    }

    @Test
    void keeps_the_transaction_failure_when_the_rollback_fails_too() {
        database.rowPresent = true;
        database.selectFailures.add(sqlException(CONNECTION_FAILURE_SQLSTATE));
        database.failRollbacks = true;

        SQLException failure = rootFailureOf(this::increment);

        assertThat(failure.getSQLState()).isEqualTo(CONNECTION_FAILURE_SQLSTATE);
        assertThat(failure.getSuppressed()).hasSize(1);
    }

    @Test
    void does_not_fail_a_committed_increment_when_the_auto_commit_cannot_be_restored() {
        database.rowPresent = true;
        database.failAutoCommitRestore = true;

        assertThat(increment().getCounter()).isEqualTo(COUNTER + 1);
        assertThat(database.updateCount).isEqualTo(1);
    }

    @Test
    void bounds_every_statement_by_the_five_seconds_budget() {
        increment();

        assertThat(database.queryTimeouts)
            .isNotEmpty()
            .allSatisfy(timeout -> assertThat(timeout).isBetween(1, 5));
    }

    private RateLimit increment() {
        return repository.incrementAndGet(KEY, 1, window()).blockingGet();
    }

    private Supplier<RateLimit> window() {
        return () -> {
            supplierCalls.incrementAndGet();
            RateLimit rateLimit = new RateLimit(KEY);
            rateLimit.setCounter(COUNTER);
            rateLimit.setLimit(LIMIT);
            rateLimit.setSubscription(SUBSCRIPTION);
            rateLimit.setResetTime(System.currentTimeMillis() + 60_000);
            return rateLimit;
        };
    }

    private static SQLException sqlException(String sqlState) {
        return new SQLException("stubbed " + sqlState, sqlState);
    }

    /** Unwrap the RxJava wrapper {@code blockingGet} adds around a checked exception. */
    private static SQLException rootFailureOf(ThrowingCallable callable) {
        Throwable thrown = catchThrowable(callable);
        assertThat(thrown).isNotNull();
        Throwable root = thrown;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertThat(root).isInstanceOf(SQLException.class);
        return (SQLException) root;
    }

    /**
     * Minimal stand-in for the rate-limit table: one optional row, plus hooks to make the next statement or the
     * connection clean-up fail the way a real database would.
     */
    private static final class FakeDatabase implements Answer<Connection> {

        private final Deque<SQLException> selectFailures = new ArrayDeque<>();
        private final Deque<SQLException> insertFailures = new ArrayDeque<>();
        private final List<Integer> queryTimeouts = new ArrayList<>();

        private boolean rowPresent;
        private boolean alwaysDeadlock;
        private boolean discardInserts;
        private boolean failRollbacks;
        private boolean failAutoCommitRestore;
        private boolean rolledBackBeforeFirstInsert;

        private int selectCount;
        private int insertCount;
        private int updateCount;
        private int rollbackCount;

        @Override
        public Connection answer(InvocationOnMock invocation) throws SQLException {
            Connection connection = mock(Connection.class);
            when(connection.getAutoCommit()).thenReturn(true);
            doAnswer(ignored -> {
                rollbackCount++;
                rolledBackBeforeFirstInsert = insertCount == 0;
                if (failRollbacks) {
                    throw sqlException(CONNECTION_FAILURE_SQLSTATE);
                }
                return null;
            })
                .when(connection)
                .rollback();
            if (failAutoCommitRestore) {
                // Turning auto-commit off to start the transaction works, restoring it afterwards fails.
                doAnswer(call -> {
                    if ((boolean) call.getArgument(0)) {
                        throw sqlException(CONNECTION_FAILURE_SQLSTATE);
                    }
                    return null;
                })
                    .when(connection)
                    .setAutoCommit(anyBoolean());
            }
            when(connection.prepareStatement(anyString())).thenAnswer(statement -> prepare(statement.getArgument(0)));
            return connection;
        }

        private PreparedStatement prepare(String sql) throws SQLException {
            PreparedStatement statement = mock(PreparedStatement.class);
            doAnswer(call -> queryTimeouts.add(call.getArgument(0)))
                .when(statement)
                .setQueryTimeout(anyInt());
            if (sql.startsWith("select")) {
                when(statement.executeQuery()).thenAnswer(ignored -> executeSelect());
            } else if (sql.startsWith("insert")) {
                when(statement.executeUpdate()).thenAnswer(ignored -> executeInsert());
            } else {
                when(statement.executeUpdate()).thenAnswer(ignored -> {
                    updateCount++;
                    return 1;
                });
            }
            return statement;
        }

        private ResultSet executeSelect() throws SQLException {
            selectCount++;
            if (alwaysDeadlock) {
                throw sqlException(DEADLOCK_SQLSTATE);
            }
            if (!selectFailures.isEmpty()) {
                throw selectFailures.poll();
            }
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.next()).thenReturn(rowPresent);
            when(resultSet.getString(1)).thenReturn(KEY);
            when(resultSet.getLong(2)).thenReturn(COUNTER);
            when(resultSet.getLong(3)).thenReturn(LIMIT);
            when(resultSet.getString(4)).thenReturn(SUBSCRIPTION);
            when(resultSet.getLong(5)).thenReturn(System.currentTimeMillis() + 60_000);
            return resultSet;
        }

        private int executeInsert() throws SQLException {
            insertCount++;
            if (!insertFailures.isEmpty()) {
                SQLException failure = insertFailures.poll();
                // A duplicate key means another seeder committed the row first.
                rowPresent = DUPLICATE_KEY_SQLSTATE.equals(failure.getSQLState());
                throw failure;
            }
            rowPresent = !discardInserts;
            return 1;
        }
    }
}
