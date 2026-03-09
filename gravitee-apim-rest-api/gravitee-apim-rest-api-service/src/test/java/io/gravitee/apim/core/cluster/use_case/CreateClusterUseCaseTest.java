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
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.AbstractUseCaseTest;
import inmemory.ClusterCrudServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.cluster.domain_service.ClusterConfigurationSchemaService;
import io.gravitee.apim.core.cluster.domain_service.ValidateClusterService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterAuditEvent;
import io.gravitee.apim.core.cluster.model.CreateCluster;
import io.gravitee.apim.core.json.JsonSchemaChecker;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.infra.json.JsonSchemaCheckerImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.json.validation.JsonSchemaValidatorImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.impl.JsonSchemaServiceImpl;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateClusterUseCaseTest extends AbstractUseCaseTest {

    private final String ROLE_ID = "role-id";

    private final ClusterCrudServiceInMemory clusterCrudService = new ClusterCrudServiceInMemory();
    private final MembershipCrudService membershipCrudService = new MembershipCrudServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();

    private CreateClusterUseCase createClusterUseCase;

    @BeforeEach
    void setUp() {
        var jsonSchemaChecker = new JsonSchemaCheckerImpl(new JsonSchemaServiceImpl(new JsonSchemaValidatorImpl()));
        var clusterConfigurationSchemaService = new ClusterConfigurationSchemaService();
        var validateClusterService = new ValidateClusterService(jsonSchemaChecker, clusterConfigurationSchemaService, new ObjectMapper());
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        createClusterUseCase = new CreateClusterUseCase(
            clusterCrudService,
            validateClusterService,
            auditService,
            membershipCrudService,
            roleQueryService
        );
        initRoles();
    }

    @Test
    void should_create() {
        String name = "Cluster 1";
        Object configuration = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        // Given
        var toCreate = CreateCluster.builder().name(name).configuration(configuration).build();

        // When
        var output = createClusterUseCase.execute(new CreateClusterUseCase.Input(toCreate, AUDIT_INFO));

        // Then
        var expected = Cluster.builder()
            .id(GENERATED_UUID)
            .name(name)
            .createdAt(INSTANT_NOW)
            .updatedAt(INSTANT_NOW)
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(configuration)
            .build();

        assertThat(output.cluster()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_create_an_audit() {
        String name = "Cluster 1";
        Object configuration = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        // Given
        var toCreate = CreateCluster.builder().name(name).configuration(configuration).build();

        // When
        createClusterUseCase.execute(new CreateClusterUseCase.Input(toCreate, AUDIT_INFO));

        // Then
        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .containsExactly(
                AuditEntity.builder()
                    .id(GENERATED_UUID)
                    .organizationId(ORG_ID)
                    .environmentId(ENV_ID)
                    .referenceType(AuditEntity.AuditReferenceType.ENVIRONMENT)
                    .referenceId(ENV_ID)
                    .user(USER_ID)
                    .properties(Map.of(AuditProperties.CLUSTER.name(), GENERATED_UUID))
                    .event(ClusterAuditEvent.CLUSTER_CREATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_create_a_primary_owner_membership() {
        String name = "Cluster 1";
        Object configuration = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        // Given
        var toCreate = CreateCluster.builder().name(name).configuration(configuration).build();

        // When
        createClusterUseCase.execute(new CreateClusterUseCase.Input(toCreate, AUDIT_INFO));

        // Then
        Membership membership = ((MembershipCrudServiceInMemory) membershipCrudService).storage().get(0);
        assertAll(
            () -> assertThat(membership.getId()).isEqualTo(GENERATED_UUID),
            () -> assertThat(membership.getMemberId()).isEqualTo(USER_ID),
            () -> assertThat(membership.getMemberType()).isEqualTo(Membership.Type.USER),
            () -> assertThat(membership.getReferenceType()).isEqualTo(Membership.ReferenceType.CLUSTER),
            () -> assertThat(membership.getReferenceId()).isEqualTo(GENERATED_UUID),
            () -> assertThat(membership.getRoleId()).isEqualTo(ROLE_ID),
            () -> assertThat(membership.getCreatedAt()).isEqualTo(INSTANT_NOW.atZone(TimeProvider.clock().getZone())),
            () -> assertThat(membership.getUpdatedAt()).isEqualTo(INSTANT_NOW.atZone(TimeProvider.clock().getZone()))
        );
    }

    @Test
    void should_throw_exception_when_name_is_null() {
        Object configuration = Map.of("protocol", "PLAINTEXT");
        // Given
        var toCreate = CreateCluster.builder().configuration(configuration).build();

        // When
        var throwable = Assertions.catchThrowable(() -> createClusterUseCase.execute(new CreateClusterUseCase.Input(toCreate, AUDIT_INFO)));

        // Then
        Assertions.assertThat(throwable).isInstanceOf(InvalidDataException.class).hasMessage("Name is required.");
    }

    @Test
    void should_throw_exception_when_configuration_is_null() {
        String name = "Cluster 1";
        // Given
        var toCreate = CreateCluster.builder().name(name).build();

        // When
        var throwable = Assertions.catchThrowable(() -> createClusterUseCase.execute(new CreateClusterUseCase.Input(toCreate, AUDIT_INFO)));

        // Then
        Assertions.assertThat(throwable).isInstanceOf(InvalidDataException.class).hasMessage("Configuration is required.");
    }

    private void initRoles() {
        List<Role> roles = List.of(
            Role.builder()
                .id(ROLE_ID)
                .scope(Role.Scope.CLUSTER)
                .name("PRIMARY_OWNER")
                .referenceType(Role.ReferenceType.ORGANIZATION)
                .referenceId(GraviteeContext.getExecutionContext().getOrganizationId())
                .build()
        );
        roleQueryService.initWith(roles);
    }
}
