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
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.SubscriptionUniqueActiveKey;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import jakarta.inject.Inject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The hash of the unique active key is a storage detail of the subscriptions table, never exposed by the repository
 * model, hence a JDBC specific test reading the stored rows.
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

    @Before
    public void init() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        subscriptionsTable = prefix() + "subscriptions";
    }

    @Test
    public void should_store_the_hash_of_the_unique_active_key_of_an_active_subscription() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));

        assertThat(storedHash("sub-1")).isNotNull().isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_not_store_the_unique_active_key_itself_anymore() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));

        assertThat(storedPreviousKey("sub-1")).isNull();
    }

    @Test
    public void should_store_no_hash_for_a_rejected_or_closed_subscription() throws Exception {
        subscriptionRepository.create(aSubscription("sub-rejected", Subscription.Status.REJECTED, "plan-1"));
        subscriptionRepository.create(aSubscription("sub-closed", Subscription.Status.CLOSED, "plan-1"));

        assertThat(storedHash("sub-rejected")).isNull();
        assertThat(storedHash("sub-closed")).isNull();
    }

    @Test
    public void should_remove_the_hash_when_the_subscription_is_closed() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));

        subscription.setStatus(Subscription.Status.CLOSED);
        subscriptionRepository.update(subscription);

        assertThat(storedHash("sub-1")).isNull();
    }

    @Test
    public void should_keep_the_hash_while_the_subscription_is_paused_and_once_it_is_resumed() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        var hash = storedHash("sub-1");

        subscription.setStatus(Subscription.Status.PAUSED);
        subscriptionRepository.update(subscription);
        var hashWhilePaused = storedHash("sub-1");
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscriptionRepository.update(subscription);

        assertThat(hashWhilePaused).isNotNull().isEqualTo(hash);
        assertThat(storedHash("sub-1")).isEqualTo(hash);
    }

    @Test
    public void should_give_the_hash_back_when_a_closed_subscription_is_restored() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.CLOSED, "plan-1"));

        subscription.setStatus(Subscription.Status.PENDING);
        subscriptionRepository.update(subscription);

        assertThat(storedHash("sub-1")).isNotNull().isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_recompute_the_hash_when_the_subscription_is_transferred_to_another_plan() throws Exception {
        var subscription = subscriptionRepository.create(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        var hashOnPlan1 = storedHash("sub-1");

        subscription.setPlan("plan-2");
        subscriptionRepository.update(subscription);

        assertThat(storedHash("sub-1")).isNotEqualTo(hashOnPlan1).isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_recompute_the_hash_when_the_channel_of_a_push_subscription_changes() throws Exception {
        var subscription = aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1");
        subscription.setType(Subscription.Type.PUSH);
        subscription.setConfiguration("{\"entrypointId\":\"webhook\",\"channel\":\"orders\"}");
        subscription = subscriptionRepository.create(subscription);
        var hashOnOrders = storedHash("sub-1");

        subscription.setConfiguration("{\"entrypointId\":\"webhook\",\"channel\":\"invoices\"}");
        subscriptionRepository.update(subscription);

        assertThat(storedHash("sub-1")).isNotEqualTo(hashOnOrders).isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_not_backfill_the_existing_subscriptions_when_liquibase_runs() throws Exception {
        // A backfill scans every subscription at startup, before the node serves requests. Makes the removed backfill
        // pending again, as on a database upgraded from 4.12.25.
        createWithoutHash(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        jdbcTemplate.update("delete from " + prefix() + "databasechangelog where id = ?", BACKFILL_CHANGESET);

        try (Connection connection = dataSource.getConnection()) {
            new Liquibase(
                "liquibase/master.yml",
                new ClassLoaderResourceAccessor(this.getClass().getClassLoader()),
                new JdbcConnection(connection)
            ).update((Contexts) null);
        }

        assertThat(storedHash("sub-1")).isNull();
        assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from " + prefix() + "databasechangelog where id = ?",
                Integer.class,
                BACKFILL_CHANGESET
            )
        ).isZero();
    }

    @Test
    public void should_write_the_hash_of_a_subscription_stored_without_it_the_next_time_it_is_saved() throws Exception {
        createWithoutHash(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        var subscription = subscriptionRepository.findById("sub-1").orElseThrow();

        subscription.setUpdatedAt(new Date());
        subscriptionRepository.update(subscription);

        assertThat(storedHash("sub-1")).isNotNull().isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_not_refuse_a_subscription_because_of_a_subscription_written_before_the_hash() throws Exception {
        // The duplicates created before the index are left as they are: only the subscription service refuses a
        // subscription to a plan the application already subscribed.
        createWithoutHash(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));

        var subscription = subscriptionRepository.create(aSubscription("sub-2", Subscription.Status.ACCEPTED, "plan-1"));

        assertThat(storedHash("sub-2")).isNotNull().isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_keep_updating_a_subscription_written_before_the_hash_while_another_one_holds_it() throws Exception {
        // Two active subscriptions of an application to a plan, duplicated before the unique index existed.
        createWithoutHash(aSubscription("sub-1", Subscription.Status.ACCEPTED, "plan-1"));
        createWithoutHash(aSubscription("sub-2", Subscription.Status.ACCEPTED, "plan-2"));
        jdbcTemplate.update("update " + subscriptionsTable + " set " + escapeReservedWord("plan") + " = 'plan-1' where id = 'sub-2'");
        var first = subscriptionRepository.findById("sub-1").orElseThrow();
        first.setStatus(Subscription.Status.PAUSED);
        subscriptionRepository.update(first);
        var second = subscriptionRepository.findById("sub-2").orElseThrow();

        second.setStatus(Subscription.Status.PAUSED);
        subscriptionRepository.update(second);

        assertThat(storedHash("sub-1")).isNotNull().isEqualTo(hashOf(first));
        assertThat(storedHash("sub-2")).isNull();
        assertThat(subscriptionRepository.findById("sub-2"))
            .get()
            .extracting(Subscription::getStatus)
            .isEqualTo(Subscription.Status.PAUSED);
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

    /**
     * As stored by a node of a previous version, which knows nothing of the hash.
     */
    private void createWithoutHash(Subscription subscription) {
        try {
            subscriptionRepository.create(subscription);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        jdbcTemplate.update("update " + subscriptionsTable + " set active_key_hash = null where id = ?", subscription.getId());
    }

    private String storedHash(String id) {
        return storedValue("active_key_hash", id);
    }

    private String storedPreviousKey(String id) {
        return storedValue("unique_active_key", id);
    }

    private String storedValue(String column, String id) {
        List<String> values = jdbcTemplate.queryForList(
            "select " + column + " from " + subscriptionsTable + " where id = ?",
            String.class,
            id
        );
        assertThat(values).hasSize(1);
        return values.getFirst();
    }

    private String prefix() {
        return graviteeProperties.getProperty("management.jdbc.prefix", "");
    }

    private static String hashOf(Subscription subscription) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(SubscriptionUniqueActiveKey.of(subscription).getBytes(UTF_8))
        );
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
