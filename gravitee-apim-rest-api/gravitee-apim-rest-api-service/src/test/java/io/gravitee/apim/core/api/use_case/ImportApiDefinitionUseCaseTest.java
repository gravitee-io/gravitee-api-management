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
package io.gravitee.apim.core.api.use_case;

import static fixtures.core.model.ApiFixtures.aProxyApiV4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.MediaFixtures;
import fixtures.core.model.PageFixtures;
import fixtures.core.model.PlanWithFlowsFixtures;
import fixtures.definition.ApiDefinitionFixtures;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.CreateCategoryApiDomainServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.NoopSwaggerOpenApiResolver;
import inmemory.NoopTemplateResolverDomainService;
import inmemory.NotificationConfigCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.PageRevisionCrudServiceInMemory;
import inmemory.PageSourceDomainServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import inmemory.WorkflowCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIdsCalculatorDomainService;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.apim.core.api.model.import_definition.ApiMemberRole;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.DocumentationValidationDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageContentException;
import io.gravitee.apim.core.documentation.exception.InvalidPageNameException;
import io.gravitee.apim.core.documentation.exception.InvalidPageParentException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.domain_service.api.ApiImportDomainServiceLegacyWrapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.sanitizer.HtmlSanitizerImpl;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.context.ManagementContext;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.exceptions.PageContentUnsafeException;
import io.gravitee.rest.api.service.sanitizer.HtmlSanitizer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.mock.env.MockEnvironment;

class ImportApiDefinitionUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2024-04-23T11:06:00Z");
    private static final String API_ID = "api-id";
    private static final String API_CROSS_ID = "api-cross-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String USER_EMAIL = "jane.doe@gravitee.io";
    private static final String EXISTING_API_ID = "existing-api-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final Set<String> TAGS = Set.of("tag");

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    ApiImportDomainServiceLegacyWrapper apiImportDomainService = mock(ApiImportDomainServiceLegacyWrapper.class);
    ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    IndexerInMemory indexer = new IndexerInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    ApiMetadataQueryServiceInMemory apiMetadataQueryServiceInMemory = new ApiMetadataQueryServiceInMemory(metadataCrudService);
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    NotificationConfigCrudServiceInMemory notificationConfigCrudService = new NotificationConfigCrudServiceInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    PageRevisionCrudServiceInMemory pageRevisionCrudService = new PageRevisionCrudServiceInMemory();
    PageSourceDomainServiceInMemory pageSourceDomainService = new PageSourceDomainServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    PlanQueryServiceInMemory planQueryServiceInMemory = new PlanQueryServiceInMemory();
    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    ValidateApiDomainService validateApiDomainService = mock(ValidateApiDomainService.class);
    WorkflowCrudServiceInMemory workflowCrudService = new WorkflowCrudServiceInMemory();
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    CreateCategoryApiDomainServiceInMemory createCategoryApiDomainService = new CreateCategoryApiDomainServiceInMemory();

    ImportApiDefinitionUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> API_ID);
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        var membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
        var apiPrimaryOwnerFactory = new ApiPrimaryOwnerFactory(
            groupQueryService,
            membershipQueryService,
            parametersQueryService,
            roleQueryService,
            userCrudService
        );
        var metadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudService);
        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
            auditDomainService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );
        var apiMetadataDomainService = new ApiMetadataDomainService(
            metadataCrudService,
            apiMetadataQueryServiceInMemory,
            auditDomainService
        );
        var createApiDomainService = new CreateApiDomainService(
            apiCrudService,
            auditDomainService,
            new ApiIndexerDomainService(
                new ApiMetadataDecoderDomainService(metadataQueryService, new FreemarkerTemplateProcessor()),
                apiPrimaryOwnerDomainService,
                new ApiCategoryQueryServiceInMemory(),
                indexer
            ),
            apiMetadataDomainService,
            apiPrimaryOwnerDomainService,
            flowCrudService,
            notificationConfigCrudService,
            parametersQueryService,
            workflowCrudService,
            createCategoryApiDomainService
        );

        var planValidatorService = new PlanValidatorDomainService(parametersQueryService, policyValidationDomainService, pageCrudService);
        var flowValidationDomainService = new FlowValidationDomainService(
            policyValidationDomainService,
            new EntrypointPluginQueryServiceInMemory()
        );
        var createPlanDomainService = new CreatePlanDomainService(
            planValidatorService,
            flowValidationDomainService,
            planCrudService,
            flowCrudService,
            auditDomainService
        );

        var createApiDocumentationDomainService = new CreateApiDocumentationDomainService(
            pageCrudService,
            pageRevisionCrudService,
            auditDomainService,
            indexer
        );
        var apiIdsCalculatorDomainService = new ApiIdsCalculatorDomainService(apiQueryService, pageQueryService, planQueryServiceInMemory);

        var htmlSanitizer = new HtmlSanitizer(new MockEnvironment());
        var documentationValidationDomainService = new DocumentationValidationDomainService(
            new HtmlSanitizerImpl(htmlSanitizer),
            new NoopTemplateResolverDomainService(),
            apiCrudService,
            new NoopSwaggerOpenApiResolver(),
            new ApiMetadataQueryServiceInMemory(),
            new ApiPrimaryOwnerDomainService(
                new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
                groupQueryService,
                membershipCrudService,
                membershipQueryService,
                roleQueryService,
                userCrudService
            ),
            new ApiDocumentationDomainService(pageQueryService, planQueryService),
            pageCrudService,
            pageSourceDomainService
        );
        roleQueryService.initWith(
            List.of(
                Role
                    .builder()
                    .id("role-id")
                    .scope(Role.Scope.API)
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION_ID)
                    .name("PRIMARY_OWNER")
                    .build()
            )
        );

        useCase =
            new ImportApiDefinitionUseCase(
                apiCrudService,
                apiImportDomainService,
                apiPrimaryOwnerFactory,
                createApiDomainService,
                validateApiDomainService,
                apiMetadataDomainService,
                createPlanDomainService,
                createApiDocumentationDomainService,
                apiIdsCalculatorDomainService,
                metadataCrudService,
                documentationValidationDomainService
            );

        parametersQueryService.initWith(
            List.of(
                new Parameter(
                    Key.API_PRIMARY_OWNER_MODE.key(),
                    ENVIRONMENT_ID,
                    ParameterReferenceType.ENVIRONMENT,
                    ApiPrimaryOwnerMode.USER.name()
                ),
                new Parameter(Key.PLAN_SECURITY_APIKEY_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email(USER_EMAIL).build()));
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                apiCrudService,
                auditCrudService,
                flowCrudService,
                groupQueryService,
                membershipCrudService,
                metadataCrudService,
                notificationConfigCrudService,
                pageCrudService,
                pageRevisionCrudService,
                parametersQueryService,
                planCrudService,
                roleQueryService,
                userCrudService,
                workflowCrudService
            )
            .forEach(InMemoryAlternative::reset);
    }

    @ParameterizedTest(name = "Test for API import with {0} definition version")
    @EnumSource(value = DefinitionVersion.class, names = { "V1", "V2" })
    void should_not_allow_import_with_api_definition_version(DefinitionVersion definitionVersion) {
        // Given
        var importDefinition = ImportDefinition
            .builder()
            .apiExport(ApiExport.builder().definitionVersion(definitionVersion).build())
            .build();

        // When
        var throwable = catchThrowable(() -> useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(ApiDefinitionVersionNotSupportedException.class);
    }

    @Test
    void should_not_allow_api_import_if_api_id_exists() {
        // Given
        apiCrudService.initWith(List.of(Api.builder().id(EXISTING_API_ID).build()));
        var importDefinition = ImportDefinition
            .builder()
            .apiExport(ApiExport.builder().id(EXISTING_API_ID).definitionVersion(DefinitionVersion.V4).build())
            .build();

        // When
        var throwable = catchThrowable(() -> useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(ApiAlreadyExistsException.class);
    }

    @Nested
    class Create {

        @BeforeEach
        void setUp() {
            when(validateApiDomainService.validateAndSanitizeForCreation(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void should_create_a_new_api_without_sub_entities_with_user_defined_id() {
            // Given
            var importDefinition = anImportDefinition();
            // Setting a user defined ID to have the same behavior as legacy import. Might be challenged
            final String customId = "a-custom-id";
            importDefinition.getApiExport().setId(customId);

            // When
            useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO));

            // Then
            var expected = expectedApi();
            expected.setId(customId);
            SoftAssertions.assertSoftly(soft -> {
                var createdApi = apiCrudService.get(customId);
                soft.assertThat(createdApi).isEqualTo(expected);
                soft.assertThat(createdApi.getCreatedAt()).isNotNull();
                soft.assertThat(createdApi.getUpdatedAt()).isNotNull();

                soft
                    .assertThat(indexer.storage())
                    .containsExactly(
                        new IndexableApi(
                            expected,
                            new PrimaryOwnerEntity(USER_ID, USER_EMAIL, "Jane Doe", PrimaryOwnerEntity.Type.USER),
                            Map.ofEntries(Map.entry("email-support", USER_EMAIL)),
                            Collections.emptySet()
                        )
                    );
            });
        }

        @Test
        void should_create_a_new_api_without_sub_entities() {
            // Given
            var importDefinition = anImportDefinition();

            // When
            useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO));

            // Then
            var expected = expectedApi();
            SoftAssertions.assertSoftly(soft -> {
                var createdApi = apiCrudService.get(API_ID);
                soft.assertThat(createdApi).isEqualTo(expected);
                soft.assertThat(createdApi.getCreatedAt()).isNotNull();
                soft.assertThat(createdApi.getUpdatedAt()).isNotNull();

                soft
                    .assertThat(indexer.storage())
                    .containsExactly(
                        new IndexableApi(
                            expected,
                            new PrimaryOwnerEntity(USER_ID, USER_EMAIL, "Jane Doe", PrimaryOwnerEntity.Type.USER),
                            Map.ofEntries(Map.entry("email-support", USER_EMAIL)),
                            Collections.emptySet()
                        )
                    );
            });
        }

        @Test
        void should_create_a_new_api_with_metadata() {
            // Given
            metadataCrudService.initWith(
                List.of(
                    Metadata
                        .builder()
                        .key("support-email-key")
                        .format(Metadata.MetadataFormat.MAIL)
                        .name("metadata-name")
                        .value("metadata-value")
                        .referenceId(API_ID)
                        .referenceType(Metadata.ReferenceType.API)
                        .build()
                )
            );
            var metadataToCreate = NewApiMetadata
                .builder()
                .format(Metadata.MetadataFormat.BOOLEAN)
                .key("metadata-boolean")
                .name("metadata-boolean")
                .value("false")
                .build();
            var metadataToUpdate = NewApiMetadata
                .builder()
                .format(Metadata.MetadataFormat.MAIL)
                .key("support-email-key")
                .name("metadata-name-updated")
                .value("metadata-value-updated")
                .build();
            var importDefinition = anImportDefinition().toBuilder().metadata(Set.of(metadataToCreate, metadataToUpdate)).build();

            // When
            useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO));

            // Then
            var expectedApi = expectedApi();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(apiCrudService.storage()).contains(expectedApi);
                var apiMetadataCreated = metadataCrudService
                    .storage()
                    .stream()
                    .filter(metadata -> metadata.getReferenceId().equals(API_ID))
                    .collect(Collectors.toSet());
                soft
                    .assertThat(apiMetadataCreated)
                    .extracting(Metadata::getKey, Metadata::getFormat, Metadata::getName, Metadata::getValue)
                    .contains(
                        tuple(
                            metadataToCreate.getKey(),
                            metadataToCreate.getFormat(),
                            metadataToCreate.getName(),
                            metadataToCreate.getValue()
                        ),
                        tuple(
                            metadataToUpdate.getKey(),
                            metadataToUpdate.getFormat(),
                            metadataToUpdate.getName(),
                            metadataToUpdate.getValue()
                        )
                    );
            });
        }

        @Test
        void should_create_a_new_api_with_a_plan() {
            // Given
            var plan = PlanWithFlowsFixtures
                .aPlanWithFlows()
                .toBuilder()
                .apiId(API_ID)
                .planDefinitionV4(PlanWithFlowsFixtures.aPlanWithFlows().getPlanDefinitionV4().toBuilder().tags(TAGS).build())
                .build();
            var importDefinition = anImportDefinition().toBuilder().plans(Set.of(plan)).build();

            // When
            useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO));

            // Then
            SoftAssertions.assertSoftly(soft -> {
                var expectedApi = expectedApi();
                soft.assertThat(apiCrudService.storage()).contains(expectedApi);

                var createdPlans = planCrudService.storage().stream().filter(p -> p.getApiId().equals(API_ID)).collect(Collectors.toSet());
                var expectedPlan = plan
                    .toBuilder()
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .needRedeployAt(Date.from(INSTANT_NOW))
                    .build();
                soft.assertThat(createdPlans).containsExactly(expectedPlan);
            });
        }

        @Test
        void should_create_a_new_api_with_a_page() {
            // Given
            var page = PageFixtures.aPage().toBuilder().referenceId(null).build();
            var importDefinition = anImportDefinition().toBuilder().pages(List.of(page)).build();
            // When
            useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO));

            // Then
            SoftAssertions.assertSoftly(soft -> {
                var expectedApi = expectedApi();
                soft.assertThat(apiCrudService.storage()).contains(expectedApi);

                var createPages = pageCrudService
                    .storage()
                    .stream()
                    .filter(p -> p.getReferenceId().equals(API_ID))
                    .collect(Collectors.toSet());
                var expectedPage = page
                    .toBuilder()
                    .referenceId(API_ID)
                    .createdAt(Date.from(INSTANT_NOW))
                    .updatedAt(Date.from(INSTANT_NOW))
                    .build();
                soft.assertThat(createPages).containsExactly(expectedPage);
            });
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        void should_throw_an_error_when_name_is_not_valid(String name) {
            var page = PageFixtures.aPage().toBuilder().name(name).referenceId(null).build();
            var importDefinition = anImportDefinition().toBuilder().pages(List.of(page)).build();

            assertThatThrownBy(() -> useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO)))
                .isInstanceOf(InvalidPageNameException.class);
        }

        @Test
        void should_throw_error_if_markdown_content_is_unsafe() {
            var page = PageFixtures
                .aPage()
                .toBuilder()
                .name("page name")
                .content(getNotSafe())
                .referenceId(null)
                .type(Page.Type.MARKDOWN)
                .build();
            var importDefinition = anImportDefinition().toBuilder().pages(List.of(page)).build();

            assertThatThrownBy(() -> useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO)))
                .isInstanceOf(PageContentUnsafeException.class);
        }

        @Test
        void should_throw_error_if_swagger_content_is_unsafe() {
            var page = PageFixtures
                .aPage()
                .toBuilder()
                .name("page name")
                .content(getNotSafe())
                .referenceId(null)
                .type(Page.Type.SWAGGER)
                .build();
            var importDefinition = anImportDefinition().toBuilder().pages(List.of(page)).build();

            assertThatThrownBy(() -> useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO)))
                .isInstanceOf(InvalidPageContentException.class);
        }

        @Test
        void should_throw_error_if_parent_is_not_a_folder() {
            var parentId = "parent-id";

            var page = PageFixtures
                .aPage()
                .toBuilder()
                .name("new name")
                .content("")
                .parentId(parentId)
                .type(Page.Type.MARKDOWN)
                .referenceId(API_ID)
                .referenceType(Page.ReferenceType.API)
                .build();
            var importDefinition = anImportDefinition()
                .toBuilder()
                .pages(List.of(page, Page.builder().id(parentId).type(Page.Type.MARKDOWN).name("page name").build()))
                .build();

            assertThatThrownBy(() -> useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO)))
                .isInstanceOf(InvalidPageParentException.class);
        }

        @Test
        void should_throw_error_when_name_is_not_unique() {
            pageQueryService.initWith(
                List.of(
                    Page
                        .builder()
                        .name("page name")
                        .type(Page.Type.MARKDOWN)
                        .referenceId("api-id")
                        .referenceType(Page.ReferenceType.API)
                        .build()
                )
            );

            var page = PageFixtures
                .aPage()
                .toBuilder()
                .name("page name")
                .content(getNotSafe())
                .referenceId(null)
                .type(Page.Type.SWAGGER)
                .build();
            var importDefinition = anImportDefinition().toBuilder().pages(List.of(page)).build();

            assertThatThrownBy(() -> useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO)))
                .isInstanceOf(ValidationDomainException.class);
        }

        @Test
        void should_create_a_new_api_with_a_media() {
            // Given
            var mediaList = List.of(MediaFixtures.aMedia());
            var importDefinition = anImportDefinition().toBuilder().apiMedia(mediaList).build();

            // When
            useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO));

            // Then
            var expectedApi = expectedApi();
            assertThat(apiCrudService.storage()).contains(expectedApi);
            verify(apiImportDomainService, times(1)).createPageAndMedia(eq(mediaList), eq(API_ID));
        }

        @Test
        void should_create_a_new_api_with_a_member() {
            // Given
            var members = Set.of(
                ApiMember
                    .builder()
                    .displayName("member")
                    .id("member-id")
                    .roles(List.of(ApiMemberRole.builder().name("role").scope(RoleScope.API).build()))
                    .build()
            );
            var importDefinition = anImportDefinition().toBuilder().members(members).build();

            // When
            useCase.execute(new ImportApiDefinitionUseCase.Input(importDefinition, AUDIT_INFO));

            // Then
            var expectedApi = expectedApi();
            assertThat(apiCrudService.storage()).contains(expectedApi);
            verify(apiImportDomainService, times(1)).createMembers(eq(members), eq(API_ID));
        }
    }

    private static ImportDefinition anImportDefinition() {
        return ImportDefinition.builder().apiExport(anApiExport()).build();
    }

    private static ApiExport anApiExport() {
        return ApiExport
            .builder()
            .id(null)
            .apiVersion("1.0.0")
            .analytics(Analytics.builder().enabled(false).build())
            .background(null)
            .categories(null)
            .crossId(API_CROSS_ID)
            .description("My Api description")
            .definitionVersion(DefinitionVersion.V4)
            .disableMembershipNotifications(false)
            .endpointGroups(anEndpointGroup())
            .groups(null)
            .labels(List.of("label-1"))
            .listeners(List.of(HttpListener.builder().paths(List.of(new Path())).build()))
            .name("My Api")
            .originContext(new ManagementContext())
            .picture(null)
            .properties(List.of(Property.builder().key("prop-key").value("prop-value").build()))
            .resources(List.of(Resource.builder().name("resource-name").type("resource-type").enabled(true).build()))
            .responseTemplates(Map.of("DEFAULT", Map.of("*.*", ResponseTemplate.builder().statusCode(200).build())))
            .tags(TAGS)
            .type(ApiType.PROXY)
            .build();
    }

    private Api expectedApi() {
        return aProxyApiV4()
            .toBuilder()
            .apiDefinitionV4(
                ApiDefinitionFixtures
                    .anApiV4()
                    .toBuilder()
                    .analytics(Analytics.builder().enabled(false).build())
                    .apiVersion("1.0.0")
                    .endpointGroups(anEndpointGroup())
                    .id(API_ID)
                    .name("My Api")
                    .properties(List.of(Property.builder().key("prop-key").value("prop-value").build()))
                    .resources(List.of(Resource.builder().name("resource-name").type("resource-type").enabled(true).build()))
                    .responseTemplates(Map.of("DEFAULT", Map.of("*.*", ResponseTemplate.builder().statusCode(200).build())))
                    .tags(Set.of("tag"))
                    .build()
            )
            .apiLifecycleState(Api.ApiLifecycleState.CREATED)
            .background(null)
            .categories(null)
            .crossId(API_CROSS_ID)
            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .deployedAt(null)
            .description("My Api description")
            .disableMembershipNotifications(false)
            .environmentId(ENVIRONMENT_ID)
            .groups(null)
            .id(API_ID)
            .labels(List.of("label-1"))
            .lifecycleState(Api.LifecycleState.STOPPED)
            .originContext(new ManagementContext())
            .picture(null)
            .type(ApiType.PROXY)
            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .visibility(Api.Visibility.PRIVATE)
            .build();
    }

    private static List<EndpointGroup> anEndpointGroup() {
        return List.of(
            EndpointGroup
                .builder()
                .name("default-group")
                .type("http-proxy")
                .sharedConfiguration("{}")
                .endpoints(
                    List.of(
                        Endpoint
                            .builder()
                            .name("default-endpoint")
                            .type("http-proxy")
                            .inheritConfiguration(true)
                            .configuration("{\"target\":\"https://api.gravitee.io/echo\"}")
                            .build()
                    )
                )
                .build()
        );
    }

    private String getNotSafe() {
        String html = "";
        html += "<script src=\"/external.jpg\" />";
        html += "<div onClick=\"alert('test');\" style=\"margin: auto\">onclick alert<div>";

        return html;
    }
}
