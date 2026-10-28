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
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.SubscriptionUniqueActiveKey;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.changelog.FastCheckService;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The unique active key is a storage detail of the subscriptions table, never exposed by the repository model, hence
 * a JDBC specific test reading the stored rows.
 *
 * @author GraviteeSource Team
 */
public class JdbcSubscriptionUniqueActiveKeyTest extends AbstractManagementRepositoryTest {

    private static final String BACKFILL_CHANGESET = "4.12.26_01_backfill_subscriptions_unique_active_key";

    @Inject
    private DataSource dataSource;

    @Inject
    private Properties graviteeProperties;

    @Inject
    private PlatformTransactionManager graviteeTransactionManager;

    private JdbcTemplate jdbcTemplate;

    private String subscriptionsTable;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @BeforeEach
    public void init() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        subscriptionsTable = prefix() + "subscriptions";
    }

    @Test
    public void should_store_the_unique_active_key_of_an_active_subscription() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));

        assertThat(storedKey("sub-1")).isNotNull().isEqualTo(SubscriptionUniqueActiveKey.of(subscription));
    }

    @Test
    public void should_store_no_key_for_a_rejected_or_closed_subscription() throws Exception {
        subscriptionRepository.create(aSubscription("sub-rejected", Subscription.Status.REJECTED, "plan-1"));
        subscriptionRepository.create(aSubscription("sub-closed", Subscription.Status.CLOSED, "plan-1"));

        assertThat(storedKey("sub-rejected")).isNull();
        assertThat(storedKey("sub-closed")).isNull();
    }

    @Test
    public void should_remove_the_key_when_the_subscription_is_closed() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));

        subscription.setStatus(Subscription.Status.CLOSED);
        subscriptionRepository.update(subscription);

        assertThat(storedKey("sub-1")).isNull();
    }

    @Test
    public void should_keep_the_key_while_the_subscription_is_paused_and_once_it_is_resumed() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        var key = storedKey("sub-1");

        subscription.setStatus(Subscription.Status.PAUSED);
        subscriptionRepository.update(subscription);
        var keyWhilePaused = storedKey("sub-1");
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscriptionRepository.update(subscription);

        assertThat(keyWhilePaused).isNotNull().isEqualTo(key);
        assertThat(storedKey("sub-1")).isEqualTo(key);
    }

    @Test
    public void should_give_the_key_back_when_a_closed_subscription_is_restored() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.CLOSED, "plan-1"));

        subscription.setStatus(Subscription.Status.PENDING);
        subscriptionRepository.update(subscription);

        assertThat(storedKey("sub-1")).isNotNull().isEqualTo(SubscriptionUniqueActiveKey.of(subscription));
    }

    @Test
    public void should_recompute_the_key_when_the_subscription_is_transferred_to_another_plan() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));

        subscription.setPlan("plan-2");
        subscriptionRepository.update(subscription);

        assertThat(storedKey("sub-1")).isEqualTo(SubscriptionUniqueActiveKey.of(subscription)).contains("plan-2");
    }

    @Test
    public void should_recompute_the_key_when_the_channel_of_a_push_subscription_changes() throws Exception {
        var subscription = aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1");
        subscription.setType(Subscription.Type.PUSH);
        subscription.setConfiguration("{\"entrypointId\":\"webhook\",\"channel\":\"orders\"}");
        subscription = subscriptionRepository.create(subscription);
        var keyOnOrders = storedKey("sub-1");

        subscription.setConfiguration("{\"entrypointId\":\"webhook\",\"channel\":\"invoices\"}");
        subscriptionRepository.update(subscription);

        assertThat(storedKey("sub-1")).isNotEqualTo(keyOnOrders).isEqualTo(SubscriptionUniqueActiveKey.of(subscription));
    }

    @Test
    public void should_not_backfill_the_existing_subscriptions_when_liquibase_runs() throws Exception {
        // A backfill scans every subscription at startup, before the node serves requests, while nothing reads the key
        // until the unique index exists. Makes the backfill pending again, as on a database upgraded from 4.12.25.
        createWithoutKey(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        jdbcTemplate.update("delete from " + prefix() + "databasechangelog where id = ?", BACKFILL_CHANGESET);
        // The test setup already ran Liquibase, which remembers that the database is up to date
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();

        try (Connection connection = dataSource.getConnection()) {
            new Liquibase(
                "liquibase/master.yml",
                new ClassLoaderResourceAccessor(this.getClass().getClassLoader()),
                new JdbcConnection(connection)
            ).update((Contexts) null);
        }

        assertThat(storedKey("sub-1")).isNull();
        assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from " + prefix() + "databasechangelog where id = ?",
                Integer.class,
                BACKFILL_CHANGESET
            )
        ).isZero();
    }

    @Test
    public void should_write_the_key_of_a_subscription_stored_without_it_the_next_time_it_is_saved() throws Exception {
        createWithoutKey(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        var subscription = subscriptionRepository.findById("sub-1").orElseThrow();

        subscription.setUpdatedAt(new Date());
        subscriptionRepository.update(subscription);

        assertThat(storedKey("sub-1")).isNotNull().isEqualTo(SubscriptionUniqueActiveKey.of(subscription));
    }

    @Test
    public void should_keep_the_caller_transaction_usable_after_a_failed_subscription_insert() throws Exception {
        // On PostgreSQL a failed statement aborts the whole transaction: every later statement of the request,
        // and its commit, would fail too.
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        var transaction = new TransactionTemplate(graviteeTransactionManager);

        transaction.executeWithoutResult(status -> {
            var failure = catchThrowable(() ->
                subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"))
            );
            assertThat(failure).isNotNull();
            try {
                subscriptionRepository.create(aSubscription("sub-2", Subscription.Status.ACCEPTED, "plan-2"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(subscriptionRepository.findById("sub-2")).isPresent();
    }

    private void createWithoutKey(Subscription subscription) {
        try {
            subscriptionRepository.create(subscription);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        jdbcTemplate.update("update " + subscriptionsTable + " set unique_active_key = null where id = ?", subscription.getId());
    }

    private String storedKey(String id) {
        List<String> keys = jdbcTemplate.queryForList(
            "select unique_active_key from " + subscriptionsTable + " where id = ?",
            String.class,
            id
        );
        assertThat(keys).hasSize(1);
        return keys.getFirst();
    }

    private String prefix() {
        return graviteeProperties.getProperty("management.jdbc.prefix", "");
    }

    private static Subscription aSubscription(String id, Subscription.Status status, String plan) {
        var subscription = new Subscription();
        subscription.setId(id);
        subscription.setEnvironmentId("DEFAULT");
        subscription.setReferenceType(SubscriptionReferenceType.API);
        subscription.setReferenceId("api-1");
        subscription.setApi("api-1");
        subscription.setApplication("app-1");
        // NOT NULL column.
        subscription.setApplicationName("app-1 name");
        subscription.setPlan(plan);
        subscription.setStatus(status);
        subscription.setCreatedAt(new Date());
        subscription.setUpdatedAt(new Date());
        return subscription;
    }
}
