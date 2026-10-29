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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.SubscriptionUniqueActiveKey;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import jakarta.inject.Inject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Nodes of the versions before the hash update subscriptions without touching it: a subscription they close keeps its
 * hash, one they transfer keeps the hash of its previous plan. Such a stale hash must not refuse a legitimate
 * subscription.
 *
 * @author GraviteeSource Team
 */
public class JdbcSubscriptionStaleUniqueActiveKeyTest extends AbstractManagementRepositoryTest {

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
        subscriptionsTable = graviteeProperties.getProperty("management.jdbc.prefix", "") + "subscriptions";
    }

    @Test
    public void should_accept_a_subscription_to_a_plan_whose_previous_subscription_a_previous_version_closed() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        closeAsPreviousVersion("sub-1");

        var subscription = subscriptionRepository.create(aSubscription("sub-2", "plan-1"));

        assertThat(storedHash("sub-1")).isNull();
        assertThat(storedHash("sub-2")).isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_repair_a_stale_hash_whose_subscription_has_no_api_type_or_configuration() throws Exception {
        // NULL in any column the hash depends on: a NULL never equals a value read, the repair must compare it as such.
        var apiProduct = aSubscription("sub-1", "plan-1");
        apiProduct.setReferenceType(SubscriptionReferenceType.API_PRODUCT);
        apiProduct.setReferenceId("api-product-1");
        apiProduct.setApi(null);
        subscriptionRepository.create(apiProduct);
        closeAsPreviousVersion("sub-1");
        var subscription = aSubscription("sub-2", "plan-1");
        subscription.setReferenceType(SubscriptionReferenceType.API_PRODUCT);
        subscription.setReferenceId("api-product-1");
        subscription.setApi(null);

        subscription = subscriptionRepository.create(subscription);

        assertThat(storedHash("sub-1")).isNull();
        assertThat(storedHash("sub-2")).isEqualTo(hashOf(subscription));
    }

    @Test
    public void should_accept_a_subscription_to_a_plan_a_previous_version_transferred_another_subscription_from() throws Exception {
        var transferred = subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        transferAsPreviousVersion("sub-1", "plan-2");
        transferred.setPlan("plan-2");

        subscriptionRepository.create(aSubscription("sub-2", "plan-1"));

        assertThat(storedHash("sub-1")).isEqualTo(hashOf(transferred));
    }

    @Test
    public void should_transfer_a_subscription_to_a_plan_whose_previous_subscription_a_previous_version_closed() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        closeAsPreviousVersion("sub-1");
        var toTransfer = subscriptionRepository.create(aSubscription("sub-2", "plan-2"));

        toTransfer.setPlan("plan-1");
        subscriptionRepository.update(toTransfer);

        assertThat(storedHash("sub-1")).isNull();
        assertThat(storedHash("sub-2")).isEqualTo(hashOf(toTransfer));
    }

    @Test
    public void should_clear_a_stale_hash_whose_up_to_date_value_another_active_subscription_holds() throws Exception {
        // A previous version transferred sub-2 to a plan the application already subscribed with sub-1: a duplicate the
        // repository cannot arbitrate, it only frees the hash sub-2 still holds.
        subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        subscriptionRepository.create(aSubscription("sub-2", "plan-2"));
        transferAsPreviousVersion("sub-2", "plan-1");

        subscriptionRepository.create(aSubscription("sub-3", "plan-2"));

        assertThat(storedHash("sub-2")).isNull();
        assertThat(storedHash("sub-1")).isNotNull();
    }

    @Test
    public void should_keep_refusing_a_second_active_subscription_when_the_existing_hash_is_up_to_date() throws Exception {
        var existing = subscriptionRepository.create(aSubscription("sub-1", "plan-1"));

        assertThatThrownBy(() -> subscriptionRepository.create(aSubscription("sub-2", "plan-1"))).isInstanceOf(DuplicateKeyException.class);
        assertThat(storedHash("sub-1")).isEqualTo(hashOf(existing));
        assertThat(subscriptionRepository.findById("sub-2")).isEmpty();
    }

    @Test
    public void should_repair_a_stale_hash_within_the_caller_transaction() throws Exception {
        // On PostgreSQL the refused insert aborts the transaction unless every statement runs in a savepoint.
        subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        closeAsPreviousVersion("sub-1");
        var transaction = new TransactionTemplate(graviteeTransactionManager);

        transaction.executeWithoutResult(status -> {
            try {
                subscriptionRepository.create(aSubscription("sub-2", "plan-1"));
                subscriptionRepository.create(aSubscription("sub-3", "plan-2"));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(subscriptionRepository.findByIdIn(List.of("sub-2", "sub-3"))).hasSize(2);
    }

    @Test
    public void should_keep_one_of_identical_subscriptions_created_at_the_same_time_over_a_stale_hash() throws Exception {
        // Each request runs in a transaction, like the subscription service: all of them run into the stale hash and
        // repair it at the same time. Exactly one of them is created; the others are refused, or fail as the victim of a
        // deadlock (MySQL, MariaDB, SQL Server) with their whole transaction rolled back, never half-committed.
        int requests = 4;
        var transaction = new TransactionTemplate(graviteeTransactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        try {
            for (int round = 0; round < 10; round++) {
                String plan = "plan-" + round;
                subscriptionRepository.create(aSubscription("stale-" + round, plan));
                closeAsPreviousVersion("stale-" + round);
                var barrier = new CyclicBarrier(requests);
                List<Future<Throwable>> results = new ArrayList<>();
                for (int request = 0; request < requests; request++) {
                    String id = "sub-" + round + "-" + request;
                    results.add(
                        executor.submit(() -> {
                            barrier.await();
                            return catchThrowable(() ->
                                transaction.executeWithoutResult(status -> {
                                    try {
                                        subscriptionRepository.create(aSubscription(id, plan));
                                    } catch (TechnicalException e) {
                                        throw new IllegalStateException(e);
                                    }
                                })
                            );
                        })
                    );
                }

                List<Throwable> failures = new ArrayList<>();
                for (Future<Throwable> result : results) {
                    if (result.get() != null) {
                        failures.add(result.get());
                    }
                }
                assertThat(failures).as("round %d", round).hasSize(requests - 1);
                assertThat(failures)
                    .as("round %d", round)
                    .allSatisfy(failure ->
                        assertThat(ExceptionUtils.getThrowableList(failure)).anyMatch(cause -> cause instanceof TechnicalException)
                    );
                assertThat(
                    jdbcTemplate.queryForObject(
                        "select count(*) from " +
                            subscriptionsTable +
                            " where " +
                            escapeReservedWord("plan") +
                            " = ? and status = 'ACCEPTED'",
                        Integer.class,
                        plan
                    )
                )
                    .as("round %d", round)
                    .isEqualTo(1);
                assertThat(storedHash("stale-" + round)).isNull();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * What a node of a previous version writes: the status, never the hash.
     */
    private void closeAsPreviousVersion(String id) {
        jdbcTemplate.update("update " + subscriptionsTable + " set status = 'CLOSED' where id = ?", id);
    }

    /**
     * What a node of a previous version writes: the plan, never the hash.
     */
    private void transferAsPreviousVersion(String id, String plan) {
        jdbcTemplate.update("update " + subscriptionsTable + " set " + escapeReservedWord("plan") + " = ? where id = ?", plan, id);
    }

    private String storedHash(String id) {
        List<String> hashes = jdbcTemplate.queryForList(
            "select active_key_hash from " + subscriptionsTable + " where id = ?",
            String.class,
            id
        );
        assertThat(hashes).hasSize(1);
        return hashes.getFirst();
    }

    private static String hashOf(Subscription subscription) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(SubscriptionUniqueActiveKey.of(subscription).getBytes(UTF_8))
        );
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
