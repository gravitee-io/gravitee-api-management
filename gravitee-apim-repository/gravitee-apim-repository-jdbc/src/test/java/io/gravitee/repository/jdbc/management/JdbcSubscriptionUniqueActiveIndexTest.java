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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.DatabaseConfigurationEnum;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.util.Date;
import java.util.Properties;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * Database specific behaviours of the unique index on active subscriptions.
 *
 * @author GraviteeSource Team
 */
public class JdbcSubscriptionUniqueActiveIndexTest extends AbstractManagementRepositoryTest {

    private static final String INDEX_CHANGESET_PREFIX = "4.12.27_01_add_subscriptions_unique_active_key_index";

    @Inject
    private DataSource dataSource;

    @Inject
    private Properties graviteeProperties;

    @Inject
    private PlatformTransactionManager graviteeTransactionManager;

    @Inject
    private JdbcDatabaseContainer<?> jdbcDatabaseContainer;

    private JdbcTemplate jdbcTemplate;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Before
    public void init() {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Puts the index back for the other tests sharing the database, whatever the outcome of the test.
     */
    @After
    public void restoreIndex() throws Exception {
        jdbcTemplate.update("delete from " + prefix() + "subscriptions");
        updateDatabase();
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

    @Test
    public void should_start_without_the_index_while_active_subscriptions_are_duplicated_and_create_it_once_resolved() throws Exception {
        dropIndex();
        subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        subscriptionRepository.create(aSubscription("sub-2", "plan-1"));

        updateDatabase();

        // No index: a third duplicate is accepted.
        subscriptionRepository.create(aSubscription("sub-3", "plan-1"));

        jdbcTemplate.update("delete from " + prefix() + "subscriptions where id in ('sub-2', 'sub-3')");
        updateDatabase();

        assertThatThrownBy(() -> subscriptionRepository.create(aSubscription("sub-4", "plan-1"))).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void should_repair_keys_left_stale_since_the_previous_startup_before_creating_the_index() throws Exception {
        dropIndex();
        subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        // Closed by a node of a previous version, which leaves the key in place.
        jdbcTemplate.update("update " + prefix() + "subscriptions set status = 'CLOSED' where id = ?", "sub-1");
        subscriptionRepository.create(aSubscription("sub-2", "plan-1"));

        updateDatabase();

        assertThat(
            jdbcTemplate.queryForList("select unique_active_key from " + prefix() + "subscriptions where id = ?", String.class, "sub-1")
        ).containsExactly((String) null);
        assertThatThrownBy(() -> subscriptionRepository.create(aSubscription("sub-3", "plan-1"))).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void should_not_scan_the_subscriptions_at_startup_once_the_index_exists() throws Exception {
        // A key left stale once the index exists is repaired by the repository, when a write conflicts with it.
        subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        jdbcTemplate.update("update " + prefix() + "subscriptions set status = 'CLOSED' where id = ?", "sub-1");

        updateDatabase();

        assertThat(
            jdbcTemplate.queryForList("select unique_active_key from " + prefix() + "subscriptions where id = ?", String.class, "sub-1")
        ).containsExactly("API:api-1:app-1:plan-1");
    }

    private void dropIndex() {
        String index = "idx_" + prefix() + "subscriptions_unique_active_key";
        String table = prefix() + "subscriptions";
        if (isDatabase(DatabaseConfigurationEnum.POSTGRESQL)) {
            jdbcTemplate.execute("drop index " + index);
        } else {
            jdbcTemplate.execute("drop index " + index + " on " + escapeReservedWord(table));
        }
        jdbcTemplate.update("delete from " + prefix() + "databasechangelog where id like ?", INDEX_CHANGESET_PREFIX + "%");
    }

    private void updateDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            new Liquibase(
                "liquibase/master.yml",
                new ClassLoaderResourceAccessor(this.getClass().getClassLoader()),
                new JdbcConnection(connection)
            ).update((Contexts) null);
        }
    }

    private boolean isDatabase(DatabaseConfigurationEnum database) {
        return jdbcDatabaseContainer.getDockerImageName().contains(database.getDockerImageName());
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
