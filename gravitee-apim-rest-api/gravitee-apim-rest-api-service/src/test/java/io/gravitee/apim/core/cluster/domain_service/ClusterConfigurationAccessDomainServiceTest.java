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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClusterConfigurationAccessDomainServiceTest {

    private static final String ORG_ID = "org-id";
    private static final String USER_ID = "user-id";
    private static final String CLUSTER_ID = "cluster-id";

    private static final Map<String, Object> FULL_CONFIGURATION = Map.of(
        "bootstrapServers",
        "broker:9093",
        "security",
        Map.of("protocol", "SASL_SSL", "sasl", Map.of("password", "secret"))
    );
    private static final Map<String, Object> REDACTED_CONFIGURATION = Map.of(
        "bootstrapServers",
        "broker:9093",
        "security",
        Map.of("protocol", "SASL_SSL")
    );

    private final PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);
    private ClusterConfigurationAccessDomainService service;

    @BeforeEach
    void setUp() {
        service = new ClusterConfigurationAccessDomainService(permissionDomainService);
    }

    @Test
    void should_return_full_configuration_to_a_configuration_editor() {
        grant(RolePermissionAction.UPDATE);
        grant(RolePermissionAction.READ);

        var result = service.visibleTo(cluster(), ORG_ID, USER_ID);

        assertThat(result.getConfiguration()).isEqualTo(FULL_CONFIGURATION);
    }

    @Test
    void should_return_configuration_without_credentials_to_a_configuration_reader() {
        grant(RolePermissionAction.READ);

        var result = service.visibleTo(cluster(), ORG_ID, USER_ID);

        assertThat(result.getConfiguration()).isEqualTo(REDACTED_CONFIGURATION);
    }

    @Test
    void should_return_no_configuration_to_a_user_without_configuration_permission() {
        var cluster = cluster();

        var result = service.visibleTo(cluster, ORG_ID, USER_ID);

        assertThat(result.getConfiguration()).isNull();
        assertThat(result.getId()).isEqualTo(CLUSTER_ID);
        assertThat(cluster.getConfiguration()).isEqualTo(FULL_CONFIGURATION);
    }

    @Test
    void should_never_return_credentials_in_a_list_even_to_a_configuration_editor() {
        grant(RolePermissionAction.UPDATE);
        grant(RolePermissionAction.READ);

        var result = service.visibleInListTo(cluster(), ORG_ID, USER_ID);

        assertThat(result.getConfiguration()).isEqualTo(REDACTED_CONFIGURATION);
    }

    @Test
    void should_return_no_configuration_in_a_list_to_a_user_without_configuration_permission() {
        var result = service.visibleInListTo(cluster(), ORG_ID, USER_ID);

        assertThat(result.getConfiguration()).isNull();
    }

    private void grant(RolePermissionAction action) {
        when(permissionDomainService.hasPermission(ORG_ID, USER_ID, RolePermission.CLUSTER_CONFIGURATION, CLUSTER_ID, action)).thenReturn(
            true
        );
    }

    private static Cluster cluster() {
        return Cluster.builder().id(CLUSTER_ID).organizationId(ORG_ID).configuration(FULL_CONFIGURATION).build();
    }
}
