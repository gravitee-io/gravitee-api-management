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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEnginePort;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityIdExtractor;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzPolicyMapper;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.Event.EventProperties;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthzAppenderTest {

    private static final Set<String> ENVS = Set.of("env-1");

    @Mock
    private EventLatestRepository eventLatestRepository;

    @Mock
    private AuthzEnginePort enginePort;

    private final AuthzEntityIdExtractor extractor = new AuthzEntityIdExtractor();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthzPolicyMapper policyMapper = new AuthzPolicyMapper(objectMapper);

    private RepositoryAuthzAppender cut;

    @BeforeEach
    void setUp() {
        cut = new RepositoryAuthzAppender(extractor, eventLatestRepository, policyMapper, enginePort);
    }

    @Test
    void incremental_sync_is_a_no_op() {
        List<ApiReactorDeployable> deployables = List.of(deployableForApi("api-1"));

        List<ApiReactorDeployable> result = cut.appends(false, deployables, ENVS).blockingGet();

        assertThat(result).isSameAs(deployables);
        verifyNoInteractions(eventLatestRepository);
        verifyNoInteractions(enginePort);
    }

    @Test
    void null_deployables_returns_empty_list_without_repo_calls() {
        List<ApiReactorDeployable> result = cut.appends(true, null, ENVS).blockingGet();

        assertThat(result).isEmpty();
        verifyNoInteractions(eventLatestRepository);
        verifyNoInteractions(enginePort);
    }

    @Test
    void empty_deployables_returns_unchanged_without_repo_calls() {
        List<ApiReactorDeployable> result = cut.appends(true, List.of(), ENVS).blockingGet();

        assertThat(result).isEmpty();
        verifyNoInteractions(eventLatestRepository);
        verifyNoInteractions(enginePort);
    }

    @Test
    void deployables_with_no_extractable_entityIds_short_circuit_before_repo_calls() {
        ApiReactorDeployable nullApi = ApiReactorDeployable.builder().apiId("api-1").build();

        List<ApiReactorDeployable> result = cut.appends(true, List.of(nullApi), ENVS).blockingGet();

        assertThat(result).containsExactly(nullApi);
        verifyNoInteractions(eventLatestRepository);
        verifyNoInteractions(enginePort);
    }

    @Test
    void stages_resource_policy_when_entityId_matches_a_claimed_entityId_and_commits() {
        ApiReactorDeployable deployable = deployableForApi("bookings");

        Event matching = policyEvent("p-1", resourcePolicyPayload("doc-1", "Booking access", "permit", "api.bookings"));
        Event unrelated = policyEvent("p-2", resourcePolicyPayload("doc-2", "Other", "permit", "api.other"));
        Event globalPolicy = policyEvent("p-3", globalPolicyPayload("doc-3", "Global", "permit"));

        when(eventLatestRepository.search(any(EventCriteria.class), eq(EventProperties.AUTHZ_POLICY_ID), eq(null), eq(null))).thenReturn(
            List.of(matching, unrelated, globalPolicy)
        );
        when(enginePort.addOrUpdatePolicy(eq("doc-1"), eq("Booking access"), eq("permit"))).thenReturn(Completable.complete());
        when(enginePort.commit()).thenReturn(Completable.complete());

        cut.appends(true, List.of(deployable), ENVS).blockingGet();

        verify(enginePort).addOrUpdatePolicy(eq("doc-1"), eq("Booking access"), eq("permit"));
        verify(enginePort, never()).addOrUpdatePolicy(eq("doc-2"), any(), any());
        verify(enginePort, never()).addOrUpdatePolicy(eq("doc-3"), any(), any());
        verify(enginePort).commit();
    }

    @Test
    void no_commit_when_no_policies_staged() {
        ApiReactorDeployable deployable = deployableForApi("bookings");

        when(eventLatestRepository.search(any(EventCriteria.class), eq(EventProperties.AUTHZ_POLICY_ID), eq(null), eq(null))).thenReturn(
            List.of()
        );

        cut.appends(true, List.of(deployable), ENVS).blockingGet();

        verify(enginePort, never()).commit();
    }

    @Test
    void repository_failure_does_not_break_api_deploy_chain() {
        ApiReactorDeployable deployable = deployableForApi("bookings");

        when(eventLatestRepository.search(any(EventCriteria.class), eq(EventProperties.AUTHZ_POLICY_ID), eq(null), eq(null))).thenThrow(
            new RuntimeException("repo down")
        );

        List<ApiReactorDeployable> result = cut.appends(true, List.of(deployable), ENVS).blockingGet();

        assertThat(result).containsExactly(deployable);
        verify(enginePort, never()).addOrUpdatePolicy(any(), any(), any());
        verify(enginePort, never()).commit();
    }

    @Test
    void engine_port_failure_during_policy_staging_is_swallowed_and_pass_through() {
        ApiReactorDeployable deployable = deployableForApi("bookings");

        when(eventLatestRepository.search(any(EventCriteria.class), eq(EventProperties.AUTHZ_POLICY_ID), eq(null), eq(null))).thenReturn(
            List.of(policyEvent("p-1", resourcePolicyPayload("doc-1", "Booking", "permit", "api.bookings")))
        );
        when(enginePort.addOrUpdatePolicy(any(), any(), any())).thenReturn(Completable.error(new RuntimeException("engine down")));

        List<ApiReactorDeployable> result = cut.appends(true, List.of(deployable), ENVS).blockingGet();

        assertThat(result).containsExactly(deployable);
        verify(enginePort, never()).commit();
    }

    @Test
    void repo_returning_null_event_list_is_treated_as_empty() {
        ApiReactorDeployable deployable = deployableForApi("bookings");

        when(eventLatestRepository.search(any(EventCriteria.class), eq(EventProperties.AUTHZ_POLICY_ID), eq(null), eq(null))).thenReturn(
            null
        );

        List<ApiReactorDeployable> result = cut.appends(true, List.of(deployable), ENVS).blockingGet();

        assertThat(result).containsExactly(deployable);
        verify(enginePort, never()).addOrUpdatePolicy(any(), any(), any());
        verify(enginePort, never()).commit();
    }

    private static ApiReactorDeployable deployableForApi(String id) {
        io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setId(id);
        definition.setName("test");
        definition.setApiVersion("1");
        definition.setDefinitionVersion(DefinitionVersion.V4);
        definition.setType(ApiType.PROXY);
        definition.setFlows(List.of());
        ReactableApi<?> reactableApi = new io.gravitee.gateway.reactive.handlers.api.v4.Api(definition);
        return ApiReactorDeployable.builder().apiId(id).reactableApi(reactableApi).build();
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

    private static Event policyEvent(String id, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setType(EventType.PUBLISH_AUTHZ_POLICY);
        event.setPayload(payload);
        return event;
    }

    private static String resourceEntityPayload(String entityId) {
        return "{\"entityId\":\"" + entityId + "\",\"kind\":\"RESOURCE\",\"attributes\":{},\"parents\":[]}";
    }

    private static String principalEntityPayload(String entityId) {
        return "{\"entityId\":\"" + entityId + "\",\"kind\":\"PRINCIPAL\",\"attributes\":{},\"parents\":[]}";
    }

    private static String resourcePolicyPayload(String docId, String name, String policyText, String entityId) {
        return (
            "{\"id\":\"" +
            docId +
            "\",\"name\":\"" +
            name +
            "\",\"policyText\":\"" +
            policyText +
            "\",\"kind\":\"RESOURCE\",\"entityId\":\"" +
            entityId +
            "\"}"
        );
    }

    private static String globalPolicyPayload(String docId, String name, String policyText) {
        return "{\"id\":\"" + docId + "\",\"name\":\"" + name + "\",\"policyText\":\"" + policyText + "\",\"kind\":\"GLOBAL\"}";
    }
}
