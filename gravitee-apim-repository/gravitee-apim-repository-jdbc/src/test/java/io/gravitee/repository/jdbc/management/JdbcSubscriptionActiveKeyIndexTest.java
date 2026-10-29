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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import io.gravitee.repository.jdbc.management.JdbcSubscriptionActiveKeyIndex.State;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The repository writes the hash only once a valid unique index on it exists, on the table of this installation: never
 * while it is missing or does not enforce uniqueness, and no more than a minute after it is dropped. Until the column
 * exists, the repository does not write it at all.
 *
 * @author GraviteeSource Team
 */
public class JdbcSubscriptionActiveKeyIndexTest extends AbstractManagementRepositoryTest {

    private static final String COLUMN = JdbcSubscriptionActiveKeyIndex.COLUMN;
    private static final String OTHER_SCHEMA = "akh_other_installation";

    @Inject
    private DataSource dataSource;

    @Inject
    private Properties graviteeProperties;

    @Inject
    private PlatformTransactionManager graviteeTransactionManager;

    private JdbcTemplate jdbcTemplate;

    private final AtomicLong now = new AtomicLong(System.currentTimeMillis());

    private String subscriptionsTable;

    private String indexName;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Before
    public void init() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        subscriptionsTable = prefix() + "subscriptions";
        indexName = "idx_" + prefix() + "subscriptions_akh";
    }

    /**
     * Puts the column and the expected index back for the other tests sharing the database, whatever the outcome.
     */
    @After
    public void restoreIndex() throws Exception {
        jdbcTemplate.update("delete from " + subscriptionsTable);
        quietly("drop schema " + OTHER_SCHEMA + " cascade");
        dropIndexQuietly();
        jdbcTemplate.update("delete from " + prefix() + "databasechangelog where id like '4.12.27_%'");
        try (Connection connection = dataSource.getConnection()) {
            new Liquibase(
                "liquibase/master.yml",
                new ClassLoaderResourceAccessor(this.getClass().getClassLoader()),
                new JdbcConnection(connection)
            ).update((Contexts) null);
        }
    }

    @Test
    public void should_be_ready_once_the_index_exists() {
        assertThat(newGate(prefix()).state()).isEqualTo(State.READY);
    }

    @Test
    public void should_not_be_ready_while_the_index_does_not_exist() {
        dropIndexQuietly();

        assertThat(newGate(prefix()).state()).isEqualTo(State.NOT_READY);
    }

    @Test
    public void should_report_the_column_missing_until_the_changesets_ran() {
        dropIndexQuietly();
        jdbcTemplate.execute("alter table " + subscriptionsTable + " drop column " + COLUMN);

        assertThat(newGate(prefix()).state()).isEqualTo(State.COLUMN_MISSING);
    }

    @Test
    public void should_not_be_ready_on_an_index_of_the_same_name_that_is_not_unique() {
        dropIndexQuietly();
        jdbcTemplate.execute("create index " + indexName + " on " + subscriptionsTable + " (" + COLUMN + ")");

        assertThat(newGate(prefix()).state()).isEqualTo(State.NOT_READY);
    }

    @Test
    public void should_stop_being_ready_within_a_minute_of_the_index_being_dropped() {
        var gate = newGate(prefix());
        assertThat(gate.state()).isEqualTo(State.READY);

        dropIndexQuietly();

        now.addAndGet(JdbcSubscriptionActiveKeyIndex.CHECK_INTERVAL.toMillis() - 1);
        assertThat(gate.state()).as("until the next check").isEqualTo(State.READY);
        now.addAndGet(1);
        assertThat(gate.state()).as("at the next check").isEqualTo(State.NOT_READY);
    }

    @Test
    public void should_not_count_the_index_of_another_installation_in_another_schema() {
        assumeTrue(isPostgreSql());
        jdbcTemplate.execute("create schema " + OTHER_SCHEMA);
        jdbcTemplate.execute("create table " + OTHER_SCHEMA + "." + subscriptionsTable + " (id varchar(64), " + COLUMN + " varchar(64))");
        jdbcTemplate.execute("create unique index on " + OTHER_SCHEMA + "." + subscriptionsTable + " (" + COLUMN + ")");
        dropIndexQuietly();

        assertThat(newGate(prefix()).state()).isEqualTo(State.NOT_READY);
    }

    @Test
    public void should_not_be_ready_on_an_index_left_invalid_by_an_interrupted_concurrent_creation() throws Exception {
        assumeTrue(isPostgreSql());
        dropIndexQuietly();
        subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        subscriptionRepository.create(aSubscription("sub-2", "plan-2"));
        jdbcTemplate.update("update " + subscriptionsTable + " set " + COLUMN + " = 'same'");
        // Fails on the duplicates, leaving the index behind, invalid: it does not refuse anything.
        quietly(
            "create unique index concurrently " +
                indexName +
                " on " +
                subscriptionsTable +
                " (" +
                COLUMN +
                ") where " +
                COLUMN +
                " is not null"
        );
        jdbcTemplate.update("update " + subscriptionsTable + " set " + COLUMN + " = null");

        assertThat(
            jdbcTemplate.queryForList("select indexrelid from pg_index where not indisvalid and indexrelid = to_regclass(?)", indexName)
        ).hasSize(1);
        assertThat(newGate(prefix()).state()).isEqualTo(State.NOT_READY);
    }

    @Test
    public void should_check_outside_the_transaction_of_the_caller() {
        // On PostgreSQL a failed statement aborts the transaction it runs in.
        assumeTrue(isPostgreSql());
        var transaction = new TransactionTemplate(graviteeTransactionManager);

        transaction.executeWithoutResult(status -> {
            try {
                subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
                // An invalid table name makes the check fail.
                assertThat(newGate("\"").state()).isNotEqualTo(State.READY);
                subscriptionRepository.create(aSubscription("sub-2", "plan-2"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(jdbcTemplate.queryForList("select id from " + subscriptionsTable + " order by id", String.class)).containsExactly(
            "sub-1",
            "sub-2"
        );
    }

    @Test
    public void should_write_no_hash_while_the_index_is_not_ready() throws Exception {
        dropIndexQuietly();

        repositoryWith(newGate(prefix())).create(aSubscription("sub-1", "plan-1"));

        assertThat(jdbcTemplate.queryForList("select " + COLUMN + " from " + subscriptionsTable, String.class)).containsExactly(
            (String) null
        );
    }

    @Test
    public void should_write_subscriptions_without_the_column_until_the_changesets_ran() throws Exception {
        // Nodes with jdbc.liquibase false, or started before the node running the changesets.
        dropIndexQuietly();
        jdbcTemplate.execute("alter table " + subscriptionsTable + " drop column " + COLUMN);
        var repository = repositoryWith(newGate(prefix()));

        var subscription = repository.create(aSubscription("sub-1", "plan-1"));
        subscription.setStatus(Subscription.Status.PAUSED);
        repository.update(subscription);

        assertThat(repository.findById("sub-1")).get().extracting(Subscription::getStatus).isEqualTo(Subscription.Status.PAUSED);
    }

    private JdbcSubscriptionActiveKeyIndex newGate(String tablePrefix) {
        return new JdbcSubscriptionActiveKeyIndex(jdbcTemplate, tablePrefix, now::get);
    }

    private JdbcSubscriptionRepository repositoryWith(JdbcSubscriptionActiveKeyIndex gate) {
        var repository = new JdbcSubscriptionRepository(prefix(), graviteeTransactionManager);
        ReflectionTestUtils.setField(repository, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(repository, "activeKeyIndex", gate);
        return repository;
    }

    private void dropIndexQuietly() {
        quietly(isPostgreSql() ? "drop index " + indexName : "drop index " + indexName + " on " + subscriptionsTable);
    }

    private void quietly(String statement) {
        try {
            jdbcTemplate.execute(statement);
        } catch (Exception e) {
            // Nothing to drop, or failing on purpose.
        }
    }

    private boolean isPostgreSql() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
        } catch (Exception e) {
            throw new IllegalStateException(e);
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
