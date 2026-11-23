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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.handlers.api.services.ApiKeyCacheService;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiKeyDeployer;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Redeploys an API through the appender, the deployer and the real key cache, so that the order in which a key's
 * mappings are loaded and registered is the one a gateway goes through.
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiKeyRedeployTest {

    private static final String API_ID = "api-1";
    private static final String PLAN_ID = "plan-1";
    private static final String APPLICATION_ID = "app-1";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String CLOSED = "CLOSED";

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyAppender appender;
    private ApiKeyDeployer deployer;
    private ApiKeyCacheService apiKeyService;

    @BeforeEach
    void setUp() {
        appender = new ApiKeyAppender(apiKeyRepository, new ApiKeyMapper(), 100, 900);
        apiKeyService = new ApiKeyCacheService();
        deployer = new ApiKeyDeployer(apiKeyService, new NoopDistributedSyncService());
    }

    @Test
    void should_keep_a_shared_key_when_its_closed_subscription_on_the_same_plan_is_loaded_after_the_accepted_one()
        throws TechnicalException {
        Subscription accepted = subscription("accepted-subscription", ACCEPTED);
        Subscription closed = subscription("closed-subscription", CLOSED);
        ApiKey sharedKey = apiKey("key-1", "shared-key", false, accepted.getId(), closed.getId());

        redeploy(List.of(accepted, closed), sharedKey);

        assertThat(apiKeyService.getByApiAndKey(API_ID, "shared-key")).hasValueSatisfying(cached -> {
            assertThat(cached.getId()).isEqualTo("key-1");
            assertThat(cached.getSubscription()).isEqualTo("accepted-subscription");
        });
    }

    @Test
    void should_keep_the_active_key_when_a_revoked_key_with_the_same_value_is_loaded_after_it() throws TechnicalException {
        Subscription closed = subscription("closed-subscription", CLOSED);
        Subscription accepted = subscription("accepted-subscription", ACCEPTED);
        ApiKey active = apiKey("active-key", "my-key", false, accepted.getId());
        ApiKey revoked = apiKey("revoked-key", "my-key", true, closed.getId(), accepted.getId());

        redeploy(List.of(closed, accepted), active, revoked);

        assertThat(apiKeyService.getByApiAndKey(API_ID, "my-key")).hasValueSatisfying(cached -> {
            assertThat(cached.getId()).isEqualTo("active-key");
            assertThat(cached.getSubscription()).isEqualTo("accepted-subscription");
        });
    }

    @Test
    void should_remove_the_key_when_its_only_subscription_is_closed() throws TechnicalException {
        ApiKey key = apiKey("key-1", "my-key", false, "subscription");
        redeploy(List.of(subscription("subscription", ACCEPTED)), key);
        assertThat(apiKeyService.getByApiAndKey(API_ID, "my-key")).isPresent();

        redeploy(List.of(subscription("subscription", CLOSED)), key);

        assertThat(apiKeyService.getByApiAndKey(API_ID, "my-key")).isEmpty();
    }

    /**
     * The repository returns the keys in the given order, and each key's mappings follow the order of its
     * subscriptions, as on a redeploy that is not the initial sync.
     */
    private void redeploy(List<Subscription> subscriptions, ApiKey... keys) throws TechnicalException {
        when(apiKeyRepository.searchAfter(any(), any(), any(), anyInt())).thenReturn(List.of(keys));
        ApiReactorDeployable deployable = ApiReactorDeployable.builder()
            .apiId(API_ID)
            .reactableApi(mock(ReactableApi.class))
            .subscriptions(subscriptions)
            .apiKeyPlans(Set.of(PLAN_ID))
            .build();

        List<ApiReactorDeployable> withKeys = appender.appends(false, List.of(deployable), Set.of("env"));

        deployer.deploy(withKeys.getFirst()).test().assertComplete();
    }

    private static Subscription subscription(String id, String status) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setApi(API_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setStatus(status);
        return subscription;
    }

    private static ApiKey apiKey(String id, String value, boolean revoked, String... subscriptions) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(id);
        apiKey.setKey(value);
        apiKey.setApplication(APPLICATION_ID);
        apiKey.setRevoked(revoked);
        apiKey.setSubscriptions(List.of(subscriptions));
        return apiKey;
    }
}
