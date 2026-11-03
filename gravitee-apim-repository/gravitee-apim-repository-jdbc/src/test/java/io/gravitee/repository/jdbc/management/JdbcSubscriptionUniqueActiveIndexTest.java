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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.changelog.FastCheckService;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The unique index on the hash of the unique active key of the subscriptions: created by Liquibase at startup on a
 * column no existing subscription has a value in, so its creation can neither read nor change them.
 *
 * @author GraviteeSource Team
 */
public class JdbcSubscriptionUniqueActiveIndexTest extends AbstractManagementRepositoryTest {

    private static final String COLUMN = "active_key_hash";
    private static final String CHANGESETS = "4.12.27_%";

    @Inject
    private DataSource dataSource;

    @Inject
    private Properties graviteeProperties;

    @Inject
    private PlatformTransactionManager graviteeTransactionManager;

    private JdbcTemplate jdbcTemplate;

    private String subscriptionsTable;

    private String indexName;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @BeforeEach
    public void init() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        subscriptionsTable = prefix() + "subscriptions";
        indexName = "idx_" + prefix() + "subscriptions_akh";
    }

    /**
     * Puts the expected column and index back for the other tests sharing the database, whatever the outcome of the test.
     */
    @AfterEach
    public void restoreIndex() throws Exception {
        jdbcTemplate.update("delete from " + subscriptionsTable);
        dropIndexQuietly();
        dropHashColumnQuietly();
        deleteChangesets(CHANGESETS);
        updateDatabase();
    }

    @Test
    public void should_create_a_unique_index_on_the_hash() throws Exception {
        assertThat(uniqueIndexOnHashExists()).isTrue();
    }

    @Test
    public void should_create_the_index_without_reading_or_changing_subscriptions_duplicated_before_it() throws Exception {
        // As left by 4.12.26: two active subscriptions of an application to a plan, sharing their unique active key.
        subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        subscriptionRepository.create(aSubscription("sub-2", "plan-2"));
        jdbcTemplate.update(
            "update " +
                subscriptionsTable +
                " set " +
                escapeReservedWord("plan") +
                " = 'plan-1', unique_active_key = 'API:api-1:app-1:plan-1'"
        );
        dropIndexQuietly();
        dropHashColumnQuietly();
        deleteChangesets(CHANGESETS);
        var before = subscriptionRows();

        updateDatabase();

        assertThat(subscriptionRows()).isEqualTo(before);
        assertThat(jdbcTemplate.queryForList("select " + COLUMN + " from " + subscriptionsTable, String.class)).containsOnlyNulls();
        assertThat(uniqueIndexOnHashExists()).isTrue();
    }

    @Test
    public void should_not_report_a_not_null_violation_as_a_duplicate_active_subscription() {
        var withoutApplicationName = aSubscription("sub-1", "plan-1");
        withoutApplicationName.setApplicationName(null);

        var failure = catchThrowable(() -> subscriptionRepository.create(withoutApplicationName));

        assertThat(failure).isInstanceOf(TechnicalException.class).isNotInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void should_keep_the_caller_transaction_usable_after_a_duplicate_active_subscription() throws Exception {
        // On PostgreSQL the refused insert would abort the transaction of the request: its later statements, and its
        // commit, would fail, and the caller would get an error instead of plan.subscribed.
        subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        var transaction = new TransactionTemplate(graviteeTransactionManager);

        transaction.executeWithoutResult(status -> {
            var failure = catchThrowable(() -> subscriptionRepository.create(aSubscription("sub-2", "plan-1")));
            assertThat(failure).isInstanceOf(DuplicateKeyException.class);
            try {
                subscriptionRepository.create(aSubscription("sub-3", "plan-2"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(subscriptionRepository.findById("sub-2")).isEmpty();
        assertThat(subscriptionRepository.findById("sub-3")).isPresent();
    }

    private boolean uniqueIndexOnHashExists() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            var metaData = connection.getMetaData();
            for (String table : List.of(subscriptionsTable, subscriptionsTable.toUpperCase(), subscriptionsTable.toLowerCase())) {
                try (ResultSet indexes = metaData.getIndexInfo(connection.getCatalog(), connection.getSchema(), table, true, false)) {
                    while (indexes.next()) {
                        if (COLUMN.equalsIgnoreCase(indexes.getString("COLUMN_NAME")) && !indexes.getBoolean("NON_UNIQUE")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /**
     * Every column but the hash, which the upgrade adds.
     */
    private List<Map<String, Object>> subscriptionRows() {
        var rows = jdbcTemplate.queryForList("select * from " + subscriptionsTable + " order by id");
        rows.forEach(row -> row.remove(COLUMN));
        return rows;
    }

    private void deleteChangesets(String changesets) {
        jdbcTemplate.update("delete from " + prefix() + "databasechangelog where id like ?", changesets);
    }

    private void dropIndexQuietly() {
        try {
            jdbcTemplate.execute(isPostgreSql() ? "drop index " + indexName : "drop index " + indexName + " on " + subscriptionsTable);
        } catch (Exception e) {
            // No such index.
        }
    }

    private void dropHashColumnQuietly() {
        try {
            jdbcTemplate.execute("alter table " + subscriptionsTable + " drop column " + COLUMN);
        } catch (Exception e) {
            // No such column.
        }
    }

    private void updateDatabase() throws Exception {
        // The test setup already ran Liquibase, which remembers that the database is up to date
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (Connection connection = dataSource.getConnection()) {
            new Liquibase(
                "liquibase/master.yml",
                new ClassLoaderResourceAccessor(this.getClass().getClassLoader()),
                new JdbcConnection(connection)
            ).update((Contexts) null);
        }
    }

    private boolean isPostgreSql() throws Exception {
        return databaseProduct().contains("postgresql");
    }

    private String databaseProduct() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase();
        }
    }

    private String prefix() {
        return graviteeProperties.getProperty("management.jdbc.prefix", "");
    }

    private static Subscription aSubscription(String id, String plan) {
        var subscription = new Subscription();
        subscription.setId(id);
        subscription.setEnvironmentId("DEFAULT");
        subscription.setReferenceType(SubscriptionReferenceType.API);
        subscription.setReferenceId("api-1");
        subscription.setApi("api-1");
        subscription.setApplication("app-1");
        subscription.setApplicationName("app-1 name");
        subscription.setPlan(plan);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setCreatedAt(new Date());
        subscription.setUpdatedAt(new Date());
        return subscription;
    }
}
