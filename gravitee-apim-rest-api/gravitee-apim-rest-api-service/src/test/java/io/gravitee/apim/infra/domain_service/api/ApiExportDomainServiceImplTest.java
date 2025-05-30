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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.FlowCrudServiceInMemory;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.apim.core.api.model.import_definition.ApiMemberRole;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.PageExport;
import io.gravitee.apim.core.api.model.import_definition.PlanDescriptor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.Excludable;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.model.AccessControl;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.media.model.Media;
import io.gravitee.apim.core.media.query_service.MediaQueryService;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.workflow.crud_service.WorkflowCrudService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
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
        .crossId("UUID")
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
    public static final ApiMember EXPECTED_MEMBER = ApiMember.builder().id("member-id").type(MembershipMemberType.USER).build();

    @Mock
    PermissionService permissionService;

    @Mock
    WorkflowCrudService workflowCrudService;

    @Mock
    MembershipCrudService membershipCrudService;

    @Mock
    UserCrudService userCrudService;

    @Mock
    RoleQueryService roleQueryService;

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

    @Mock
    PageCrudService pageCrudService;

    @InjectMocks
    ApiExportDomainServiceImpl sut;

    @BeforeEach
    void setUp() {
        GraviteeContext.setCurrentEnvironment("DEFAULT");
        UuidString.overrideGenerator(str -> "UUID");
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
        GraviteeDefinition export = sut.export(
            apiId,
            AuditInfo.builder().environmentId("DEFAULT").build(),
            EnumSet.noneOf(Excludable.class)
        );

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
        GraviteeDefinition export = sut.export(
            apiId,
            AuditInfo.builder().environmentId("DEFAULT").build(),
            EnumSet.noneOf(Excludable.class)
        );

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
        GraviteeDefinition export = sut.export(
            apiId,
            AuditInfo.builder().environmentId("DEFAULT").build(),
            EnumSet.noneOf(Excludable.class)
        );

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
        when(integrationCrudService.findApiIntegrationById(anyString()))
            .thenReturn(Optional.of(new Integration.ApiIntegration(apiId, null, null, "provider", null, null, null, null)));

        // When
        GraviteeDefinition export = sut.export(
            apiId,
            AuditInfo.builder().environmentId("DEFAULT").build(),
            EnumSet.noneOf(Excludable.class)
        );

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
        GraviteeDefinition export = sut.export(
            apiId,
            AuditInfo.builder().environmentId("DEFAULT").build(),
            EnumSet.noneOf(Excludable.class)
        );

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
        when(integrationCrudService.findApiIntegrationById(anyString()))
            .thenReturn(Optional.of(new Integration.ApiIntegration(apiId, null, null, "provider", null, null, null, null)));

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

    @Test
    void deep_validation_export() {
        // Given
        String apiId = "apiId";
        Api api = Api
            .builder()
            .id(apiId)
            .description("Gravitee.io")
            .type(ApiType.PROXY)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionHttpV4(io.gravitee.definition.model.v4.Api.builder().build())
            .build();
        api.setDefinitionVersion(DefinitionVersion.V4);
        when(apiCrudService.findById(anyString())).thenReturn(Optional.of(api));
        when(pageQueryService.searchByApiId(anyString())).thenReturn(pages());
        when(planCrudService.findByApiId(anyString())).thenReturn(plans(apiId));
        when(metadataCrudService.findByApiId(anyString())).thenReturn(metadataApi(apiId));
        when(metadataCrudService.findByEnvId(anyString())).thenReturn(metadataEnv("DEFAULT"));
        when(membershipCrudService.findByApiId(anyString())).thenReturn(members());
        when(userCrudService.findBaseUsersByIds(any())).thenReturn(users());
        when(roleQueryService.findByIds(anySet())).thenReturn(roles());

        // When
        GraviteeDefinition export = sut.export(
            apiId,
            AuditInfo.builder().environmentId("DEFAULT").build(),
            EnumSet.noneOf(Excludable.class)
        );

        // Then
        assertThat(export.pages())
            .containsOnly(
                PageExport
                    .builder()
                    .name("My Folder")
                    .crossId("UUID")
                    .order(1)
                    .type(Page.Type.FOLDER)
                    .visibility(Page.Visibility.PUBLIC)
                    .build(),
                PageExport
                    .builder()
                    .name("My Title")
                    .crossId("UUID")
                    .order(1)
                    .type(Page.Type.MARKDOWN)
                    .visibility(Page.Visibility.PUBLIC)
                    .content("Read the doc")
                    .accessControls(Set.of(AccessControl.builder().referenceId("my-group").referenceType("GROUP").build()))
                    .build(),
                PageExport
                    .builder()
                    .name("My Swagger")
                    .crossId("UUID")
                    .order(1)
                    .type(Page.Type.SWAGGER)
                    .visibility(Page.Visibility.PUBLIC)
                    .content("Read the doc")
                    .build(),
                PageExport
                    .builder()
                    .name("Aside")
                    .crossId("UUID")
                    .order(1)
                    .type(Page.Type.SYSTEM_FOLDER)
                    .visibility(Page.Visibility.PUBLIC)
                    .published(true)
                    .build(),
                PageExport
                    .builder()
                    .name("My Link")
                    .crossId("UUID")
                    .order(1)
                    .type(Page.Type.LINK)
                    .visibility(Page.Visibility.PUBLIC)
                    .content("Read the doc")
                    .build(),
                PageExport
                    .builder()
                    .name("My Translation")
                    .crossId("UUID")
                    .order(1)
                    .type(Page.Type.TRANSLATION)
                    .visibility(Page.Visibility.PUBLIC)
                    .content("Lire la documentation")
                    .build(),
                PageExport
                    .builder()
                    .name("My Template")
                    .crossId("UUID")
                    .order(1)
                    .type(Page.Type.MARKDOWN_TEMPLATE)
                    .visibility(Page.Visibility.PUBLIC)
                    .content("Read the doc")
                    .build(),
                PageExport
                    .builder()
                    .name("My asciidoc")
                    .crossId("UUID")
                    .order(1)
                    .type(Page.Type.ASCIIDOC)
                    .visibility(Page.Visibility.PUBLIC)
                    .content("Read the asciidoc")
                    .build()
            );
        assertThat(export.members())
            .containsOnly(
                ApiMember
                    .builder()
                    .displayName("Bruce Wayne")
                    .type(MembershipMemberType.USER)
                    .roles(List.of(ApiMemberRole.builder().name("PRIMARY_OWNER").scope(RoleScope.APPLICATION).build()))
                    .build()
            );
        assertThat(export.metadata())
            .containsOnly(
                NewApiMetadata
                    .builder()
                    .key("metadata-key")
                    .name("metadata-name")
                    .format(Metadata.MetadataFormat.STRING)
                    .value("metadata-value")
                    .defaultValue("metadata-value-env")
                    .build()
            );
        assertThat(((GraviteeDefinition.V4) export).plans())
            .containsOnly(
                PlanDescriptor.V4
                    .builder()
                    .id("plan-id")
                    .crossId("test-plan-cross-id")
                    .definitionVersion(DefinitionVersion.V4)
                    .description("free plan")
                    .type(Plan.PlanType.API)
                    .order(0)
                    .excludedGroups(List.of("my-group"))
                    .selectionRule("/**")
                    .status(PlanStatus.PUBLISHED)
                    .flows(List.of())
                    .validation(Plan.PlanValidationType.AUTO)
                    .apiId(apiId)
                    .characteristics(List.of())
                    .security(PlanSecurity.builder().type("API_KEY").build())
                    .build()
            );
    }

    private Set<BaseUserEntity> users() {
        return Set.of(BaseUserEntity.builder().firstname("Bruce").lastname("Wayne").build());
    }

    List<Page> pages() {
        Page folder = Page.builder().name("My Folder").order(1).type(Page.Type.FOLDER).visibility(Page.Visibility.PUBLIC).build();
        Page markdownPage = Page
            .builder()
            .name("My Title")
            .order(1)
            .type(Page.Type.MARKDOWN)
            .content("Read the doc")
            .visibility(Page.Visibility.PUBLIC)
            .accessControls(Set.of(new AccessControl("my-group", "GROUP")))
            .build();
        Page asideFolder = Page
            .builder()
            .name("Aside")
            .order(1)
            .published(true)
            .type(Page.Type.SYSTEM_FOLDER)
            .visibility(Page.Visibility.PUBLIC)
            .build();

        Page swaggerPage = Page
            .builder()
            .name("My Swagger")
            .order(1)
            .type(Page.Type.SWAGGER)
            .content("Read the doc")
            .visibility(Page.Visibility.PUBLIC)
            .build();
        Page linkPage = Page
            .builder()
            .name("My Link")
            .order(1)
            .type(Page.Type.LINK)
            .content("Read the doc")
            .visibility(Page.Visibility.PUBLIC)
            .build();
        Page translationPage = Page
            .builder()
            .name("My Translation")
            .order(1)
            .type(Page.Type.TRANSLATION)
            .content("Lire la documentation")
            .visibility(Page.Visibility.PUBLIC)
            .build();
        Page markdownTemplatePage = Page
            .builder()
            .name("My Template")
            .order(1)
            .type(Page.Type.MARKDOWN_TEMPLATE)
            .content("Read the doc")
            .visibility(Page.Visibility.PUBLIC)
            .build();

        Page asciidocPage = Page
            .builder()
            .name("My asciidoc")
            .order(1)
            .type(Page.Type.ASCIIDOC)
            .content("Read the asciidoc")
            .visibility(Page.Visibility.PUBLIC)
            .build();

        return List.of(folder, markdownPage, swaggerPage, asideFolder, linkPage, translationPage, markdownTemplatePage, asciidocPage);
    }

    Set<Plan> plans(String apiId) {
        Plan publishedPlan = Plan
            .builder()
            .definitionVersion(DefinitionVersion.V4)
            .id("plan-id")
            .crossId("test-plan-cross-id")
            .apiId(apiId)
            .description("free plan")
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .excludedGroups(List.of("my-group"))
            .planDefinitionHttpV4(
                io.gravitee.definition.model.v4.plan.Plan
                    .builder()
                    .security(PlanSecurity.builder().type("API_KEY").build())
                    .selectionRule("/**")
                    .build()
            )
            .build()
            .setPlanStatus(io.gravitee.definition.model.v4.plan.PlanStatus.PUBLISHED);

        var closedPlan = Plan
            .builder()
            .definitionVersion(DefinitionVersion.V4)
            .id("closedPlan-id")
            .crossId("closed-test-plan-cross-id")
            .apiId(apiId)
            .description("free closedPlan")
            .type(Plan.PlanType.API)
            .planDefinitionHttpV4(
                io.gravitee.definition.model.v4.plan.Plan
                    .builder()
                    .security(PlanSecurity.builder().type("API_KEY").build())
                    .selectionRule("/**")
                    .build()
            )
            .validation(Plan.PlanValidationType.AUTO)
            .build()
            .setPlanStatus(io.gravitee.definition.model.v4.plan.PlanStatus.CLOSED);
        return Set.of(publishedPlan, closedPlan);
    }

    List<Metadata> metadataApi(String apiId) {
        Metadata metadata = Metadata
            .builder()
            .referenceId(apiId)
            .referenceType(Metadata.ReferenceType.API)
            .key("metadata-key")
            .name("metadata-name")
            .value("metadata-value")
            .format(Metadata.MetadataFormat.STRING)
            .build();
        return List.of(metadata);
    }

    List<Metadata> metadataEnv(String env) {
        Metadata metadata = Metadata
            .builder()
            .referenceId(env)
            .referenceType(Metadata.ReferenceType.ENVIRONMENT)
            .key("metadata-key")
            .name("metadata-name")
            .value("metadata-value-env")
            .format(Metadata.MetadataFormat.STRING)
            .build();
        return List.of(metadata);
    }

    Set<Membership> members() {
        return Set.of(Membership.builder().id("johndoe").memberType(Membership.Type.USER).roleId("role-id").build());
    }

    Set<Role> roles() {
        return Set.of(Role.builder().name("PRIMARY_OWNER").scope(Role.Scope.APPLICATION).id("role-id").build());
    }
}
