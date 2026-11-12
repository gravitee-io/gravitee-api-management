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
package io.gravitee.apim.core.plan.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import fixtures.core.model.ApiCRDFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.PlanCrudServiceInMemory;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidatePlanDomainServiceTest {

    private static final AuditInfo AUDIT_INFO = new AuditInfo("org", "env", null);

    private final PlanValidatorDomainService planValidator = mock(PlanValidatorDomainService.class);
    private final VerifyPlanPortRangesDomainService portRangesValidator = mock(VerifyPlanPortRangesDomainService.class);

    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();

    private final ValidatePlanDomainService service = new ValidatePlanDomainService(planValidator, portRangesValidator, planCrudService);

    @Test
    void should_refuse_a_plan_id_of_another_api() {
        givenExistingPlan(GenericPlanEntity.ReferenceType.API, "another-api-id");

        var result = validateBaseSpecPlans();

        assertThat(errorMessages(result)).containsExactly("invalid plan [plan-name]. Error: plan id [plan-id] does not belong to this API");
        assertThat(result.value()).hasValueSatisfying(sanitized -> assertThat(sanitized.plans()).isEmpty());
    }

    @Test
    void should_refuse_a_plan_id_of_an_api_product() {
        givenExistingPlan(GenericPlanEntity.ReferenceType.API_PRODUCT, ApiCRDFixtures.API_ID);

        assertThat(errorMessages(validateBaseSpecPlans())).containsExactly(
            "invalid plan [plan-name]. Error: plan id [plan-id] does not belong to this API"
        );
    }

    @Test
    void should_accept_a_plan_id_of_the_imported_api() {
        givenExistingPlan(GenericPlanEntity.ReferenceType.API, ApiCRDFixtures.API_ID);

        var result = validateBaseSpecPlans();

        assertThat(errorMessages(result)).isEmpty();
        assertThat(result.value()).hasValueSatisfying(sanitized -> assertThat(sanitized.plans()).containsKey(ApiCRDFixtures.PLAN_NAME));
    }

    @Test
    void should_accept_a_plan_id_no_plan_has_yet() {
        var result = validateBaseSpecPlans();

        assertThat(errorMessages(result)).isEmpty();
        assertThat(result.value()).hasValueSatisfying(sanitized -> assertThat(sanitized.plans()).containsKey(ApiCRDFixtures.PLAN_NAME));
    }

    @Test
    void should_accept_a_plan_without_id() {
        ApiCRDSpec spec = ApiCRDFixtures.newBaseSpec().type("PROXY").build();
        var planWithoutId = spec.getPlans().get(ApiCRDFixtures.PLAN_NAME).toBuilder().id(null).build();

        var result = validatePlans(spec, Map.of(ApiCRDFixtures.PLAN_NAME, planWithoutId));

        assertThat(errorMessages(result)).isEmpty();
        assertThat(result.value()).hasValueSatisfying(sanitized -> assertThat(sanitized.plans()).containsKey(ApiCRDFixtures.PLAN_NAME));
    }

    @Test
    void should_reject_native_keyless_plan_coexisting_with_active_auth_plan() {
        ApiCRDSpec spec = ApiCRDFixtures.newBaseNativeSpec().build();
        Map<String, PlanCRD> plans = Map.of(
            "keyless",
            nativePlan("keyless", "key-less", PlanStatus.PUBLISHED),
            "apikey",
            nativePlan("apikey", "api-key", PlanStatus.PUBLISHED)
        );

        var result = validatePlans(spec, plans);

        assertThat(hasMutualExclusivityError(result)).isTrue();
    }

    @Test
    void should_reject_native_keyless_plan_coexisting_with_deprecated_auth_plan() {
        ApiCRDSpec spec = ApiCRDFixtures.newBaseNativeSpec().build();
        Map<String, PlanCRD> plans = Map.of(
            "keyless",
            nativePlan("keyless", "key-less", PlanStatus.PUBLISHED),
            "apikey",
            nativePlan("apikey", "api-key", PlanStatus.DEPRECATED)
        );

        var result = validatePlans(spec, plans);

        assertThat(hasMutualExclusivityError(result)).isTrue();
    }

    @Test
    void should_accept_native_keyless_plan_when_the_auth_plan_is_closed() {
        ApiCRDSpec spec = ApiCRDFixtures.newBaseNativeSpec().build();
        Map<String, PlanCRD> plans = Map.of(
            "keyless",
            nativePlan("keyless", "key-less", PlanStatus.PUBLISHED),
            "apikey",
            nativePlan("apikey", "api-key", PlanStatus.CLOSED)
        );

        var result = validatePlans(spec, plans);

        assertThat(hasMutualExclusivityError(result)).isFalse();
    }

    @Test
    void should_accept_native_multiple_authentication_plans() {
        ApiCRDSpec spec = ApiCRDFixtures.newBaseNativeSpec().build();
        Map<String, PlanCRD> plans = Map.of(
            "apikey",
            nativePlan("apikey", "api-key", PlanStatus.PUBLISHED),
            "jwt",
            nativePlan("jwt", "jwt", PlanStatus.PUBLISHED)
        );

        var result = validatePlans(spec, plans);

        assertThat(hasMutualExclusivityError(result)).isFalse();
    }

    private void givenExistingPlan(GenericPlanEntity.ReferenceType referenceType, String referenceId) {
        planCrudService.initWith(
            List.of(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .id(ApiCRDFixtures.PLAN_ID)
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .build()
            )
        );
    }

    private Validator.Result<ValidatePlanDomainService.Input> validateBaseSpecPlans() {
        ApiCRDSpec spec = ApiCRDFixtures.newBaseSpec().type("PROXY").build();
        return validatePlans(spec, spec.getPlans());
    }

    private Validator.Result<ValidatePlanDomainService.Input> validatePlans(ApiCRDSpec spec, Map<String, PlanCRD> plans) {
        return service.validateAndSanitize(new ValidatePlanDomainService.Input(AUDIT_INFO, spec, spec.getId(), plans, List.of()));
    }

    private static List<String> errorMessages(Validator.Result<ValidatePlanDomainService.Input> result) {
        return result.errors().orElseGet(List::of).stream().map(Validator.Error::getMessage).toList();
    }

    private static PlanCRD nativePlan(String id, String securityType, PlanStatus status) {
        return PlanCRD.builder()
            .id(id)
            .name(id)
            .security(new PlanSecurity(securityType, "{}"))
            .mode(PlanMode.STANDARD)
            .status(status)
            .build();
    }

    private static boolean hasMutualExclusivityError(Validator.Result<ValidatePlanDomainService.Input> result) {
        return result
            .errors()
            .orElseGet(List::of)
            .stream()
            .map(Validator.Error::getMessage)
            .anyMatch(message -> message.contains("mutually exclusive"));
    }
}
