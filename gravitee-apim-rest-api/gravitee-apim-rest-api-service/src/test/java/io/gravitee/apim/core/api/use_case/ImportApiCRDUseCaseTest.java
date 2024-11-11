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
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.definition.ApiDefinitionFixtures;
import fixtures.definition.FlowFixtures;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.CRDMembersDomainServiceInMemory;
import inmemory.CategoryQueryServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.IntegrationAgentInMemory;
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
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import inmemory.UserDomainServiceInMemory;
import inmemory.ValidateResourceDomainServiceInMemory;
import inmemory.WorkflowCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiCRDDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.api.model.crd.PageCRD;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryIdsDomainService;
import io.gravitee.apim.core.category.model.Category;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.DocumentationValidationDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.ValidatePageAccessControlsDomainService;
import io.gravitee.apim.core.documentation.domain_service.ValidatePagesDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.metadata.model.Metadata;
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
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.apim.infra.domain_service.documentation.ValidatePageSourceDomainServiceImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.common.UuidString;
import io.vertx.rxjava3.core.Vertx;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ImportApiCRDUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String API_ID = "my-api";
    private static final String API_CROSS_ID = "my-api-cross-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String ACTOR_USER_ID = "actor-user-id";
    private static final String TAG = "tag1";
    private static final String GROUP_ID_1 = UuidString.generateRandom();
    private static final String GROUP_ID_2 = UuidString.generateRandom();
    private static final String GROUP_NAME = "developers";
    private static final String USER_ENTITY_SOURCE = "gravitee";
    private static final String USER_ENTITY_SOURCE_ID = "jane.doe@gravitee.io";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, ACTOR_USER_ID);

    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory(apiCrudService);
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    SubscriptionQueryServiceInMemory subscriptionQueryService = new SubscriptionQueryServiceInMemory(subscriptionCrudService);
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    NotificationConfigCrudServiceInMemory notificationConfigCrudService = new NotificationConfigCrudServiceInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    ApiMetadataQueryServiceInMemory apiMetadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudService);
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    UserDomainServiceInMemory userDomainService = new UserDomainServiceInMemory();
    WorkflowCrudServiceInMemory workflowCrudService = new WorkflowCrudServiceInMemory();
    CategoryQueryServiceInMemory categoryQueryService = new CategoryQueryServiceInMemory();

    ValidateApiDomainService validateApiDomainService = mock(ValidateApiDomainService.class);
    PlanSynchronizationService planSynchronizationService = mock(PlanSynchronizationService.class);
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory(planCrudService);
    IndexerInMemory indexer = new IndexerInMemory();
    UpdateApiDomainService updateApiDomainService;
    ApiMetadataDomainService apiMetadataDomainService = mock(ApiMetadataDomainService.class);
    ApiCategoryQueryServiceInMemory apiCategoryQueryService = new ApiCategoryQueryServiceInMemory();
    ApiStateDomainService apiStateDomainService = mock(ApiStateDomainService.class);
    VerifyApiPathDomainService verifyApiPathDomainService = mock(VerifyApiPathDomainService.class);
    ValidateResourceDomainServiceInMemory validateResourceDomainService = new ValidateResourceDomainServiceInMemory();
    DocumentationValidationDomainService validationDomainService = mock(DocumentationValidationDomainService.class);
    CRDMembersDomainServiceInMemory crdMembersDomainService = new CRDMembersDomainServiceInMemory();

    ImportApiCRDUseCase useCase;

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
        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
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
            ),
            apiCrudService,
            new IntegrationAgentInMemory()
        );
        var metadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudService);
        var membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
        var apiPrimaryOwnerFactory = new ApiPrimaryOwnerFactory(
            groupQueryService,
            membershipQueryService,
            parametersQueryService,
            roleQueryService,
            userCrudService
        );

        var createApiDomainService = buildCreateApiDomainService(auditDomainService, membershipQueryService, metadataQueryService);

        updateApiDomainService = mock(UpdateApiDomainService.class);
        PageRevisionCrudServiceInMemory pageRevisionCrudService = new PageRevisionCrudServiceInMemory();
        var createApiDocumentationDomainService = new CreateApiDocumentationDomainService(
            pageCrudService,
            pageRevisionCrudService,
            auditDomainService,
            indexer
        );
        var updateApiDocumentationDomainService = new UpdateApiDocumentationDomainService(
            pageCrudService,
            pageRevisionCrudService,
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
            indexer
        );

        var pageSourceValidator = new ValidatePageSourceDomainServiceImpl(new ObjectMapper(), mock(Vertx.class));
        var accessControlValidator = new ValidatePageAccessControlsDomainService(groupQueryService);

        userDomainService.initWith(
            List.of(
                BaseUserEntity
                    .builder()
                    .source(USER_ENTITY_SOURCE)
                    .sourceId(USER_ENTITY_SOURCE_ID)
                    .id(USER_ID)
                    .organizationId(ORGANIZATION_ID)
                    .build(),
                BaseUserEntity
                    .builder()
                    .source(USER_ENTITY_SOURCE)
                    .sourceId(ACTOR_USER_ID)
                    .id(ACTOR_USER_ID)
                    .organizationId(ORGANIZATION_ID)
                    .build()
            )
        );

        var crdValidator = new ValidateApiCRDDomainService(
            new ValidateCategoryIdsDomainService(categoryQueryService),
            verifyApiPathDomainService,
            new ValidateCRDMembersDomainService(userDomainService, roleQueryService),
            new ValidateGroupsDomainService(groupQueryService),
            validateResourceDomainService,
            new ValidatePagesDomainService(pageSourceValidator, accessControlValidator, validationDomainService)
        );

        categoryQueryService.reset();

        useCase =
            new ImportApiCRDUseCase(
                apiCrudService,
                apiQueryService,
                apiPrimaryOwnerFactory,
                validateApiDomainService,
                createApiDomainService,
                createPlanDomainService,
                apiStateDomainService,
                updateApiDomainService,
                planQueryService,
                updatePlanDomainService,
                deletePlanDomainService,
                subscriptionQueryService,
                closeSubscriptionDomainService,
                reorderPlanDomainService,
                crdMembersDomainService,
                apiMetadataDomainService,
                pageQueryService,
                pageCrudService,
                createApiDocumentationDomainService,
                updateApiDocumentationDomainService,
                crdValidator
            );

        enableApiPrimaryOwnerMode();
        parametersQueryService.define(
            new Parameter(Key.PLAN_SECURITY_KEYLESS_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
        );
        parametersQueryService.define(
            new Parameter(Key.PLAN_SECURITY_APIKEY_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
        );
        when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        when(planSynchronizationService.checkSynchronized(any(), any(), any(), any())).thenReturn(true);
        when(validateApiDomainService.validateAndSanitizeForCreation(any(), any(), any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        givenExistingUsers(
            List.of(
                BaseUserEntity
                    .builder()
                    .organizationId(ORGANIZATION_ID)
                    .id(ACTOR_USER_ID)
                    .source(USER_ENTITY_SOURCE)
                    .sourceId(ACTOR_USER_ID)
                    .email("devops@gravitee.io")
                    .build(),
                BaseUserEntity
                    .builder()
                    .organizationId(ORGANIZATION_ID)
                    .id(USER_ID)
                    .source(USER_ENTITY_SOURCE)
                    .sourceId(USER_ENTITY_SOURCE_ID)
                    .firstname("Jane")
                    .lastname("Doe")
                    .email("jane.doe@gravitee.io")
                    .build()
            )
        );

        groupQueryService.initWith(
            List.of(
                Group.builder().id(GROUP_ID_1).build(),
                Group.builder().id(GROUP_ID_2).environmentId(ENVIRONMENT_ID).name(GROUP_NAME).build()
            )
        );

        when(verifyApiPathDomainService.validateAndSanitize(any())).thenAnswer(call -> Validator.Result.ofValue(call.getArgument(0)));
    }

    private CreateApiDomainService buildCreateApiDomainService(
        AuditDomainService auditDomainService,
        MembershipQueryServiceInMemory membershipQueryService,
        ApiMetadataQueryServiceInMemory metadataQueryService
    ) {
        var apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
            auditDomainService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );

        return new CreateApiDomainService(
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
            workflowCrudService
        );
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                apiCrudService,
                applicationCrudService,
                auditCrudService,
                flowCrudService,
                groupQueryService,
                membershipCrudService,
                metadataCrudService,
                notificationConfigCrudService,
                pageCrudService,
                parametersQueryService,
                parametersQueryService,
                planCrudService,
                subscriptionCrudService,
                userCrudService,
                workflowCrudService
            )
            .forEach(InMemoryAlternative::reset);
        reset(policyValidationDomainService, planSynchronizationService);
    }

    @Nested
    class Create {

        @Test
        void should_not_create_api_if_validation_fails() {
            when(validateApiDomainService.validateAndSanitizeForCreation(any(), any(), any(), any()))
                .thenThrow(new ValidationDomainException("Validation error"));

            var throwable = catchThrowable(() -> useCase.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().build())));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(throwable).isInstanceOf(ValidationDomainException.class);
                soft.assertThat(apiCrudService.storage()).isEmpty();
            });
        }

        @Test
        void should_create_and_index_a_new_api() {
            var expected = expectedApi();

            useCase.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().build()));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(apiCrudService.storage()).contains(expected);
                soft
                    .assertThat(indexer.storage())
                    .containsExactly(
                        new IndexableApi(
                            expected,
                            new PrimaryOwnerEntity(ACTOR_USER_ID, "devops@gravitee.io", "devops@gravitee.io", PrimaryOwnerEntity.Type.USER),
                            Map.ofEntries(Map.entry("email-support", "devops@gravitee.io")),
                            Collections.emptySet()
                        )
                    );
            });
        }

        @Test
        void should_create_plans() {
            useCase.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().build()));

            assertThat(planCrudService.storage())
                .hasSize(1)
                .extracting(Plan::getId, Plan::getName, Plan::getPublishedAt)
                .containsExactly(tuple("keyless-id", "Keyless", INSTANT_NOW.atZone(ZoneId.systemDefault())));
            assertThat(flowCrudService.storage()).extracting(Flow::getName).containsExactly("plan-flow");
        }

        @Test
        void should_return_CRD_status() {
            var result = useCase.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().build()));

            assertThat(result.status())
                .isEqualTo(
                    ApiCRDStatus
                        .builder()
                        .id(API_ID)
                        .crossId(API_CROSS_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .organizationId(ORGANIZATION_ID)
                        .state("STARTED")
                        .plans(Map.of("keyless-key", "keyless-id"))
                        .errors(
                            ApiCRDStatus.Errors
                                .builder()
                                .severe(List.of())
                                .warning(List.of("Group [non-existing-group] could not be found in environment [environment-id]"))
                                .build()
                        )
                        .build()
                );
        }

        @Test
        void should_return_CRD_status_with_warnings() {
            var result = useCase.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().categories(Set.of("unknown-category")).build()));

            assertThat(result.status())
                .isEqualTo(
                    ApiCRDStatus
                        .builder()
                        .id(API_ID)
                        .crossId(API_CROSS_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .organizationId(ORGANIZATION_ID)
                        .state("STARTED")
                        .plans(Map.of("keyless-key", "keyless-id"))
                        .errors(
                            ApiCRDStatus.Errors
                                .builder()
                                .severe(List.of())
                                .warning(
                                    List.of(
                                        "Group [non-existing-group] could not be found in environment [environment-id]",
                                        "category [unknown-category] is not defined in environment [environment-id]"
                                    )
                                )
                                .build()
                        )
                        .build()
                );
        }

        @Test
        void should_sanitize_and_create_members() {
            roleQueryService.initWith(
                List.of(
                    Role
                        .builder()
                        .name("USER")
                        .referenceType(Role.ReferenceType.ORGANIZATION)
                        .referenceId(ORGANIZATION_ID)
                        .id("user_role_id")
                        .scope(Role.Scope.API)
                        .build()
                )
            );

            var member = MemberCRD.builder().source(USER_ENTITY_SOURCE).sourceId(USER_ENTITY_SOURCE_ID).role("USER").build();

            useCase.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().members(Set.of(member)).build()));

            assertThat(crdMembersDomainService.getApiMembers(API_ID)).contains(member.toBuilder().id(USER_ID).build());
        }

        @Test
        void should_create_pages() {
            var pages = new HashMap<String, PageCRD>();
            var folder = getMarkdownsFolderPage();
            pages.put("markdowns-folder", folder);

            var markdown = getMarkdownPage(folder);
            pages.put("markdown", markdown);

            when(validationDomainService.validateAndSanitizeForUpdate(any(), anyString(), anyBoolean()))
                .thenAnswer(call -> call.getArgument(0));

            useCase.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().pages(pages).build()));

            assertThat(pageCrudService.storage())
                .hasSize(2)
                .extracting(Page::getId, Page::getCrossId, Page::getName)
                .containsExactly(
                    tuple(markdown.getId(), markdown.getCrossId(), markdown.getName()),
                    tuple(folder.getId(), folder.getCrossId(), folder.getName())
                );
        }

        @Test
        void should_start_the_api() {
            when(updateApiDomainService.update(eq(API_ID), any(ApiCRDSpec.class), eq(AUDIT_INFO))).thenReturn(expectedApi());

            useCase.execute(
                new ImportApiCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD().definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("MANAGEMENT").build()).build()
                )
            );

            verify(apiStateDomainService, times(1)).start(argThat(api -> API_ID.equals(api.getId())), any());
        }

        @Test
        void should_not_start_the_api_with_no_plan() {
            when(updateApiDomainService.update(eq(API_ID), any(ApiCRDSpec.class), eq(AUDIT_INFO))).thenReturn(expectedApi());

            useCase.execute(
                new ImportApiCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD()
                        .plans(Map.of())
                        .definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("MANAGEMENT").build())
                        .build()
                )
            );

            verify(apiStateDomainService, never()).start(argThat(api -> API_ID.equals(api.getId())), any());
        }

        @Test
        void should_not_stop_the_api_on_creation() {
            when(updateApiDomainService.update(eq(API_ID), any(ApiCRDSpec.class), eq(AUDIT_INFO))).thenReturn(expectedApi());

            useCase.execute(
                new ImportApiCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD()
                        .state("STOPPED")
                        .definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("MANAGEMENT").build())
                        .build()
                )
            );

            verify(apiStateDomainService, never()).stop(argThat(api -> API_ID.equals(api.getId())), any());
            verify(apiStateDomainService, never()).start(argThat(api -> API_ID.equals(api.getId())), any());
            verify(apiStateDomainService, never()).deploy(argThat(api -> API_ID.equals(api.getId())), eq("Updated by GKO"), any());
        }

        @Test
        void should_not_stop_the_api_with_no_plan() {
            when(updateApiDomainService.update(eq(API_ID), any(ApiCRDSpec.class), eq(AUDIT_INFO))).thenReturn(expectedApi());

            useCase.execute(
                new ImportApiCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD()
                        .plans(Map.of())
                        .state("STOPPED")
                        .definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("MANAGEMENT").build())
                        .build()
                )
            );

            verify(apiStateDomainService, never()).stop(argThat(api -> API_ID.equals(api.getId())), any());
        }

        @Test
        void should_not_deploy_the_api() {
            when(updateApiDomainService.update(eq(API_ID), any(ApiCRDSpec.class), eq(AUDIT_INFO))).thenReturn(expectedApi());

            useCase.execute(
                new ImportApiCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD().definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("KUBERNETES").build()).build()
                )
            );

            verify(apiStateDomainService, never()).start(argThat(api -> API_ID.equals(api.getId())), eq(AUDIT_INFO));

            verify(apiStateDomainService, never()).stop(argThat(api -> API_ID.equals(api.getId())), eq(AUDIT_INFO));
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

        private static final Plan KEYLESS = aKeylessV4().toBuilder().apiId(API_ID).build().setPlanTags(Set.of(TAG));
        private static final Plan API_KEY = anApiKeyV4().toBuilder().apiId(API_ID).build().setPlanTags(Set.of(TAG));

        @BeforeEach
        void setUp() {
            // TODO fake update API for now until we get rid of Legacy
            when(updateApiDomainService.update(any(), any(), any())).thenAnswer(invocation -> API_PROXY_V4);
        }

        @Test
        void should_return_CRD_status() {
            givenExistingApi();
            givenExistingPlans(List.of(KEYLESS));

            var result = useCase.execute(
                new ImportApiCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD()
                        .plans(
                            Map.of(
                                "keyless-key",
                                PlanCRD
                                    .builder()
                                    .id(KEYLESS.getId())
                                    .name(KEYLESS.getName())
                                    .security(KEYLESS.getPlanSecurity())
                                    .mode(KEYLESS.getPlanMode())
                                    .validation(KEYLESS.getValidation())
                                    .status(KEYLESS.getPlanStatus())
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

            assertThat(result.status())
                .isEqualTo(
                    ApiCRDStatus
                        .builder()
                        .id(API_ID)
                        .crossId(API_CROSS_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .organizationId(ORGANIZATION_ID)
                        .state("STARTED")
                        .plans(Map.of("keyless-key", KEYLESS.getId(), "apikey-key", "generated-id"))
                        .errors(
                            ApiCRDStatus.Errors
                                .builder()
                                .severe(List.of())
                                .warning(List.of("Group [non-existing-group] could not be found in environment [environment-id]"))
                                .build()
                        )
                        .build()
                );
        }

        @Test
        void should_return_CRD_status_with_warnings() {
            givenExistingApi();
            givenExistingPlans(List.of(KEYLESS));

            var result = useCase.execute(
                new ImportApiCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD()
                        .categories(Set.of("unknown-category"))
                        .plans(
                            Map.of(
                                "keyless-key",
                                PlanCRD
                                    .builder()
                                    .id(KEYLESS.getId())
                                    .name(KEYLESS.getName())
                                    .security(KEYLESS.getPlanSecurity())
                                    .mode(KEYLESS.getPlanMode())
                                    .validation(KEYLESS.getValidation())
                                    .status(KEYLESS.getPlanStatus())
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

            assertThat(result.status())
                .isEqualTo(
                    ApiCRDStatus
                        .builder()
                        .id(API_ID)
                        .crossId(API_CROSS_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .organizationId(ORGANIZATION_ID)
                        .state("STARTED")
                        .plans(Map.of("keyless-key", KEYLESS.getId(), "apikey-key", "generated-id"))
                        .errors(
                            ApiCRDStatus.Errors
                                .builder()
                                .severe(List.of())
                                .warning(
                                    List.of(
                                        "Group [non-existing-group] could not be found in environment [environment-id]",
                                        "category [unknown-category] is not defined in environment [environment-id]"
                                    )
                                )
                                .build()
                        )
                        .build()
                );
        }

        @Test
        void should_create_new_plans() {
            givenExistingApi();
            givenExistingPlans(List.of(KEYLESS));

            useCase.execute(
                new ImportApiCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD()
                        .plans(
                            Map.of(
                                "keyless-key",
                                PlanCRD
                                    .builder()
                                    .id(KEYLESS.getId())
                                    .name(KEYLESS.getName())
                                    .security(KEYLESS.getPlanSecurity())
                                    .mode(KEYLESS.getPlanMode())
                                    .validation(KEYLESS.getValidation())
                                    .status(KEYLESS.getPlanStatus())
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
            assertThat(planCrudService.storage())
                .hasSize(2)
                .extracting(Plan::getId, Plan::getName)
                .containsExactly(tuple("keyless", "Keyless"), tuple("generated-id", "API Key"));

            assertThat(flowCrudService.storage()).extracting(Flow::getName).contains("apikey-flow");
        }

        @Test
        void should_update_existing_plans() {
            // Given
            givenExistingApi();
            givenExistingPlans(List.of(KEYLESS));

            // When
            useCase.execute(
                new ImportApiCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD()
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
            assertThat(planCrudService.storage())
                .hasSize(1)
                .extracting(Plan::getId, Plan::getName, Plan::getDescription)
                .containsExactly(tuple("keyless", "Updated Keyless", "Updated description"));
            assertThat(flowCrudService.storage()).extracting(Flow::getName).containsExactly("updated flow");
        }

        @Test
        void should_delete_existing_plans_not_present_in_crd_anymore() {
            givenExistingApi();
            givenExistingPlans(List.of(KEYLESS, API_KEY));

            useCase.execute(
                new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().plans(Map.of("keyless-key", PlanAdapter.INSTANCE.toCRD(KEYLESS))).build())
            );

            assertThat(planCrudService.storage()).hasSize(1).extracting(Plan::getId).containsExactly("keyless");
        }

        @Test
        void should_close_any_active_subscriptions_before_deleting_plans() {
            givenExistingApi();
            givenExistingPlans(List.of(KEYLESS, API_KEY));

            var application = givenExistingApplication(anApplicationEntity());

            givenExistingSubscriptions(
                aSubscription()
                    .toBuilder()
                    .id("sub1")
                    .apiId(API_ID)
                    .applicationId(application.getId())
                    .planId(API_KEY.getId())
                    .status(SubscriptionEntity.Status.ACCEPTED)
                    .build(),
                aSubscription()
                    .toBuilder()
                    .id("sub2")
                    .apiId(API_ID)
                    .applicationId(application.getId())
                    .planId(API_KEY.getId())
                    .status(SubscriptionEntity.Status.PENDING)
                    .build(),
                aSubscription()
                    .toBuilder()
                    .id("sub3")
                    .apiId(API_ID)
                    .applicationId(application.getId())
                    .planId(API_KEY.getId())
                    .status(SubscriptionEntity.Status.PAUSED)
                    .build()
            );

            // When
            useCase.execute(
                new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().plans(Map.of("keyless-key", PlanAdapter.INSTANCE.toCRD(KEYLESS))).build())
            );

            // Then
            assertThat(planCrudService.storage()).hasSize(1).extracting(Plan::getId).containsExactly("keyless");
            assertThat(subscriptionCrudService.storage())
                .extracting(SubscriptionEntity::getId, SubscriptionEntity::getStatus)
                .containsExactly(
                    tuple("sub1", SubscriptionEntity.Status.CLOSED),
                    tuple("sub2", SubscriptionEntity.Status.REJECTED),
                    tuple("sub3", SubscriptionEntity.Status.CLOSED)
                );
        }

        @Test
        void should_refresh_remaining_plan_order_after_deletion() {
            givenExistingApi();
            givenExistingPlans(List.of(KEYLESS.toBuilder().order(2).build(), API_KEY.toBuilder().order(1).build()));

            useCase.execute(
                new ImportApiCRDUseCase.Input(
                    AUDIT_INFO,
                    aCRD().plans(Map.of("keyless-key", PlanAdapter.INSTANCE.toCRD(KEYLESS.toBuilder().order(2).build()))).build()
                )
            );

            assertThat(planCrudService.storage()).hasSize(1).extracting(Plan::getId, Plan::getOrder).containsExactly(tuple("keyless", 1));
        }

        @Test
        void should_update_pages() {
            var pages = new HashMap<String, PageCRD>();
            var folder = getMarkdownsFolderPage();
            pages.put("markdowns-folder", folder);

            var markdown = getMarkdownPage(folder);
            pages.put("markdown", markdown);

            when(validationDomainService.validateAndSanitizeForUpdate(any(), anyString(), anyBoolean()))
                .thenAnswer(call -> call.getArgument(0));

            useCase.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().pages(pages).build()));

            assertThat(pageCrudService.storage())
                .hasSize(2)
                .extracting(Page::getId, Page::getCrossId, Page::getName)
                .containsExactly(
                    tuple(markdown.getId(), markdown.getCrossId(), markdown.getName()),
                    tuple(folder.getId(), folder.getCrossId(), folder.getName())
                );

            folder.setName("new-markdowns-folder");
            pages.put("markdowns-folder", folder);

            markdown.setName("new-markdown");
            pages.put("markdown", markdown);

            useCase.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().pages(pages).build()));

            assertThat(pageCrudService.storage())
                .hasSize(2)
                .extracting(Page::getId, Page::getCrossId, Page::getName)
                .containsExactly(
                    tuple(markdown.getId(), markdown.getCrossId(), markdown.getName()),
                    tuple(folder.getId(), folder.getCrossId(), folder.getName())
                );
        }
    }

    @Test
    void should_save_metadata() {
        var metadata = List.of(
            ApiMetadata.builder().apiId(API_ID).key("metadata-key").value("metadata-value").format(Metadata.MetadataFormat.STRING).build()
        );

        useCase.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().metadata(metadata).build()));

        verify(apiMetadataDomainService, times(1)).importApiMetadata(API_ID, metadata, AUDIT_INFO);
    }

    @Test
    void should_clean_categories_and_keep_existing_categories() {
        categoryQueryService.reset();
        categoryQueryService.initWith(List.of(Category.builder().name("existing").key("existing").id("existing-id").build()));

        var categories = Set.of("existing", "unknown");

        useCase.execute(new ImportApiCRDUseCase.Input(AUDIT_INFO, aCRD().categories(categories).build()));

        var api = apiCrudService.get(API_ID);

        assertThat(api.getCategories()).isNotEmpty();
        assertThat(api.getCategories()).doesNotContain("unknown");
    }

    @Test
    void should_not_deploy_the_api() {
        givenExistingApi();

        when(updateApiDomainService.update(eq(API_ID), any(ApiCRDSpec.class), eq(AUDIT_INFO))).thenReturn(expectedApi());

        useCase.execute(
            new ImportApiCRDUseCase.Input(
                AUDIT_INFO,
                aCRD().definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("KUBERNETES").build()).build()
            )
        );

        verify(apiStateDomainService, never()).deploy(argThat(api -> API_ID.equals(api.getId())), eq("Updated by GKO"), eq(AUDIT_INFO));
        verify(apiStateDomainService, never()).start(argThat(api -> API_ID.equals(api.getId())), eq(AUDIT_INFO));
        verify(apiStateDomainService, never()).stop(argThat(api -> API_ID.equals(api.getId())), eq(AUDIT_INFO));
    }

    @Test
    void should_deploy_the_api() {
        givenExistingApi();

        when(updateApiDomainService.update(eq(API_ID), any(ApiCRDSpec.class), eq(AUDIT_INFO))).thenReturn(expectedApi());

        useCase.execute(
            new ImportApiCRDUseCase.Input(
                AUDIT_INFO,
                aCRD()
                    .state("STARTED")
                    .definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("MANAGEMENT").build())
                    .build()
            )
        );

        verify(apiStateDomainService).deploy(argThat(api -> API_ID.equals(api.getId())), eq("Updated by GKO"), eq(AUDIT_INFO));
        verify(apiStateDomainService, never()).start(argThat(api -> API_ID.equals(api.getId())), eq(AUDIT_INFO));
        verify(apiStateDomainService, never()).stop(argThat(api -> API_ID.equals(api.getId())), eq(AUDIT_INFO));
    }

    @Test
    void should_stop_the_api() {
        givenExistingApi();

        when(updateApiDomainService.update(eq(API_ID), any(ApiCRDSpec.class), eq(AUDIT_INFO))).thenReturn(expectedApi());

        useCase.execute(
            new ImportApiCRDUseCase.Input(
                AUDIT_INFO,
                aCRD()
                    .state("STOPPED")
                    .definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("MANAGEMENT").build())
                    .build()
            )
        );

        verify(apiStateDomainService, never()).deploy(argThat(api -> API_ID.equals(api.getId())), eq("Updated by GKO"), eq(AUDIT_INFO));
        verify(apiStateDomainService).stop(argThat(api -> API_ID.equals(api.getId())), eq(AUDIT_INFO));
    }

    @Test
    void should_start_the_api() {
        apiQueryService.initWith(List.of(Update.API_PROXY_V4.toBuilder().lifecycleState(Api.LifecycleState.STOPPED).build()));

        when(updateApiDomainService.update(eq(API_ID), any(ApiCRDSpec.class), eq(AUDIT_INFO))).thenReturn(expectedApi());

        useCase.execute(
            new ImportApiCRDUseCase.Input(
                AUDIT_INFO,
                aCRD()
                    .state("STARTED")
                    .definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("MANAGEMENT").build())
                    .build()
            )
        );

        var inOrder = inOrder(apiStateDomainService);
        inOrder.verify(apiStateDomainService).deploy(argThat(api -> API_ID.equals(api.getId())), eq("Updated by GKO"), eq(AUDIT_INFO));
        inOrder.verify(apiStateDomainService).start(argThat(api -> API_ID.equals(api.getId())), eq(AUDIT_INFO));
    }

    void givenExistingApi() {
        apiQueryService.initWith(List.of(Update.API_PROXY_V4));
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

    private static ApiCRDSpec.ApiCRDSpecBuilder aCRD() {
        return ApiCRDSpec
            .builder()
            .analytics(Analytics.builder().enabled(false).build())
            .crossId(API_CROSS_ID)
            .definitionContext(DefinitionContext.builder().origin("KUBERNETES").syncFrom("KUBERNETES").build())
            .description("api-description")
            .endpointGroups(
                List.of(
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
                )
            )
            .flows(List.of())
            .id(API_ID)
            .labels(Set.of("label-1"))
            .lifecycleState("CREATED")
            .listeners(
                List.of(
                    HttpListener
                        .builder()
                        .paths(List.of(Path.builder().path("/http_proxy").build()))
                        .entrypoints(List.of(Entrypoint.builder().type("http-proxy").configuration("{}").build()))
                        .build()
                )
            )
            .name("My Api")
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
            .groups(Set.of(GROUP_ID_1, "non-existing-group", GROUP_NAME));
    }

    private Api expectedApi() {
        return aProxyApiV4()
            .toBuilder()
            .originContext(
                new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED, OriginContext.Origin.KUBERNETES.name())
            )
            .id(API_ID)
            .environmentId(ENVIRONMENT_ID)
            .crossId(API_CROSS_ID)
            .visibility(Api.Visibility.PRIVATE)
            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .deployedAt(null)
            .disableMembershipNotifications(true)
            .categories(Set.of())
            .picture(null)
            .background(null)
            .groups(null)
            .apiLifecycleState(Api.ApiLifecycleState.CREATED)
            .apiDefinitionV4(
                ApiDefinitionFixtures
                    .aHttpProxyApiV4(API_ID)
                    .toBuilder()
                    .id(API_ID)
                    .name("My Api")
                    .apiVersion("1.0.0")
                    .properties(List.of(Property.builder().key("prop-key").value("prop-value").build()))
                    .resources(List.of(Resource.builder().name("resource-name").type("resource-type").enabled(true).build()))
                    .responseTemplates(Map.of("DEFAULT", Map.of("*.*", ResponseTemplate.builder().statusCode(200).build())))
                    .tags(Set.of(TAG))
                    .build()
            )
            .groups(Set.of(GROUP_ID_1, GROUP_ID_2))
            .build();
    }

    private void enableApiPrimaryOwnerMode() {
        parametersQueryService.initWith(
            List.of(
                new Parameter(
                    Key.API_PRIMARY_OWNER_MODE.key(),
                    ENVIRONMENT_ID,
                    ParameterReferenceType.ENVIRONMENT,
                    ApiPrimaryOwnerMode.USER.name()
                )
            )
        );
    }

    private void givenExistingUsers(List<BaseUserEntity> users) {
        userCrudService.initWith(users);
        userDomainService.initWith(users);
    }

    private PageCRD getMarkdownPage(PageCRD folder) {
        return PageCRD
            .builder()
            .id(UUID.randomUUID().toString())
            .crossId(UUID.randomUUID().toString())
            .parentId(folder.getId())
            .name("hello-markdown")
            .type(PageCRD.Type.MARKDOWN)
            .parentId("markdowns-folder")
            .content("Hello world!")
            .visibility(PageCRD.Visibility.PUBLIC)
            .build();
    }

    private PageCRD getMarkdownsFolderPage() {
        return PageCRD
            .builder()
            .id(UUID.randomUUID().toString())
            .crossId(UUID.randomUUID().toString())
            .name("markdowns")
            .type(PageCRD.Type.FOLDER)
            .visibility(PageCRD.Visibility.PUBLIC)
            .build();
    }
}
