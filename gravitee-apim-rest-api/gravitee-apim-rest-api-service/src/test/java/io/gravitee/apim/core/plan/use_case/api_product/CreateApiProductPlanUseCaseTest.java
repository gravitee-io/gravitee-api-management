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
package io.gravitee.apim.core.plan.use_case.api_product;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.exception.UnauthorizedPlanSecurityTypeException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.use_case.api_product.CreateApiProductPlanUseCase.Input;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateApiProductPlanUseCaseTest {

    private ApiProduct API_PRODUCT = BASE().build();
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String GENERATED_ID = "generated-id";

    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    ApiProductCrudServiceInMemory apiProductCrudServiceInMemory = new ApiProductCrudServiceInMemory();
    AuditDomainService auditDomainService = new AuditDomainService(
        auditCrudService,
        new UserCrudServiceInMemory(),
        new JacksonJsonDiffProcessor()
    );
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    PlanValidatorDomainService planValidatorService = new PlanValidatorDomainService(
        parametersQueryService,
        policyValidationDomainService,
        pageCrudService
    );
    FlowValidationDomainService flowValidationDomainService = new FlowValidationDomainService(
        policyValidationDomainService,
        new EntrypointPluginQueryServiceInMemory()
    );
    CreatePlanDomainService createPlanDomainService = new CreatePlanDomainService(
        planValidatorService,
        flowValidationDomainService,
        planCrudService,
        flowCrudService,
        auditDomainService
    );

    CreateApiProductPlanUseCase createPlanUseCase = new CreateApiProductPlanUseCase(createPlanDomainService, apiProductCrudServiceInMemory);

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> GENERATED_ID);
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @BeforeEach
    void setUp() {
        parametersQueryService.initWith(
            List.of(
                new Parameter(Key.PLAN_SECURITY_KEYLESS_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true"),
                new Parameter(Key.PLAN_SECURITY_MTLS_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
            )
        );
    }

    @AfterEach
    void tearDown() {
        apiProductCrudServiceInMemory.reset();
        parametersQueryService.reset();
    }

    @Test
    void should_create_plan_with_default_values() {
        // Given
        var api = givenExistingApiProduct(API_PRODUCT);

        // When
        var result = createPlanUseCase.execute(
            new Input(
                api.getId(),
                _api ->
                    PlanFixtures.HttpV4.aKeyless()
                        .toBuilder()
                        .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                        .referenceId(api.getId())
                        .apiType(null)
                        .build(),
                AUDIT_INFO
            )
        );
        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("keyless");
        assertThat(result.plan())
            .extracting(
                createdPlan -> createdPlan.getPlanMode().name(),
                createdPlan -> createdPlan.getReferenceId(),
                createdPlan -> createdPlan.getReferenceType(),
                Plan::getEnvironmentId,
                Plan::getReferenceId,
                Plan::getPlanStatus,
                Plan::getDefinitionVersion
            )
            .containsExactly(
                PlanMode.STANDARD.name(),
                api.getId(),
                GenericPlanEntity.ReferenceType.API_PRODUCT,
                api.getEnvironmentId(),
                api.getId(),
                PlanStatus.STAGING,
                DefinitionVersion.V4
            );
    }

    @Test
    void should_not_allow_to_create_secured_plan() {
        // Given
        var api = givenExistingApiProduct(API_PRODUCT);
        var input = new Input(api.getId(), _api -> PlanFixtures.HttpV4.anApiKey().toBuilder().id(null).build(), AUDIT_INFO);

        // When
        var throwable = Assertions.catchThrowable(() -> createPlanUseCase.execute(input));

        // Then
        Assertions.assertThat(throwable).isInstanceOf(UnauthorizedPlanSecurityTypeException.class);
    }

    @Test
    void should_create_mtls_plan_for_http_api() {
        // Given
        var api = givenExistingApiProduct(API_PRODUCT);
        var mtlsPlan = PlanFixtures.HttpV4.anMtlsPlan().toBuilder().id(null).build();
        mtlsPlan.setReferenceType(GenericPlanEntity.ReferenceType.API_PRODUCT);
        mtlsPlan.setReferenceId(api.getId());
        var input = new Input(api.getId(), _api -> mtlsPlan, AUDIT_INFO);

        // When
        var result = createPlanUseCase.execute(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(GENERATED_ID);
        assertThat(result.plan())
            .extracting(
                Plan::getReferenceId,
                createdPlan -> createdPlan.getPlanSecurity().getType(),
                Plan::getReferenceType,
                Plan::getPlanStatus,
                Plan::getDefinitionVersion
            )
            .containsExactly(api.getId(), "mtls", GenericPlanEntity.ReferenceType.API_PRODUCT, PlanStatus.STAGING, DefinitionVersion.V4);
    }

    @Test
    void should_create_push_plan_with_null_security_type() {
        // Given
        var api = givenExistingApiProduct(API_PRODUCT);
        var pushPlan = PlanFixtures.HttpV4.aPushPlan().toBuilder().id(null).build();
        pushPlan.setReferenceType(GenericPlanEntity.ReferenceType.API_PRODUCT);
        pushPlan.setReferenceId(api.getId());
        var input = new Input(api.getId(), _api -> pushPlan, AUDIT_INFO);

        // When
        var result = createPlanUseCase.execute(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(GENERATED_ID);
        assertThat(result.plan())
            .extracting(
                Plan::getReferenceId,
                Plan::getPlanSecurity,
                Plan::getReferenceType,
                Plan::getPlanStatus,
                Plan::getDefinitionVersion
            )
            .containsExactly(api.getId(), null, GenericPlanEntity.ReferenceType.API_PRODUCT, PlanStatus.STAGING, DefinitionVersion.V4);
    }

    private ApiProduct givenExistingApiProduct(ApiProduct api) {
        apiProductCrudServiceInMemory.initWith(List.of(api));
        return api;
    }

    private ApiProduct.ApiProductBuilder BASE() {
        return ApiProduct.builder()
            .id("my-api-product")
            .name("my-api-product")
            .environmentId("environment-id")
            .description("api-product-description")
            .version("1.0.0")
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()));
    }
}
