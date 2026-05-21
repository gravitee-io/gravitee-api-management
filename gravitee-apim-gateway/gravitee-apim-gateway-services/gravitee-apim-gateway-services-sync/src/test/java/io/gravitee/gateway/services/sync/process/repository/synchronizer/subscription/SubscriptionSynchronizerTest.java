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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.SubscriptionDeployer;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.repository.fetcher.SubscriptionFetcher;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.repository.management.model.Subscription;
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
class SubscriptionSynchronizerTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private SubscriptionFetcher subscriptionFetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private SubscriptionDeployer subscriptionDeployer;

    private SubscriptionSynchronizer cut;
    private PlanService planCache;

    @BeforeEach
    public void beforeEach() {
        planCache = new PlanService();
        cut = new SubscriptionSynchronizer(
            subscriptionFetcher,
            new SubscriptionMapper(objectMapper, mock(ApiProductRegistry.class)),
            deployerFactory,
            planCache,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );
        lenient().when(deployerFactory.createSubscriptionDeployer()).thenReturn(subscriptionDeployer);
        lenient().when(subscriptionDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(subscriptionDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(subscriptionDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(subscriptionDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Nested
    class NoEventTest {

        @Test
        void should_not_fetch_init_events() {
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env")).test().assertComplete();
            verifyNoInteractions(subscriptionFetcher);
        }

        @Test
        void should_fetch_incremental_events() throws InterruptedException {
            when(subscriptionFetcher.fetchLatest(any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(1L, 1L, Set.of("env")).test().await().assertComplete();
            verify(subscriptionFetcher).fetchLatest(1L, 1L, Set.of("env"));
        }

        @Test
        void should_not_synchronize_api_keys_when_no_events() throws InterruptedException {
            when(subscriptionFetcher.fetchLatest(any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(subscriptionDeployer);
        }
    }

    @Nested
    class SubscriptionSynchronizationTest {

        @Test
        void should_ignore_subscriptions_without_deployed_plan() throws InterruptedException {
            io.gravitee.repository.management.model.Subscription subscription = new io.gravitee.repository.management.model.Subscription();
            subscription.setId("subscription");
            subscription.setApi("api");
            subscription.setPlan("plan");
            subscription.setStatus(io.gravitee.repository.management.model.Subscription.Status.ACCEPTED);

            when(subscriptionFetcher.fetchLatest(any(), any(), any())).thenReturn(Flowable.just(List.of(subscription)));
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(subscriptionDeployer);
        }

        @Test
        void should_deploy_subscriptions_when_fetching_subscriptions() throws InterruptedException {
            io.gravitee.repository.management.model.Subscription subscription = new io.gravitee.repository.management.model.Subscription();
            subscription.setId("subscription");
            subscription.setApi("api");
            subscription.setPlan("plan");
            subscription.setStatus(io.gravitee.repository.management.model.Subscription.Status.ACCEPTED);

            planCache.register(ApiReactorDeployable.builder().apiId("api").subscribablePlans(Set.of("plan")).build());

            when(subscriptionFetcher.fetchLatest(any(), any(), any())).thenReturn(Flowable.just(List.of(subscription)));
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(subscriptionDeployer).deploy(any());
            verify(subscriptionDeployer).doAfterDeployment(any());
        }

        @Test
        void should_deploy_each_subscription_exactly_once_across_multiple_pages() throws InterruptedException {
            io.gravitee.repository.management.model.Subscription a = subscription("sub-a");
            io.gravitee.repository.management.model.Subscription b = subscription("sub-b");
            io.gravitee.repository.management.model.Subscription c = subscription("sub-c");
            planCache.register(ApiReactorDeployable.builder().apiId("api").subscribablePlans(Set.of("plan")).build());

            when(subscriptionFetcher.fetchLatest(any(), any(), any())).thenReturn(Flowable.just(List.of(a, b), List.of(c)));
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(subscriptionDeployer, org.mockito.Mockito.times(3)).deploy(any());
            verify(subscriptionDeployer, org.mockito.Mockito.times(3)).doAfterDeployment(any());
        }

        private io.gravitee.repository.management.model.Subscription subscription(String id) {
            io.gravitee.repository.management.model.Subscription s = new io.gravitee.repository.management.model.Subscription();
            s.setId(id);
            s.setApi("api");
            s.setPlan("plan");
            s.setStatus(io.gravitee.repository.management.model.Subscription.Status.ACCEPTED);
            return s;
        }

        @Test
        void should_deploy_all_subscriptions_when_real_fetcher_streams_same_ms_pages_via_repository()
            throws InterruptedException, io.gravitee.repository.exceptions.TechnicalException {
            // Wire the real SubscriptionFetcher against a stubbed SubscriptionRepository so the
            // cursor-advance code in the fetcher is exercised through the full synchronizer chain.
            // Scenario: 5 subs sharing the same updatedAt across 3 pages (2,2,1) — boundary cursor
            // semantics must not drop any sub at the page boundary.
            io.gravitee.repository.management.api.SubscriptionRepository repo = mock(
                io.gravitee.repository.management.api.SubscriptionRepository.class
            );
            int bulkItems = 2;
            io.gravitee.gateway.services.sync.process.repository.fetcher.SubscriptionFetcher realFetcher =
                new io.gravitee.gateway.services.sync.process.repository.fetcher.SubscriptionFetcher(repo, bulkItems);

            SubscriptionSynchronizer realCut = new SubscriptionSynchronizer(
                realFetcher,
                new SubscriptionMapper(objectMapper, mock(ApiProductRegistry.class)),
                deployerFactory,
                planCache,
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
            );

            long sameMs = 7777L;
            io.gravitee.repository.management.model.Subscription a = sub("sub-a", sameMs);
            io.gravitee.repository.management.model.Subscription b = sub("sub-b", sameMs);
            io.gravitee.repository.management.model.Subscription c = sub("sub-c", sameMs);
            io.gravitee.repository.management.model.Subscription d = sub("sub-d", sameMs);
            io.gravitee.repository.management.model.Subscription e = sub("sub-e", sameMs);
            planCache.register(ApiReactorDeployable.builder().apiId("api").subscribablePlans(Set.of("plan")).build());

            // Page 1: cursor = null, return a + b
            when(
                repo.searchAfter(any(), any(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(bulkItems))
            ).thenReturn(List.of(a, b));
            // Page 2: cursor at b's (updatedAt, id), return c + d
            when(
                repo.searchAfter(
                    any(),
                    any(),
                    org.mockito.ArgumentMatchers.argThat(cursor -> cursor != null && "sub-b".equals(cursor.id())),
                    org.mockito.ArgumentMatchers.eq(bulkItems)
                )
            ).thenReturn(List.of(c, d));
            // Page 3 (short): cursor at d, return only e
            when(
                repo.searchAfter(
                    any(),
                    any(),
                    org.mockito.ArgumentMatchers.argThat(cursor -> cursor != null && "sub-d".equals(cursor.id())),
                    org.mockito.ArgumentMatchers.eq(bulkItems)
                )
            ).thenReturn(List.of(e));

            realCut.synchronize(1L, 2L, Set.of("env")).test().await().assertComplete();

            // Every sub deployed exactly once — no skip, no duplicate across the same-ms boundary
            verify(subscriptionDeployer, org.mockito.Mockito.times(5)).deploy(any());
            verify(subscriptionDeployer, org.mockito.Mockito.times(5)).doAfterDeployment(any());
        }

        private io.gravitee.repository.management.model.Subscription sub(String id, long updatedAtMs) {
            io.gravitee.repository.management.model.Subscription s = subscription(id);
            s.setUpdatedAt(new java.util.Date(updatedAtMs));
            return s;
        }

        @Test
        void should_undeploy_subscriptions_when_fetching_inactive_api_keys() throws InterruptedException {
            io.gravitee.repository.management.model.Subscription subscription = new io.gravitee.repository.management.model.Subscription();
            subscription.setId("subscription");
            subscription.setApi("api");
            subscription.setPlan("plan");
            subscription.setStatus(Subscription.Status.CLOSED);
            planCache.register(ApiReactorDeployable.builder().apiId("api").subscribablePlans(Set.of("plan")).build());

            when(subscriptionFetcher.fetchLatest(any(), any(), any())).thenReturn(Flowable.just(List.of(subscription)));
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(subscriptionDeployer).undeploy(any());
        }
    }
}
