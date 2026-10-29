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

import io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import javax.sql.DataSource;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Whether the unique index on the hash of the unique active key of the subscriptions can protect the writes: the
 * repository writes the hash only then. A hash written while the index is missing, or still being built, could
 * duplicate the hash of another subscription, and the index could not be created anymore without repairing data. Nodes
 * do not always run the Liquibase changesets creating it ({@code jdbc.liquibase: false}, upgrade mode), and some
 * databases build it online, while the table is written.
 * <p>
 * The catalog query is the predicate of the {@code 4.12.27_02} changesets: a valid unique index on the hash alone, on the
 * table of this installation. Checked by the writing thread on the first write, then at most once a minute, on a
 * connection of its own, outside the transaction of the caller: on PostgreSQL a failed statement would abort it.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class JdbcSubscriptionActiveKeyIndex {

    public static final String COLUMN = "active_key_hash";

    enum State {
        /**
         * The changesets did not run yet: the column cannot be written.
         */
        COLUMN_MISSING,
        /**
         * The column exists, the hash cannot be written yet.
         */
        NOT_READY,
        READY,
    }

    static final Duration CHECK_INTERVAL = Duration.ofMinutes(1);
    private static final Duration WARNING_INTERVAL = Duration.ofHours(1);
    private static final int QUERY_TIMEOUT_SECONDS = 10;

    private static final String POSTGRESQL_COLUMN =
        "SELECT COUNT(*) FROM pg_attribute WHERE attrelid = to_regclass(?) AND attname = 'active_key_hash' AND NOT attisdropped";
    private static final String POSTGRESQL_INDEX =
        "SELECT COUNT(*) FROM pg_index i WHERE i.indrelid = to_regclass(?)" +
        " AND i.indisunique AND i.indisvalid AND i.indisready AND i.indimmediate AND i.indnkeyatts = 1 AND i.indexprs IS NULL" +
        " AND i.indkey[0] = (SELECT a.attnum FROM pg_attribute a WHERE a.attrelid = i.indrelid AND a.attname = 'active_key_hash')" +
        " AND (i.indpred IS NULL OR pg_get_expr(i.indpred, i.indrelid) = '(active_key_hash IS NOT NULL)')";
    private static final String MYSQL_COLUMN =
        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = 'active_key_hash'";
    private static final String MYSQL_INDEX =
        "SELECT COUNT(*) FROM (SELECT index_name FROM information_schema.statistics" +
        " WHERE table_schema = DATABASE() AND table_name = ? GROUP BY index_name" +
        " HAVING MAX(non_unique) = 0 AND COUNT(*) = 1 AND MAX(column_name) = 'active_key_hash' AND MAX(sub_part) IS NULL) usable";
    private static final String MSSQL_COLUMN =
        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID(?) AND name = 'active_key_hash'";
    private static final String MSSQL_INDEX =
        "SELECT COUNT(*) FROM sys.indexes i WHERE i.object_id = OBJECT_ID(?)" +
        " AND i.is_unique = 1 AND i.is_disabled = 0 AND i.is_hypothetical = 0 AND i.has_filter = 1" +
        " AND i.filter_definition = '([active_key_hash] IS NOT NULL)'" +
        " AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.is_included_column = 0) = 1" +
        " AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns col ON col.object_id = ic.object_id AND col.column_id = ic.column_id" +
        " WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND col.name = 'active_key_hash')";

    private final JdbcTemplate jdbcTemplate;
    private final String table;
    private final LongSupplier clock;
    private final AtomicBoolean checking = new AtomicBoolean();
    private volatile boolean checked;
    private volatile State state = State.NOT_READY;
    private volatile long nextCheck;
    private volatile long nextWarning;

    @Autowired
    public JdbcSubscriptionActiveKeyIndex(@Lazy JdbcTemplate jdbcTemplate, @Value("${management.jdbc.prefix:}") String prefix) {
        this(jdbcTemplate, prefix, System::currentTimeMillis);
    }

    JdbcSubscriptionActiveKeyIndex(JdbcTemplate jdbcTemplate, String prefix, LongSupplier clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.table = prefix + "subscriptions";
        this.clock = clock;
    }

    /**
     * Never fails: an index that cannot be checked is not ready.
     */
    State state() {
        if (!checked) {
            // Concurrent first writes all wait for the first check: none of them may write a hash before it.
            synchronized (this) {
                if (!checked) {
                    refresh();
                    checked = true;
                }
            }
        } else if (clock.getAsLong() >= nextCheck && checking.compareAndSet(false, true)) {
            // The other threads keep the previous answer meanwhile.
            try {
                refresh();
            } finally {
                checking.set(false);
            }
        }
        return state;
    }

    private void refresh() {
        State previous = state;
        State current;
        String unreadiness;
        try {
            if (count(columnQuery()) == 0) {
                current = State.COLUMN_MISSING;
                unreadiness = "the column " + COLUMN + " does not exist yet, the Liquibase changesets of 4.12.27 have not run";
            } else if (count(indexQuery()) == 0) {
                current = State.NOT_READY;
                unreadiness = "no valid unique index on " + COLUMN + " exists yet, it is created by the Liquibase changesets of 4.12.27";
            } else {
                current = State.READY;
                unreadiness = null;
            }
        } catch (Exception e) {
            // Unknown: the column is written only when it is known to exist.
            current = previous == State.COLUMN_MISSING ? State.COLUMN_MISSING : State.NOT_READY;
            unreadiness = "it cannot be checked (" + e.getMessage() + ")";
        }
        long now = clock.getAsLong();
        nextCheck = now + CHECK_INTERVAL.toMillis();
        if (current == State.READY) {
            if (previous != State.READY || !checked) {
                log.info(
                    "Unique index on {}.{} is ready: an application cannot have several active subscriptions to the same plan anymore",
                    table,
                    COLUMN
                );
            }
        } else if (previous == State.READY || now >= nextWarning) {
            log.warn(
                "Unique index on {}.{} is not ready, {}: concurrent identical subscription requests may create several active subscriptions of an application to a plan",
                table,
                COLUMN,
                unreadiness
            );
            nextWarning = now + WARNING_INTERVAL.toMillis();
        }
        state = current;
    }

    private int count(String query) throws Exception {
        DataSource dataSource = jdbcTemplate.getDataSource();
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.setString(1, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private static String columnQuery() {
        if (AbstractJdbcRepositoryConfiguration.isMySqlOrMariaDb()) {
            return MYSQL_COLUMN;
        }
        if (AbstractJdbcRepositoryConfiguration.isSqlServer()) {
            return MSSQL_COLUMN;
        }
        return POSTGRESQL_COLUMN;
    }

    private static String indexQuery() {
        if (AbstractJdbcRepositoryConfiguration.isMySqlOrMariaDb()) {
            return MYSQL_INDEX;
        }
        if (AbstractJdbcRepositoryConfiguration.isSqlServer()) {
            return MSSQL_INDEX;
        }
        return POSTGRESQL_INDEX;
    }
}
