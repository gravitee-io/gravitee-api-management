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

import static io.gravitee.repository.management.model.EventType.PUBLISH_API;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.manager.ActionOnApi;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiKeyDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.SubscriptionDeployer;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEnginePort;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityIdExtractor;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzPolicyMapper;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.Event.EventProperties;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.LifecycleState;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthzAppenderIntegrationTest {

    private static final Set<String> ENVS = Set.of();
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

    @Mock
    private EventLatestRepository eventLatestRepository;

    @Mock
    private AuthzEnginePort enginePort;

    private ApiSynchronizer cut;

    @BeforeEach
    void setUp() {
        AuthzAppender realAppender = new RepositoryAuthzAppender(
            AuthzEntityIdExtractor.INSTANCE,
            eventLatestRepository,
            new AuthzPolicyMapper(objectMapper),
            enginePort
        );
        cut = new ApiSynchronizer(
            eventsFetcher,
            apiManager,
            new ApiMapper(objectMapper, new EnvironmentService(environmentRepository, organizationRepository)),
            new PlanAppender(objectMapper, planRepository, gatewayConfiguration, null),
            new SubscriptionAppender(
                subscriptionRepository,
                new SubscriptionMapper(objectMapper, mock(io.gravitee.gateway.handlers.api.registry.ApiProductRegistry.class)),
                mock(io.gravitee.gateway.handlers.api.registry.ApiProductRegistry.class),
                100
            ),
            new ApiKeyAppender(apiKeyRepository, new ApiKeyMapper(), 100, 900),
            realAppender,
            deployerFactory,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            1 // appenderParallelism: sequential for test determinism
        );
        when(eventsFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createApiDeployer()).thenReturn(apiDeployer);
        lenient().when(apiDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(apiDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(deployerFactory.createApiKeyDeployer()).thenReturn(apiKeyDeployer);
        lenient().when(apiKeyDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(apiKeyDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(deployerFactory.createSubscriptionDeployer()).thenReturn(subscriptionDeployer);
        lenient().when(subscriptionDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(subscriptionDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
    }

    @Test
    void cold_start_with_matching_resource_policy_stages_engine_state_and_still_deploys_api() throws Exception {
        Event apiEvent = publishApiEvent("bookings");
        when(eventsFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(apiEvent)));
        when(apiManager.requiredActionFor(argThat(argument -> argument.getId().equals("bookings")))).thenReturn(ActionOnApi.DEPLOY);

        Event policyEvent = policyEvent(
            "p-1",
            "{\"id\":\"doc-1\",\"name\":\"Booking access\",\"policyText\":\"permit\",\"kind\":\"RESOURCE\",\"entityId\":\"api.bookings\"}"
        );
        when(eventLatestRepository.search(any(EventCriteria.class), eq(EventProperties.AUTHZ_POLICY_ID), eq(null), eq(null))).thenReturn(
            List.of(policyEvent)
        );
        when(enginePort.addOrUpdatePolicy(eq("doc-1"), eq("Booking access"), eq("permit"))).thenReturn(Completable.complete());
        when(enginePort.commit()).thenReturn(Completable.complete());

        cut.synchronize(-1L, Instant.now().toEpochMilli(), ENVS).test().await().assertComplete();

        verify(apiDeployer).deploy(any());
        verify(apiDeployer).doAfterDeployment(any());
        verify(enginePort, atLeastOnce()).addOrUpdatePolicy(eq("doc-1"), eq("Booking access"), eq("permit"));
        verify(enginePort, atLeastOnce()).commit();
    }

    private static Event policyEvent(String id, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setType(EventType.PUBLISH_AUTHZ_POLICY);
        event.setPayload(payload);
        return event;
    }

    private Event publishApiEvent(String id) throws JsonProcessingException {
        io.gravitee.definition.model.v4.plan.PlanSecurity planSecurity = new io.gravitee.definition.model.v4.plan.PlanSecurity();
        planSecurity.setType("api-key");
        io.gravitee.definition.model.v4.plan.Plan plan = io.gravitee.definition.model.v4.plan.Plan.builder()
            .id("planId")
            .security(planSecurity)
            .status(io.gravitee.definition.model.v4.plan.PlanStatus.PUBLISHED)
            .build();

        io.gravitee.definition.model.v4.Api def = new io.gravitee.definition.model.v4.Api();
        def.setId(id);
        def.setName("test");
        def.setApiVersion("1");
        def.setDefinitionVersion(DefinitionVersion.V4);
        def.setType(ApiType.PROXY);
        def.setFlows(List.of());
        def.setPlans(List.of(plan));

        Api repoApi = new Api();
        repoApi.setId(id);
        repoApi.setName("test");
        repoApi.setLifecycleState(LifecycleState.STARTED);
        repoApi.setEnvironmentId("env");
        repoApi.setDefinitionVersion(DefinitionVersion.V4);
        repoApi.setType(ApiType.PROXY);
        repoApi.setDefinition(objectMapper.writeValueAsString(def));

        Event event = new Event();
        event.setId(id);
        event.setType(PUBLISH_API);
        event.setPayload(objectMapper.writeValueAsString(repoApi));
        return event;
    }

    private static Event entityEvent(String id, String groupKey, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setType(EventType.PUBLISH_AUTHZ_ENTITY);
        Map<String, String> props = new HashMap<>();
        props.put(EventProperties.AUTHZ_ENTITY_ID.getValue(), groupKey);
        event.setProperties(props);
        event.setPayload(payload);
        return event;
    }
}
