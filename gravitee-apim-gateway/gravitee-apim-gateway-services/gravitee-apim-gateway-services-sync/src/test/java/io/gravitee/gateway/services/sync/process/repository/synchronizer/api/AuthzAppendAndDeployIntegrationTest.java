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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.deployer.ApiDeployer;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.service.AuthzRegistry;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEnginePort;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityIdExtractor;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzPolicyMapper;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.Event.EventProperties;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthzAppendAndDeployIntegrationTest {

    private static final Set<String> ENVS = Set.of("env-1");

    private final ObjectMapper objectMapper = new GraviteeMapper();
    private final AuthzPolicyMapper policyMapper = new AuthzPolicyMapper(objectMapper);

    private RecordingPort enginePort;
    private EventLatestRepository eventLatestRepository;
    private RepositoryAuthzAppender appender;
    private ApiDeployer apiDeployer;
    private ApiManager apiManager;
    private AuthzRegistry authzRegistry;

    @BeforeEach
    void setUp() {
        enginePort = new RecordingPort();
        eventLatestRepository = mock(EventLatestRepository.class);
        apiManager = mock(ApiManager.class);
        authzRegistry = new AuthzRegistry(null);
        appender = new RepositoryAuthzAppender(AuthzEntityIdExtractor.INSTANCE, eventLatestRepository, policyMapper, enginePort);
        apiDeployer = new ApiDeployer(
            apiManager,
            new PlanService(),
            new NoopDistributedSyncService(),
            authzRegistry,
            AuthzEntityIdExtractor.INSTANCE,
            enginePort
        );
    }

    @Test
    void cold_sync_commits_policies_BEFORE_entities_so_the_first_commit_carries_forward_references() {
        ApiReactorDeployable deployable = deployableForApi("bookings");
        Event policyEvent = policyEvent(
            "p-1",
            "{\"id\":\"doc-1\",\"name\":\"Booking access\",\"policyText\":\"permit\",\"kind\":\"RESOURCE\",\"entityId\":\"api.bookings\"}"
        );
        when(eventLatestRepository.search(any(EventCriteria.class), eq(EventProperties.AUTHZ_POLICY_ID), eq(null), eq(null))).thenReturn(
            List.of(policyEvent)
        );

        appender.appends(true, List.of(deployable), ENVS).blockingGet();
        apiDeployer.deploy(deployable).blockingAwait();

        List<EngineOp> ops = List.copyOf(enginePort.ops);

        int firstPolicyAdd = indexOf(ops, op -> "addOrUpdatePolicy".equals(op.op()) && "doc-1".equals(op.id()));
        int firstCommit = indexOf(ops, op -> "commit".equals(op.op()));
        int firstEntityAdd = indexOf(ops, op -> "addOrUpdateEntity".equals(op.op()) && "Resource::\"api.bookings\"".equals(op.id()));
        int secondCommit = indexOfFrom(ops, op -> "commit".equals(op.op()), firstCommit + 1);

        assertThat(firstPolicyAdd).as("appender stages the RESOURCE policy first").isGreaterThanOrEqualTo(0);
        assertThat(firstCommit).as("appender commits AFTER policy staging").isGreaterThan(firstPolicyAdd);
        assertThat(firstEntityAdd).as("ApiDeployer stages the entity AFTER the appender commit").isGreaterThan(firstCommit);
        assertThat(secondCommit).as("ApiDeployer commits AFTER entity staging").isGreaterThan(firstEntityAdd);

        // The first commit reaches the PDP with the doc-1 policy but without the api.bookings
        // entity it references. The PDP MUST tolerate this forward reference (or the appender's
        // soft-fail wrapper silently swallows the rejection). The contract is pinned here: any
        // change that makes the PDP reject forward references will break cold sync.
        boolean entityStagedBeforeFirstCommit = ops
            .subList(0, firstCommit)
            .stream()
            .anyMatch(op -> "addOrUpdateEntity".equals(op.op()) && "Resource::\"api.bookings\"".equals(op.id()));
        assertThat(entityStagedBeforeFirstCommit)
            .as("entity api.bookings is NOT staged before the appender commit — PDP receives a forward reference")
            .isFalse();
    }

    @Test
    void appender_skips_commit_when_no_policy_matches_so_only_ApiDeployer_commit_is_observed() {
        ApiReactorDeployable deployable = deployableForApi("bookings");
        when(eventLatestRepository.search(any(EventCriteria.class), eq(EventProperties.AUTHZ_POLICY_ID), eq(null), eq(null))).thenReturn(
            List.of()
        );

        appender.appends(true, List.of(deployable), ENVS).blockingGet();
        apiDeployer.deploy(deployable).blockingAwait();

        long commits = enginePort.ops
            .stream()
            .filter(op -> "commit".equals(op.op()))
            .count();
        assertThat(commits).as("only ApiDeployer commits when the appender has nothing to stage").isEqualTo(1L);
        assertThat(enginePort.ops).anyMatch(op -> "addOrUpdateEntity".equals(op.op()) && "Resource::\"api.bookings\"".equals(op.id()));
    }

    private static int indexOf(List<EngineOp> ops, java.util.function.Predicate<EngineOp> predicate) {
        return indexOfFrom(ops, predicate, 0);
    }

    private static int indexOfFrom(List<EngineOp> ops, java.util.function.Predicate<EngineOp> predicate, int from) {
        for (int i = from; i < ops.size(); i++) {
            if (predicate.test(ops.get(i))) {
                return i;
            }
        }
        return -1;
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

    private static Event policyEvent(String id, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setType(EventType.PUBLISH_AUTHZ_POLICY);
        event.setPayload(payload);
        return event;
    }

    private record EngineOp(String op, String id) {}

    private static class RecordingPort implements AuthzEnginePort {

        final ConcurrentLinkedQueue<EngineOp> ops = new ConcurrentLinkedQueue<>();

        @Override
        public Completable addOrUpdateEntity(String uid, Map<String, Object> attributes, List<String> parents) {
            ops.add(new EngineOp("addOrUpdateEntity", uid));
            return Completable.complete();
        }

        @Override
        public Completable removeEntity(String uid) {
            ops.add(new EngineOp("removeEntity", uid));
            return Completable.complete();
        }

        @Override
        public Completable addOrUpdatePolicy(String docId, String name, String policyText) {
            ops.add(new EngineOp("addOrUpdatePolicy", docId));
            return Completable.complete();
        }

        @Override
        public Completable removePolicy(String docId) {
            ops.add(new EngineOp("removePolicy", docId));
            return Completable.complete();
        }

        @Override
        public Completable commit() {
            ops.add(new EngineOp("commit", null));
            return Completable.complete();
        }
    }
}
