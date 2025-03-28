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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.FlowCrudServiceInMemory;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.PageExport;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.Excludable;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.media.model.Media;
import io.gravitee.apim.core.media.query_service.MediaQueryService;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.workflow.crud_service.WorkflowCrudService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiExportDomainServiceImplTest {

    private static final String API_ID = UUID.randomUUID().toString();
    public static final Media MEDIA = Media.builder().apiId(API_ID).id("media").build();

    private static final Page MARKDOWN_PAGE = Page
        .builder()
        .id("page")
        .type(Page.Type.MARKDOWN)
        .referenceId(API_ID)
        .referenceType(Page.ReferenceType.API)
        .build();
    public static final PageExport EXPECTED_MARKDOWN_PAGE = PageExport
        .builder()
        .id("page")
        .type(Page.Type.MARKDOWN)
        .referenceId(API_ID)
        .referenceType(Page.ReferenceType.API)
        .build();

    private static final Metadata METADATA = Metadata.builder().key("hehe").name("haha").value("hoohoo").build();
    public static final Map<String, String> EXPECTED_METADATA = Map.of("haha", "hoohoo");

    private static final Membership MEMBER = Membership
        .builder()
        .id("member")
        .memberId("member-id")
        .memberType(Membership.Type.USER)
        .build();
    public static final ApiMember EXPECTED_MEMBER = ApiMember.builder().id("member").build();

    @Mock
    PermissionService permissionService;

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

    @Mock
    MediaQueryService mediaService;

    @Mock
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();

    @Mock
    IntegrationCrudService integrationCrudService;

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

    @AfterEach
    void tearDown() {
        flowCrudService.reset();
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
            .groups(Set.of("group-1"))
            .build();
        when(apiCrudService.findById(anyString())).thenReturn(Optional.of(api));

        when(metadataCrudService.findByApiId(anyString())).thenReturn(List.of(METADATA));
        when(membershipCrudService.findByApiId(anyString())).thenReturn(List.of(MEMBER));
        when(pageQueryService.searchByApiId(anyString())).thenReturn(List.of(MARKDOWN_PAGE));
        when(mediaService.findAllByApiId(anyString())).thenReturn(List.of(MEDIA));

        // When
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build(), EnumSet.noneOf(Excludable.class));

        // Then
        assertThat(export.api().type()).isEqualTo(ApiType.PROXY);
        assertThat(export.api().metadata()).isEqualTo(EXPECTED_METADATA);
        assertThat(export.api().groups()).hasSize(1);
        assertThat(export.api().groups()).contains("group-1");
        assertThat(export.pages()).hasSize(1);
        assertThat(export.pages()).contains(EXPECTED_MARKDOWN_PAGE);
        assertThat(export.members()).hasSize(1);
        assertThat(export.members()).contains(EXPECTED_MEMBER);
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
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build(), EnumSet.noneOf(Excludable.class));

        // Then
        assertThat(export.api().type()).isEqualTo(ApiType.PROXY);
    }

    @Test
    void exportServiceMustMapTypeWhenExportV4Native() {
        // Given
        String apiId = UUID.randomUUID().toString();

        when(metadataCrudService.findByApiId(anyString())).thenReturn(List.of(METADATA));
        when(membershipCrudService.findByApiId(anyString())).thenReturn(List.of(MEMBER));
        when(pageQueryService.searchByApiId(anyString())).thenReturn(List.of(MARKDOWN_PAGE));
        when(mediaService.findAllByApiId(anyString())).thenReturn(List.of(MEDIA));

        Api api = ApiFixtures.aNativeApi();
        when(apiCrudService.findById(anyString())).thenReturn(Optional.of(api));

        // When
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build(), EnumSet.noneOf(Excludable.class));

        // Then
        assertThat(export.api().type()).isEqualTo(ApiType.NATIVE);
        assertThat(export.api().metadata()).isEqualTo(EXPECTED_METADATA);
        assertThat(export.api().groups()).hasSize(1);
        assertThat(export.api().groups()).contains("group-1");
        assertThat(export.pages()).hasSize(1);
        assertThat(export.pages()).contains(EXPECTED_MARKDOWN_PAGE);
        assertThat(export.members()).hasSize(1);
        assertThat(export.members()).contains(EXPECTED_MEMBER);
    }

    @Test
    void export_service_must_map_type_when_export_federated() {
        // Given
        String apiId = UUID.randomUUID().toString();

        when(metadataCrudService.findByApiId(anyString())).thenReturn(List.of(METADATA));
        when(membershipCrudService.findByApiId(anyString())).thenReturn(List.of(MEMBER));
        when(pageQueryService.searchByApiId(anyString())).thenReturn(List.of(MARKDOWN_PAGE));
        when(mediaService.findAllByApiId(anyString())).thenReturn(List.of(MEDIA));

        Api api = ApiFixtures.aFederatedApi();
        when(apiCrudService.findById(anyString())).thenReturn(Optional.of(api));
        when(integrationCrudService.findById(anyString()))
            .thenReturn(Optional.of(Integration.builder().id(apiId).provider("provider").build()));

        // When
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build(), EnumSet.noneOf(Excludable.class));

        // Then
        assertThat(export.api().definitionVersion()).isEqualTo(DefinitionVersion.FEDERATED);
        assertThat(export.api().metadata()).isEqualTo(EXPECTED_METADATA);
        assertThat(export.api().groups()).hasSize(1);
        assertThat(export.api().groups()).contains("group-1");
        assertThat(export.pages()).hasSize(1);
        assertThat(export.pages()).contains(EXPECTED_MARKDOWN_PAGE);
        assertThat(export.members()).hasSize(1);
        assertThat(export.members()).contains(EXPECTED_MEMBER);
    }

    @Test
    void export_service_must_map_type_when_export_V2() {
        // Given
        String apiId = UUID.randomUUID().toString();

        when(metadataCrudService.findByApiId(anyString())).thenReturn(List.of(METADATA));
        when(membershipCrudService.findByApiId(anyString())).thenReturn(List.of(MEMBER));
        when(pageQueryService.searchByApiId(anyString())).thenReturn(List.of(MARKDOWN_PAGE));
        when(mediaService.findAllByApiId(anyString())).thenReturn(List.of(MEDIA));

        Api api = ApiFixtures.aProxyApiV2();
        when(apiCrudService.findById(anyString())).thenReturn(Optional.of(api));

        // When
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build(), EnumSet.noneOf(Excludable.class));

        // Then
        assertThat(export.api().definitionVersion()).isEqualTo(DefinitionVersion.V2);
        assertThat(export.api().metadata()).isEqualTo(EXPECTED_METADATA);
        assertThat(export.api().groups()).hasSize(1);
        assertThat(export.api().groups()).contains("group-1");
        assertThat(export.pages()).hasSize(1);
        assertThat(export.pages()).contains(EXPECTED_MARKDOWN_PAGE);
        assertThat(export.members()).hasSize(1);
        assertThat(export.members()).contains(EXPECTED_MEMBER);
    }

    @Test
    void export_service_must_map_type_when_export_V4_native_with_exclusions() {
        // Given
        String apiId = UUID.randomUUID().toString();

        Api api = ApiFixtures.aNativeApi().toBuilder().groups(Set.of("group-1")).build();

        when(apiCrudService.findById(anyString())).thenReturn(Optional.of(api));

        // When
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build(), EnumSet.allOf(Excludable.class));

        // Then
        assertThat(export.api().type()).isEqualTo(ApiType.NATIVE);
        assertThat(export.api().metadata()).isEmpty();
        assertThat(export.api().groups()).isNull();
        assertThat(export.pages()).isNull();
        assertThat(export.members()).isNull();

        verify(metadataCrudService, never()).findByApiId(anyString());
        verify(membershipCrudService, never()).findByApiId(anyString());
        verify(pageQueryService, never()).searchByApiId(anyString());
        verify(mediaService, never()).findAllByApiId(anyString());
    }

    @Test
    void export_service_must_map_type_when_export_V4_http_with_exclusions() {
        // Given
        String apiId = UUID.randomUUID().toString();

        Api api = ApiFixtures.aMessageApiV4().toBuilder().groups(Set.of("group-1")).build();

        when(apiCrudService.findById(anyString())).thenReturn(Optional.of(api));

        // When
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build(), EnumSet.allOf(Excludable.class));

        // Then
        assertThat(export.api().type()).isEqualTo(ApiType.MESSAGE);
        assertThat(export.api().metadata()).isEmpty();
        assertThat(export.api().groups()).isNull();
        assertThat(export.pages()).isNull();
        assertThat(export.members()).isNull();

        verify(metadataCrudService, never()).findByApiId(anyString());
        verify(membershipCrudService, never()).findByApiId(anyString());
        verify(pageQueryService, never()).searchByApiId(anyString());
        verify(mediaService, never()).findAllByApiId(anyString());
    }

    @Test
    void export_service_must_map_type_when_export_V2_with_exclusions() {
        // Given
        String apiId = UUID.randomUUID().toString();

        Api api = ApiFixtures.aProxyApiV2().toBuilder().groups(Set.of("group-1")).build();

        when(apiCrudService.findById(anyString())).thenReturn(Optional.of(api));

        // When
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build(), EnumSet.allOf(Excludable.class));

        // Then
        assertThat(export.api().metadata()).isEmpty();
        assertThat(export.api().groups()).isNull();
        assertThat(export.pages()).isNull();
        assertThat(export.members()).isNull();

        verify(metadataCrudService, never()).findByApiId(anyString());
        verify(membershipCrudService, never()).findByApiId(anyString());
        verify(pageQueryService, never()).searchByApiId(anyString());
        verify(mediaService, never()).findAllByApiId(anyString());
    }

    @Test
    void export_service_must_map_type_when_export_federated_with_exclusions() {
        // Given
        String apiId = UUID.randomUUID().toString();

        Api api = ApiFixtures.aFederatedApi().toBuilder().groups(Set.of("group-1")).build();

        when(apiCrudService.findById(anyString())).thenReturn(Optional.of(api));
        when(integrationCrudService.findById(anyString()))
            .thenReturn(Optional.of(Integration.builder().id(apiId).provider("provider").build()));

        // When
        GraviteeDefinition export = sut.export(apiId, AuditInfo.builder().build(), EnumSet.allOf(Excludable.class));

        // Then
        assertThat(export.api().definitionVersion()).isEqualTo(DefinitionVersion.FEDERATED);
        assertThat(export.api().metadata()).isEmpty();
        assertThat(export.api().groups()).isNull();
        assertThat(export.pages()).isNull();
        assertThat(export.members()).isNull();

        verify(metadataCrudService, never()).findByApiId(anyString());
        verify(membershipCrudService, never()).findByApiId(anyString());
        verify(pageQueryService, never()).searchByApiId(anyString());
        verify(mediaService, never()).findAllByApiId(anyString());
    }
}
