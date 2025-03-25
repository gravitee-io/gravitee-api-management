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

import static fixtures.ApplicationModelFixtures.anApplicationEntity;
import static fixtures.core.model.ApiFixtures.aProxyApiV4;
import static fixtures.core.model.PlanFixtures.aKeylessV4;
import static fixtures.core.model.PlanFixtures.anApiKeyV4;
import static fixtures.core.model.SubscriptionFixtures.aSubscription;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.definition.FlowFixtures;
import inmemory.*;
import io.gravitee.apim.core.api.domain_service.*;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRD;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.datetime.TimeProvider;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeletePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanSynchronizationService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.RejectSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ImportCRDUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String API_ID = "my-api";
    private static final String API_CROSS_ID = "my-api-cross-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String TAG = "tag1";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory(apiCrudService);
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    SubscriptionQueryServiceInMemory subscriptionQueryService = new SubscriptionQueryServiceInMemory(subscriptionCrudService);
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();

    ValidateApiDomainService validateApiDomainService = mock(ValidateApiDomainService.class);
    PlanSynchronizationService planSynchronizationService = mock(PlanSynchronizationService.class);
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory(planCrudService);
    IndexerInMemory indexer = new IndexerInMemory();
    UpdateApiDomainService updateApiDomainService;

    ImportCRDUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        var triggerNotificationDomainService = new TriggerNotificationDomainServiceInMemory();
        var auditDomainService = new AuditDomainService(auditCrudService, new UserCrudServiceInMemory(), new JacksonJsonDiffProcessor());
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
        var reorderPlanDomainService = new ReorderPlanDomainService(planQueryService, planCrudService);
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
        var deletePlanDomainService = new DeletePlanDomainService(planCrudService, subscriptionQueryService, auditDomainService);
        var closeSubscriptionDomainService = new CloseSubscriptionDomainService(
            subscriptionCrudService,
            applicationCrudService,
            auditDomainService,
            triggerNotificationDomainService,
            new RejectSubscriptionDomainService(
                subscriptionCrudService,
                planCrudService,
                auditDomainService,
                triggerNotificationDomainService,
                new UserCrudServiceInMemory()
            ),
            new RevokeApiKeyDomainService(
                new ApiKeyCrudServiceInMemory(),
                new ApiKeyQueryServiceInMemory(),
                subscriptionCrudService,
                auditDomainService,
                triggerNotificationDomainService
            )
        );

        var createApiDomainService = mock(CreateApiDomainService.class);
        var apiMetadataDomainService = mock(ApiMetadataDomainService.class);
        var deployApiDomainService = mock(DeployApiDomainService.class);
        updateApiDomainService = mock(UpdateApiDomainService.class);

        useCase =
            new ImportCRDUseCase(
                apiCrudService,
                apiQueryService,
                createApiDomainService,
                createPlanDomainService,
                apiMetadataDomainService,
                deployApiDomainService,
                updateApiDomainService,
                planQueryService,
                updatePlanDomainService,
                deletePlanDomainService,
                subscriptionQueryService,
                closeSubscriptionDomainService,
                reorderPlanDomainService
            );

        parametersQueryService.initWith(
            List.of(
                new Parameter(Key.PLAN_SECURITY_KEYLESS_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true"),
                new Parameter(Key.PLAN_SECURITY_APIKEY_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
            )
        );
        when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        when(planSynchronizationService.checkSynchronized(any(), any(), any(), any())).thenReturn(true);

        // Simulate API creation for while we are using Legacy services
        when(createApiDomainService.create(any(ApiCRD.class), any(AuditInfo.class)))
            .thenAnswer(invocation -> {
                ApiCRD apiCRD = invocation.getArgument(0);
                AuditInfo auditInfo = invocation.getArgument(1);

                var apiDefinition = ApiAdapter.INSTANCE.toApiDefinition(apiCRD);

                return Api
                    .builder()
                    .id(API_ID)
                    .crossId(apiCRD.getCrossId())
                    .environmentId(auditInfo.environmentId())
                    .definitionVersion(DefinitionVersion.V4)
                    .apiDefinitionV4(apiDefinition)
                    .type(apiDefinition.getType())
                    .apiLifecycleState(Api.ApiLifecycleState.valueOf(apiCRD.getLifecycleState()))
                    .lifecycleState(Api.LifecycleState.valueOf(apiCRD.getState()))
                    .build();
            });
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                apiCrudService,
                applicationCrudService,
                auditCrudService,
                flowCrudService,
                pageCrudService,
                planCrudService,
                parametersQueryService,
                subscriptionCrudService
            )
            .forEach(InMemoryAlternative::reset);
        reset(policyValidationDomainService, planSynchronizationService);
    }

    @Nested
    class Create {

        @Test
        void should_create_plans() {
            // Given

            // When
            useCase.execute(new ImportCRDUseCase.Input(AUDIT_INFO, aCRD()));

            // Then
            Assertions
                .assertThat(planCrudService.storage())
                .hasSize(1)
                .extracting(Plan::getId, Plan::getName, Plan::getPublishedAt)
                .containsExactly(tuple("keyless-id", "Keyless", INSTANT_NOW.atZone(ZoneId.systemDefault())));
            Assertions.assertThat(flowCrudService.storage()).extracting(Flow::getName).containsExactly("plan-flow");
        }

        @Test
        void should_return_CRD_status() {
            // Given

            // When
            var result = useCase.execute(new ImportCRDUseCase.Input(AUDIT_INFO, aCRD()));

            // Then
            Assertions
                .assertThat(result.status())
                .isEqualTo(
                    ApiCRDStatus
                        .builder()
                        .id(API_ID)
                        .crossId(API_CROSS_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .organizationId(ORGANIZATION_ID)
                        .state("STARTED")
                        .plans(Map.of("keyless-key", "keyless-id"))
                        .build()
                );
        }
    }

    @Nested
    class Update {

        private static final Api API_PROXY_V4 = aProxyApiV4()
            .toBuilder()
            .id(API_ID)
            .environmentId(ENVIRONMENT_ID)
            .crossId(API_CROSS_ID)
            .build();
        private static final Plan KEYLESS = aKeylessV4().toBuilder().apiId(API_ID).tags(Set.of(TAG)).build();
        private static final Plan API_KEY = anApiKeyV4().toBuilder().apiId(API_ID).tags(Set.of(TAG)).build();

        @BeforeEach
        void setUp() {
            // TODO fake update API for now until we get rid of Legacy
            when(updateApiDomainService.update(any(), any(), any())).thenAnswer(invocation -> API_PROXY_V4);
        }

        @Test
        void should_return_CRD_status() {
            // Given
            givenExistingApi(API_PROXY_V4);
            givenExistingPlans(List.of(KEYLESS));

            // When
            var result = useCase.execute(
                new ImportCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD()
                        .toBuilder()
                        .plans(
                            Map.of(
                                "keyless-key",
                                PlanCRD
                                    .builder()
                                    .id(KEYLESS.getId())
                                    .name(KEYLESS.getName())
                                    .security(KEYLESS.getSecurity())
                                    .mode(KEYLESS.getMode())
                                    .validation(KEYLESS.getValidation())
                                    .status(KEYLESS.getStatus())
                                    .type(KEYLESS.getType())
                                    .flows(List.of(FlowFixtures.aSimpleFlowV4().withName("keyless-flow")))
                                    .build(),
                                "apikey-key",
                                PlanCRD
                                    .builder()
                                    .name("API Key")
                                    .security(PlanSecurity.builder().type("API_KEY").build())
                                    .mode(PlanMode.STANDARD)
                                    .validation(Plan.PlanValidationType.AUTO)
                                    .status(PlanStatus.STAGING)
                                    .type(Plan.PlanType.API)
                                    .flows(List.of(FlowFixtures.aSimpleFlowV4().withName("apikey-flow")))
                                    .build()
                            )
                        )
                        .build()
                )
            );

            // Then
            Assertions
                .assertThat(result.status())
                .isEqualTo(
                    ApiCRDStatus
                        .builder()
                        .id(API_ID)
                        .crossId(API_CROSS_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .organizationId(ORGANIZATION_ID)
                        .state("STARTED")
                        .plans(Map.of("keyless-key", KEYLESS.getId(), "apikey-key", "generated-id"))
                        .build()
                );
        }

        @Test
        void should_create_new_plans() {
            // Given
            givenExistingApi(API_PROXY_V4);
            givenExistingPlans(List.of(KEYLESS));

            // When
            useCase.execute(
                new ImportCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD()
                        .toBuilder()
                        .plans(
                            Map.of(
                                "keyless-key",
                                PlanCRD
                                    .builder()
                                    .id(KEYLESS.getId())
                                    .name(KEYLESS.getName())
                                    .security(KEYLESS.getSecurity())
                                    .mode(KEYLESS.getMode())
                                    .validation(KEYLESS.getValidation())
                                    .status(KEYLESS.getStatus())
                                    .type(KEYLESS.getType())
                                    .flows(List.of(FlowFixtures.aSimpleFlowV4().withName("keyless-flow")))
                                    .build(),
                                "api-key",
                                PlanCRD
                                    .builder()
                                    .name("API Key")
                                    .security(PlanSecurity.builder().type("API_KEY").build())
                                    .mode(PlanMode.STANDARD)
                                    .validation(Plan.PlanValidationType.AUTO)
                                    .status(PlanStatus.STAGING)
                                    .type(Plan.PlanType.API)
                                    .flows(List.of(FlowFixtures.aSimpleFlowV4().withName("apikey-flow")))
                                    .build()
                            )
                        )
                        .build()
                )
            );

            // Then
            Assertions
                .assertThat(planCrudService.storage())
                .hasSize(2)
                .extracting(Plan::getId, Plan::getName)
                .containsExactly(tuple("keyless", "Keyless"), tuple("generated-id", "API Key"));

            Assertions.assertThat(flowCrudService.storage()).extracting(Flow::getName).contains("apikey-flow");
        }

        @Test
        void should_update_existing_plans() {
            // Given
            givenExistingApi(API_PROXY_V4);
            givenExistingPlans(List.of(KEYLESS));

            // When
            useCase.execute(
                new ImportCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD()
                        .toBuilder()
                        .plans(
                            Map.of(
                                "keyless-key",
                                PlanAdapter.INSTANCE
                                    .toCRD(KEYLESS)
                                    .toBuilder()
                                    .name("Updated Keyless")
                                    .description("Updated description")
                                    .flows(List.of(FlowFixtures.aSimpleFlowV4().toBuilder().name("updated flow").build()))
                                    .build()
                            )
                        )
                        .build()
                )
            );

            // Then
            Assertions
                .assertThat(planCrudService.storage())
                .hasSize(1)
                .extracting(Plan::getId, Plan::getName, Plan::getDescription)
                .containsExactly(tuple("keyless", "Updated Keyless", "Updated description"));
            Assertions.assertThat(flowCrudService.storage()).extracting(Flow::getName).containsExactly("updated flow");
        }

        @Test
        void should_delete_existing_plans_not_present_in_crd_anymore() {
            // Given
            givenExistingApi(API_PROXY_V4);
            givenExistingPlans(List.of(KEYLESS, API_KEY));

            // When
            useCase.execute(
                new ImportCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD().toBuilder().plans(Map.of("keyless-key", PlanAdapter.INSTANCE.toCRD(KEYLESS))).build()
                )
            );

            // Then
            Assertions.assertThat(planCrudService.storage()).hasSize(1).extracting(Plan::getId).containsExactly("keyless");
        }

        @Test
        void should_close_any_active_subscriptions_before_deleting_plans() {
            // Given
            givenExistingApi(API_PROXY_V4);
            givenExistingPlans(List.of(KEYLESS, API_KEY));
            var application = givenExistingApplication(anApplicationEntity());
            givenExistingSubscriptions(
                aSubscription()
                    .toBuilder()
                    .id("sub1")
                    .applicationId(application.getId())
                    .planId(API_KEY.getId())
                    .status(SubscriptionEntity.Status.ACCEPTED)
                    .build(),
                aSubscription()
                    .toBuilder()
                    .id("sub2")
                    .applicationId(application.getId())
                    .planId(API_KEY.getId())
                    .status(SubscriptionEntity.Status.PENDING)
                    .build(),
                aSubscription()
                    .toBuilder()
                    .id("sub3")
                    .applicationId(application.getId())
                    .planId(API_KEY.getId())
                    .status(SubscriptionEntity.Status.PAUSED)
                    .build()
            );

            // When
            useCase.execute(
                new ImportCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD().toBuilder().plans(Map.of("keyless-key", PlanAdapter.INSTANCE.toCRD(KEYLESS))).build()
                )
            );

            // Then
            Assertions.assertThat(planCrudService.storage()).hasSize(1).extracting(Plan::getId).containsExactly("keyless");
            Assertions
                .assertThat(subscriptionCrudService.storage())
                .extracting(SubscriptionEntity::getId, SubscriptionEntity::getStatus)
                .containsExactly(
                    tuple("sub1", SubscriptionEntity.Status.CLOSED),
                    tuple("sub2", SubscriptionEntity.Status.REJECTED),
                    tuple("sub3", SubscriptionEntity.Status.CLOSED)
                );
        }

        @Test
        void should_refresh_remaining_plan_order_after_deletion() {
            // Given
            givenExistingApi(API_PROXY_V4);
            givenExistingPlans(List.of(KEYLESS.toBuilder().order(2).build(), API_KEY.toBuilder().order(1).build()));

            // When
            useCase.execute(
                new ImportCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD()
                        .toBuilder()
                        .plans(Map.of("keyless-key", PlanAdapter.INSTANCE.toCRD(KEYLESS.toBuilder().order(2).build())))
                        .build()
                )
            );

            // Then
            Assertions
                .assertThat(planCrudService.storage())
                .hasSize(1)
                .extracting(Plan::getId, Plan::getOrder)
                .containsExactly(tuple("keyless", 1));
        }
    }

    void givenExistingApi(Api api) {
        apiQueryService.initWith(List.of(api));
    }

    void givenExistingPlans(List<Plan> plans) {
        planCrudService.initWith(plans);
    }

    BaseApplicationEntity givenExistingApplication(BaseApplicationEntity application) {
        applicationCrudService.initWith(List.of(application));
        return application;
    }

    void givenExistingSubscriptions(SubscriptionEntity... subscriptions) {
        subscriptionQueryService.initWith(Arrays.asList(subscriptions));
    }

    private static ApiCRD aCRD() {
        return ApiCRD
            .builder()
            .id(API_ID)
            .crossId(API_CROSS_ID)
            .type("PROXY")
            .state("STARTED")
            .lifecycleState("CREATED")
            .definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("MANAGEMENT").build())
            .plans(
                Map.of(
                    "keyless-key",
                    PlanCRD
                        .builder()
                        .id("keyless-id")
                        .name("Keyless")
                        .security(PlanSecurity.builder().type("KEY_LESS").build())
                        .mode(PlanMode.STANDARD)
                        .validation(Plan.PlanValidationType.AUTO)
                        .status(PlanStatus.PUBLISHED)
                        .type(Plan.PlanType.API)
                        .flows(List.of(FlowFixtures.aSimpleFlowV4().withName("plan-flow")))
                        .build()
                )
            )
            .properties(List.of(Property.builder().key("prop-key").value("prop-value").build()))
            .resources(List.of(Resource.builder().name("resource-name").type("resource-type").enabled(true).build()))
            .responseTemplates(Map.of("DEFAULT", Map.of("*.*", ResponseTemplate.builder().statusCode(200).build())))
            .state("STARTED")
            .tags(Set.of(TAG))
            .type("PROXY")
            .version("1.0.0")
            .visibility("PRIVATE")
            .build();
    }
}
