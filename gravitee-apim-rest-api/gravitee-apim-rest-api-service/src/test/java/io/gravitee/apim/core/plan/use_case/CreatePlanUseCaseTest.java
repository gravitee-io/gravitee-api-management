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
package io.gravitee.apim.core.plan.use_case;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.definition.FlowFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.exception.NativeApiWithMultipleFlowsException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.exception.PlanInvalidException;
import io.gravitee.apim.core.plan.exception.UnauthorizedPlanSecurityTypeException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.apim.core.plan.use_case.CreatePlanUseCase.Input;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreatePlanUseCaseTest {

    private static final Api API = ApiFixtures.aProxyApiV4();
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String GENERATED_ID = "generated-id";

    private static final Function<Api, List<? extends AbstractFlow>> EMPTY_FLOW_PROVIDER = _api -> Collections.emptyList();

    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
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

    CreatePlanUseCase createPlanUseCase = new CreatePlanUseCase(createPlanDomainService, apiCrudService);

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
        apiCrudService.reset();
        parametersQueryService.reset();
    }

    @Test
    void should_create_plan_with_default_values() {
        // Given
        var api = givenExistingApi(API);

        // When
        var result = createPlanUseCase.execute(
            new Input(api.getId(), _api -> PlanFixtures.HttpV4.aKeyless().toBuilder().id(null).build(), EMPTY_FLOW_PROVIDER, AUDIT_INFO)
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(GENERATED_ID);
        assertThat(result.plan())
            .extracting(
                PlanWithFlows::getApiId,
                createdPlan -> createdPlan.getPlanType().name(),
                createdPlan -> createdPlan.getPlanMode().name()
            )
            .containsExactly(api.getId(), Plan.PlanType.API.name(), PlanMode.STANDARD.name());
    }

    @Test
    void should_not_allow_to_create_secured_plan() {
        // Given
        var api = givenExistingApi(API);
        var input = new Input(
            api.getId(),
            _api -> PlanFixtures.HttpV4.anApiKey().toBuilder().id(null).build(),
            EMPTY_FLOW_PROVIDER,
            AUDIT_INFO
        );

        // When
        var throwable = Assertions.catchThrowable(() -> createPlanUseCase.execute(input));

        // Then
        Assertions.assertThat(throwable).isInstanceOf(UnauthorizedPlanSecurityTypeException.class);
    }

    @Test
    void should_throw_when_create_a_federated_plan() {
        // Given
        var api = givenExistingApi(ApiFixtures.aFederatedApi());

        // When
        var throwable = Assertions.catchThrowable(() ->
            createPlanUseCase.execute(
                new Input(api.getId(), _api -> PlanFixtures.HttpV4.anApiKey().toBuilder().id(null).build(), EMPTY_FLOW_PROVIDER, AUDIT_INFO)
            )
        );

        // Then
        Assertions.assertThat(throwable).isInstanceOf(PlanInvalidException.class).hasMessage("Can't manually create Federated Plan");
    }

    @Test
    void should_throw_when_creating_mtls_plan_for_tcp_api() {
        // Given
        var api = givenExistingApi(ApiFixtures.aTcpApiV4());
        var mtlsPlan = PlanFixtures.HttpV4.anMtlsPlan().toBuilder().id(null).build();
        var input = new Input(api.getId(), _api -> mtlsPlan, EMPTY_FLOW_PROVIDER, AUDIT_INFO);

        // When
        var throwable = Assertions.catchThrowable(() -> createPlanUseCase.execute(input));

        // Then
        Assertions.assertThat(throwable).isInstanceOf(PlanInvalidException.class).hasMessage("Cannot create mTLS plan for TCP API");
    }

    @Test
    void should_create_mtls_plan_for_http_api() {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4());
        var mtlsPlan = PlanFixtures.HttpV4.anMtlsPlan().toBuilder().id(null).build();
        var input = new Input(api.getId(), _api -> mtlsPlan, EMPTY_FLOW_PROVIDER, AUDIT_INFO);

        // When
        var result = createPlanUseCase.execute(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(GENERATED_ID);
        assertThat(result.plan())
            .extracting(PlanWithFlows::getApiId, createdPlan -> createdPlan.getPlanSecurity().getType())
            .containsExactly(api.getId(), "mtls");
    }

    @Test
    void should_create_push_plan_with_null_security_type() {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4());
        var pushPlan = PlanFixtures.HttpV4.aPushPlan().toBuilder().id(null).build();
        var input = new Input(api.getId(), _api -> pushPlan, EMPTY_FLOW_PROVIDER, AUDIT_INFO);

        // When
        var result = createPlanUseCase.execute(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(GENERATED_ID);
        assertThat(result.plan()).extracting(PlanWithFlows::getApiId, Plan::getPlanSecurity).containsExactly(api.getId(), null);
    }

    @Test
    void should_create_native_plan() {
        // Given
        var api = givenExistingApi(ApiFixtures.aNativeApi());
        var input = new Input(
            api.getId(),
            _api -> PlanFixtures.NativeV4.aKeyless().toBuilder().id(null).build(),
            EMPTY_FLOW_PROVIDER,
            AUDIT_INFO
        );

        // When
        var result = createPlanUseCase.execute(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(GENERATED_ID);
        assertThat(result.plan())
            .extracting(
                PlanWithFlows::getApiId,
                createdPlan -> createdPlan.getPlanType().name(),
                creadedPlan -> creadedPlan.getPlanMode().name()
            )
            .containsExactly(api.getId(), Plan.PlanType.API.name(), PlanMode.STANDARD.name());
    }

    @Test
    void should_create_native_plan_with_flow() {
        // Given
        var api = givenExistingApi(ApiFixtures.aNativeApi());
        List<NativeFlow> flows = List.of(FlowFixtures.aNativeFlowV4());
        var input = new Input(
            api.getId(),
            _api -> PlanFixtures.NativeV4.aKeyless().toBuilder().id(null).build(),
            _api -> flows,
            AUDIT_INFO
        );

        // When
        var result = createPlanUseCase.execute(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(GENERATED_ID);
        assertThat(result.plan())
            .extracting(
                PlanWithFlows::getApiId,
                createdPlan -> createdPlan.getPlanType().name(),
                creadedPlan -> creadedPlan.getPlanMode().name()
            )
            .containsExactly(api.getId(), Plan.PlanType.API.name(), PlanMode.STANDARD.name());
    }

    @Test
    void should_throw_when_native_plan_has_more_then_one_flow() {
        // Given
        var api = givenExistingApi(ApiFixtures.aNativeApi());
        List<NativeFlow> flows = List.of(FlowFixtures.aNativeFlowV4(), FlowFixtures.aNativeFlowV4());
        var input = new Input(
            api.getId(),
            _api -> PlanFixtures.NativeV4.aKeyless().toBuilder().id(null).build(),
            _api -> flows,
            AUDIT_INFO
        );

        // When
        var throwable = Assertions.catchThrowable(() -> createPlanUseCase.execute(input));

        // Then
        Assertions.assertThat(throwable)
            .isInstanceOf(NativeApiWithMultipleFlowsException.class)
            .hasMessage("Native APIs cannot have more than one flow");
    }

    @Test
    void should_throw_when_creating_mtls_plan_for_native_api() {
        // Given
        var api = givenExistingApi(ApiFixtures.aNativeApi());
        var mtlsPlan = PlanFixtures.HttpV4.anMtlsPlan().toBuilder().id(null).build();
        var input = new Input(api.getId(), _api -> mtlsPlan, EMPTY_FLOW_PROVIDER, AUDIT_INFO);

        // When
        var throwable = Assertions.catchThrowable(() -> createPlanUseCase.execute(input));

        // Then
        Assertions.assertThat(throwable).isInstanceOf(UnauthorizedPlanSecurityTypeException.class);
    }

    @Test
    void should_throw_when_creating_push_plan_for_native_api() {
        // Given
        var api = givenExistingApi(ApiFixtures.aNativeApi());
        var pushPlan = PlanFixtures.HttpV4.aPushPlan().toBuilder().id(null).build();
        var input = new Input(api.getId(), _api -> pushPlan, EMPTY_FLOW_PROVIDER, AUDIT_INFO);

        // When
        var throwable = Assertions.catchThrowable(() -> createPlanUseCase.execute(input));

        // Then
        Assertions.assertThat(throwable)
            .isInstanceOf(PlanInvalidException.class)
            .hasMessage("Plan mode 'Push' is forbidden for Native APIs");
    }

    private Api givenExistingApi(Api api) {
        apiCrudService.initWith(List.of(api));
        return api;
    }
}
