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
package io.gravitee.apim.core.cluster.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import inmemory.ClusterQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.cluster.domain_service.ClusterBoundApisQueryService.ClusterUse;
import io.gravitee.apim.core.cluster.exception.ClusterBindingNotAllowedException;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClusterBindingPermissionDomainServiceTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";
    private static final String USER_ID = "user-id";

    private final ClusterQueryServiceInMemory clusterQueryService = new ClusterQueryServiceInMemory();
    private final PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);
    private final ClusterBindingPermissionDomainService service = new ClusterBindingPermissionDomainService(
        clusterQueryService,
        permissionDomainService,
        new ObjectMapper()
    );

    @BeforeEach
    void setUp() {
        clusterQueryService.initWith(
            List.of(
                cluster("prod-id", "prod", "Production", ClusterType.KAFKA_CLUSTER),
                cluster("staging-id", "staging", "Staging", ClusterType.KAFKA_CLUSTER),
                cluster("mesh-id", "mesh", "Mesh", ClusterType.KAFKA_VIRTUAL_CLUSTER)
            )
        );
        givenMemberOf("staging-id");
    }

    @Test
    void should_let_a_member_bind_an_api_to_the_cluster() {
        assertThatCode(() -> validate(List.of(), List.of(new ClusterUse("staging", "plain")))).doesNotThrowAnyException();
    }

    @Test
    void should_refuse_a_non_member_binding_an_api_to_the_cluster_with_its_name() {
        assertThatThrownBy(() -> validate(List.of(), List.of(new ClusterUse("prod", "plain"))))
            .isInstanceOf(ClusterBindingNotAllowedException.class)
            .isInstanceOf(NotAllowedDomainException.class)
            .hasMessageContaining("[Production]");
    }

    @Test
    void should_refuse_a_non_member_changing_the_connection_of_a_binding() {
        assertThatThrownBy(() -> validate(List.of(new ClusterUse("prod", "plain")), List.of(new ClusterUse("prod", "sasl")))).isInstanceOf(
            ClusterBindingNotAllowedException.class
        );
    }

    @Test
    void should_let_a_non_member_keep_or_drop_an_existing_binding() {
        var previous = List.of(new ClusterUse("prod", "plain"), new ClusterUse("prod", "sasl"));

        assertThatCode(() -> validate(previous, List.of(new ClusterUse("prod", "plain")))).doesNotThrowAnyException();
        assertThatCode(() -> validate(previous, List.of())).doesNotThrowAnyException();
    }

    @Test
    void should_not_check_virtual_clusters() {
        assertThatCode(() -> validate(List.of(), List.of(new ClusterUse("mesh", null)))).doesNotThrowAnyException();
    }

    @Test
    void should_leave_unknown_clusters_to_the_binding_validation() {
        assertThatCode(() -> validate(List.of(), List.of(new ClusterUse("unknown", "plain")))).doesNotThrowAnyException();
    }

    @Test
    void should_not_check_a_call_without_user() {
        assertThatCode(() ->
            service.validate(ORG_ID, ENV_ID, null, List.of(), List.of(new ClusterUse("prod", "plain")))
        ).doesNotThrowAnyException();
        verifyNoInteractions(permissionDomainService);
    }

    @Test
    void should_check_the_bindings_a_native_api_definition_changes() {
        var onPlain = ApiFixtures.aNativeApiBoundToCluster(
            "api-id",
            ENV_ID,
            Api.LifecycleState.STARTED,
            "prod",
            "plain"
        ).getApiDefinitionNativeV4();
        var onSasl = ApiFixtures.aNativeApiBoundToCluster(
            "api-id",
            ENV_ID,
            Api.LifecycleState.STARTED,
            "prod",
            "sasl"
        ).getApiDefinitionNativeV4();

        assertThatCode(() -> service.validate(ORG_ID, ENV_ID, USER_ID, onPlain, onPlain)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validate(ORG_ID, ENV_ID, USER_ID, onPlain, onSasl)).isInstanceOf(
            ClusterBindingNotAllowedException.class
        );
        assertThatThrownBy(() -> service.validate(ORG_ID, ENV_ID, USER_ID, null, onPlain)).isInstanceOf(
            ClusterBindingNotAllowedException.class
        );
    }

    private void validate(List<ClusterUse> previousUses, List<ClusterUse> uses) {
        service.validate(ORG_ID, ENV_ID, USER_ID, previousUses, uses);
    }

    private void givenMemberOf(String clusterId) {
        when(
            permissionDomainService.hasPermission(ORG_ID, USER_ID, RolePermission.CLUSTER_DEFINITION, clusterId, RolePermissionAction.READ)
        ).thenReturn(true);
    }

    private static Cluster cluster(String id, String crossId, String name, ClusterType type) {
        return Cluster.builder().id(id).crossId(crossId).name(name).type(type).environmentId(ENV_ID).build();
    }
}
