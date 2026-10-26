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
package io.gravitee.repository.jdbc;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Subscribing to an API runs in one JDBC transaction, and attaching the subscription to the application's shared
 * API key is only one of its statements. Attaching a subscription that is already attached (a retry, or another
 * request that got there first) must leave that transaction usable: on PostgreSQL a failed statement aborts the
 * whole transaction, so catching the error is not enough. Attaching a subscription must also never leave a link to a key
 * that does not exist: {@code key_subscriptions} has no foreign key to {@code keys}.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class JdbcApiKeyRepositoryTransactionTest extends AbstractRepositoryTest {

    private static final String SHARED_KEY_ID = "shared-key-in-transaction";
    private static final String KEY_BEING_DELETED = "key-being-deleted";
    private static final String ENVIRONMENT_BEING_DELETED = "environment-being-deleted";
    private static final long TIMEOUT_SECONDS = 60;

    @Inject
    private ApiKeyRepository apiKeyRepository;

    @Inject
    private PlatformTransactionManager transactionManager;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private Properties graviteeProperties;

    @Test
    public void should_commit_a_transaction_that_attaches_the_same_subscription_twice() throws Exception {
        apiKeyRepository.create(apiKey(SHARED_KEY_ID, "sub-initial"));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            try {
                apiKeyRepository.addSubscription(SHARED_KEY_ID, "sub-retried");
                apiKeyRepository.addSubscription(SHARED_KEY_ID, "sub-retried");
                // Any later statement of the same transaction, like the rest of a subscribe request.
                apiKeyRepository.create(apiKey("key-created-later-in-transaction", "sub-other"));
            } catch (TechnicalException e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(apiKeyRepository.findById(SHARED_KEY_ID).orElseThrow().getSubscriptions()).containsExactlyInAnyOrder(
            "sub-initial",
            "sub-retried"
        );
        assertThat(apiKeyRepository.findById("key-created-later-in-transaction")).isPresent();
        assertThat(countLinks(SHARED_KEY_ID, "sub-retried")).isEqualTo(1);
    }

    @Test
    public void should_write_no_link_when_adding_a_subscription_to_a_missing_key() throws Exception {
        assertThat(apiKeyRepository.addSubscription("missing-key", "sub-of-a-missing-key")).isEmpty();

        assertThat(countLinks("missing-key", "sub-of-a-missing-key")).isZero();
    }

    @Test
    public void should_write_no_link_to_a_key_deleted_while_the_subscription_is_added() throws Exception {
        apiKeyRepository.create(apiKey(KEY_BEING_DELETED, "sub-initial").toBuilder().environmentId(ENVIRONMENT_BEING_DELETED).build());
        CountDownLatch deleted = new CountDownLatch(1);
        CountDownLatch adding = new CountDownLatch(1);
        ExecutorService otherConnection = Executors.newSingleThreadExecutor();
        try {
            Future<?> deletion = otherConnection.submit(() ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    try {
                        apiKeyRepository.deleteByEnvironmentId(ENVIRONMENT_BEING_DELETED);
                        deleted.countDown();
                        // Commit while the subscription is being added. Only a check made before the link is written needs this
                        // timing to leave an orphan link; the expected outcome does not depend on it.
                        assertThat(adding.await(TIMEOUT_SECONDS, SECONDS)).isTrue();
                        Thread.sleep(500);
                    } catch (TechnicalException e) {
                        throw new IllegalStateException(e);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                })
            );
            assertThat(deleted.await(TIMEOUT_SECONDS, SECONDS)).isTrue();

            adding.countDown();
            Optional<ApiKey> added = apiKeyRepository.addSubscription(KEY_BEING_DELETED, "sub-added-while-deleted");
            deletion.get(TIMEOUT_SECONDS, SECONDS);

            assertThat(added).isEmpty();
            assertThat(apiKeyRepository.findById(KEY_BEING_DELETED)).isEmpty();
            assertThat(countLinks(KEY_BEING_DELETED, "sub-added-while-deleted")).isZero();
        } finally {
            otherConnection.shutdownNow();
        }
    }

    private int countLinks(String keyId, String subscriptionId) {
        String keySubscriptions = graviteeProperties.getProperty("management.jdbc.prefix", "") + "key_subscriptions";
        return jdbcTemplate.queryForObject(
            "select count(*) from " + keySubscriptions + " where key_id = ? and subscription_id = ?",
            Integer.class,
            keyId,
            subscriptionId
        );
    }

    private static ApiKey apiKey(String id, String subscriptionId) {
        return ApiKey.builder()
            .id(id)
            .key(id + "-value")
            .environmentId("DEFAULT")
            .application("shared-app")
            .subscriptions(new ArrayList<>(List.of(subscriptionId)))
            .createdAt(new Date())
            .updatedAt(new Date())
            .build();
    }

    @Override
    protected String getTestCasesPath() {
        return "";
    }

    @Override
    protected String getModelPackage() {
        return "";
    }

    @Override
    protected void createModel(Object object) {}
}
