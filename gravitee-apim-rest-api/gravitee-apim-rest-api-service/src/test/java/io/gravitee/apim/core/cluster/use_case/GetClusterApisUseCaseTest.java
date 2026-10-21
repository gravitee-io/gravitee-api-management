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
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ClusterApiSearchQueryServiceInMemory;
import inmemory.ClusterCrudServiceInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.cluster.domain_service.ClusterApisDomainService;
import io.gravitee.apim.core.cluster.domain_service.ClusterBoundApisQueryService;
import io.gravitee.apim.core.cluster.domain_service.ClusterInUseDomainService;
import io.gravitee.apim.core.cluster.domain_service.DeployedClustersDomainService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetClusterApisUseCaseTest {

    private static final String ENV_ID = "env-id";
    private static final String CLUSTER_ID = "cluster-id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", ENV_ID);

    private final ClusterCrudServiceInMemory clusterCrudService = new ClusterCrudServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final ClusterApiSearchQueryServiceInMemory clusterApiSearchQueryService = new ClusterApiSearchQueryServiceInMemory();
    private final ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService = new ApiAuthorizationDomainServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private GetClusterApisUseCase useCase;

    @BeforeEach
    void setUp() {
        apiQueryService.reset();
        clusterApiSearchQueryService.reset();
        apiAuthorizationDomainService.reset();
        clusterCrudService.initWith(
            List.of(
                Cluster.builder()
                    .id(CLUSTER_ID)
                    .crossId("prod")
                    .type(ClusterType.KAFKA_CLUSTER)
                    .name("Production")
                    .environmentId(ENV_ID)
                    .lifecycleState(ClusterLifecycleState.DEPLOYED)
                    .configuration(
                        Map.of(
                            "connections",
                            List.of(
                                Map.of("crossId", "sasl", "name", "SASL", "bootstrapServers", "kafka:9093"),
                                Map.of("crossId", "plain", "name", "Plaintext", "bootstrapServers", "kafka:9092")
                            )
                        )
                    )
                    .build()
            )
        );
        useCase = new GetClusterApisUseCase(
            clusterCrudService,
            new ClusterApisDomainService(
                clusterApiSearchQueryService,
                apiQueryService,
                new ClusterInUseDomainService(
                    new ClusterBoundApisQueryService(apiQueryService, objectMapper),
                    new DeployedClustersDomainService(new EventLatestQueryServiceInMemory(), objectMapper),
                    objectMapper
                ),
                objectMapper
            ),
            apiAuthorizationDomainService,
            objectMapper
        );
    }

    private void givenApis(List<Api> apis) {
        apiQueryService.initWith(apis);
        clusterApiSearchQueryService.initWith(apis);
    }

    @Test
    void should_mark_readable_only_the_apis_the_user_is_a_member_of() {
        var payments = ApiFixtures.aMessageApiBoundToCluster("payments", ENV_ID, Api.LifecycleState.STARTED, "prod", "sasl");
        var orders = ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, "prod", "plain");
        givenApis(List.of(payments, orders));
        apiAuthorizationDomainService.initWith(List.of(payments));

        var output = useCase.execute(
            new GetClusterApisUseCase.Input(CLUSTER_ID, null, new PageableImpl(1, 10), EXECUTION_CONTEXT, "user-id", false)
        );

        assertThat(output.apis().getContent())
            .extracting(item -> item.clusterApi().api().getId(), GetClusterApisUseCase.Item::readable)
            .containsExactly(tuple("orders", false), tuple("payments", true));
    }

    @Test
    void should_mark_every_api_readable_for_an_admin() {
        givenApis(List.of(ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, "prod", "plain")));

        var output = useCase.execute(
            new GetClusterApisUseCase.Input(CLUSTER_ID, null, new PageableImpl(1, 10), EXECUTION_CONTEXT, "admin-id", true)
        );

        assertThat(output.apis().getContent()).extracting(GetClusterApisUseCase.Item::readable).containsExactly(true);
    }

    @Test
    void should_name_the_connections_of_the_cluster() {
        givenApis(List.of(ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, "prod", "plain")));

        var output = useCase.execute(
            new GetClusterApisUseCase.Input(CLUSTER_ID, null, new PageableImpl(1, 10), EXECUTION_CONTEXT, "user-id", false)
        );

        assertThat(output.connectionNames()).containsOnly(Map.entry("sasl", "SASL"), Map.entry("plain", "Plaintext"));
    }

    @Test
    void should_return_no_api_when_none_uses_the_cluster() {
        givenApis(List.of(ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STARTED, "other", "plain")));

        var output = useCase.execute(
            new GetClusterApisUseCase.Input(CLUSTER_ID, null, new PageableImpl(1, 10), EXECUTION_CONTEXT, "user-id", false)
        );

        assertThat(output.apis().getContent()).isEmpty();
    }
}
