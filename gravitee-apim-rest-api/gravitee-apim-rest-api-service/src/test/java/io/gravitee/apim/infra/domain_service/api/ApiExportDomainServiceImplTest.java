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
package io.gravitee.apim.infra.domain_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.workflow.crud_service.WorkflowCrudService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiExportDomainServiceImplTest {

    @Mock
    PermissionService permissionService;

    @Mock
    MediaService mediaService;

    @Mock
    WorkflowCrudService workflowCrudService;

    @Mock
    MembershipCrudService membershipCrudService;

    @Mock
    MetadataCrudService metadataCrudService;

    @Mock
    PageQueryService pageQueryService;

    @Mock
    ApiCrudService apiCrudService;

    @Mock
    ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;

    @Mock
    PlanCrudService planCrudService;

    @InjectMocks
    ApiExportDomainServiceImpl sut;

    @BeforeEach
    void setUp() {
        lenient()
            .when(
                permissionService.hasPermission(
                    any(ExecutionContext.class),
                    any(RolePermission.class),
                    anyString(),
                    ArgumentMatchers.<RolePermissionAction>any()
                )
            )
            .thenReturn(true);
    }

    @Test
    void exportServiceMustMapTypeWhenExportV4() {
        // Given
        String apiId = UUID.randomUUID().toString();
        var definition = new io.gravitee.definition.model.v4.Api();
        Api api = Api
            .builder()
            .id(apiId)
            .type(ApiType.PROXY)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionHttpV4(definition)
            .build();
        when(apiCrudService.findById(anyString())).thenReturn(Optional.of(api));

        // When
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build(), EnumSet.noneOf(ApiExportDomainService.Excludable.class));

        // Then
        assertThat(export.api().type()).isEqualTo(ApiType.PROXY);
    }

    @Test
    void exportServiceMustMapTypeWhenExportV4WithoutPermissions() {
        // Given
        String apiId = UUID.randomUUID().toString();
        var definition = new io.gravitee.definition.model.v4.Api();
        Api api = Api
            .builder()
            .id(apiId)
            .type(ApiType.PROXY)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionHttpV4(definition)
            .build();
        when(apiCrudService.findById(anyString())).thenReturn(Optional.of(api));
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                any(RolePermission.class),
                anyString(),
                ArgumentMatchers.<RolePermissionAction>any()
            )
        )
            .thenReturn(false);

        // When
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build(), EnumSet.noneOf(ApiExportDomainService.Excludable.class));

        // Then
        assertThat(export.api().type()).isEqualTo(ApiType.PROXY);
    }

    @Test
    void exportServiceMustMapTypeWhenExportV4Native() {
        // Given
        String apiId = UUID.randomUUID().toString();
        var definition = new NativeApi();
        Api api = Api
            .builder()
            .id(apiId)
            .type(ApiType.NATIVE)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionNativeV4(definition)
            .build();
        when(apiCrudService.findById(anyString())).thenReturn(Optional.of(api));

        // When
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build(), EnumSet.noneOf(ApiExportDomainService.Excludable.class));

        // Then
        assertThat(export.api().type()).isEqualTo(ApiType.NATIVE);
    }
}
