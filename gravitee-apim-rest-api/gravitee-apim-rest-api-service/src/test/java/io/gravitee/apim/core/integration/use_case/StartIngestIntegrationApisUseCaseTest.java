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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.IntegrationFixture;
import fixtures.core.model.LicenseFixtures;
import inmemory.A2aAgentFetcherInMemory;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.AsyncJobCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.CreateCategoryApiDomainServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.IntegrationAgentInMemory;
import inmemory.IntegrationCrudServiceInMemory;
import inmemory.LicenseCrudServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.NotificationConfigCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
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
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.integration.exception.FederatedAgentIngestionException;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanSynchronizationService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.domain_service.plan.PlanSynchronizationLegacyWrapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedAgent;
import io.gravitee.definition.model.federation.FederatedPlan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StartIngestIntegrationApisUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String INTEGRATION_ID = "integration-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private final ObjectMapper objectMapper = new ObjectMapper();
    SynchronizationService synchronizationService = new SynchronizationService(objectMapper);

    IntegrationCrudServiceInMemory integrationCrudService = new IntegrationCrudServiceInMemory();
    AsyncJobCrudServiceInMemory asyncJobCrudService = new AsyncJobCrudServiceInMemory();
    LicenseCrudServiceInMemory licenseCrudService = new LicenseCrudServiceInMemory();
    IntegrationAgentInMemory integrationAgent = new IntegrationAgentInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    IndexerInMemory indexer = new IndexerInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    ApiCategoryQueryServiceInMemory apiCategoryQueryService1 = new ApiCategoryQueryServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    EntrypointPluginQueryServiceInMemory entrypointConnectorPluginService = new EntrypointPluginQueryServiceInMemory();
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    ApiCategoryQueryServiceInMemory apiCategoryQueryService2 = new ApiCategoryQueryServiceInMemory();
    WorkflowCrudServiceInMemory workflowCrudService = new WorkflowCrudServiceInMemory();
    CreateCategoryApiDomainServiceInMemory createCategoryApiDomainService = new CreateCategoryApiDomainServiceInMemory();
    ApiCategoryQueryServiceInMemory apiCategoryQueryService = new ApiCategoryQueryServiceInMemory();
    NotificationConfigCrudServiceInMemory notificationConfigCrudService = new NotificationConfigCrudServiceInMemory();
    A2aAgentFetcherInMemory a2aAgentFetcher = new A2aAgentFetcherInMemory();

    ApiMetadataQueryServiceInMemory metadataQueryService1 = new ApiMetadataQueryServiceInMemory(metadataCrudService);
    ApiMetadataQueryServiceInMemory apiMetadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudService);
    ApiMetadataQueryServiceInMemory metadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudService);
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);

    LicenseManager licenseManager = mock(LicenseManager.class);
    CategoryDomainService categoryDomainService = mock(CategoryDomainService.class);

    AuditDomainService auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());

    ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
        auditDomainService,
        groupQueryService,
        membershipCrudService,
        membershipQueryService,
        roleQueryService,
        userCrudService
    );

    ApiIndexerDomainService apiIndexerDomainService = new ApiIndexerDomainService(
        new ApiMetadataDecoderDomainService(metadataQueryService, new FreemarkerTemplateProcessor()),
        apiPrimaryOwnerDomainService,
        apiCategoryQueryService1,
        indexer
    );

    GroupValidationService groupValidationService = new GroupValidationService(groupQueryService);

    ValidateFederatedApiDomainService validateFederatedApi = new ValidateFederatedApiDomainService(
        groupValidationService,
        categoryDomainService
    );

    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    PlanValidatorDomainService planValidatorService = new PlanValidatorDomainService(
        parametersQueryService,
        policyValidationDomainService,
        pageCrudService
    );
    FlowValidationDomainService flowValidationDomainService = new FlowValidationDomainService(
        policyValidationDomainService,
        entrypointConnectorPluginService
    );
    CreatePlanDomainService createPlanDomainService = new CreatePlanDomainService(
        planValidatorService,
        flowValidationDomainService,
        planCrudService,
        flowCrudService,
        auditDomainService
    );
    ApiPrimaryOwnerFactory apiPrimaryOwnerFactory = new ApiPrimaryOwnerFactory(
        groupQueryService,
        membershipQueryService,
        parametersQueryService,
        roleQueryService,
        userCrudService
    );

    PlanSynchronizationService planSynchronizationService = new PlanSynchronizationLegacyWrapper(synchronizationService);
    PlanValidatorDomainService planValidatorDomainService = new PlanValidatorDomainService(
        parametersQueryService,
        policyValidationDomainService,
        pageCrudService
    );
    ReorderPlanDomainService reorderPlanDomainService = new ReorderPlanDomainService(planQueryService, planCrudService);
    UpdatePlanDomainService updatePlanDomainService = new UpdatePlanDomainService(
        planQueryService,
        planCrudService,
        planValidatorDomainService,
        flowValidationDomainService,
        flowCrudService,
        auditDomainService,
        planSynchronizationService,
        reorderPlanDomainService
    );
    ApiPrimaryOwnerDomainService apiPrimaryOwnerService = new ApiPrimaryOwnerDomainService(
        auditDomainService,
        groupQueryService,
        membershipCrudService,
        membershipQueryService,
        roleQueryService,
        userCrudService
    );
    UpdateFederatedApiDomainService updateFederatedApiDomainService = new UpdateFederatedApiDomainService(
        apiCrudService,
        auditDomainService,
        new ValidateFederatedApiDomainService(new GroupValidationService(groupQueryService), categoryDomainService),
        categoryDomainService,
        new ApiIndexerDomainService(
            new ApiMetadataDecoderDomainService(metadataQueryService1, new FreemarkerTemplateProcessor()),
            apiPrimaryOwnerService,
            apiCategoryQueryService2,
            indexer
        )
    );
    CreateApiDomainService createApiDomainService = new CreateApiDomainService(
        apiCrudService,
        auditDomainService,
        new ApiIndexerDomainService(
            new ApiMetadataDecoderDomainService(metadataQueryService, new FreemarkerTemplateProcessor()),
            apiPrimaryOwnerDomainService,
            apiCategoryQueryService,
            indexer
        ),
        new ApiMetadataDomainService(metadataCrudService, apiMetadataQueryService, auditDomainService),
        apiPrimaryOwnerDomainService,
        flowCrudService,
        notificationConfigCrudService,
        parametersQueryService,
        workflowCrudService,
        createCategoryApiDomainService
    );

    private StartIngestIntegrationApisUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(seed -> seed != null ? seed : "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        useCase = new StartIngestIntegrationApisUseCase(
            integrationCrudService,
            asyncJobCrudService,
            integrationAgent,
            new LicenseDomainService(licenseCrudService, licenseManager),
            createApiDomainService,
            updateFederatedApiDomainService,
            apiIndexerDomainService,
            a2aAgentFetcher,
            apiPrimaryOwnerFactory,
            validateFederatedApi,
            createPlanDomainService,
            updatePlanDomainService,
            planCrudService,
            apiCrudService
        );

        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    @AfterEach
    void tearDown() {
        Stream.of(
            integrationCrudService,
            asyncJobCrudService,
            licenseCrudService,
            integrationAgent,
            metadataCrudService,
            indexer,
            auditCrudService,
            userCrudService,
            roleQueryService,
            groupQueryService,
            membershipCrudService,
            apiCrudService,
            planCrudService,
            apiCategoryQueryService1,
            flowCrudService,
            parametersQueryService,
            pageCrudService,
            entrypointConnectorPluginService,
            planQueryService,
            apiCategoryQueryService2,
            workflowCrudService,
            createCategoryApiDomainService,
            apiCategoryQueryService,
            notificationConfigCrudService,
            metadataQueryService1,
            apiMetadataQueryService,
            metadataQueryService,
            membershipQueryService,
            a2aAgentFetcher
        ).forEach(InMemoryAlternative::reset);
        reset(licenseManager);
    }

    @Nested
    class ApiDiscovery {

        @Test
        void should_create_a_job_and_return_pending_status_when_a_job_has_started() {
            // Given
            givenAnIntegration(IntegrationFixture.anApiIntegration(ENVIRONMENT_ID).withId(INTEGRATION_ID));
            integrationAgent.configureApisNumberToIngest(INTEGRATION_ID, 10L);

            // When
            var result = useCase
                .execute(new StartIngestIntegrationApisUseCase.Input(INTEGRATION_ID, List.of(), AUDIT_INFO))
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .values();

            // Then
            assertThat(result).containsExactly(AsyncJob.Status.PENDING);
            assertThat(asyncJobCrudService.storage()).contains(
                AsyncJob.builder()
                    .id("generated-id")
                    .sourceId(INTEGRATION_ID)
                    .environmentId(ENVIRONMENT_ID)
                    .initiatorId(USER_ID)
                    .type(AsyncJob.Type.FEDERATED_APIS_INGESTION)
                    .status(AsyncJob.Status.PENDING)
                    .upperLimit(10L)
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .deadLine(INSTANT_NOW.atZone(ZoneId.systemDefault()).plus(Duration.ofMinutes(5)))
                    .build()
            );
        }

        @Test
        void should_return_done_status_when_no_job_has_started_because_no_apis_to_ingest() {
            // Given
            givenAnIntegration(IntegrationFixture.anApiIntegration(ENVIRONMENT_ID).withId(INTEGRATION_ID));
            integrationAgent.configureApisNumberToIngest(INTEGRATION_ID, 0L);

            // When
            var result = useCase
                .execute(new StartIngestIntegrationApisUseCase.Input(INTEGRATION_ID, List.of(), AUDIT_INFO))
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .values();

            // Then
            assertThat(asyncJobCrudService.storage()).isEmpty();
            assertThat(result).containsExactly(AsyncJob.Status.SUCCESS);
        }

        @Test
        void should_throw_when_no_license_found() {
            when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anOssLicense());

            useCase
                .execute(new StartIngestIntegrationApisUseCase.Input(INTEGRATION_ID, List.of(), AUDIT_INFO))
                .test()
                .assertError(NotAllowedDomainException.class);
        }

        @Test
        void should_throw_when_no_integration_is_found() {
            // When
            var obs = useCase.execute(new StartIngestIntegrationApisUseCase.Input("unknown", List.of(), AUDIT_INFO)).test();

            // Then
            obs.assertError(IntegrationNotFoundException.class);
        }
    }

    private void givenAnIntegration(Integration integration) {
        integrationCrudService.initWith(List.of(integration));
    }

    @Nested
    class A2aDiscovery {

        private FederatedAgent createFederatedAgent(String name, String version, String url) {
            return FederatedAgent.builder()
                .name(name)
                .description("Test agent description")
                .version(version)
                .url(url)
                .provider(new FederatedAgent.Provider("test-org", "https://provider.example.com"))
                .capabilities(Map.of())
                .skills(List.of())
                .defaultInputModes(List.of())
                .defaultOutputModes(List.of())
                .build();
        }

        @Test
        void should_return_success_status_for_successful_a2a_integration_with_single_well_known_url() {
            // Given
            var a2aIntegration = IntegrationFixture.anA2aIntegrationWithId("a2a-integration-id").withEnvironmentId(ENVIRONMENT_ID);
            givenAnIntegration(a2aIntegration);
            parametersQueryService.initWith(List.of(Parameter.builder().key(Key.API_PRIMARY_OWNER_MODE.key()).value("USER").build()));
            userCrudService.initWith(List.of(BaseUserEntity.builder().id("user-id").build()));

            var federatedAgent = createFederatedAgent("Test Agent", "1.0.0", "https://example.com/.well-known/agent.json");
            a2aAgentFetcher.add("https://example.com/.well-known/agent.json", federatedAgent);

            // When
            var result = useCase
                .execute(new StartIngestIntegrationApisUseCase.Input("a2a-integration-id", List.of(), AUDIT_INFO))
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .values();

            // Then
            assertThat(result).containsExactly(AsyncJob.Status.SUCCESS);
            assertThat(apiCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "updatedAt")
                .containsOnly(
                    Api.builder()
                        .id(
                            "environment-ida2a-integration-idhttps://example.com/.well-known/agent.jsonhttps://example.com/.well-known/agent.json"
                        )
                        .environmentId("environment-id")
                        .name("Test Agent")
                        .description("Test agent description")
                        .version("1.0.0")
                        .originContext(new OriginContext.Integration("a2a-integration-id", "A2A Integration", "A2A"))
                        .definitionVersion(DefinitionVersion.FEDERATED_AGENT)
                        .groups(Set.of())
                        .apiDefinitionValue(federatedAgent)
                        .lifecycleState(null)
                        .build()
                );
            assertThat(planCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "updatedAt")
                .containsOnly(
                    Plan.builder()
                        .id(
                            "environment-ida2a-integration-idhttps://example.com/.well-known/agent.jsonhttps://example.com/.well-known/agent.jsonkey-less"
                        )
                        .definitionVersion(DefinitionVersion.FEDERATED_AGENT)
                        .name("Key less plan")
                        .description("Default plan")
                        .validation(Plan.PlanValidationType.MANUAL)
                        .referenceType(GenericPlanEntity.ReferenceType.API)
                        .referenceId(
                            "environment-ida2a-integration-idhttps://example.com/.well-known/agent.jsonhttps://example.com/.well-known/agent.json"
                        )
                        .order(0)
                        .environmentId("environment-id")
                        .characteristics(List.of())
                        .excludedGroups(List.of())
                        .commentRequired(false)
                        .federatedPlanDefinition(
                            FederatedPlan.builder()
                                .id(
                                    "environment-ida2a-integration-idhttps://example.com/.well-known/agent.jsonhttps://example.com/.well-known/agent.jsonkey-less"
                                )
                                .providerId("test-org")
                                .security(PlanSecurity.builder().type("key-less").build())
                                .mode(PlanMode.STANDARD)
                                .status(PlanStatus.PUBLISHED)
                                .build()
                        )
                        .build()
                );
        }

        @Test
        void should_return_success_status_for_a2a_integration_with_multiple_well_known_urls() {
            // Given
            var wellKnownUrls = List.of(
                new Integration.A2aIntegration.WellKnownUrl("https://example1.com/.well-known/agent.json"),
                new Integration.A2aIntegration.WellKnownUrl("https://example2.com/.well-known/agent.json")
            );
            parametersQueryService.initWith(List.of(Parameter.builder().key(Key.API_PRIMARY_OWNER_MODE.key()).value("USER").build()));
            userCrudService.initWith(List.of(BaseUserEntity.builder().id("user-id").build()));
            var a2aIntegration = IntegrationFixture.anA2aIntegrationWithWellKnownUrls(wellKnownUrls).withEnvironmentId(ENVIRONMENT_ID);
            givenAnIntegration(a2aIntegration);

            var federatedAgent1 = createFederatedAgent("Agent 1", "1.0.0", "https://example1.com/.well-known/agent.json");
            var federatedAgent2 = createFederatedAgent("Agent 2", "2.0.0", "https://example2.com/.well-known/agent.json");

            a2aAgentFetcher.initWithMap(
                Map.of(
                    "https://example1.com/.well-known/agent.json",
                    federatedAgent1,
                    "https://example2.com/.well-known/agent.json",
                    federatedAgent2
                )
            );

            // When
            var result = useCase
                .execute(new StartIngestIntegrationApisUseCase.Input(a2aIntegration.id(), List.of(), AUDIT_INFO))
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .values();

            // Then
            assertThat(result).containsExactly(AsyncJob.Status.SUCCESS);
            assertThat(apiCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "updatedAt")
                .containsExactlyInAnyOrder(
                    Api.builder()
                        .id(
                            "environment-ida2a-integration-idhttps://example1.com/.well-known/agent.jsonhttps://example1.com/.well-known/agent.json"
                        )
                        .environmentId("environment-id")
                        .name("Agent 1")
                        .description("Test agent description")
                        .version("1.0.0")
                        .originContext(new OriginContext.Integration("a2a-integration-id", "A2A Integration", "A2A"))
                        .definitionVersion(DefinitionVersion.FEDERATED_AGENT)
                        .groups(Set.of())
                        .createdAt(ZonedDateTime.parse("2023-10-22T12:15:30+02:00[Europe/Paris]"))
                        .updatedAt(ZonedDateTime.parse("2023-10-22T12:15:30+02:00[Europe/Paris]"))
                        .apiDefinitionValue(federatedAgent1)
                        .lifecycleState(null)
                        .build(),
                    Api.builder()
                        .id(
                            "environment-ida2a-integration-idhttps://example2.com/.well-known/agent.jsonhttps://example2.com/.well-known/agent.json"
                        )
                        .environmentId("environment-id")
                        .name("Agent 2")
                        .description("Test agent description")
                        .version("2.0.0")
                        .originContext(new OriginContext.Integration("a2a-integration-id", "A2A Integration", "A2A"))
                        .definitionVersion(DefinitionVersion.FEDERATED_AGENT)
                        .groups(Set.of())
                        .apiDefinitionValue(federatedAgent2)
                        .lifecycleState(null)
                        .build()
                );
            assertThat(planCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "updatedAt")
                .containsExactlyInAnyOrder(
                    Plan.builder()
                        .id(
                            "environment-ida2a-integration-idhttps://example1.com/.well-known/agent.jsonhttps://example1.com/.well-known/agent.jsonkey-less"
                        )
                        .definitionVersion(DefinitionVersion.FEDERATED_AGENT)
                        .name("Key less plan")
                        .description("Default plan")
                        .validation(Plan.PlanValidationType.MANUAL)
                        .referenceType(GenericPlanEntity.ReferenceType.API)
                        .referenceId(
                            "environment-ida2a-integration-idhttps://example1.com/.well-known/agent.jsonhttps://example1.com/.well-known/agent.json"
                        )
                        .order(0)
                        .environmentId("environment-id")
                        .characteristics(List.of())
                        .excludedGroups(List.of())
                        .commentRequired(false)
                        .federatedPlanDefinition(
                            FederatedPlan.builder()
                                .id(
                                    "environment-ida2a-integration-idhttps://example1.com/.well-known/agent.jsonhttps://example1.com/.well-known/agent.jsonkey-less"
                                )
                                .providerId("test-org")
                                .security(PlanSecurity.builder().type("key-less").build())
                                .mode(PlanMode.STANDARD)
                                .status(PlanStatus.PUBLISHED)
                                .build()
                        )
                        .build(),
                    Plan.builder()
                        .id(
                            "environment-ida2a-integration-idhttps://example2.com/.well-known/agent.jsonhttps://example2.com/.well-known/agent.jsonkey-less"
                        )
                        .definitionVersion(DefinitionVersion.FEDERATED_AGENT)
                        .name("Key less plan")
                        .description("Default plan")
                        .validation(Plan.PlanValidationType.MANUAL)
                        .referenceType(GenericPlanEntity.ReferenceType.API)
                        .referenceId(
                            "environment-ida2a-integration-idhttps://example2.com/.well-known/agent.jsonhttps://example2.com/.well-known/agent.json"
                        )
                        .order(0)
                        .environmentId("environment-id")
                        .characteristics(List.of())
                        .excludedGroups(List.of())
                        .commentRequired(false)
                        .federatedPlanDefinition(
                            FederatedPlan.builder()
                                .id(
                                    "environment-ida2a-integration-idhttps://example2.com/.well-known/agent.jsonhttps://example2.com/.well-known/agent.jsonkey-less"
                                )
                                .providerId("test-org")
                                .security(PlanSecurity.builder().type("key-less").build())
                                .mode(PlanMode.STANDARD)
                                .status(PlanStatus.PUBLISHED)
                                .build()
                        )
                        .build()
                );
        }

        @Test
        void should_return_error_status_when_a2a_agent_fetching_fails() {
            // Given
            var a2aIntegration = IntegrationFixture.anA2aIntegrationWithId("a2a-integration-id").withEnvironmentId(ENVIRONMENT_ID);
            givenAnIntegration(a2aIntegration);

            //When, then
            useCase
                .execute(new StartIngestIntegrationApisUseCase.Input("a2a-integration-id", List.of(), AUDIT_INFO))
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(throwable -> {
                    assertThat(throwable)
                        .isInstanceOf(FederatedAgentIngestionException.class)
                        .hasMessage("Failed to ingest data from the following URLs: https://example.com/.well-known/agent.json");
                    return true;
                });

            assertThat(apiCrudService.storage()).isEmpty();
            assertThat(planCrudService.storage()).isEmpty();
        }

        @Test
        void should_return_success_status_for_a2a_integration_with_empty_well_known_urls() {
            // Given
            var a2aIntegration = IntegrationFixture.anA2aIntegrationWithWellKnownUrls(List.of()).withEnvironmentId(ENVIRONMENT_ID);
            givenAnIntegration(a2aIntegration);

            // When
            var result = useCase
                .execute(new StartIngestIntegrationApisUseCase.Input(a2aIntegration.id(), List.of(), AUDIT_INFO))
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .values();

            // Then
            assertThat(result).containsExactly(AsyncJob.Status.SUCCESS);
            assertThat(apiCrudService.storage()).isEmpty();
            assertThat(planCrudService.storage()).isEmpty();
        }
    }
}
