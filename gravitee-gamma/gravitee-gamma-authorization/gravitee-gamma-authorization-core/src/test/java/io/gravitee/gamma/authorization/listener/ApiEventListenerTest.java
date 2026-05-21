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
package io.gravitee.gamma.authorization.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.McpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.gamma.authorization.api.AuthzAuditPort;
import io.gravitee.gamma.authorization.api.AuthzEntityRepository;
import io.gravitee.gamma.authorization.api.AuthzEventPublisher;
import io.gravitee.gamma.authorization.domain.AuthzEntity;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.repository.InMemoryAuthzEntityRepository;
import io.gravitee.gamma.authorization.repository.InMemoryAuthzPolicyRepository;
import io.gravitee.gamma.authorization.service.AuthzEntityIdValidator;
import io.gravitee.gamma.authorization.service.AuthzEntityServiceImpl;
import io.gravitee.gamma.authorization.service.AuthzSchemaServiceImpl;
import io.gravitee.rest.api.service.event.ApiEvent;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiEventListenerTest {

    private static final String ENV = "env-1";

    private InMemoryAuthzEntityRepository entityRepository;
    private AuthzEntityServiceImpl entityService;
    private ApiEventListener listener;
    private EventManager eventManager;

    @BeforeEach
    void setUp() {
        entityRepository = new InMemoryAuthzEntityRepository();
        AuthzEntityIdValidator validator = new AuthzEntityIdValidator();
        InMemoryAuthzPolicyRepository policyRepository = new InMemoryAuthzPolicyRepository();
        entityService = new AuthzEntityServiceImpl(
            entityRepository,
            policyRepository,
            validator,
            new AuthzSchemaServiceImpl(entityRepository, policyRepository),
            mock(AuthzEventPublisher.class),
            mock(AuthzAuditPort.class)
        );
        eventManager = new EventManagerImpl();
        listener = new ApiEventListener(eventManager, entityService, new EntityIdExtractor());
    }

    @Test
    void deploy_event_upserts_api_alias_for_a_proxy_api() {
        Api api = proxyApi("api-1", null);

        listener.handle(ApiEvent.DEPLOY, api);

        assertThat(entityRepository.findByEntityId(ENV, "api.api-1")).map(AuthzEntity::source).contains("apim");
        assertThat(entityRepository.findAll(ENV)).hasSize(1);
    }

    @Test
    void deploy_event_upserts_api_alias_and_per_tool_aliases_for_an_mcp_proxy_api() {
        Api api = mcpProxyApi(
            "api-1",
            "bookings",
            List.of(mcpToolFlow("get-booking", Set.of("tools/call")), mcpToolFlow("list-bookings", Set.of("tools/call")))
        );

        listener.handle(ApiEvent.DEPLOY, api);

        assertThat(entityRepository.findAll(ENV))
            .extracting(AuthzEntity::entityId)
            .containsExactlyInAnyOrder("api.bookings", "mcp.bookings.get-booking", "mcp.bookings.list-bookings");
    }

    @Test
    void update_event_replaces_attributes_in_place_preserving_internal_id() {
        Api initial = proxyApi("api-1", "Initial Name");

        listener.handle(ApiEvent.DEPLOY, initial);
        String firstInternalId = entityRepository.findByEntityId(ENV, "api.api-1").orElseThrow().id();

        Api renamed = proxyApi("api-1", "Renamed");
        listener.handle(ApiEvent.UPDATE, renamed);

        AuthzEntity reloaded = entityRepository.findByEntityId(ENV, "api.api-1").orElseThrow();
        assertThat(reloaded.id()).isEqualTo(firstInternalId);
        assertThat(reloaded.attributes()).containsEntry("apiName", "Renamed");
    }

    @Test
    void undeploy_event_cascades_delete_for_api_and_its_tool_aliases() {
        Api api = mcpProxyApi("api-1", "bookings", List.of(mcpToolFlow("get-booking", Set.of("tools/call"))));
        listener.handle(ApiEvent.DEPLOY, api);
        assertThat(entityRepository.findAll(ENV)).hasSize(2);

        listener.handle(ApiEvent.UNDEPLOY, api);

        assertThat(entityRepository.findAll(ENV)).isEmpty();
    }

    @Test
    void dynamic_property_events_are_ignored() {
        Api api = proxyApi("api-1", null);

        listener.handle(ApiEvent.START_DYNAMIC_PROPERTY_V4, api);

        assertThat(entityRepository.findAll(ENV)).isEmpty();
    }

    @Test
    void subscribe_filters_out_non_v4_repo_api_events() {
        listener.subscribe();

        io.gravitee.repository.management.model.Api repoApi = new io.gravitee.repository.management.model.Api();
        repoApi.setId("api-1");
        repoApi.setEnvironmentId(ENV);
        repoApi.setDefinitionVersion(DefinitionVersion.V2);
        eventManager.publishEvent(ApiEvent.DEPLOY, repoApi);

        assertThat(entityRepository.findAll(ENV)).isEmpty();
    }

    @Test
    void subscribe_routes_v4_repo_api_event_through_adapter_into_handle() {
        Api preBuiltCoreApi = mcpProxyApi("api-1", "bookings", List.of(mcpToolFlow("get-booking", Set.of("tools/call"))));
        ApiEventListener listenerWithStub = new ApiEventListener(eventManager, entityService, new EntityIdExtractor(), repoApi ->
            preBuiltCoreApi
        );
        listenerWithStub.subscribe();

        io.gravitee.repository.management.model.Api repoApi = new io.gravitee.repository.management.model.Api();
        repoApi.setId("api-1");
        repoApi.setEnvironmentId(ENV);
        repoApi.setDefinitionVersion(DefinitionVersion.V4);
        eventManager.publishEvent(ApiEvent.DEPLOY, repoApi);

        assertThat(entityRepository.findAll(ENV))
            .extracting(AuthzEntity::entityId)
            .containsExactlyInAnyOrder("api.bookings", "mcp.bookings.get-booking");
    }

    @Test
    void unsubscribe_stops_receiving_events_and_is_idempotent() {
        listener.subscribe();
        listener.unsubscribe();

        Api api = proxyApi("api-1", null);
        io.gravitee.repository.management.model.Api repoApi = new io.gravitee.repository.management.model.Api();
        repoApi.setId("api-1");
        repoApi.setEnvironmentId(ENV);
        repoApi.setDefinitionVersion(DefinitionVersion.V4);
        eventManager.publishEvent(ApiEvent.DEPLOY, repoApi);

        assertThat(entityRepository.findAll(ENV)).isEmpty();

        listener.unsubscribe();
    }

    @Test
    void deploy_event_with_partial_batch_failure_continues_remaining_entities() {
        InMemoryAuthzEntityRepository delegate = entityRepository;
        AuthzEntityRepository faultyRepo = new AuthzEntityRepository() {
            @Override
            public AuthzEntity save(AuthzEntity entity) {
                if ("mcp.bookings.get-booking".equals(entity.entityId())) {
                    throw new RuntimeException("simulated transient repo failure");
                }
                return delegate.save(entity);
            }

            @Override
            public java.util.Optional<AuthzEntity> findById(String environmentId, String id) {
                return delegate.findById(environmentId, id);
            }

            @Override
            public java.util.Optional<AuthzEntity> findByEntityId(String environmentId, String entityId) {
                return delegate.findByEntityId(environmentId, entityId);
            }

            @Override
            public java.util.List<AuthzEntity> findAll(String environmentId) {
                return delegate.findAll(environmentId);
            }

            @Override
            public java.util.List<AuthzEntity> findByKind(String environmentId, AuthzEntityKind kind) {
                return delegate.findByKind(environmentId, kind);
            }

            @Override
            public java.util.List<AuthzEntity> findByEntityIdPrefix(String environmentId, String prefix) {
                return delegate.findByEntityIdPrefix(environmentId, prefix);
            }

            @Override
            public boolean deleteById(String environmentId, String id) {
                return delegate.deleteById(environmentId, id);
            }

            @Override
            public boolean deleteByEntityId(String environmentId, String entityId) {
                return delegate.deleteByEntityId(environmentId, entityId);
            }
        };
        InMemoryAuthzPolicyRepository faultyPolicyRepository = new InMemoryAuthzPolicyRepository();
        AuthzEntityServiceImpl faultyService = new AuthzEntityServiceImpl(
            faultyRepo,
            faultyPolicyRepository,
            new AuthzEntityIdValidator(),
            new AuthzSchemaServiceImpl(faultyRepo, faultyPolicyRepository),
            mock(AuthzEventPublisher.class),
            mock(AuthzAuditPort.class)
        );
        ApiEventListener faultyListener = new ApiEventListener(eventManager, faultyService, new EntityIdExtractor());

        Api api = mcpProxyApi("api-1", "bookings", List.of(mcpToolFlow("get-booking", Set.of("tools/call"))));
        faultyListener.handle(ApiEvent.DEPLOY, api);

        assertThat(delegate.findAll(ENV)).extracting(AuthzEntity::entityId).containsExactly("api.bookings");
    }

    @Test
    void update_event_cascades_delete_for_aliases_the_redeployed_api_no_longer_claims() {
        Api initial = mcpProxyApi(
            "api-1",
            "bookings",
            List.of(mcpToolFlow("get-booking", Set.of("tools/call")), mcpToolFlow("list-bookings", Set.of("tools/call")))
        );
        listener.handle(ApiEvent.DEPLOY, initial);
        assertThat(entityRepository.findAll(ENV)).hasSize(3);

        Api redeployed = mcpProxyApi("api-1", "bookings", List.of(mcpToolFlow("get-booking", Set.of("tools/call"))));
        listener.handle(ApiEvent.UPDATE, redeployed);

        assertThat(entityRepository.findAll(ENV))
            .extracting(AuthzEntity::entityId)
            .containsExactlyInAnyOrder("api.bookings", "mcp.bookings.get-booking");
    }

    @Test
    void event_with_blank_environmentId_is_skipped() {
        Api apiWithoutEnv = io.gravitee.apim.core.api.model.Api.builder().id("api-1").environmentId("").build();
        io.gravitee.definition.model.v4.Api def = new io.gravitee.definition.model.v4.Api();
        def.setId("api-1");
        def.setName("api-1");
        def.setApiVersion("1");
        def.setDefinitionVersion(DefinitionVersion.V4);
        def.setType(ApiType.PROXY);
        def.setFlows(List.of());
        apiWithoutEnv.setApiDefinitionValue(def);

        listener.handle(ApiEvent.DEPLOY, apiWithoutEnv);

        assertThat(entityRepository.findAll(ENV)).isEmpty();
        assertThat(entityRepository.findAll("")).isEmpty();
    }

    private static Api proxyApi(String id, String name) {
        io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setId(id);
        definition.setName(name == null ? id : name);
        definition.setApiVersion("1");
        definition.setDefinitionVersion(DefinitionVersion.V4);
        definition.setType(ApiType.PROXY);
        definition.setFlows(List.of());

        Api api = Api.builder().id(id).environmentId(ENV).name(name == null ? id : name).version("1").build();
        api.setApiDefinitionValue(definition);
        return api;
    }

    private static Api mcpProxyApi(String id, String crossId, List<Flow> flows) {
        io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setId(id);
        definition.setName("mcp-" + id);
        definition.setApiVersion("1");
        definition.setDefinitionVersion(DefinitionVersion.V4);
        definition.setType(ApiType.MCP_PROXY);
        definition.setFlows(flows);

        Api api = Api.builder().id(id).crossId(crossId).environmentId(ENV).name("mcp-" + id).version("1").build();
        api.setApiDefinitionValue(definition);
        return api;
    }

    private static Flow mcpToolFlow(String name, Set<String> methods) {
        McpSelector selector = new McpSelector();
        selector.setMethods(methods);

        Flow flow = new Flow();
        flow.setName(name);
        flow.setSelectors(List.<Selector>of(selector));
        return flow;
    }
}
