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
package io.gravitee.apim.core.cluster.use_case;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.ClusterCrudServiceInMemory;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.cluster.domain_service.UndeployClusterDomainService;
import io.gravitee.apim.core.cluster.domain_service.VirtualClusterBoundApisQueryService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UndeployClusterUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String CLUSTER_ID = "cluster-id";
    private static final String CLUSTER_CROSS_ID = "my-cluster";
    private final String ORG_ID = "org-id";
    private final String ENV_ID = "env-id";
    private final String USER_ID = "user-id";
    private final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId(ORG_ID)
        .environmentId(ENV_ID)
        .actor(AuditActor.builder().userId(USER_ID).build())
        .build();

    private final ClusterCrudServiceInMemory clusterCrudService = new ClusterCrudServiceInMemory();
    private final EventCrudInMemory eventCrudInMemory = new EventCrudInMemory();
    private final EventLatestCrudInMemory eventLatestCrudInMemory = new EventLatestCrudInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UndeployClusterUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var undeployClusterDomainService = new UndeployClusterDomainService(
            clusterCrudService,
            eventCrudInMemory,
            eventLatestCrudInMemory,
            auditService
        );
        var virtualClusterBoundApisQueryService = new VirtualClusterBoundApisQueryService(apiQueryService, objectMapper);
        apiQueryService.reset();
        useCase = new UndeployClusterUseCase(clusterCrudService, undeployClusterDomainService, virtualClusterBoundApisQueryService);
    }

    @Test
    void should_undeploy_cluster() {
        Cluster existing = Cluster.builder()
            .id(CLUSTER_ID)
            .crossId(CLUSTER_CROSS_ID)
            .type(ClusterType.KAFKA_CLUSTER)
            .name("My Cluster")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .lifecycleState(ClusterLifecycleState.DEPLOYED)
            .version(1)
            .configuration(Map.of("connections", List.of()))
            .build();
        clusterCrudService.initWith(List.of(existing));

        var output = useCase.execute(new UndeployClusterUseCase.Input(CLUSTER_ID, AUDIT_INFO));

        assertThat(output.cluster().getLifecycleState()).isEqualTo(ClusterLifecycleState.UNDEPLOYED);
        assertThat(output.cluster().getDeployedAt()).isEqualTo(INSTANT_NOW);
        assertThat(output.cluster().getVersion()).isEqualTo(1);
    }

    @Test
    void should_publish_undeploy_event() {
        Cluster existing = Cluster.builder()
            .id(CLUSTER_ID)
            .crossId(CLUSTER_CROSS_ID)
            .type(ClusterType.KAFKA_CLUSTER)
            .name("My Cluster")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .lifecycleState(ClusterLifecycleState.DEPLOYED)
            .version(1)
            .configuration(Map.of("connections", List.of()))
            .build();
        clusterCrudService.initWith(List.of(existing));

        useCase.execute(new UndeployClusterUseCase.Input(CLUSTER_ID, AUDIT_INFO));

        assertThat(eventCrudInMemory.storage()).hasSize(1);
        assertThat(eventCrudInMemory.storage().get(0).getType()).isEqualTo(EventType.UNDEPLOY_CLUSTER);
        assertThat(eventCrudInMemory.storage().get(0).getProperties()).containsAllEntriesOf(
            Map.ofEntries(entry(Event.EventProperties.USER, USER_ID), entry(Event.EventProperties.CLUSTER_ID, CLUSTER_CROSS_ID))
        );
    }

    @Test
    void should_create_audit_log() {
        Cluster existing = Cluster.builder()
            .id(CLUSTER_ID)
            .crossId(CLUSTER_CROSS_ID)
            .type(ClusterType.KAFKA_CLUSTER)
            .name("My Cluster")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .lifecycleState(ClusterLifecycleState.DEPLOYED)
            .version(1)
            .configuration(Map.of("connections", List.of()))
            .build();
        clusterCrudService.initWith(List.of(existing));

        useCase.execute(new UndeployClusterUseCase.Input(CLUSTER_ID, AUDIT_INFO));

        assertThat(auditCrudService.storage()).hasSize(1);
        assertThat(auditCrudService.storage().get(0).getEvent()).isEqualTo("CLUSTER_UNDEPLOYED");
    }

    @Test
    void should_undeploy_virtual_cluster_when_no_started_bound_api() {
        Cluster existing = Cluster.builder()
            .id(CLUSTER_ID)
            .crossId(CLUSTER_CROSS_ID)
            .type(ClusterType.KAFKA_VIRTUAL_CLUSTER)
            .name("My Virtual Cluster")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .lifecycleState(ClusterLifecycleState.DEPLOYED)
            .version(1)
            .configuration(Map.of("backends", List.of()))
            .build();
        clusterCrudService.initWith(List.of(existing));
        // A stopped API bound to the same virtual cluster must not block the undeploy.
        apiQueryService.initWith(
            List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("stopped-api", ENV_ID, Api.LifecycleState.STOPPED, CLUSTER_CROSS_ID))
        );

        var output = useCase.execute(new UndeployClusterUseCase.Input(CLUSTER_ID, AUDIT_INFO));

        assertThat(output.cluster().getLifecycleState()).isEqualTo(ClusterLifecycleState.UNDEPLOYED);
    }

    @Test
    void should_throw_when_undeploying_virtual_cluster_with_started_bound_apis() {
        Cluster existing = Cluster.builder()
            .id(CLUSTER_ID)
            .crossId(CLUSTER_CROSS_ID)
            .type(ClusterType.KAFKA_VIRTUAL_CLUSTER)
            .name("My Virtual Cluster")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .lifecycleState(ClusterLifecycleState.DEPLOYED)
            .version(1)
            .configuration(Map.of("backends", List.of()))
            .build();
        clusterCrudService.initWith(List.of(existing));
        apiQueryService.initWith(
            List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("started-api", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_CROSS_ID))
        );

        assertThatThrownBy(() -> useCase.execute(new UndeployClusterUseCase.Input(CLUSTER_ID, AUDIT_INFO)))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("Cannot undeploy virtual cluster")
            .hasMessageContaining("started-api");

        // The block must have no side effect: the cluster stays deployed and no undeploy event is emitted.
        assertThat(clusterCrudService.storage().get(0).getLifecycleState()).isEqualTo(ClusterLifecycleState.DEPLOYED);
        assertThat(eventCrudInMemory.storage()).isEmpty();
    }
}
