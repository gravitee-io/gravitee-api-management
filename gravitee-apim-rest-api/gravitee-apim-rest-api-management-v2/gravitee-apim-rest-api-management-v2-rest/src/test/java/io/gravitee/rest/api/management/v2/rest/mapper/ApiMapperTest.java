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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import fixtures.ApiFixtures;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.rest.api.management.v2.rest.model.BaseOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.FailoverV4;
import io.gravitee.rest.api.management.v2.rest.model.IntegrationOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.KubernetesOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.ManagementOriginContext;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;

public class ApiMapperTest {

    private final ApiMapper apiMapper = Mappers.getMapper(ApiMapper.class);

    @Test
    void shouldMapToUpdateApiEntityV4() {
        var updateApi = ApiFixtures.anUpdateApiV4();
        updateApi.failover(
            FailoverV4
                .builder()
                .enabled(true)
                .perSubscription(false)
                .maxFailures(3)
                .openStateDuration(11000L)
                .slowCallDuration(500L)
                .build()
        );

        var updateApiEntity = apiMapper.map(updateApi, "api-id");
        assertThat(updateApiEntity).isNotNull();
        assertThat(updateApiEntity.getId()).isEqualTo("api-id");
        assertThat(updateApiEntity.getCrossId()).isNull();
        assertThat(updateApiEntity.getName()).isEqualTo(updateApi.getName());
        assertThat(updateApiEntity.getApiVersion()).isEqualTo(updateApi.getApiVersion());
        assertThat(updateApiEntity.getDefinitionVersion().name()).isEqualTo(updateApi.getDefinitionVersion().name());
        assertThat(updateApiEntity.getType().name()).isEqualTo(updateApi.getType().name());
        assertThat(updateApiEntity.getDescription()).isEqualTo(updateApi.getDescription());
        assertThat(new ArrayList<>(updateApiEntity.getTags())).isEqualTo(updateApi.getTags());
        assertThat(updateApiEntity.getListeners()).isNotNull(); // Tested in ListenerMapperTest
        assertThat(updateApiEntity.getEndpointGroups()).isNotNull(); // Tested in EndpointMapperTest
        assertThat(updateApiEntity.getAnalytics()).isNotNull();
        assertThat(updateApiEntity.getProperties()).isNotNull(); // Tested in PropertiesMapperTest
        assertThat(updateApiEntity.getResources()).isNotNull(); // Tested in ResourceMapperTest
        assertThat(updateApiEntity.getPlans().size()).isEqualTo(0);
        assertThat(updateApiEntity.getFlowExecution()).usingRecursiveAssertion().isEqualTo(updateApi.getFlowExecution());
        assertThat(updateApiEntity.getFlows()).isNotNull(); // Tested in FlowMapperTest
        assertThat(updateApiEntity.getResponseTemplates()).isNotNull();
        assertThat(updateApiEntity.getServices()).isNotNull(); // Tested in ServiceMapperTest
        assertThat(new ArrayList<>(updateApiEntity.getGroups())).isEqualTo(updateApi.getGroups());
        assertThat(updateApiEntity.getVisibility().name()).isEqualTo(updateApi.getVisibility().name());
        assertThat(new ArrayList<>(updateApiEntity.getCategories())).isEqualTo(updateApi.getCategories());
        assertThat(updateApiEntity.getLabels()).isEqualTo(updateApi.getLabels());
        assertThat(updateApiEntity.getMetadata()).isNull();
        assertThat(updateApiEntity.getLifecycleState().name()).isEqualTo(updateApi.getLifecycleState().name());
        assertThat(updateApiEntity.isDisableMembershipNotifications()).isEqualTo(updateApi.getDisableMembershipNotifications());
        assertThat(updateApiEntity.getFailover())
            .isEqualTo(
                Failover
                    .builder()
                    .enabled(true)
                    .perSubscription(false)
                    .maxFailures(3)
                    .openStateDuration(11000)
                    .slowCallDuration(500)
                    .build()
            );
    }

    @Test
    void shouldMapToUpdateApiEntityV2() {
        var updateApi = ApiFixtures.anUpdateApiV2();

        var updateApiEntity = apiMapper.map(updateApi);
        assertThat(updateApiEntity).isNotNull();
        assertThat(updateApiEntity.getCrossId()).isNull();
        assertThat(updateApiEntity.getName()).isEqualTo(updateApi.getName());
        assertThat(updateApiEntity.getVersion()).isEqualTo(updateApi.getApiVersion());
        assertThat(updateApiEntity.getGraviteeDefinitionVersion()).isEqualTo(DefinitionVersion.V2.getLabel());
        assertThat(updateApiEntity.getDescription()).isEqualTo(updateApi.getDescription());
        assertThat(new ArrayList<>(updateApiEntity.getTags())).isEqualTo(updateApi.getTags());
        assertThat(updateApiEntity.getProxy()).isNotNull(); // To be tested in ProxyMapperTest ?
        assertThat(updateApiEntity.getProperties()).isNotNull(); // Tested in PropertiesMapperTest
        assertThat(updateApiEntity.getResources()).isNotNull(); // Tested in ResourceMapperTest
        assertThat(updateApiEntity.getPlans().size()).isEqualTo(0);
        assertThat(updateApiEntity.getFlowMode().name()).isEqualTo(updateApi.getFlowMode().getValue());
        assertThat(updateApiEntity.getFlows()).isNotNull(); // Tested in FlowMapperTest
        assertThat(updateApiEntity.getResponseTemplates()).isNotNull();
        assertThat(updateApiEntity.getServices()).isNotNull(); // Tested in ServiceMapperTest
        assertThat(new ArrayList<>(updateApiEntity.getGroups())).isEqualTo(updateApi.getGroups());
        assertThat(updateApiEntity.getVisibility().name()).isEqualTo(updateApi.getVisibility().name());
        assertThat(new ArrayList<>(updateApiEntity.getCategories())).isEqualTo(updateApi.getCategories());
        assertThat(updateApiEntity.getLabels()).isEqualTo(updateApi.getLabels());
        assertThat(updateApiEntity.getMetadata()).isNull();
        assertThat(updateApiEntity.getLifecycleState().name()).isEqualTo(updateApi.getLifecycleState().name());
        assertThat(updateApiEntity.isDisableMembershipNotifications()).isEqualTo(updateApi.getDisableMembershipNotifications());
        assertThat(updateApiEntity.getExecutionMode().name()).isEqualTo(updateApi.getExecutionMode().getValue());
    }

    @Nested
    class ComputeOriginContext {

        static Stream<Arguments> computeOriginContext() {
            return Stream.of(
                arguments(new OriginContext.Management(), new ManagementOriginContext().origin(BaseOriginContext.OriginEnum.MANAGEMENT)),
                arguments(
                    new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED),
                    new KubernetesOriginContext()
                        .mode(KubernetesOriginContext.ModeEnum.FULLY_MANAGED)
                        .syncFrom(KubernetesOriginContext.SyncFromEnum.KUBERNETES)
                        .origin(BaseOriginContext.OriginEnum.KUBERNETES)
                ),
                arguments(
                    new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED, "MANAGEMENT"),
                    new KubernetesOriginContext()
                        .mode(KubernetesOriginContext.ModeEnum.FULLY_MANAGED)
                        .syncFrom(KubernetesOriginContext.SyncFromEnum.MANAGEMENT)
                        .origin(BaseOriginContext.OriginEnum.KUBERNETES)
                ),
                arguments(
                    new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED, "KUBERNETES"),
                    new KubernetesOriginContext()
                        .mode(KubernetesOriginContext.ModeEnum.FULLY_MANAGED)
                        .syncFrom(KubernetesOriginContext.SyncFromEnum.KUBERNETES)
                        .origin(BaseOriginContext.OriginEnum.KUBERNETES)
                ),
                arguments(
                    new OriginContext.Integration("integID"),
                    new IntegrationOriginContext().integrationId("integID").origin(BaseOriginContext.OriginEnum.INTEGRATION)
                ),
                arguments(
                    new FederatedApiEntity.OriginContextView(new OriginContext.Integration("integID"), "provider", "inte name"),
                    new IntegrationOriginContext()
                        .integrationId("integID")
                        .provider("provider")
                        .integrationName("inte name")
                        .origin(BaseOriginContext.OriginEnum.INTEGRATION)
                )
            );
        }

        @ParameterizedTest
        @MethodSource
        void computeOriginContext(OriginContext input, BaseOriginContext expected) {
            // Given
            GenericApiEntity api = new ApiEntity().withOriginContext(input);

            // When
            var context = ApiMapper.INSTANCE.computeOriginContext(api);

            // Then
            assertThat(context).isEqualTo(expected);
        }
    }
}
