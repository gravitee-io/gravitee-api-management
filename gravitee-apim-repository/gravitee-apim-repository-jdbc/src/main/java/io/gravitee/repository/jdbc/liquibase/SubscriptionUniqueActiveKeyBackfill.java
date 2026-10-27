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
package io.gravitee.repository.jdbc.liquibase;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static io.gravitee.repository.jdbc.management.JdbcSubscriptionRepository.UNIQUE_ACTIVE_KEY_COLUMN;

import io.gravitee.repository.management.api.SubscriptionUniqueActiveKey;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.CustomLog;

/**
 * Recomputes the {@code unique_active_key} of every subscription from the columns it derives from, the only source of
 * truth: nodes of the previous versions neither set the column on insert nor touch it on update, so it may be missing
 * or stale (left on a closed subscription, or on the plan a subscription has been transferred from).
 * <p>
 * Subscriptions are read in batches, in id order, and each batch is committed on its own: a single transaction would
 * lock active subscriptions for the whole run, while nodes of the previous version keep serving requests. A key is only
 * written if the subscription still holds the values it has been computed from. A subscription changed in the meantime
 * is skipped: a node of this version wrote its up-to-date key along with the change, while a node of a previous
 * version left the key as it was, missing or stale, until the next repair. Running it again gives the same result.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class SubscriptionUniqueActiveKeyBackfill implements CustomTaskChange {

    public static final int BATCH_SIZE = 1000;

    private String tablePrefix = "";

    /**
     * Liquibase only hands a changeset parameter to a custom change that has both a getter and a setter for it.
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix == null ? "" : tablePrefix;
    }

    /**
     * Never fails, as the node would not start. Liquibase then records the changeset as run, and the subscriptions the
     * backfill has not reached keep their missing or stale key until a repair runs again.
     */
    @Override
    public void execute(Database database) throws CustomChangeException {
        Connection connection = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
        try {
            repair(connection);
        } catch (Exception e) {
            log.error(
                "Unexpected error while backfilling the unique active key of subscriptions: the subscriptions not reached keep a missing or stale key",
                e
            );
            rollback(connection);
        }
    }

    private static void rollback(Connection connection) {
        try {
            if (!connection.getAutoCommit()) {
                // PostgreSQL refuses any further statement in a failed transaction, Liquibase's included.
                connection.rollback();
            }
        } catch (SQLException e) {
            log.error("Unable to roll back the failed batch of the unique active key backfill", e);
        }
    }

    /**
     * @return the number of subscriptions whose key has been fixed.
     */
    public int repair(Connection connection) throws SQLException {
        String table = tablePrefix + "subscriptions";
        String select =
            "select id, status, reference_type, reference_id, api, application, " +
            escapeReservedWord("plan") +
            ", type, configuration, " +
            UNIQUE_ACTIVE_KEY_COLUMN +
            " from " +
            table +
            " where id > ? order by id";

        int scanned = 0;
        int fixed = 0;
        // Every id sorts after the empty string.
        String lastId = "";
        int read;
        do {
            List<Row> batch = readBatch(connection, select, lastId);
            read = batch.size();
            if (read > 0) {
                fixed += repairBatch(connection, table, batch);
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }
                scanned += read;
                lastId = batch.getLast().id();
            }
        } while (read == BATCH_SIZE);

        log.info("Unique active key of subscriptions backfilled: {} scanned, {} fixed", scanned, fixed);
        return fixed;
    }

    private static List<Row> readBatch(Connection connection, String select, String afterId) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(select) // NOSONAR only the configured table prefix is concatenated, the id is bound
        ) {
            statement.setMaxRows(BATCH_SIZE);
            statement.setFetchSize(BATCH_SIZE);
            statement.setString(1, afterId);
            List<Row> rows = new ArrayList<>(BATCH_SIZE);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(toRow(resultSet));
                }
            }
            return rows;
        }
    }

    private static Row toRow(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");
        String currentKey = resultSet.getString(UNIQUE_ACTIVE_KEY_COLUMN);
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setReferenceId(resultSet.getString("reference_id"));
        subscription.setApi(resultSet.getString("api"));
        subscription.setApplication(resultSet.getString("application"));
        subscription.setPlan(resultSet.getString("plan"));
        subscription.setConfiguration(resultSet.getString("configuration"));
        try {
            subscription.setStatus(valueOf(Subscription.Status.class, resultSet.getString("status")));
            subscription.setReferenceType(valueOf(SubscriptionReferenceType.class, resultSet.getString("reference_type")));
            subscription.setType(valueOf(Subscription.Type.class, resultSet.getString("type")));
        } catch (IllegalArgumentException e) {
            // The repository cannot read this subscription either.
            log.warn("Unique active key of subscription [{}] not backfilled: {}", id, e.getMessage());
            return new Row(id, null, currentKey);
        }
        return new Row(id, subscription, currentKey);
    }

    /**
     * Updates are batched per statement: the condition on the unchanged columns depends on which of them are null.
     */
    private static int repairBatch(Connection connection, String table, List<Row> batch) throws SQLException {
        Map<String, List<Row>> toFix = new LinkedHashMap<>();
        for (Row row : batch) {
            if (row.subscription() == null) {
                continue;
            }
            String expected = SubscriptionUniqueActiveKey.of(row.subscription());
            if (!Objects.equals(expected, row.currentKey())) {
                toFix.computeIfAbsent(updateStatement(table, row.subscription()), sql -> new ArrayList<>()).add(row);
            }
        }

        int fixed = 0;
        for (Map.Entry<String, List<Row>> statementRows : toFix.entrySet()) {
            try (PreparedStatement statement = connection.prepareStatement(statementRows.getKey())) {
                for (Row row : statementRows.getValue()) {
                    Subscription subscription = row.subscription();
                    int index = 1;
                    statement.setString(index++, SubscriptionUniqueActiveKey.of(subscription));
                    statement.setString(index++, subscription.getId());
                    for (String value : unchangedValues(subscription)) {
                        if (value != null) {
                            statement.setString(index++, value);
                        }
                    }
                    statement.addBatch();
                }
                for (int count : statement.executeBatch()) {
                    fixed += count == Statement.SUCCESS_NO_INFO ? 1 : count;
                }
            }
        }
        return fixed;
    }

    private static String updateStatement(String table, Subscription subscription) {
        StringBuilder sql = new StringBuilder("update ")
            .append(table)
            .append(" set ")
            .append(UNIQUE_ACTIVE_KEY_COLUMN)
            .append(" = ? where id = ?");
        List<String> columns = unchangedColumns();
        List<String> values = unchangedValues(subscription);
        for (int i = 0; i < columns.size(); i++) {
            sql.append(" and ").append(columns.get(i)).append(values.get(i) == null ? " is null" : " = ?");
        }
        return sql.toString();
    }

    /**
     * Columns the key derives from that a node may change while the backfill runs: the reference, the application and
     * the type of a subscription never change.
     */
    private static List<String> unchangedColumns() {
        return List.of("status", escapeReservedWord("plan"), "configuration");
    }

    private static List<String> unchangedValues(Subscription subscription) {
        List<String> values = new ArrayList<>(3);
        values.add(subscription.getStatus() == null ? null : subscription.getStatus().name());
        values.add(subscription.getPlan());
        values.add(subscription.getConfiguration());
        return values;
    }

    private static <E extends Enum<E>> E valueOf(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    private record Row(String id, Subscription subscription, String currentKey) {}

    @Override
    public String getConfirmationMessage() {
        return "Unique active key of subscriptions backfilled";
    }

    @Override
    public void setUp() {
        // Nothing to set up.
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // No resource needed.
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
