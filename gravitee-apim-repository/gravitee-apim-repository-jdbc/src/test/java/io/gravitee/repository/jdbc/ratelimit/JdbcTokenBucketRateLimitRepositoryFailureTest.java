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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.ratelimit.api.TokenBucketConsumeResult;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Covers how {@link JdbcTokenBucketRateLimitRepository} handles database failures: the seed/lock
 * handshake on a first request and the bounded retry around it.
 *
 * <p>These paths are driven by errors a container-backed test can only produce by racing — a
 * duplicate key from a concurrent seeder, an InnoDB deadlock — which is precisely the flakiness the
 * seed-outside-the-lock design removes. Here they are injected deterministically against a stubbed
 * JDBC layer. Functional behaviour against real databases stays in
 * {@link JdbcTokenBucketRateLimitRepositoryTest}.
 */
class JdbcTokenBucketRateLimitRepositoryFailureTest {

    private static final String KEY = "k";
    private static final String SUBSCRIPTION = "sub";
    private static final long CAPACITY = 10L;
    private static final long NOW = 1_000L;

    /** SQLState of an InnoDB/range-lock deadlock: transient, safe to replay. */
    private static final String DEADLOCK_SQLSTATE = "40001";

    /** SQLState of a primary-key clash: the concurrent seeder that lost. */
    private static final String DUPLICATE_KEY_SQLSTATE = "23505";

    /** SQLState of a connection failure: neither transient nor an integrity violation, so never replayed. */
    private static final String CONNECTION_FAILURE_SQLSTATE = "08006";

    private final DataSource dataSource = mock(DataSource.class);
    private final FakeDatabase database = new FakeDatabase();
    private final JdbcTokenBucketRateLimitRepository repository = new JdbcTokenBucketRateLimitRepository("");

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenAnswer(database);
        ReflectionTestUtils.setField(repository, "jdbcTemplate", new JdbcTemplate(dataSource));
    }

    @Test
    void seeds_the_row_outside_the_transaction_then_consumes_it_on_first_request() {
        TokenBucketConsumeResult result = consume();

        assertThat(result.allowed()).isTrue();
        assertThat(database.insertCount).isEqualTo(1);
        // One locking SELECT that finds nothing, one that finds the seeded row.
        assertThat(database.selectCount).isEqualTo(2);
        assertThat(database.updateCount).isEqualTo(1);
    }

    @Test
    void rolls_back_the_empty_locking_select_before_seeding() {
        consume();

        // The range lock the empty SELECT may hold must be released before the seed INSERT runs,
        // otherwise concurrent first requests deadlock on each other.
        assertThat(database.rollbackCount).isEqualTo(1);
        assertThat(database.rolledBackBeforeFirstInsert).isTrue();
    }

    @Test
    void tolerates_a_concurrent_seeder_winning_the_insert() {
        database.insertFailures.add(sqlException(DUPLICATE_KEY_SQLSTATE));

        TokenBucketConsumeResult result = consume();

        assertThat(result.allowed()).isTrue();
        // The duplicate key is swallowed: the row exists either way, so the locking pass consumes it.
        assertThat(database.insertCount).isEqualTo(1);
        assertThat(database.updateCount).isEqualTo(1);
    }

    @Test
    void rethrows_a_seed_failure_that_is_not_a_duplicate_key() {
        database.insertFailures.add(sqlException(CONNECTION_FAILURE_SQLSTATE));

        assertThat(rootFailureOf(this::consume).getSQLState()).isEqualTo(CONNECTION_FAILURE_SQLSTATE);
        assertThat(database.insertCount).isEqualTo(1);
    }

    @Test
    void retries_a_deadlocked_transaction_and_succeeds() {
        database.rowPresent = true;
        database.selectFailures.add(sqlException(DEADLOCK_SQLSTATE));

        TokenBucketConsumeResult result = consume();

        assertThat(result.allowed()).isTrue();
        assertThat(database.selectCount).isEqualTo(2);
        assertThat(database.rollbackCount).isEqualTo(1);
    }

    @Test
    void does_not_retry_a_failure_that_is_not_transient() {
        database.rowPresent = true;
        database.selectFailures.add(sqlException(CONNECTION_FAILURE_SQLSTATE));

        assertThat(rootFailureOf(this::consume).getSQLState()).isEqualTo(CONNECTION_FAILURE_SQLSTATE);
        assertThat(database.selectCount).isEqualTo(1);
    }

    @Test
    void gives_up_after_a_bounded_number_of_deadlocks() {
        database.rowPresent = true;
        database.alwaysDeadlock = true;

        assertThat(rootFailureOf(this::consume).getSQLState()).isEqualTo(DEADLOCK_SQLSTATE);
        // The initial attempt plus MAX_RETRIES, then the deadlock propagates rather than looping forever.
        assertThat(database.selectCount).isEqualTo(6);
    }

    @Test
    void gives_up_when_the_row_keeps_vanishing_between_the_seed_and_the_lock() {
        // Simulates an external purge deleting the row as fast as it is seeded.
        database.discardInserts = true;

        assertThat(rootFailureOf(this::consume).getMessage()).contains("could not be consumed after 6 attempts");
        assertThat(database.insertCount).isEqualTo(6);
    }

    @Test
    void rejects_invalid_arguments_before_touching_the_database() {
        assertThatThrownBy(() -> repository.refillAndTryConsume(KEY, 1, 1, 0, CAPACITY, NOW, seed()).blockingGet()).isInstanceOf(
            IllegalArgumentException.class
        );

        // A contract violation must not leave a seeded row behind.
        verifyNoInteractions(dataSource);
    }

    private TokenBucketConsumeResult consume() {
        return repository.refillAndTryConsume(KEY, 1, 0, 1_000L, CAPACITY, NOW, seed()).blockingGet();
    }

    private Supplier<TokenBucket> seed() {
        return () -> {
            TokenBucket bucket = new TokenBucket(KEY);
            bucket.setSubscription(SUBSCRIPTION);
            return bucket;
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
     * Minimal stand-in for the token-bucket table: one optional row, plus hooks to make the next
     * statement fail the way a real database would.
     */
    private static final class FakeDatabase implements Answer<Connection> {

        private final Deque<SQLException> selectFailures = new ArrayDeque<>();
        private final Deque<SQLException> insertFailures = new ArrayDeque<>();

        private boolean rowPresent;
        private boolean alwaysDeadlock;
        private boolean discardInserts;
        private boolean rolledBackBeforeFirstInsert;

        private int selectCount;
        private int insertCount;
        private int updateCount;
        private int rollbackCount;

        @Override
        public Connection answer(InvocationOnMock invocation) throws SQLException {
            Connection connection = mock(Connection.class);
            doAnswer(ignored -> {
                rollbackCount++;
                rolledBackBeforeFirstInsert = insertCount == 0;
                return null;
            })
                .when(connection)
                .rollback();
            when(connection.prepareStatement(anyString())).thenAnswer(statement -> prepare(statement.getArgument(0)));
            return connection;
        }

        private PreparedStatement prepare(String sql) throws SQLException {
            PreparedStatement statement = mock(PreparedStatement.class);
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
            when(resultSet.getLong(2)).thenReturn(CAPACITY);
            when(resultSet.getLong(3)).thenReturn(NOW);
            when(resultSet.getString(4)).thenReturn(SUBSCRIPTION);
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
