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
package io.gravitee.apim.core.shared_policy_group.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.SharedPolicyGroupFixtures;
import inmemory.SharedPolicyGroupCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plugin.model.FlowPhase;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRD;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
class ValidateSharedPolicyGroupCRDDomainServiceTest {

    static final String ORG_ID = "TEST";
    static final String ENV_ID = "TEST";
    static final String USER_SOURCE = "MEMORY";
    static final String ACTOR_USER_ID = "ACTOR";

    static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .actor(AuditActor.builder().userSource(USER_SOURCE).userSourceId(ACTOR_USER_ID).userId(ACTOR_USER_ID).build())
        .environmentId(ENV_ID)
        .organizationId(ORG_ID)
        .build();

    final PolicyValidationDomainService policyGroupValidationService = mock(PolicyValidationDomainService.class);

    final SharedPolicyGroupCrudServiceInMemory sharedPolicyGroupCrudService = new SharedPolicyGroupCrudServiceInMemory(List.of());
    final ValidateCreateSharedPolicyGroupDomainService validateCreateSharedPolicyGroupDomainService =
        new ValidateCreateSharedPolicyGroupDomainService(sharedPolicyGroupCrudService, policyGroupValidationService);
    final ValidateUpdateSharedPolicyGroupDomainService validateUpdateSharedPolicyGroupDomainService =
        new ValidateUpdateSharedPolicyGroupDomainService(sharedPolicyGroupCrudService, policyGroupValidationService);

    final ValidateSharedPolicyGroupCRDDomainService cut = new ValidateSharedPolicyGroupCRDDomainService(
        sharedPolicyGroupCrudService,
        validateCreateSharedPolicyGroupDomainService,
        validateUpdateSharedPolicyGroupDomainService
    );

    private static SharedPolicyGroupCRD aCRDWithSteps() {
        return SharedPolicyGroupFixtures.aSharedPolicyGroupCRD()
            .toBuilder()
            .steps(List.of(Step.builder().policy("test-policy").name("Test step").enabled(true).configuration("{}").build()))
            .build();
    }

    @Nested
    class Create {

        @Test
        void should_generate_ids() {
            SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();

            var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

            result.peek(
                sanitized -> {
                    assertThat(sanitized.crd()).isNotSameAs(aCRD);
                    assertThat(sanitized.crd().getSharedPolicyGroupId()).isNotBlank();
                    assertThat(sanitized.crd().getCrossId()).isNotBlank();
                    assertThat(sanitized.crd().getHrid()).isEqualTo(aCRD.getHrid());
                },
                errors -> assertThat(errors).isNotNull()
            );
        }

        @Test
        void should_keep_existing_ids() {
            SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD()
                .toBuilder()
                .crossId("existing-cross-id")
                .sharedPolicyGroupId("existing-id")
                .build();

            var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

            result.peek(
                sanitized -> {
                    assertThat(sanitized.crd()).isNotSameAs(aCRD);
                    assertThat(sanitized.crd().getSharedPolicyGroupId()).isEqualTo("existing-id");
                    assertThat(sanitized.crd().getCrossId()).isEqualTo("existing-cross-id");
                    assertThat(sanitized.crd().getHrid()).isEqualTo(aCRD.getHrid());
                },
                errors -> assertThat(errors).isNotNull()
            );
        }

        @Test
        void should_return_no_errors() {
            SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();

            var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

            result.peek(sanitized -> {}, errors -> assertThat(errors).isEmpty());
        }

        @Test
        void should_return_error_when_name_is_missing() {
            SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD().toBuilder().name(null).build();

            var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

            result.peek(
                sanitized -> {},
                errors -> assertThat(errors).hasSize(1).extracting(Validator.Error::getMessage).contains("Name is required.")
            );
        }

        @Test
        void should_return_error_when_api_type_is_missing() {
            SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD().toBuilder().apiType(null).build();

            var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

            result.peek(
                sanitized -> {},
                errors -> assertThat(errors).hasSize(1).extracting(Validator.Error::getMessage).contains("ApiType is required.")
            );
        }

        @Test
        void should_return_error_when_phase_is_missing() {
            SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD().toBuilder().phase(null).build();

            var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

            result.peek(
                sanitized -> {},
                errors -> assertThat(errors).hasSize(1).extracting(Validator.Error::getMessage).contains("Phase is required.")
            );
        }

        @Test
        void should_return_error_when_phase_is_invalid_for_api_type() {
            SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD()
                .toBuilder()
                .apiType(ApiType.PROXY)
                .phase(FlowPhase.SUBSCRIBE)
                .build();

            var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

            result.peek(sanitized -> {}, errors -> assertThat(errors).hasSize(1));
        }

        @Test
        void should_return_error_when_policy_validation_fails() {
            when(policyGroupValidationService.validateAndSanitizeConfiguration(anyString(), anyString())).thenThrow(
                new RuntimeException("Invalid policy configuration")
            );
            SharedPolicyGroupCRD aCRD = aCRDWithSteps();

            var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

            result.peek(
                sanitized -> {},
                errors -> assertThat(errors).hasSize(1).extracting(Validator.Error::getMessage).contains("Invalid policy configuration")
            );
        }
    }

    @Nested
    class Update {

        @Test
        void should_return_no_errors() {
            SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD().toBuilder().crossId("spg-cross-id").build();
            sharedPolicyGroupCrudService.initWith(List.of(aCRD.toSharedPolicyGroup()));

            var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

            result.peek(sanitized -> {}, errors -> assertThat(errors).isEmpty());
        }

        @Test
        void should_return_error_when_name_is_missing() {
            SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD()
                .toBuilder()
                .crossId("spg-cross-id")
                .name(null)
                .build();
            sharedPolicyGroupCrudService.initWith(List.of(aCRD.toSharedPolicyGroup()));

            var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

            result.peek(
                sanitized -> {},
                errors -> assertThat(errors).hasSize(1).extracting(Validator.Error::getMessage).contains("Name is required.")
            );
        }

        @Test
        void should_set_spg_id_from_existing() {
            SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD().toBuilder().crossId("spg-cross-id").build();
            var existingSpg = aCRD.toSharedPolicyGroup().toBuilder().id("existing-spg-id").environmentId(ENV_ID).build();
            sharedPolicyGroupCrudService.initWith(List.of(existingSpg));

            var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

            result.peek(sanitized -> {}, errors -> assertThat(errors).isEmpty());
            assertThat(aCRD.getSharedPolicyGroupId()).isEqualTo("existing-spg-id");
        }

        @Test
        void should_return_error_when_policy_validation_fails() {
            when(policyGroupValidationService.validateAndSanitizeConfiguration(anyString(), anyString())).thenThrow(
                new RuntimeException("Invalid policy configuration")
            );
            SharedPolicyGroupCRD aCRD = aCRDWithSteps().toBuilder().crossId("spg-cross-id").build();
            sharedPolicyGroupCrudService.initWith(List.of(aCRD.toSharedPolicyGroup()));

            var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

            result.peek(
                sanitized -> {},
                errors -> assertThat(errors).hasSize(1).extracting(Validator.Error::getMessage).contains("Invalid policy configuration")
            );
        }
    }
}
