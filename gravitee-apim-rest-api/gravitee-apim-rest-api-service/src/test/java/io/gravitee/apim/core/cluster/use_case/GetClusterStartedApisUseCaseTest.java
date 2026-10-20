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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ClusterCrudServiceInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.cluster.domain_service.ClusterBoundApisQueryService;
import io.gravitee.apim.core.cluster.domain_service.ClusterInUseDomainService;
import io.gravitee.apim.core.cluster.domain_service.DeployedClustersDomainService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.definition.model.cluster.ClusterType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetClusterStartedApisUseCaseTest {

    private static final String ENV_ID = "env-id";
    private static final String CLUSTER_ID = "cluster-id";

    private final ClusterCrudServiceInMemory clusterCrudService = new ClusterCrudServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private GetClusterStartedApisUseCase useCase;

    @BeforeEach
    void setUp() {
        apiQueryService.reset();
        clusterCrudService.initWith(
            List.of(
                Cluster.builder()
                    .id(CLUSTER_ID)
                    .crossId("prod")
                    .type(ClusterType.KAFKA_CLUSTER)
                    .name("Production")
                    .environmentId(ENV_ID)
                    .lifecycleState(ClusterLifecycleState.DEPLOYED)
                    .configuration(Map.of("connections", List.of()))
                    .build()
            )
        );
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiBoundToCluster("on-main", ENV_ID, Api.LifecycleState.STARTED, "prod", "main"),
                ApiFixtures.aNativeApiBoundToCluster("on-spare", ENV_ID, Api.LifecycleState.STARTED, "prod", "spare"),
                ApiFixtures.aMessageApiBoundToCluster("stopped", ENV_ID, Api.LifecycleState.STOPPED, "prod", "main")
            )
        );
        useCase = new GetClusterStartedApisUseCase(
            clusterCrudService,
            new ClusterInUseDomainService(
                new ClusterBoundApisQueryService(apiQueryService, objectMapper),
                new DeployedClustersDomainService(new EventLatestQueryServiceInMemory(), objectMapper),
                objectMapper
            )
        );
    }

    @Test
    void should_list_the_started_apis_using_the_cluster() {
        var output = useCase.execute(new GetClusterStartedApisUseCase.Input(CLUSTER_ID, ENV_ID, Set.of()));

        assertThat(output.apis()).extracting(Api::getId).containsExactly("on-main", "on-spare");
    }

    @Test
    void should_list_the_started_apis_using_the_given_connections() {
        var output = useCase.execute(new GetClusterStartedApisUseCase.Input(CLUSTER_ID, ENV_ID, Set.of("spare")));

        assertThat(output.apis()).extracting(Api::getId).containsExactly("on-spare");
    }
}
