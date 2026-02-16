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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductSubscriptionRefresherTest {

    private static final String PLAN_1 = "plan-1";
    private static final String ENV_1 = "env-1";
    private static final String SUB_1 = "sub-1";
    private static final String API_1 = "api-1";
    private static final String KEY_ID = "key-id";

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @Mock
    private ApiKeyMapper apiKeyMapper;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ApiKeyService apiKeyService;

    private ApiProductSubscriptionRefresher cut;

    @BeforeEach
    void setUp() {
        cut = new ApiProductSubscriptionRefresher(
            subscriptionRepository,
            apiKeyRepository,
            subscriptionMapper,
            apiKeyMapper,
            subscriptionService,
            apiKeyService
        );
    }

    @Nested
    class RefreshTest {

        @Test
        void returns_complete_when_plans_null_or_empty() throws TechnicalException {
            cut.refresh(null, Set.of(ENV_1)).test().assertComplete();
            cut.refresh(Set.of(), Set.of(ENV_1)).test().assertComplete();
            verify(subscriptionRepository, never()).search(any(), any());
        }

        @Test
        void loads_and_deploys_subscriptions() throws TechnicalException {
            var repoSub = productSubscription(SUB_1, PLAN_1, ENV_1);
            var gatewaySub = Subscription.builder().id(SUB_1).api(API_1).plan(PLAN_1).build();
            when(subscriptionRepository.search(any(), any())).thenReturn(List.of(repoSub));
            when(subscriptionMapper.to(repoSub)).thenReturn(List.of(gatewaySub));

            cut.refresh(Set.of(PLAN_1), Set.of(ENV_1)).test().assertComplete();

            verify(subscriptionService).register(gatewaySub);
        }

        @Test
        void loads_and_deploys_api_keys() throws TechnicalException {
            var repoSub = productSubscription(SUB_1, PLAN_1, ENV_1);
            var gatewaySub = Subscription.builder().id(SUB_1).api(API_1).plan(PLAN_1).build();
            var repoKey = productApiKey(KEY_ID, ENV_1, SUB_1);
            var gatewayKey = ApiKey.builder().id(KEY_ID).api(API_1).subscription(SUB_1).build();
            when(subscriptionRepository.search(any(), any())).thenReturn(List.of(repoSub));
            when(subscriptionMapper.to(repoSub)).thenReturn(List.of(gatewaySub));
            when(apiKeyRepository.findByCriteria(any(), any())).thenReturn(List.of(repoKey));
            when(apiKeyMapper.to(repoKey, gatewaySub)).thenReturn(gatewayKey);

            cut.refresh(Set.of(PLAN_1), Set.of(ENV_1)).test().assertComplete();

            verify(subscriptionService).register(gatewaySub);
            verify(apiKeyService).register(gatewayKey);
        }

        @Test
        void does_not_call_api_key_repository_when_no_subscriptions() throws TechnicalException {
            when(subscriptionRepository.search(any(), any())).thenReturn(List.of());

            cut.refresh(Set.of(PLAN_1), Set.of(ENV_1)).test().assertComplete();

            verify(subscriptionRepository).search(any(), any());
            verify(apiKeyRepository, never()).findByCriteria(any(), any());
        }

        @Test
        void throws_when_repository_fails() throws TechnicalException {
            when(subscriptionRepository.search(any(), any())).thenThrow(new TechnicalException("DB error"));

            var thrown = catchThrowable(() -> cut.refresh(Set.of(PLAN_1), Set.of(ENV_1)).blockingAwait());

            assertThat(thrown)
                .isInstanceOf(SyncException.class)
                .hasMessageContaining("Error occurred when refreshing subscriptions for API Product");
            assertThat(thrown.getCause().getCause()).isInstanceOf(TechnicalException.class);
        }
    }

    @Nested
    class UnregisterRemovedApisTest {

        @Test
        void returns_complete_when_removedApiIds_null_or_empty() throws TechnicalException {
            cut.unregisterRemovedApis(null, Set.of(PLAN_1), Set.of(ENV_1)).test().assertComplete();
            cut.unregisterRemovedApis(Set.of(), Set.of(PLAN_1), Set.of(ENV_1)).test().assertComplete();
            verify(subscriptionRepository, never()).search(any(), any());
        }

        @Test
        void returns_complete_when_plans_empty() throws TechnicalException {
            cut.unregisterRemovedApis(Set.of("api-removed"), Set.of(), Set.of(ENV_1)).test().assertComplete();
            verify(subscriptionRepository, never()).search(any(), any());
        }

        @Test
        void unregisters_subscriptions_and_api_keys_for_removed_apis() throws TechnicalException {
            var repoSub = productSubscription(SUB_1, PLAN_1, ENV_1);
            var subForRemoved = Subscription.builder().id(SUB_1).api("api-removed").plan(PLAN_1).build();
            var repoKey = productApiKey(KEY_ID, ENV_1, SUB_1);
            var gatewayKey = ApiKey.builder().id(KEY_ID).api("api-removed").subscription(SUB_1).build();

            when(subscriptionRepository.search(any(), any())).thenReturn(List.of(repoSub));
            when(subscriptionMapper.toSubscriptionForApi(repoSub, "api-removed")).thenReturn(subForRemoved);
            when(apiKeyRepository.findByCriteria(any(), any())).thenReturn(List.of(repoKey));
            when(apiKeyMapper.to(repoKey, subForRemoved)).thenReturn(gatewayKey);

            cut.unregisterRemovedApis(Set.of("api-removed"), Set.of(PLAN_1), Set.of(ENV_1)).test().assertComplete();

            verify(subscriptionService).unregister(subForRemoved);
            verify(apiKeyService).unregister(gatewayKey);
        }
    }

    private static io.gravitee.repository.management.model.Subscription productSubscription(String id, String plan, String env) {
        var s = new io.gravitee.repository.management.model.Subscription();
        s.setId(id);
        s.setReferenceId("product-1");
        s.setReferenceType(SubscriptionReferenceType.API_PRODUCT);
        s.setPlan(plan);
        s.setStatus(io.gravitee.repository.management.model.Subscription.Status.ACCEPTED);
        s.setEnvironmentId(env);
        return s;
    }

    private static io.gravitee.repository.management.model.ApiKey productApiKey(String id, String env, String subscriptionId) {
        var k = new io.gravitee.repository.management.model.ApiKey();
        k.setId(id);
        k.setKey("key-" + id);
        k.setSubscriptions(List.of(subscriptionId));
        k.setEnvironmentId(env);
        return k;
    }
}
