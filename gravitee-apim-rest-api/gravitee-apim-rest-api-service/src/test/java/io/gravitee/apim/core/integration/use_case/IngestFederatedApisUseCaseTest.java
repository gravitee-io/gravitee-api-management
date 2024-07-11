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
package io.gravitee.apim.core.integration.use_case;

import static fixtures.core.model.IntegrationJobFixture.aPendingIngestJob;
import static fixtures.core.model.RoleFixtures.apiPrimaryOwnerRoleId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.IntegrationApiFixtures;
import fixtures.core.model.LicenseFixtures;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.IntegrationCrudServiceInMemory;
import inmemory.IntegrationJobCrudServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.NotificationConfigCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.PageRevisionCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import inmemory.WorkflowCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.GroupValidationService;
import io.gravitee.apim.core.api.domain_service.UpdateFederatedApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateFederatedApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.audit.model.event.MembershipAuditEvent;
import io.gravitee.apim.core.audit.model.event.PageAuditEvent;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.apim.core.integration.model.IntegrationJob;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.apim.core.search.model.IndexablePage;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.domain_service.plan.PlanSynchronizationLegacyWrapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedApi;
import io.gravitee.definition.model.federation.FederatedPlan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class IngestFederatedApisUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant UPDATE_TIME = Instant.parse("2023-11-22T10:15:30Z");
    private static final String INGEST_JOB_ID = "job-id";
    private static final String INTEGRATION_ID = "integration-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final IntegrationJob INGEST_JOB = aPendingIngestJob()
        .toBuilder()
        .id(INGEST_JOB_ID)
        .sourceId(INTEGRATION_ID)
        .initiatorId(USER_ID)
        .environmentId(ENVIRONMENT_ID)
        .build();

    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    CategoryDomainService categoryDomainService = mock(CategoryDomainService.class);
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    IntegrationJobCrudServiceInMemory integrationJobCrudService = new IntegrationJobCrudServiceInMemory();
    IntegrationCrudServiceInMemory integrationCrudService = new IntegrationCrudServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    ApiMetadataQueryServiceInMemory apiMetadataQueryServiceInMemory = new ApiMetadataQueryServiceInMemory(metadataCrudService);
    NotificationConfigCrudServiceInMemory notificationConfigCrudService = new NotificationConfigCrudServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    WorkflowCrudServiceInMemory workflowCrudService = new WorkflowCrudServiceInMemory();
    LicenseManager licenseManager = mock(LicenseManager.class);
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory(planCrudService);
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();

    IndexerInMemory indexer = new IndexerInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    PageQueryServiceInMemory pageQueryServiceInMemory = new PageQueryServiceInMemory(pageCrudService);
    PageRevisionCrudServiceInMemory pageRevisionCrudService = new PageRevisionCrudServiceInMemory();
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainService = new TriggerNotificationDomainServiceInMemory();

    GroupValidationService groupValidationService = new GroupValidationService(groupQueryService);
    ValidateFederatedApiDomainService validateFederatedApiDomainService = spy(
        new ValidateFederatedApiDomainService(groupValidationService, categoryDomainService)
    );
    IngestFederatedApisUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(seed -> seed != null ? seed : "generated-id");
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());

        var metadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudService);
        var membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
        var apiPrimaryOwnerFactory = new ApiPrimaryOwnerFactory(
            groupQueryService,
            membershipQueryService,
            parametersQueryService,
            roleQueryService,
            userCrudService
        );
        var apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
            auditDomainService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
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
            new ApiMetadataDomainService(metadataCrudService, apiMetadataQueryServiceInMemory, auditDomainService),
            apiPrimaryOwnerDomainService,
            new FlowCrudServiceInMemory(),
            notificationConfigCrudService,
            parametersQueryService,
            workflowCrudService
        );

        var planValidatorService = new PlanValidatorDomainService(
            parametersQueryService,
            policyValidationDomainService,
            new PageCrudServiceInMemory()
        );
        var flowValidationDomainService = new FlowValidationDomainService(
            policyValidationDomainService,
            new EntrypointPluginQueryServiceInMemory()
        );
        var createApiDocumentationDomainService = new CreateApiDocumentationDomainService(
            pageCrudService,
            pageRevisionCrudService,
            auditDomainService,
            indexer
        );

        var updateApiDocumentationDomainService = new UpdateApiDocumentationDomainService(
            pageCrudService,
            pageRevisionCrudService,
            auditDomainService,
            indexer
        );

        var synchronizationService = new SynchronizationService(new ObjectMapper());

        var planSynchronizationService = new PlanSynchronizationLegacyWrapper(synchronizationService);

        var reorderPlanDomainService = new ReorderPlanDomainService(planQueryService, planCrudService);

        var createPlanDomainService = new CreatePlanDomainService(
            planValidatorService,
            flowValidationDomainService,
            planCrudService,
            flowCrudService,
            auditDomainService
        );

        var updatePlanDomainService = new UpdatePlanDomainService(
            planQueryService,
            planCrudService,
            planValidatorService,
            flowValidationDomainService,
            flowCrudService,
            auditDomainService,
            planSynchronizationService,
            reorderPlanDomainService
        );

        var apiIndexerDomainService = new ApiIndexerDomainService(
            new ApiMetadataDecoderDomainService(metadataQueryService, new FreemarkerTemplateProcessor()),
            apiPrimaryOwnerDomainService,
            new ApiCategoryQueryServiceInMemory(),
            indexer
        );

        var updateFederatedApiDomainService = new UpdateFederatedApiDomainService(
            apiCrudService,
            auditDomainService,
            validateFederatedApiDomainService,
            categoryDomainService,
            apiIndexerDomainService
        );

        useCase =
            new IngestFederatedApisUseCase(
                integrationJobCrudService,
                apiPrimaryOwnerFactory,
                validateFederatedApiDomainService,
                apiCrudService,
                planCrudService,
                pageQueryServiceInMemory,
                createApiDomainService,
                updateFederatedApiDomainService,
                createPlanDomainService,
                updatePlanDomainService,
                createApiDocumentationDomainService,
                updateApiDocumentationDomainService,
                triggerNotificationDomainService
            );

        enableApiPrimaryOwnerMode(ApiPrimaryOwnerMode.USER);
        when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));

        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        givenExistingUsers(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );

        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anEnterpriseLicense());

        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));

        membershipQueryService.initWith(
            List.of(
                Membership
                    .builder()
                    .id("member-id")
                    .memberId("my-member-id")
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId("uid-1")
                    .roleId("api-po-id-organization-id")
                    .build()
            )
        );
        groupQueryService.initWith(
            List.of(
                Group
                    .builder()
                    .id("group-1")
                    .environmentId("environment-id")
                    .eventRules(List.of(new Group.GroupEventRule(Group.GroupEvent.API_CREATE)))
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id("my-member-id").email("one_valid@email.com").build()));
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                apiCrudService,
                auditCrudService,
                groupQueryService,
                integrationCrudService,
                membershipCrudService,
                metadataCrudService,
                apiMetadataQueryServiceInMemory,
                notificationConfigCrudService,
                parametersQueryService,
                roleQueryService,
                userCrudService,
                workflowCrudService,
                planCrudService,
                planQueryService,
                flowCrudService,
                pageCrudService,
                pageQueryServiceInMemory,
                pageRevisionCrudService
            )
            .forEach(InMemoryAlternative::reset);
        reset(licenseManager);
        triggerNotificationDomainService.reset();

        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Nested
    class FederatedApiCreation {

        @Test
        void should_create_and_index_a_federated_api() {
            // Given
            givenAnIngestJob(INGEST_JOB);
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .uniqueId("uid-1")
                        .id("asset-1")
                        .name("api-1")
                        .description("my description")
                        .version("1.1.1")
                        .connectionDetails(Map.of("url", "https://example.com"))
                        .build()
                );

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                Api expectedApi = Api
                    .builder()
                    .id("environment-idintegration-iduid-1")
                    .definitionVersion(DefinitionVersion.FEDERATED)
                    .name("api-1")
                    .description("my description")
                    .version("1.1.1")
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .environmentId(ENVIRONMENT_ID)
                    .lifecycleState(null)
                    .originContext(new OriginContext.Integration(INTEGRATION_ID))
                    .federatedApiDefinition(
                        FederatedApi
                            .builder()
                            .id("environment-idintegration-iduid-1")
                            .providerId("asset-1")
                            .apiVersion("1.1.1")
                            .name("api-1")
                            .server(Map.of("url", "https://example.com"))
                            .build()
                    )
                    .build();
                soft.assertThat(apiCrudService.storage()).containsExactlyInAnyOrder(expectedApi);
                soft
                    .assertThat(indexer.storage())
                    .containsExactly(
                        new IndexableApi(
                            expectedApi,
                            new PrimaryOwnerEntity(USER_ID, "jane.doe@gravitee.io", "Jane Doe", PrimaryOwnerEntity.Type.USER),
                            Map.of(),
                            Collections.emptySet()
                        )
                    );
            });
        }

        @Test
        void should_create_federated_api_with_default_version_if_none_exist() {
            // Given
            var expectedDefaultApiVersion = "0.0.0";
            givenAnIngestJob(INGEST_JOB);
            var apiToIngest = IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID).toBuilder().version(null).build();

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            //Then
            assertThat(apiCrudService.storage()).extracting(Api::getVersion).containsExactly(expectedDefaultApiVersion);
        }

        @Test
        void should_create_an_audit() {
            // Given
            givenAnIngestJob(INGEST_JOB);
            var apiToIngest = (IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID));

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            // Then
            assertThat(auditCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
                .containsExactly(
                    // API Audit
                    AuditEntity
                        .builder()
                        .id("generated-id")
                        .organizationId(ORGANIZATION_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .referenceType(AuditEntity.AuditReferenceType.API)
                        .referenceId("environment-idintegration-idasset-uid")
                        .user(USER_ID)
                        .properties(Collections.emptyMap())
                        .event(ApiAuditEvent.API_CREATED.name())
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .build(),
                    // Membership Audit
                    AuditEntity
                        .builder()
                        .id("generated-id")
                        .organizationId(ORGANIZATION_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .referenceType(AuditEntity.AuditReferenceType.API)
                        .referenceId("environment-idintegration-idasset-uid")
                        .user(USER_ID)
                        .properties(Map.of("USER", USER_ID))
                        .event(MembershipAuditEvent.MEMBERSHIP_CREATED.name())
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .build()
                );
        }

        @ParameterizedTest
        @EnumSource(value = ApiPrimaryOwnerMode.class, mode = EnumSource.Mode.INCLUDE, names = { "USER", "HYBRID" })
        void should_create_primary_owner_membership_when_user_or_hybrid_mode_is_enabled(ApiPrimaryOwnerMode mode) {
            // Given
            enableApiPrimaryOwnerMode(mode);
            givenAnIngestJob(INGEST_JOB);
            var apiToIngest = (IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID));

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            // Then
            assertThat(membershipCrudService.storage())
                .contains(
                    Membership
                        .builder()
                        .id("generated-id")
                        .roleId(apiPrimaryOwnerRoleId(ORGANIZATION_ID))
                        .memberId(USER_ID)
                        .memberType(Membership.Type.USER)
                        .referenceId("environment-idintegration-idasset-uid")
                        .referenceType(Membership.ReferenceType.API)
                        .source("system")
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .build()
                );
        }

        @Test
        void should_not_create_default_metadata() {
            // Given
            givenAnIngestJob(INGEST_JOB);
            var apiToIngest = (IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID));

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            // Then
            assertThat(metadataCrudService.storage()).isEmpty();
        }

        @Test
        void should_not_create_default_email_notification_configuration() {
            // Given
            givenAnIngestJob(INGEST_JOB);
            var apiToIngest = (IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID));

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            // Then
            assertThat(notificationConfigCrudService.storage()).isEmpty();
        }

        @Test
        void should_ignore_api_review_mode() {
            // Given
            enableApiReview();
            givenAnIngestJob(INGEST_JOB);
            var apisToIngest = List.of(
                IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID).toBuilder().id("asset-1").name("api-1").build(),
                IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID).toBuilder().id("asset-2").name("api-2").build()
            );

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, apisToIngest, false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete();

            // Then
            assertThat(workflowCrudService.storage()).isEmpty();
        }

        private void enableApiReview() {
            parametersQueryService.define(
                new Parameter(Key.API_REVIEW_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
            );
        }
    }

    @Nested
    class FederatedApiUpdate {

        @Test
        void should_update_federated_api_if_exists() {
            // Given
            givenAnIngestJob(INGEST_JOB);
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .uniqueId("uid-1")
                        .name("api-1-updated")
                        .description("my description updated")
                        .version("1.1.2")
                        .build()
                );
            givenExistingApi(
                ApiFixtures
                    .aFederatedApi()
                    .toBuilder()
                    .id(ENVIRONMENT_ID + INTEGRATION_ID + "uid-1")
                    .name("api-1")
                    .deployedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .description("my description")
                    .version("1.1.1")
                    .build()
            );
            TimeProvider.overrideClock(Clock.fixed(UPDATE_TIME, ZoneId.systemDefault()));

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                Api expectedApi = Api
                    .builder()
                    .id("environment-idintegration-iduid-1")
                    .definitionVersion(DefinitionVersion.FEDERATED)
                    .name("api-1-updated")
                    .description("my description updated")
                    .version("1.1.2")
                    .picture("api-picture")
                    .background("api-background")
                    .categories(Set.of())
                    .deployedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(UPDATE_TIME.atZone(ZoneId.systemDefault()))
                    .visibility(Api.Visibility.PUBLIC)
                    .apiLifecycleState(Api.ApiLifecycleState.PUBLISHED)
                    .groups(Set.of("group-1"))
                    .labels(List.of("label-1"))
                    .environmentId(ENVIRONMENT_ID)
                    .lifecycleState(null)
                    .disableMembershipNotifications(true)
                    .originContext(new OriginContext.Integration(INTEGRATION_ID))
                    .federatedApiDefinition(
                        FederatedApi
                            .builder()
                            .id(ENVIRONMENT_ID + INTEGRATION_ID + "uid-1")
                            .providerId("asset-id")
                            .apiVersion("1.1.2")
                            .name("api-1-updated")
                            .build()
                    )
                    .build();
                soft.assertThat(apiCrudService.storage()).containsExactlyInAnyOrder(expectedApi);
                soft
                    .assertThat(indexer.storage())
                    .containsExactly(
                        new IndexableApi(
                            expectedApi,
                            new PrimaryOwnerEntity(USER_ID, "jane.doe@gravitee.io", "Jane Doe", PrimaryOwnerEntity.Type.USER),
                            Map.of(),
                            Collections.emptySet()
                        )
                    );
            });
        }

        @Test
        void should_create_updated_api_audit_log() {
            //Given
            givenAnIngestJob(INGEST_JOB);
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .plans(
                            List.of(
                                new IntegrationApi.Plan("plan1", "Updated Plan 1", "Updated description 1", IntegrationApi.PlanType.API_KEY)
                            )
                        )
                        .build()
                );
            givenExistingApi(
                ApiFixtures
                    .aFederatedApi()
                    .toBuilder()
                    .id(ENVIRONMENT_ID + INTEGRATION_ID + "asset-uid")
                    .name("api-1")
                    .description("my description")
                    .version("1.1.1")
                    .build()
            );
            TimeProvider.overrideClock(Clock.fixed(UPDATE_TIME, ZoneId.systemDefault()));

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            // Then
            assertThat(auditCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
                .contains(
                    // API Audit
                    AuditEntity
                        .builder()
                        .id("generated-id")
                        .organizationId(ORGANIZATION_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .referenceType(AuditEntity.AuditReferenceType.API)
                        .referenceId("environment-idintegration-idasset-uid")
                        .user(USER_ID)
                        .properties(Collections.emptyMap())
                        .event(ApiAuditEvent.API_UPDATED.name())
                        .createdAt(UPDATE_TIME.atZone(ZoneId.systemDefault()))
                        .build()
                );
        }
    }

    @Nested
    class PlanCreation {

        @Test
        void should_create_all_plans_associated() {
            // Given
            givenAnIngestJob(INGEST_JOB);
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .plans(
                            List.of(
                                new IntegrationApi.Plan("plan1", "My Plan 1", "Description 1", IntegrationApi.PlanType.API_KEY),
                                new IntegrationApi.Plan("plan2", "My Plan 2", "Description 2", IntegrationApi.PlanType.API_KEY)
                            )
                        )
                        .build()
                );

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            // Then
            SoftAssertions.assertSoftly(soft ->
                soft
                    .assertThat(planCrudService.storage())
                    .containsExactlyInAnyOrder(
                        Plan
                            .builder()
                            .id("environment-idintegration-idasset-uidplan1")
                            .name("My Plan 1")
                            .description("Description 1")
                            .validation(Plan.PlanValidationType.MANUAL)
                            .apiId("environment-idintegration-idasset-uid")
                            .federatedPlanDefinition(
                                FederatedPlan
                                    .builder()
                                    .id("environment-idintegration-idasset-uidplan1")
                                    .providerId("plan1")
                                    .security(PlanSecurity.builder().type(PlanSecurityType.API_KEY.getLabel()).build())
                                    .status(PlanStatus.PUBLISHED)
                                    .mode(PlanMode.STANDARD)
                                    .build()
                            )
                            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .build(),
                        Plan
                            .builder()
                            .id("environment-idintegration-idasset-uidplan2")
                            .name("My Plan 2")
                            .description("Description 2")
                            .validation(Plan.PlanValidationType.MANUAL)
                            .apiId("environment-idintegration-idasset-uid")
                            .federatedPlanDefinition(
                                FederatedPlan
                                    .builder()
                                    .id("environment-idintegration-idasset-uidplan2")
                                    .providerId("plan2")
                                    .security(PlanSecurity.builder().type(PlanSecurityType.API_KEY.getLabel()).build())
                                    .status(PlanStatus.PUBLISHED)
                                    .mode(PlanMode.STANDARD)
                                    .build()
                            )
                            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .build()
                    )
            );
        }
    }

    @Nested
    class PlanUpdate {

        @Test
        void should_update_plans_if_exist() {
            // Given
            givenAnIngestJob(INGEST_JOB);
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .plans(
                            List.of(
                                new IntegrationApi.Plan("plan1", "Updated Plan 1", "Updated description 1", IntegrationApi.PlanType.API_KEY)
                            )
                        )
                        .build()
                );
            givenExistingApi(
                ApiFixtures
                    .aFederatedApi()
                    .toBuilder()
                    .id(ENVIRONMENT_ID + INTEGRATION_ID + "asset-uid")
                    .name("api-1")
                    .description("my description")
                    .version("1.1.1")
                    .build()
            );
            givenExistingPlans(
                Plan
                    .builder()
                    .id("environment-idintegration-idasset-uidplan1")
                    .name("My Plan 1")
                    .description("Description 1")
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .definitionVersion(DefinitionVersion.FEDERATED)
                    .federatedPlanDefinition(
                        FederatedPlan
                            .builder()
                            .id("generated-id")
                            .providerId("plan1")
                            .security(PlanSecurity.builder().type(PlanSecurityType.API_KEY.getLabel()).build())
                            .status(PlanStatus.PUBLISHED)
                            .mode(PlanMode.STANDARD)
                            .build()
                    )
                    .validation(Plan.PlanValidationType.MANUAL)
                    .apiId("environment-idintegration-idasset-uid")
                    .build()
            );
            TimeProvider.overrideClock(Clock.fixed(UPDATE_TIME, ZoneId.systemDefault()));

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            // Then
            SoftAssertions.assertSoftly(soft ->
                soft
                    .assertThat(planCrudService.storage())
                    .containsExactlyInAnyOrder(
                        Plan
                            .builder()
                            .id("environment-idintegration-idasset-uidplan1")
                            .name("Updated Plan 1")
                            .description("Updated description 1")
                            .validation(Plan.PlanValidationType.MANUAL)
                            .apiId("environment-idintegration-idasset-uid")
                            .federatedPlanDefinition(
                                FederatedPlan
                                    .builder()
                                    .id("generated-id")
                                    .providerId("plan1")
                                    .security(PlanSecurity.builder().type(PlanSecurityType.API_KEY.getLabel()).build())
                                    .status(PlanStatus.PUBLISHED)
                                    .mode(PlanMode.STANDARD)
                                    .build()
                            )
                            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .updatedAt(UPDATE_TIME.atZone(ZoneId.systemDefault()))
                            .build()
                    )
            );
        }

        @Test
        void should_create_update_plan_audit_log() {
            //Given
            givenAnIngestJob(INGEST_JOB);
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .plans(
                            List.of(
                                new IntegrationApi.Plan("plan1", "Updated Plan 1", "Updated description 1", IntegrationApi.PlanType.API_KEY)
                            )
                        )
                        .build()
                );
            givenExistingApi(
                ApiFixtures
                    .aFederatedApi()
                    .toBuilder()
                    .id(ENVIRONMENT_ID + INTEGRATION_ID + "asset-uid")
                    .name("api-1")
                    .description("my description")
                    .version("1.1.1")
                    .build()
            );
            givenExistingPlans(
                Plan
                    .builder()
                    .id("environment-idintegration-idasset-uidplan1")
                    .name("My Plan 1")
                    .description("Description 1")
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .definitionVersion(DefinitionVersion.FEDERATED)
                    .federatedPlanDefinition(
                        FederatedPlan
                            .builder()
                            .id("generated-id")
                            .providerId("plan1")
                            .security(PlanSecurity.builder().type(PlanSecurityType.API_KEY.getLabel()).build())
                            .status(PlanStatus.PUBLISHED)
                            .mode(PlanMode.STANDARD)
                            .build()
                    )
                    .validation(Plan.PlanValidationType.MANUAL)
                    .apiId("environment-idintegration-idasset-uid")
                    .build()
            );

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            assertThat(auditCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
                .contains(
                    // Plan Update Audit
                    AuditEntity
                        .builder()
                        .id("generated-id")
                        .organizationId(ORGANIZATION_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .referenceType(AuditEntity.AuditReferenceType.API)
                        .referenceId("environment-idintegration-idasset-uid")
                        .user(USER_ID)
                        .properties(Map.of("PLAN", "environment-idintegration-idasset-uidplan1"))
                        .event(PlanAuditEvent.PLAN_UPDATED.name())
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .build()
                );
        }
    }

    @Nested
    class DocumentationCreation {

        @BeforeEach
        void setUp() {
            givenAnIngestJob(INGEST_JOB);
        }

        @Test
        void should_create_swagger_documentation() {
            // Given
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .pages(List.of(new IntegrationApi.Page(IntegrationApi.PageType.SWAGGER, "someSwaggerDoc")))
                        .build()
                );

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            var expectedPage = Page
                .builder()
                .id("generated-id")
                .name("An alien API-oas.yml")
                .referenceId("environment-idintegration-idasset-uid")
                .referenceType(Page.ReferenceType.API)
                .type(Page.Type.SWAGGER)
                .visibility(Page.Visibility.PRIVATE)
                .createdAt(Date.from(INSTANT_NOW))
                .updatedAt(Date.from(INSTANT_NOW))
                .content("someSwaggerDoc")
                .homepage(true)
                .published(true)
                .configuration(Map.of("tryIt", "true", "viewer", "Swagger"))
                .build();

            //Then
            assertThat(pageCrudService.storage()).isNotEmpty().contains(expectedPage);
        }

        @Test
        void should_create_api_audit_log() {
            //Given
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .pages(List.of(new IntegrationApi.Page(IntegrationApi.PageType.SWAGGER, "someSwaggerDoc")))
                        .build()
                );

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            assertThat(auditCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
                .contains(
                    // Page Audit
                    AuditEntity
                        .builder()
                        .id("generated-id")
                        .organizationId(ORGANIZATION_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .referenceType(AuditEntity.AuditReferenceType.API)
                        .referenceId("environment-idintegration-idasset-uid")
                        .user(USER_ID)
                        .properties(Map.of("PAGE", "generated-id"))
                        .event(PageAuditEvent.PAGE_CREATED.name())
                        .createdAt(INSTANT_NOW.atZone(ZoneId.of("UTC")))
                        .build()
                );
        }

        @Test
        void should_create_and_index_a_federated_page() {
            //Given
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .pages(List.of(new IntegrationApi.Page(IntegrationApi.PageType.SWAGGER, "someSwaggerDoc")))
                        .build()
                );

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            var expectedPage = Page
                .builder()
                .id("generated-id")
                .name("An alien API-oas.yml")
                .referenceId("environment-idintegration-idasset-uid")
                .referenceType(Page.ReferenceType.API)
                .type(Page.Type.SWAGGER)
                .visibility(Page.Visibility.PRIVATE)
                .createdAt(Date.from(INSTANT_NOW))
                .updatedAt(Date.from(INSTANT_NOW))
                .content("someSwaggerDoc")
                .homepage(true)
                .published(true)
                .configuration(Map.of("tryIt", "true", "viewer", "Swagger"))
                .build();

            // Then
            assertThat(indexer.storage()).contains(new IndexablePage(expectedPage));
        }

        @Test
        void should_create_and_index_a_federated_page_for_asyncapi() {
            //Given
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .pages(List.of(new IntegrationApi.Page(IntegrationApi.PageType.ASYNCAPI, "some async Doc")))
                        .build()
                );

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            var expectedPage = Page
                .builder()
                .id("generated-id")
                .name("An alien API.json")
                .referenceId("environment-idintegration-idasset-uid")
                .referenceType(Page.ReferenceType.API)
                .type(Page.Type.ASYNCAPI)
                .visibility(Page.Visibility.PRIVATE)
                .createdAt(Date.from(INSTANT_NOW))
                .updatedAt(Date.from(INSTANT_NOW))
                .content("some async Doc")
                .homepage(true)
                .published(true)
                .build();

            // Then
            assertThat(pageCrudService.storage()).contains(expectedPage);
        }

        @Test
        void should_not_create_documentation_if_pages_list_is_null() {
            //Given
            var apiToIngest = IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID).toBuilder().pages(null).build();

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            //Then
            assertThat(pageCrudService.storage()).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(value = IntegrationApi.PageType.class, mode = EnumSource.Mode.EXCLUDE, names = { "SWAGGER", "ASYNCAPI" })
        void should_not_create_documentation_if_pageType_is_other_than_SWAGGER(IntegrationApi.PageType pageType) {
            //Given
            var apiToIngest = IntegrationApiFixtures
                .anIntegrationApiForIntegration(INTEGRATION_ID)
                .toBuilder()
                .pages(List.of(new IntegrationApi.Page(pageType, "somePageTypeContent")))
                .build();

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            //Then
            assertThat(pageCrudService.storage()).isEmpty();
        }
    }

    @Nested
    class DocumentationUpdate {

        @BeforeEach
        void setUp() {
            givenAnIngestJob(INGEST_JOB);
        }

        @Test
        void should_update_swagger_doc_page_if_exists() {
            // Given
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .uniqueId("uid-1")
                        .pages(List.of(new IntegrationApi.Page(IntegrationApi.PageType.SWAGGER, "updatedSwaggerDoc")))
                        .build()
                );
            givenExistingApi(
                ApiFixtures
                    .aFederatedApi()
                    .toBuilder()
                    .id(ENVIRONMENT_ID + INTEGRATION_ID + "uid-1")
                    .name("An alien API")
                    .description("my description")
                    .version("1.1.1")
                    .build()
            );
            givenExistingPage(
                Page
                    .builder()
                    .id("generated-id")
                    .name("An alien API-oas.yml")
                    .referenceId("environment-idintegration-iduid-1")
                    .referenceType(Page.ReferenceType.API)
                    .type(Page.Type.SWAGGER)
                    .visibility(Page.Visibility.PRIVATE)
                    .createdAt(Date.from(INSTANT_NOW))
                    .updatedAt(Date.from(INSTANT_NOW))
                    .content("someOldSwaggerDoc")
                    .homepage(true)
                    .published(true)
                    .configuration(Map.of("tryIt", "true", "viewer", "Swagger"))
                    .build()
            );
            TimeProvider.overrideClock(Clock.fixed(UPDATE_TIME, ZoneId.systemDefault()));

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            var expectedPage = Page
                .builder()
                .id("generated-id")
                .name("An alien API-oas.yml")
                .referenceId("environment-idintegration-iduid-1")
                .referenceType(Page.ReferenceType.API)
                .type(Page.Type.SWAGGER)
                .visibility(Page.Visibility.PRIVATE)
                .createdAt(Date.from(INSTANT_NOW))
                .updatedAt(Date.from(UPDATE_TIME))
                .content("updatedSwaggerDoc")
                .homepage(true)
                .published(true)
                .configuration(Map.of("tryIt", "true", "viewer", "Swagger"))
                .build();

            //Then
            assertThat(pageCrudService.storage()).isNotEmpty().contains(expectedPage);
        }

        @Test
        void should_update_async_api_doc_page_if_exists() {
            //Given
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .uniqueId("uid-1")
                        .pages(List.of(new IntegrationApi.Page(IntegrationApi.PageType.ASYNCAPI, "some updated async Doc")))
                        .build()
                );
            givenExistingApi(
                ApiFixtures
                    .aFederatedApi()
                    .toBuilder()
                    .id(ENVIRONMENT_ID + INTEGRATION_ID + "uid-1")
                    .name("An alien API")
                    .description("my description")
                    .version("1.1.1")
                    .build()
            );
            givenExistingPage(
                Page
                    .builder()
                    .id("generated-id")
                    .name("An alien API.json")
                    .referenceId("environment-idintegration-iduid-1")
                    .referenceType(Page.ReferenceType.API)
                    .type(Page.Type.ASYNCAPI)
                    .visibility(Page.Visibility.PRIVATE)
                    .createdAt(Date.from(INSTANT_NOW))
                    .updatedAt(Date.from(INSTANT_NOW))
                    .content("some async Doc")
                    .homepage(true)
                    .published(true)
                    .build()
            );
            TimeProvider.overrideClock(Clock.fixed(UPDATE_TIME, ZoneId.systemDefault()));

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            var expectedPage = Page
                .builder()
                .id("generated-id")
                .name("An alien API.json")
                .referenceId("environment-idintegration-iduid-1")
                .referenceType(Page.ReferenceType.API)
                .type(Page.Type.ASYNCAPI)
                .visibility(Page.Visibility.PRIVATE)
                .createdAt(Date.from(INSTANT_NOW))
                .updatedAt(Date.from(UPDATE_TIME))
                .content("some updated async Doc")
                .homepage(true)
                .published(true)
                .build();

            // Then
            assertThat(pageCrudService.storage()).contains(expectedPage);
        }

        @Test
        void should_create_doc_update_audit_log() {
            //Given
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .pages(List.of(new IntegrationApi.Page(IntegrationApi.PageType.ASYNCAPI, "some updated async Doc")))
                        .build()
                );
            givenExistingPage(
                Page
                    .builder()
                    .id("generated-id")
                    .name("An alien API.json")
                    .referenceId("environment-idintegration-idasset-uid")
                    .referenceType(Page.ReferenceType.API)
                    .type(Page.Type.ASYNCAPI)
                    .visibility(Page.Visibility.PRIVATE)
                    .createdAt(Date.from(INSTANT_NOW))
                    .updatedAt(Date.from(INSTANT_NOW))
                    .content("some async Doc")
                    .homepage(true)
                    .published(true)
                    .build()
            );
            givenExistingApi(
                ApiFixtures
                    .aFederatedApi()
                    .toBuilder()
                    .id(ENVIRONMENT_ID + INTEGRATION_ID + "asset-uid")
                    .name("An alien API")
                    .description("my description")
                    .version("1.1.1")
                    .build()
            );
            TimeProvider.overrideClock(Clock.fixed(UPDATE_TIME, ZoneId.systemDefault()));

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            assertThat(auditCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
                .contains(
                    // Page Audit
                    AuditEntity
                        .builder()
                        .id("generated-id")
                        .organizationId(ORGANIZATION_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .referenceType(AuditEntity.AuditReferenceType.API)
                        .referenceId("environment-idintegration-idasset-uid")
                        .user(USER_ID)
                        .properties(Map.of("PAGE", "generated-id"))
                        .event(PageAuditEvent.PAGE_UPDATED.name())
                        .createdAt(UPDATE_TIME.atZone(ZoneId.of("UTC")))
                        .build()
                );
        }

        @Test
        void should_update_doc_if_api_name_changed() {
            // Given
            var apiToIngest =
                (
                    IntegrationApiFixtures
                        .anIntegrationApiForIntegration(INTEGRATION_ID)
                        .toBuilder()
                        .uniqueId("uid-1")
                        .name("new-name")
                        .pages(List.of(new IntegrationApi.Page(IntegrationApi.PageType.SWAGGER, "updatedSwaggerDoc")))
                        .build()
                );
            givenExistingApi(
                ApiFixtures
                    .aFederatedApi()
                    .toBuilder()
                    .id(ENVIRONMENT_ID + INTEGRATION_ID + "uid-1")
                    .name("old-name")
                    .description("my description")
                    .version("1.1.1")
                    .build()
            );
            givenExistingPage(
                Page
                    .builder()
                    .id("generated-id")
                    .name("old-name-oas.yml")
                    .referenceId("environment-idintegration-iduid-1")
                    .referenceType(Page.ReferenceType.API)
                    .type(Page.Type.SWAGGER)
                    .visibility(Page.Visibility.PRIVATE)
                    .createdAt(Date.from(INSTANT_NOW))
                    .updatedAt(Date.from(INSTANT_NOW))
                    .content("someOldSwaggerDoc")
                    .homepage(true)
                    .published(true)
                    .configuration(Map.of("tryIt", "true", "viewer", "Swagger"))
                    .build()
            );
            TimeProvider.overrideClock(Clock.fixed(UPDATE_TIME, ZoneId.systemDefault()));

            // When
            useCase
                .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(apiToIngest), false))
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            var expectedPage = Page
                .builder()
                .id("generated-id")
                .name("new-name-oas.yml")
                .referenceId("environment-idintegration-iduid-1")
                .referenceType(Page.ReferenceType.API)
                .type(Page.Type.SWAGGER)
                .visibility(Page.Visibility.PRIVATE)
                .createdAt(Date.from(INSTANT_NOW))
                .updatedAt(Date.from(UPDATE_TIME))
                .content("updatedSwaggerDoc")
                .homepage(true)
                .published(true)
                .configuration(Map.of("tryIt", "true", "viewer", "Swagger"))
                .build();

            //Then
            assertThat(pageCrudService.storage()).isNotEmpty().contains(expectedPage);
            assertThat(auditCrudService.storage()).extracting(AuditEntity::getEvent).containsExactly("API_UPDATED", "PAGE_UPDATED");
        }
    }

    @Test
    void should_do_nothing_when_no_job_is_found() {
        // When
        useCase
            .execute(
                new IngestFederatedApisUseCase.Input(
                    ORGANIZATION_ID,
                    INGEST_JOB_ID,
                    List.of(IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID)),
                    false
                )
            )
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete();

        // Then
        assertThat(apiCrudService.storage()).isEmpty();
    }

    @Test
    void should_do_nothing_when_no_apis_to_ingest() {
        // Given
        givenAnIngestJob(INGEST_JOB);

        // When
        useCase
            .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, List.of(), false))
            .test()
            .awaitDone(10, TimeUnit.SECONDS);

        // Then
        Assertions.assertThat(apiCrudService.storage()).isEmpty();
    }

    @Test
    void should_create_a_federated_api_for_each_integration_apis() {
        // Given
        givenAnIngestJob(INGEST_JOB);
        var apisToIngest = List.of(
            IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID).toBuilder().uniqueId("api-id-1").name("api-1").build(),
            IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID).toBuilder().uniqueId("api-id-2").name("api-2").build()
        );

        // When
        useCase
            .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, apisToIngest, false))
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete();

        // Then
        Assertions.assertThat(apiCrudService.storage()).extracting(Api::getName).containsExactlyInAnyOrder("api-1", "api-2");
    }

    @Test
    void should_skip_creating_federated_api_when_validation_fails() {
        // Given
        givenAnIngestJob(INGEST_JOB);
        var apisToIngest = List.of(
            IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID).toBuilder().uniqueId("api-id-1").name("api-1").build(),
            IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID).toBuilder().uniqueId("api-id-2").name("api-2").build(),
            IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID).toBuilder().uniqueId("api-id-3").name("api-3").build()
        );
        doThrow(new ValidationDomainException("validation failed"))
            .when(validateFederatedApiDomainService)
            .validateAndSanitizeForCreation(argThat(api -> api.getName().equals("api-2")));

        // When
        useCase
            .execute(new IngestFederatedApisUseCase.Input(ORGANIZATION_ID, INGEST_JOB_ID, apisToIngest, false))
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete();

        // Then
        Assertions.assertThat(apiCrudService.storage()).extracting(Api::getName).containsExactlyInAnyOrder("api-1", "api-3");
    }

    private void givenAnIngestJob(IntegrationJob job) {
        integrationJobCrudService.initWith(List.of(job));
    }

    private void givenExistingApi(Api... apis) {
        apiCrudService.initWith(List.of(apis));
    }

    private void givenExistingPlans(Plan... plans) {
        planCrudService.initWith(List.of(plans));
    }

    private void givenExistingPage(Page... pages) {
        pageCrudService.initWith(List.of(pages));
    }

    private void givenExistingUsers(List<BaseUserEntity> users) {
        userCrudService.initWith(users);
    }

    private void enableApiPrimaryOwnerMode(ApiPrimaryOwnerMode mode) {
        parametersQueryService.initWith(
            List.of(new Parameter(Key.API_PRIMARY_OWNER_MODE.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, mode.name()))
        );
    }
}
