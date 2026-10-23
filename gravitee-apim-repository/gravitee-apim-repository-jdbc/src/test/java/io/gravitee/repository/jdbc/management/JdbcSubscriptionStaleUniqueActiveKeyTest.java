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

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.SubscriptionUniqueActiveKey;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Nodes of the versions before the unique active key update subscriptions without touching it: a subscription they
 * close keeps its key, one they transfer keeps the key of its previous plan. Such a stale key must not refuse a
 * legitimate subscription once the unique index exists.
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

        assertThat(storedKey("sub-1")).isNull();
        assertThat(storedKey("sub-2")).isEqualTo(SubscriptionUniqueActiveKey.of(subscription));
    }

    @Test
    public void should_accept_a_subscription_to_a_plan_a_previous_version_transferred_another_subscription_from() throws Exception {
        var transferred = subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        transferAsPreviousVersion("sub-1", "plan-2");
        transferred.setPlan("plan-2");

        subscriptionRepository.create(aSubscription("sub-2", "plan-1"));

        assertThat(storedKey("sub-1")).isEqualTo(SubscriptionUniqueActiveKey.of(transferred));
    }

    @Test
    public void should_transfer_a_subscription_to_a_plan_whose_previous_subscription_a_previous_version_closed() throws Exception {
        subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        closeAsPreviousVersion("sub-1");
        var toTransfer = subscriptionRepository.create(aSubscription("sub-2", "plan-2"));

        toTransfer.setPlan("plan-1");
        subscriptionRepository.update(toTransfer);

        assertThat(storedKey("sub-1")).isNull();
        assertThat(storedKey("sub-2")).isEqualTo(SubscriptionUniqueActiveKey.of(toTransfer));
    }

    @Test
    public void should_clear_a_stale_key_whose_up_to_date_value_another_active_subscription_holds() throws Exception {
        // A previous version transferred sub-2 to a plan the application already subscribed with sub-1: a duplicate the
        // repository cannot arbitrate, it only frees the key sub-2 still holds.
        subscriptionRepository.create(aSubscription("sub-1", "plan-1"));
        subscriptionRepository.create(aSubscription("sub-2", "plan-2"));
        transferAsPreviousVersion("sub-2", "plan-1");

        subscriptionRepository.create(aSubscription("sub-3", "plan-2"));

        assertThat(storedKey("sub-2")).isNull();
        assertThat(storedKey("sub-1")).isNotNull();
    }

    @Test
    public void should_keep_refusing_a_second_active_subscription_when_the_existing_key_is_up_to_date() throws Exception {
        var existing = subscriptionRepository.create(aSubscription("sub-1", "plan-1"));

        assertThatThrownBy(() -> subscriptionRepository.create(aSubscription("sub-2", "plan-1"))).isInstanceOf(DuplicateKeyException.class);
        assertThat(storedKey("sub-1")).isEqualTo(SubscriptionUniqueActiveKey.of(existing));
        assertThat(subscriptionRepository.findById("sub-2")).isEmpty();
    }

    @Test
    public void should_repair_a_stale_key_within_the_caller_transaction() throws Exception {
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

    /**
     * What a node of a previous version writes: the status, never the key.
     */
    private void closeAsPreviousVersion(String id) {
        jdbcTemplate.update("update " + subscriptionsTable + " set status = 'CLOSED' where id = ?", id);
    }

    /**
     * What a node of a previous version writes: the plan, never the key.
     */
    private void transferAsPreviousVersion(String id, String plan) {
        jdbcTemplate.update("update " + subscriptionsTable + " set " + escapeReservedWord("plan") + " = ? where id = ?", plan, id);
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
