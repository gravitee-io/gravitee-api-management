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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.api;

import static io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SubscriptionMapper;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.Date;
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
class DistributedApiSynchronizerTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private DistributedEventFetcher eventsFetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private ApiDeployer apiDeployer;

    private DistributedApiSynchronizer cut;

    @BeforeEach
    public void beforeEach() {
        cut =
            new DistributedApiSynchronizer(
                eventsFetcher,
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
                deployerFactory,
                new ApiMapper(objectMapper, new SubscriptionMapper(objectMapper), new ApiKeyMapper(objectMapper))
            );
        when(eventsFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createApiDeployer()).thenReturn(apiDeployer);
        lenient().when(apiDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(apiDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(apiDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(apiDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Nested
    class NoEventTest {

        @Test
        void should_not_synchronize_apis_when_no_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), eq(DistributedEventType.API), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();
            verifyNoInteractions(apiDeployer);
        }

        @Test
        void should_fetch_init_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();
            verify(eventsFetcher).fetchLatest(eq(-1L), any(), eq(DistributedEventType.API), eq(Set.of(DistributedSyncAction.DEPLOY)));
        }

        @Test
        void should_fetch_incremental_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli()).test().await().assertComplete();
            verify(eventsFetcher)
                .fetchLatest(
                    any(),
                    any(),
                    eq(DistributedEventType.API),
                    eq(Set.of(DistributedSyncAction.DEPLOY, DistributedSyncAction.UNDEPLOY))
                );
        }
    }

    @Nested
    class DistributedEventApiV2Test {

        private io.gravitee.definition.model.Api api;
        private Api reactableApi;

        @BeforeEach
        public void init() {
            api = new io.gravitee.definition.model.Api();
            api.setId("api");
            api.setDefinitionVersion(DefinitionVersion.V2);
            Proxy proxy = new Proxy();
            proxy.setVirtualHosts(List.of(new VirtualHost("/test")));
            api.setProxy(proxy);
            io.gravitee.definition.model.Plan plan = new io.gravitee.definition.model.Plan();
            plan.setId("planId");
            plan.setApi("apiId");
            plan.setSecurity(API_KEY.name());
            plan.setStatus("PUBLISHED");
            api.setPlans(List.of(plan));

            reactableApi = new Api(api);
        }

        @Test
        void should_register_api_when_fetching_publish_events() throws InterruptedException, JsonProcessingException {
            DistributedEvent distributedEvent = DistributedEvent
                .builder()
                .id("api")
                .payload(objectMapper.writeValueAsString(reactableApi))
                .type(DistributedEventType.API)
                .syncAction(DistributedSyncAction.DEPLOY)
                .updatedAt(new Date())
                .build();

            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(distributedEvent));
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

            verify(apiDeployer).deploy(any());
            verify(apiDeployer).doAfterDeployment(any());
        }

        @Test
        void should_unregister_api_when_fetching_close_events() throws InterruptedException, JsonProcessingException {
            DistributedEvent distributedEvent = DistributedEvent
                .builder()
                .id("api")
                .payload(objectMapper.writeValueAsString(reactableApi))
                .type(DistributedEventType.API)
                .syncAction(DistributedSyncAction.UNDEPLOY)
                .updatedAt(new Date())
                .build();

            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(distributedEvent));
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

            verify(apiDeployer).undeploy(any());
            verify(apiDeployer).doAfterUndeployment(any());
        }
    }

    @Nested
    class DistributedEventApiV4Test {

        private io.gravitee.definition.model.v4.Api api;
        private io.gravitee.gateway.reactive.handlers.api.v4.Api reactableApi;

        @BeforeEach
        public void init() {
            api = new io.gravitee.definition.model.v4.Api();
            api.setId("api");
            api.setDefinitionVersion(DefinitionVersion.V4);
            api.setType(ApiType.PROXY);
            PlanSecurity planSecurity = new PlanSecurity();
            planSecurity.setType("api-key");
            io.gravitee.definition.model.v4.plan.Plan plan = io.gravitee.definition.model.v4.plan.Plan
                .builder()
                .id("planId")
                .security(planSecurity)
                .status(PlanStatus.PUBLISHED)
                .build();
            api.setPlans(List.of(plan));

            reactableApi = new io.gravitee.gateway.reactive.handlers.api.v4.Api(api);
        }

        @Test
        void should_register_api_when_fetching_publish_events() throws InterruptedException, JsonProcessingException {
            DistributedEvent distributedEvent = DistributedEvent
                .builder()
                .id("api")
                .payload(objectMapper.writeValueAsString(reactableApi))
                .type(DistributedEventType.API)
                .syncAction(DistributedSyncAction.DEPLOY)
                .updatedAt(new Date())
                .build();

            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(distributedEvent));
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

            verify(apiDeployer).deploy(any());
            verify(apiDeployer).doAfterDeployment(any());
        }

        @Test
        void should_unregister_api_when_fetching_close_events() throws InterruptedException, JsonProcessingException {
            DistributedEvent distributedEvent = DistributedEvent
                .builder()
                .id("api")
                .payload(objectMapper.writeValueAsString(reactableApi))
                .type(DistributedEventType.API)
                .syncAction(DistributedSyncAction.UNDEPLOY)
                .updatedAt(new Date())
                .build();

            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(distributedEvent));
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

            verify(apiDeployer).undeploy(any());
            verify(apiDeployer).doAfterUndeployment(any());
        }
    }
}
