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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.SharedPolicyGroupFixtures;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupCrudService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRD;
import io.gravitee.apim.core.validation.Validator;
import java.util.Optional;
import java.util.UUID;
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

    static final AuditInfo AUDIT_INFO = AuditInfo
        .builder()
        .actor(AuditActor.builder().userSource(USER_SOURCE).userSourceId(ACTOR_USER_ID).userId(ACTOR_USER_ID).build())
        .environmentId(ENV_ID)
        .organizationId(ORG_ID)
        .build();

    SharedPolicyGroupCrudService sharedPolicyGroupCrudService = mock(SharedPolicyGroupCrudService.class);
    ValidateCreateSharedPolicyGroupDomainService validateCreateSharedPolicyGroupDomainService = mock(
        ValidateCreateSharedPolicyGroupDomainService.class
    );
    ValidateUpdateSharedPolicyGroupDomainService validateUpdateSharedPolicyGroupDomainService = mock(
        ValidateUpdateSharedPolicyGroupDomainService.class
    );

    ValidateSharedPolicyGroupCRDDomainService cut = new ValidateSharedPolicyGroupCRDDomainService(
        sharedPolicyGroupCrudService,
        validateCreateSharedPolicyGroupDomainService,
        validateUpdateSharedPolicyGroupDomainService
    );

    @Test
    void should_return_generate_id_on_creation() {
        SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();
        aCRD.setCrossId(null);
        aCRD.setSharedPolicyGroupId(null);
        aCRD.setHrid("test-hrid");

        when(sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(any(), any())).thenReturn(Optional.empty());
        when(
            validateCreateSharedPolicyGroupDomainService.validateAndSanitize(
                new ValidateCreateSharedPolicyGroupDomainService.Input(AUDIT_INFO, any())
            )
        )
            .thenAnswer(a -> Validator.Result.ofValue(a.getArgument(0)));

        var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

        result.peek(
            sanitized -> {
                assertThat(sanitized.crd().getSharedPolicyGroupId()).isNotBlank();
                assertThat(sanitized.crd().getCrossId()).isNotBlank();
                assertThat(sanitized.crd().getHrid()).isEqualTo("test-hrid");
            },
            errors -> {
                assertThat(errors).isNotNull();
            }
        );
    }

    @Test
    void should_return_creation_hrid_set_on_create_when_ids_exist() {
        SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();
        String id = UUID.randomUUID().toString();
        aCRD.setSharedPolicyGroupId(id);
        aCRD.setHrid(null);

        when(sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(any(), any())).thenReturn(Optional.empty());
        when(
            validateCreateSharedPolicyGroupDomainService.validateAndSanitize(
                new ValidateCreateSharedPolicyGroupDomainService.Input(AUDIT_INFO, any())
            )
        )
            .thenAnswer(a -> Validator.Result.ofValue(a.getArgument(0)));

        var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

        result.peek(
            sanitized -> {
                assertThat(sanitized.crd().getSharedPolicyGroupId()).isEqualTo(id);
                assertThat(sanitized.crd().getCrossId()).isEqualTo(aCRD.getCrossId());
                assertThat(sanitized.crd().getHrid()).isEqualTo(aCRD.getCrossId());
            },
            errors -> {
                assertThat(errors).isNotNull();
            }
        );
    }

    @Test
    void should_return_no_warning_or_errors_on_creation() {
        SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();

        when(sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(any(), any())).thenReturn(Optional.empty());
        when(
            validateCreateSharedPolicyGroupDomainService.validateAndSanitize(
                new ValidateCreateSharedPolicyGroupDomainService.Input(AUDIT_INFO, any())
            )
        )
            .thenReturn(Validator.Result.ofValue(null));

        var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

        result.peek(sanitized -> {}, errors -> assertThat(errors).isEmpty());
        verify(validateCreateSharedPolicyGroupDomainService, times(1)).validateAndSanitize(any());
        verify(validateUpdateSharedPolicyGroupDomainService, times(0)).validateAndSanitize(any());
    }

    @Test
    void should_return_errors_on_creation() {
        SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();

        when(sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(any(), any())).thenReturn(Optional.empty());
        when(
            validateCreateSharedPolicyGroupDomainService.validateAndSanitize(
                new ValidateCreateSharedPolicyGroupDomainService.Input(AUDIT_INFO, any())
            )
        )
            .thenReturn(Validator.Result.withError(Validator.Error.severe("validation failed")));

        var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

        result.peek(sanitized -> {}, errors -> assertThat(errors).hasSize(1));
        verify(validateCreateSharedPolicyGroupDomainService, times(1)).validateAndSanitize(any());
        verify(validateUpdateSharedPolicyGroupDomainService, times(0)).validateAndSanitize(any());
    }

    @Test
    void should_return_no_warning_or_errors_on_update() {
        SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();

        var sanitizedSPG = aCRD.toSharedPolicyGroup().toBuilder().hrid(aCRD.getCrossId()).build();

        when(sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(ENV_ID, aCRD.getCrossId()))
            .thenReturn(Optional.of(aCRD.toSharedPolicyGroup()));
        when(
            validateUpdateSharedPolicyGroupDomainService.validateAndSanitize(
                new ValidateUpdateSharedPolicyGroupDomainService.Input(AUDIT_INFO, sanitizedSPG)
            )
        )
            .thenReturn(Validator.Result.ofValue(null));

        var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

        result.peek(sanitized -> {}, errors -> assertThat(errors).isEmpty());
        verify(validateCreateSharedPolicyGroupDomainService, times(0)).validateAndSanitize(any());
        verify(validateUpdateSharedPolicyGroupDomainService, times(1)).validateAndSanitize(any());
    }

    @Test
    void should_return_errors_on_update() {
        SharedPolicyGroupCRD aCRD = SharedPolicyGroupFixtures.aSharedPolicyGroupCRD();
        when(sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(ENV_ID, aCRD.getCrossId()))
            .thenReturn(Optional.of(aCRD.toSharedPolicyGroup()));
        when(sharedPolicyGroupCrudService.findByEnvironmentIdAndCrossId(ENV_ID, aCRD.getCrossId()))
            .thenReturn(Optional.of(aCRD.toSharedPolicyGroup()));

        var sanitizedSPG = aCRD.toSharedPolicyGroup().toBuilder().hrid(aCRD.getCrossId()).build();

        when(
            validateUpdateSharedPolicyGroupDomainService.validateAndSanitize(
                new ValidateUpdateSharedPolicyGroupDomainService.Input(AUDIT_INFO, sanitizedSPG)
            )
        )
            .thenReturn(Validator.Result.withError(Validator.Error.severe("validation failed")));

        var result = cut.validateAndSanitize(new ValidateSharedPolicyGroupCRDDomainService.Input(AUDIT_INFO, aCRD));

        result.peek(sanitized -> {}, errors -> assertThat(errors).hasSize(1));
        verify(validateCreateSharedPolicyGroupDomainService, times(0)).validateAndSanitize(any());
        verify(validateUpdateSharedPolicyGroupDomainService, times(1)).validateAndSanitize(any());
    }
}
