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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import inmemory.ApplicationCrudServiceInMemory;
import inmemory.CRDMembersDomainServiceInMemory;
import io.gravitee.apim.core.application.model.crd.ApplicationCRDSpec;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import java.util.Date;
import java.util.List;
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

    ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    CRDMembersDomainServiceInMemory membersDomainService = new CRDMembersDomainServiceInMemory();

    private ValidateApplicationCRDDomainService cut = new ValidateApplicationCRDDomainService(
        groupsValidator,
        membersValidator,
        settingsValidator,
        applicationCrudService,
        membersDomainService
    );

    @Test
    void should_reject_a_member_change_on_the_existing_application_without_the_member_permission() {
        var existing = new BaseApplicationEntity();
        existing.setId(APP_ID);
        existing.setEnvironmentId(ENV_ID);
        applicationCrudService.initWith(List.of(existing));
        membersDomainService.denyApplicationMembersUpdate(APP_ID);
        givenEveryValidatorAccepts();

        assertThatExceptionOfType(ForbiddenAccessException.class).isThrownBy(() ->
            cut.validateAndSanitize(new ValidateApplicationCRDDomainService.Input(AUDIT_INFO, anApplicationCRD()))
        );
    }

    @Test
    void should_not_check_the_members_of_an_application_to_create() {
        membersDomainService.denyApplicationMembersUpdate(APP_ID);
        givenEveryValidatorAccepts();

        assertThatNoException().isThrownBy(() ->
            cut.validateAndSanitize(new ValidateApplicationCRDDomainService.Input(AUDIT_INFO, anApplicationCRD()))
        );
    }

    private void givenEveryValidatorAccepts() {
        when(groupsValidator.validateAndSanitize(any(ValidateGroupsDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
        when(membersValidator.validateAndSanitize(any(ValidateCRDMembersDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
        when(settingsValidator.validateAndSanitize(any(ValidateApplicationSettingsDomainService.Input.class))).thenAnswer(call ->
            Validator.Result.ofValue(call.getArgument(0))
        );
    }

    @Test
    void should_preserve_pre_set_id() {
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

        cut
            .validateAndSanitize(new ValidateApplicationCRDDomainService.Input(AUDIT_INFO, crd))
            .peek(
                sanitized -> Assertions.assertThat(sanitized.spec().getId()).isEqualTo(APP_ID),
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

    @Test
    void should_not_validate_groups_that_are_ignored() {
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
        crd.setGroups(java.util.Set.of("spec-group"));
        crd.setIgnoreGroups(true);

        cut.validateAndSanitize(new ValidateApplicationCRDDomainService.Input(AUDIT_INFO, crd));

        var groupsInput = org.mockito.ArgumentCaptor.forClass(ValidateGroupsDomainService.Input.class);
        org.mockito.Mockito.verify(groupsValidator).validateAndSanitize(groupsInput.capture());
        Assertions.assertThat(groupsInput.getValue().groups()).isNull();
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
