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

import static io.gravitee.repository.management.model.Event.EventProperties.API_ID;
import static io.gravitee.repository.management.model.EventType.PUBLISH_API;
import static io.gravitee.repository.management.model.EventType.STOP_API;
import static io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY;
import static org.mockito.ArgumentMatchers.*;
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
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.manager.ActionOnApi;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiKeyDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.SubscriptionDeployer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.repository.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.LifecycleState;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
class ApiSynchronizerTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private ApiManager apiManager;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private ApiDeployer apiDeployer;

    @Mock
    private SubscriptionDeployer subscriptionDeployer;

    @Mock
    private ApiKeyDeployer apiKeyDeployer;

    @Mock
    private LatestEventFetcher eventsFetcher;

    private ApiSynchronizer cut;

    @BeforeEach
    public void beforeEach() {
        cut =
            new ApiSynchronizer(
                eventsFetcher,
                apiManager,
                new ApiMapper(objectMapper, new EnvironmentService(environmentRepository, organizationRepository)),
                new PlanAppender(objectMapper, planRepository, gatewayConfiguration),
                new SubscriptionAppender(subscriptionRepository, new SubscriptionMapper(objectMapper)),
                new ApiKeyAppender(apiKeyRepository, new ApiKeyMapper()),
                deployerFactory,
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
            );
        when(eventsFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createApiDeployer()).thenReturn(apiDeployer);
        lenient().when(apiDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(apiDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(apiDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(apiDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());

        lenient().when(deployerFactory.createApiKeyDeployer()).thenReturn(apiKeyDeployer);
        lenient().when(apiKeyDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(apiKeyDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(apiKeyDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(apiKeyDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());

        lenient().when(deployerFactory.createSubscriptionDeployer()).thenReturn(subscriptionDeployer);
        lenient().when(subscriptionDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(subscriptionDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(subscriptionDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(subscriptionDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Nested
    class NoEventTest {

        @Test
        void should_not_synchronize_apis_when_no_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(apiManager);
            verifyNoInteractions(apiDeployer);
            verifyNoInteractions(apiKeyDeployer);
            verifyNoInteractions(subscriptionDeployer);
        }

        @Test
        void should_not_synchronize_apis_when_events_with_unknown_type() throws InterruptedException {
            Event event = new Event();
            event.setId("event");
            when(eventsFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(apiManager);
            verifyNoInteractions(apiDeployer);
            verifyNoInteractions(apiKeyDeployer);
            verifyNoInteractions(subscriptionDeployer);
        }

        @Test
        void should_fetch_init_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("event")).test().await().assertComplete();
            verify(eventsFetcher)
                .fetchLatest(eq(-1L), any(), eq(API_ID), eq(Set.of("event")), eq(Set.of(EventType.PUBLISH_API, EventType.START_API)));
        }

        @Test
        void should_fetch_incremental_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of("event")).test().await().assertComplete();
            verify(eventsFetcher)
                .fetchLatest(
                    any(),
                    any(),
                    eq(API_ID),
                    eq(Set.of("event")),
                    eq(Set.of(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API))
                );
        }
    }

    @Nested
    class EventV2Test {

        private io.gravitee.definition.model.Api api;
        private Api repoApi;

        @BeforeEach
        public void init() throws JsonProcessingException {
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

            repoApi = new Api();
            repoApi.setId("api");
            repoApi.setName("name");
            repoApi.setLifecycleState(LifecycleState.STARTED);
            repoApi.setEnvironmentId("env");
            repoApi.setDefinitionVersion(DefinitionVersion.V2);
            repoApi.setDefinition(objectMapper.writeValueAsString(api));
        }

        @Test
        void should_register_api_when_fetching_publish_events() throws InterruptedException, JsonProcessingException {
            Event event = new Event();
            event.setId("api");
            event.setPayload(objectMapper.writeValueAsString(repoApi));
            event.setType(PUBLISH_API);

            when(eventsFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            when(apiManager.requiredActionFor(argThat(argument -> argument.getId().equals("api")))).thenReturn(ActionOnApi.DEPLOY);
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiDeployer).deploy(any());
            verify(apiDeployer).doAfterDeployment(any());
            verify(subscriptionDeployer).deploy(any());
            verify(subscriptionDeployer).doAfterDeployment(any());
            verify(apiKeyDeployer).deploy(any());
            verify(apiKeyDeployer).doAfterDeployment(any());
        }

        @Test
        void should_unregister_api_when_fetching_close_events() throws InterruptedException {
            Event event = new Event();
            event.setId("api");
            event.setProperties(Map.of(API_ID.getValue(), "api"));
            event.setType(STOP_API);

            when(eventsFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiDeployer).undeploy(any());
            verify(subscriptionDeployer).undeploy(any());
            verify(apiKeyDeployer).undeploy(any());
        }
    }

    @Nested
    class EventV4Test {

        private io.gravitee.definition.model.v4.Api api;
        private Api repoApi;

        @BeforeEach
        public void init() throws JsonProcessingException {
            api = new io.gravitee.definition.model.v4.Api();
            api.setId("api");
            api.setDefinitionVersion(DefinitionVersion.V4);
            PlanSecurity planSecurity = new PlanSecurity();
            planSecurity.setType("api-key");
            io.gravitee.definition.model.v4.plan.Plan plan = io.gravitee.definition.model.v4.plan.Plan
                .builder()
                .id("planId")
                .security(planSecurity)
                .status(PlanStatus.PUBLISHED)
                .build();
            api.setPlans(List.of(plan));

            repoApi = new Api();
            repoApi.setId("api");
            repoApi.setName("name");
            repoApi.setLifecycleState(LifecycleState.STARTED);
            repoApi.setEnvironmentId("env");
            repoApi.setDefinitionVersion(DefinitionVersion.V4);
            repoApi.setType(ApiType.PROXY);
            repoApi.setDefinition(objectMapper.writeValueAsString(api));
        }

        @Test
        void should_register_api_when_fetching_publish_events() throws InterruptedException, JsonProcessingException {
            Event event = new Event();
            event.setId("api");
            event.setPayload(objectMapper.writeValueAsString(repoApi));
            event.setType(PUBLISH_API);

            when(eventsFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            when(apiManager.requiredActionFor(argThat(argument -> argument.getId().equals("api")))).thenReturn(ActionOnApi.DEPLOY);
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiDeployer).deploy(any());
            verify(apiDeployer).doAfterDeployment(any());
            verify(subscriptionDeployer).deploy(any());
            verify(subscriptionDeployer).doAfterDeployment(any());
            verify(apiKeyDeployer).deploy(any());
            verify(apiKeyDeployer).doAfterDeployment(any());
        }

        @Test
        void should_unregister_api_when_fetching_close_events() throws InterruptedException {
            Event event = new Event();
            event.setId("api");
            event.setProperties(Map.of(API_ID.getValue(), "api"));
            event.setType(STOP_API);

            when(eventsFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(apiDeployer).undeploy(any());
            verify(subscriptionDeployer).undeploy(any());
            verify(apiKeyDeployer).undeploy(any());
        }
    }
}
