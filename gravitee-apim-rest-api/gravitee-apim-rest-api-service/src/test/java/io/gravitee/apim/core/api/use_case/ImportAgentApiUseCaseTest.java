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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiHostValidatorDomainServiceGoogleImpl;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.KafkaPortRangeCrudServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.NotificationConfigCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import inmemory.WorkflowCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.ApiPathIndex;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateAgentApiDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.model.NewAgentApi;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.VerifyPlanPortRangesDomainService;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.agent.StandaloneAgentDefinition;
import io.gravitee.definition.model.v4.agent.definition.AgentModel;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportAgentApiUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    NotificationConfigCrudServiceInMemory notificationConfigCrudService = new NotificationConfigCrudServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    ApiMetadataQueryServiceInMemory apiMetadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudService);
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    WorkflowCrudServiceInMemory workflowCrudService = new WorkflowCrudServiceInMemory();
    IndexerInMemory indexer = new IndexerInMemory();
    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);

    ImportAgentApiUseCase useCase;

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
        var membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var apiPrimaryOwnerFactory = new ApiPrimaryOwnerFactory(
            groupQueryService,
            membershipQueryService,
            parametersQueryService,
            roleQueryService,
            userCrudService
        );
        var apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
            auditService,
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );
        var createApiDomainService = new CreateApiDomainService(
            apiCrudService,
            auditService,
            new ApiIndexerDomainService(
                new ApiMetadataDecoderDomainService(apiMetadataQueryService, new FreemarkerTemplateProcessor()),
                apiPrimaryOwnerDomainService,
                new ApiCategoryQueryServiceInMemory(),
                indexer
            ),
            new ApiMetadataDomainService(metadataCrudService, apiMetadataQueryService, auditService),
            apiPrimaryOwnerDomainService,
            flowCrudService,
            notificationConfigCrudService,
            parametersQueryService,
            workflowCrudService,
            new inmemory.CreateCategoryApiDomainServiceInMemory()
        );
        var kafkaPortRanges = new KafkaPortRangeCrudServiceInMemory();
        var createPlanDomainService = new CreatePlanDomainService(
            new PlanValidatorDomainService(parametersQueryService, policyValidationDomainService, pageCrudService),
            new FlowValidationDomainService(policyValidationDomainService, new EntrypointPluginQueryServiceInMemory()),
            planCrudService,
            flowCrudService,
            auditService,
            new VerifyPlanPortRangesDomainService(kafkaPortRanges),
            kafkaPortRanges
        );
        var validateAgentApiDomainService = new ValidateAgentApiDomainService(
            new VerifyApiPathDomainService(
                new ApiQueryServiceInMemory(apiCrudService),
                new InstallationAccessQueryServiceInMemory(),
                new ApiHostValidatorDomainServiceGoogleImpl(),
                new ApiPathIndex()
            )
        );
        useCase = new ImportAgentApiUseCase(
            apiPrimaryOwnerFactory,
            createApiDomainService,
            createPlanDomainService,
            validateAgentApiDomainService
        );

        parametersQueryService.initWith(
            List.of(
                new Parameter(
                    Key.API_PRIMARY_OWNER_MODE.key(),
                    ENVIRONMENT_ID,
                    ParameterReferenceType.ENVIRONMENT,
                    ApiPrimaryOwnerMode.USER.name()
                ),
                new Parameter(Key.PLAN_SECURITY_KEYLESS_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
            )
        );
        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        userCrudService.initWith(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );
        when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any())).thenAnswer(invocation ->
            invocation.getArgument(1)
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(
            apiCrudService,
            auditCrudService,
            flowCrudService,
            groupQueryService,
            membershipCrudService,
            notificationConfigCrudService,
            parametersQueryService,
            metadataCrudService,
            pageCrudService,
            planCrudService,
            userCrudService,
            workflowCrudService
        ).forEach(InMemoryAlternative::reset);
        indexer.reset();
    }

    @Test
    void should_import_agent_and_recreate_its_plans() {
        // When
        var created = useCase.execute(new ImportAgentApiUseCase.Input(anAgent(), Set.of(aKeylessPlan()), AUDIT_INFO)).apiWithFlows();

        // Then: the agent is created and its plan re-created
        assertThat(created.getType()).isEqualTo(ApiType.AGENT);
        assertThat(apiCrudService.storage()).hasSize(1);
        assertThat(planCrudService.storage()).hasSize(1);
        assertThat(planCrudService.storage().get(0).getName()).isEqualTo("Keyless");
        // Import is a copy: the plan gets a fresh id, not the exported one ("my-plan")
        assertThat(planCrudService.storage().get(0).getId()).isEqualTo("generated-id");
    }

    @Test
    void should_import_agent_without_plans() {
        // When
        var created = useCase.execute(new ImportAgentApiUseCase.Input(anAgent(), Set.of(), AUDIT_INFO)).apiWithFlows();

        // Then
        assertThat(created.getType()).isEqualTo(ApiType.AGENT);
        assertThat(apiCrudService.storage()).hasSize(1);
        assertThat(planCrudService.storage()).isEmpty();
    }

    private static NewAgentApi anAgent() {
        return NewAgentApi.builder()
            .name("Imported Agent")
            .apiVersion("1.0.0")
            .type(ApiType.AGENT)
            .kind("standalone")
            .listeners(List.of(HttpListener.builder().paths(List.of()).build()))
            .standalone(StandaloneAgentDefinition.builder().model(AgentModel.builder().type("openai").build()).output("answer").build())
            .build();
    }

    private static PlanWithFlows aKeylessPlan() {
        var plan = fixtures.core.model.PlanFixtures.HttpV4.aKeyless()
            .toBuilder()
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .build()
            .setPlanStatus(PlanStatus.PUBLISHED)
            .setPlanTags(Set.of());
        return new PlanWithFlows(plan, List.of());
    }
}
