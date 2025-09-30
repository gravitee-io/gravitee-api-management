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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Application.AuditEvent.APPLICATION_UPDATED;
import static io.gravitee.rest.api.model.ApiKeyMode.EXCLUSIVE;
import static io.gravitee.rest.api.model.ApiKeyMode.SHARED;
import static io.gravitee.rest.api.model.ApiKeyMode.UNSPECIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationArchivedException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_UpdateApiKeyModeTest {

    private static final String APPLICATION_ID = "id-app";
    private static final String APPLICATION_NAME = "myApplication";
    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserService userService;

    @Mock
    private Application existingApplication;

    @Mock
    private AuditService auditService;

    @Mock
    private ParameterService parameterService;

    @Test
    public void shouldUpdate() throws TechnicalException {
        // 'Shared API KEY' setting is enabled, allows to update to SHARED mode
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_SHARED_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(true);

        Application applicationToUpdate = new Application();
        applicationToUpdate.setId(APPLICATION_ID);
        applicationToUpdate.setName(APPLICATION_NAME);
        applicationToUpdate.setStatus(ApplicationStatus.ACTIVE);
        applicationToUpdate.setType(ApplicationType.SIMPLE);
        applicationToUpdate.setApiKeyMode(ApiKeyMode.UNSPECIFIED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(applicationToUpdate));

        when(applicationRepository.update(any())).thenReturn(applicationToUpdate);

        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        final ApplicationEntity applicationEntity = applicationService.updateApiKeyMode(
            GraviteeContext.getExecutionContext(),
            APPLICATION_ID,
            SHARED
        );

        verify(applicationRepository).update(argThat(application -> application.getApiKeyMode() == ApiKeyMode.SHARED));
        verify(auditService).createApplicationAuditLog(
            any(),
            argThat(auditLogData -> auditLogData.getEvent().equals(APPLICATION_UPDATED)),
            eq(APPLICATION_ID)
        );

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test(expected = ApplicationNotFoundException.class)
    public void shouldNotUpdateBecauseNotFound() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        applicationService.updateApiKeyMode(GraviteeContext.getExecutionContext(), APPLICATION_ID, EXCLUSIVE);
    }

    @Test(expected = ApplicationArchivedException.class)
    public void shouldNotUpdateBecauseApplicationArchived() throws TechnicalException {
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ARCHIVED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));

        applicationService.updateApiKeyMode(GraviteeContext.getExecutionContext(), APPLICATION_ID, SHARED);
    }

    @Test(expected = InvalidApplicationApiKeyModeException.class)
    public void should_throw_exception_trying_to_update_apiKeyMode_shared() throws TechnicalException {
        // existing application has a SHARED api key mode
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.SHARED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));

        // this should throw exception cause API key mode update is forbidden
        applicationService.updateApiKeyMode(GraviteeContext.getExecutionContext(), APPLICATION_ID, UNSPECIFIED);
    }

    @Test(expected = InvalidApplicationApiKeyModeException.class)
    public void should_throw_exception_trying_to_set_apiKeyMode_shared_with_env_setting_disabled() throws TechnicalException {
        // 'Shared API KEY' setting is disabled
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_SHARED_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(false);

        // existing application has a UNSPECIFIED api key mode
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));

        // this should throw exception cause shard API key setting is disabled
        applicationService.updateApiKeyMode(GraviteeContext.getExecutionContext(), APPLICATION_ID, SHARED);
    }
}
