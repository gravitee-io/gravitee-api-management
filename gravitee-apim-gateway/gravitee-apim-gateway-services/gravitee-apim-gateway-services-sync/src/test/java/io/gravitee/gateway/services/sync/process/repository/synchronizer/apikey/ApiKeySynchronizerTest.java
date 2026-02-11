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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiKeyDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.repository.fetcher.ApiKeyFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiKeySynchronizerTest {

    @Mock
    private ApiKeyFetcher apiKeyFetcher;

    @Mock
    private SubscriptionCacheService subscriptionService;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private ApiKeyDeployer apiKeyDeployer;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeySynchronizer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new ApiKeySynchronizer(
            apiKeyFetcher,
            subscriptionService,
            new ApiKeyMapper(),
            deployerFactory,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );
        lenient().when(deployerFactory.createApiKeyDeployer()).thenReturn(apiKeyDeployer);
        lenient().when(apiKeyDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(apiKeyDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(apiKeyDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(apiKeyDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Nested
    class NoEventTest {

        @Test
        void should_not_fetch_init_events() {
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env")).test().assertComplete();
            verifyNoInteractions(apiKeyFetcher);
        }

        @Test
        void should_fetch_incremental_events() throws InterruptedException {
            when(apiKeyFetcher.fetchLatest(any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(1L, 1L, Set.of("env")).test().await().assertComplete();
            verify(apiKeyFetcher).fetchLatest(1L, 1L, Set.of("env"));
        }

        @Test
        void should_not_synchronize_api_keys_when_no_events() throws InterruptedException {
            when(apiKeyFetcher.fetchLatest(any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(apiKeyDeployer);
        }
    }

    @Nested
    class ApiKeySynchronizationTest {

        @Test
        void should_deploy_api_keys_when_fetching_api_keys() throws InterruptedException {
            ApiKey apiKey = new ApiKey();
            apiKey.setId("api");
            apiKey.setKey("key");
            apiKey.setSubscriptions(List.of("subscription"));

            Subscription subscription = new Subscription();
            subscription.setId("subscription");
            subscription.setApi("api");
            subscription.setPlan("plan");
            subscription.setStatus(io.gravitee.repository.management.model.Subscription.Status.ACCEPTED.name());
            when(subscriptionService.getAllById("subscription")).thenReturn(List.of(subscription));

            when(apiKeyFetcher.fetchLatest(any(), any(), any())).thenReturn(Flowable.just(List.of(apiKey)));
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiKeyDeployer).deploy(any());
            verify(apiKeyDeployer).doAfterDeployment(any());
        }

        @Test
        void should_undeploy_api_keys_when_fetching_inactive_api_keys() throws InterruptedException {
            ApiKey apiKey = new ApiKey();
            apiKey.setId("api");
            apiKey.setKey("key");
            apiKey.setSubscriptions(List.of("subscription"));

            Subscription subscription = new Subscription();
            subscription.setId("subscription");
            subscription.setApi("api");
            subscription.setPlan("plan");
            subscription.setStatus(io.gravitee.repository.management.model.Subscription.Status.CLOSED.name());
            when(subscriptionService.getAllById("subscription")).thenReturn(List.of(subscription));

            when(apiKeyFetcher.fetchLatest(any(), any(), any())).thenReturn(Flowable.just(List.of(apiKey)));
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiKeyDeployer).undeploy(any());
        }
    }
}
