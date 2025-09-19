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
package io.gravitee.apim.core.application.domain_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.application.model.crd.ApplicationCRDSpec;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.service.common.IdBuilder;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Date;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidateApplicationCRDDomainServiceTest {

    private static final String ORG_ID = "TEST";
    private static final String ENV_ID = "TEST";
    private static final String HRID = "test-hrid";
    private static final String APP_ID = UuidString.generateRandom();
    private static final String APP_NAME = "test_app";
    private static final String APP_DESCRIPTION = "test_app_description";
    private static final String APP_TYPE = "Linux";
    private static final Date NOW = new Date();
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId(ORG_ID).environmentId(ENV_ID).build();

    ValidateGroupsDomainService groupsValidator = mock(ValidateGroupsDomainService.class);
    ValidateCRDMembersDomainService membersValidator = mock(ValidateCRDMembersDomainService.class);
    ValidateApplicationSettingsDomainService settingsValidator = mock(ValidateApplicationSettingsDomainService.class);

    private ValidateApplicationCRDDomainService cut = new ValidateApplicationCRDDomainService(
        groupsValidator,
        membersValidator,
        settingsValidator
    );

    @Test
    void should_give_error_for_application_with_no_id_and_hrid() {
        ApplicationCRDSpec crd = anApplicationCRD();
        crd.setId(null);

        cut
            .validateAndSanitize(new ValidateApplicationCRDDomainService.Input(AUDIT_INFO, crd))
            .peek(
                sanitized -> Assertions.assertThat(sanitized.spec()).isEqualTo(crd.toBuilder().build()),
                errors -> {
                    Assertions.assertThat(errors).isNotEmpty();
                    Assertions.assertThat(errors.getFirst().getMessage()).isEqualTo(
                        "when no hrid is set in the payload an ID should be passed to identify the resource"
                    );
                }
            );
    }

    @Test
    void should_set_id_when_application_hrid_but_no_id() {
        when(groupsValidator.validateAndSanitize(any(ValidateGroupsDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
        when(membersValidator.validateAndSanitize(any(ValidateCRDMembersDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
        when(settingsValidator.validateAndSanitize(any(ValidateApplicationSettingsDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );

        ApplicationCRDSpec crd = anApplicationCRD();
        crd.setHrid(HRID);
        crd.setId(null);

        cut
            .validateAndSanitize(new ValidateApplicationCRDDomainService.Input(AUDIT_INFO, crd))
            .peek(
                sanitized ->
                    Assertions.assertThat(sanitized.spec()).isEqualTo(
                        crd.toBuilder().id(IdBuilder.builder(AUDIT_INFO, HRID).buildId()).build()
                    ),
                errors -> Assertions.assertThat(errors).isEmpty()
            );
    }

    @Test
    void should_set_hrid_when_application_id_is_not_null() {
        when(groupsValidator.validateAndSanitize(any(ValidateGroupsDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
        when(membersValidator.validateAndSanitize(any(ValidateCRDMembersDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
        when(settingsValidator.validateAndSanitize(any(ValidateApplicationSettingsDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );

        ApplicationCRDSpec crd = anApplicationCRD();
        crd.setHrid(null);

        cut
            .validateAndSanitize(new ValidateApplicationCRDDomainService.Input(AUDIT_INFO, crd))
            .peek(
                sanitized -> Assertions.assertThat(sanitized.spec()).isEqualTo(crd.toBuilder().hrid(crd.getId()).build()),
                errors -> Assertions.assertThat(errors).isEmpty()
            );
    }

    private static ApplicationCRDSpec anApplicationCRD() {
        return ApplicationCRDSpec.builder()
            .id(APP_ID)
            .name(APP_NAME)
            .description(APP_DESCRIPTION)
            .type(APP_TYPE)
            .createdAt(NOW)
            .updatedAt(NOW)
            .settings(new ApplicationSettings(new SimpleApplicationSettings(APP_TYPE, "junit"), null))
            .build();
    }
}
