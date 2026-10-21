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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Subscribing to an API runs in one JDBC transaction, and attaching the subscription to the application's shared
 * API key is only one of its statements. Attaching a subscription that is already attached (a retry, or another
 * request that got there first) must leave that transaction usable: on PostgreSQL a failed statement aborts the
 * whole transaction, so catching the error is not enough.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class JdbcApiKeyRepositoryTransactionTest extends AbstractRepositoryTest {

    private static final String SHARED_KEY_ID = "shared-key-in-transaction";

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
