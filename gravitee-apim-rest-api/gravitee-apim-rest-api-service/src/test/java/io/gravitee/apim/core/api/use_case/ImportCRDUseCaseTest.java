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

import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.definition.FlowFixtures;
import inmemory.ApiQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.DeployApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRD;
import io.gravitee.apim.core.api.model.crd.ApiCRDStatus;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.datetime.TimeProvider;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plugin.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ImportCRDUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String API_ID = "my-api";
    private static final String API_CROSS_ID = "my-api-cross-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();

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

        var createApiDomainService = mock(CreateApiDomainService.class);
        var apiMetadataDomainService = mock(ApiMetadataDomainService.class);
        var deployApiDomainService = mock(DeployApiDomainService.class);

        useCase =
            new ImportCRDUseCase(
                apiQueryService,
                createApiDomainService,
                createPlanDomainService,
                apiMetadataDomainService,
                deployApiDomainService
            );

        parametersQueryService.initWith(
            List.of(
                new Parameter(Key.PLAN_SECURITY_KEYLESS_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true"),
                new Parameter(Key.PLAN_SECURITY_APIKEY_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
            )
        );
        when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));

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
            .of(auditCrudService, flowCrudService, pageCrudService, planCrudService, parametersQueryService)
            .forEach(InMemoryAlternative::reset);
        reset(policyValidationDomainService);
    }

    @Nested
    class Create {

        // TODO test API creation when migrated currently we rely on legacy services

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
            .flows(List.of())
            .build();
    }
}
