/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service;

import static io.gravitee.repository.management.model.Application.METADATA_REGISTRATION_PAYLOAD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.configuration.application.registration.ClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.application.ClientRegistrationService;
import io.gravitee.rest.api.service.exceptions.ApplicationArchivedException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApplicationRenewClientSecretException;
import io.gravitee.rest.api.service.impl.ApplicationServiceImpl;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.register.ClientRegistrationResponse;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_RenewClientSecretTest {

    private static final String APP = "my-app";
    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String ENVIRONMENT_ID = "DEFAULT";

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ClientRegistrationService clientRegistrationService;

    @Mock
    private RoleService roleService;

    @Test(expected = ApplicationNotFoundException.class)
    public void should_throw_exception_cause_application_not_exists() throws TechnicalException {
        when(applicationRepository.findById(APP)).thenReturn(Optional.empty());

        applicationService.renewClientSecret(GraviteeContext.getExecutionContext(), APP);
    }

    @Test(expected = ApplicationArchivedException.class)
    public void should_throw_exception_cause_application_archived() throws TechnicalException {
        Application app = fakeApp();
        app.setStatus(ApplicationStatus.ARCHIVED);

        when(applicationRepository.findById(APP)).thenReturn(Optional.of(app));

        applicationService.renewClientSecret(GraviteeContext.getExecutionContext(), APP);
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_exception_cause_client_registration_disabled() throws TechnicalException {
        Application app = fakeApp();
        app.setStatus(ApplicationStatus.ACTIVE);

        when(applicationRepository.findById(APP)).thenReturn(Optional.of(app));

        // client registration is disabled
        when(
            parameterService.findAsBoolean(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.APPLICATION_REGISTRATION_ENABLED),
                any(),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        )
            .thenReturn(false);

        applicationService.renewClientSecret(GraviteeContext.getExecutionContext(), APP);
    }

    @Test(expected = ApplicationRenewClientSecretException.class)
    public void should_throw_exception_cause_no_auth_setting() throws TechnicalException {
        Application app = fakeApp();
        app.setStatus(ApplicationStatus.ACTIVE);
        app.setMetadata(Map.of(METADATA_REGISTRATION_PAYLOAD, "{\"my\":\"payload\"}"));

        when(applicationRepository.findById(APP)).thenReturn(Optional.of(app));

        // client registration is enabled
        when(
            parameterService.findAsBoolean(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.APPLICATION_REGISTRATION_ENABLED),
                any(),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        )
            .thenReturn(true);

        applicationService.renewClientSecret(GraviteeContext.getExecutionContext(), APP);
    }

    @Test
    public void should_update_application_with_clientId_renewed_from_clientRegistrationService() throws TechnicalException {
        Application app = fakeApp();
        app.setStatus(ApplicationStatus.ACTIVE);
        app.setType(ApplicationType.BROWSER);
        app.setMetadata(new HashMap<>(Map.of(METADATA_REGISTRATION_PAYLOAD, "{\"my\":\"payload\"}")));
        when(applicationRepository.findById(APP)).thenReturn(Optional.of(app));
        when(applicationRepository.update(any())).thenReturn(app);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = new MembershipEntity();
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        // client registration is enabled
        when(
            parameterService.findAsBoolean(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.APPLICATION_REGISTRATION_ENABLED),
                any(),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        )
            .thenReturn(true);
        ClientRegistrationProviderEntity clientRegistrationProvider = new ClientRegistrationProviderEntity();
        clientRegistrationProvider.setRenewClientSecretSupport(true);
        when(clientRegistrationService.findAll(GraviteeContext.getExecutionContext())).thenReturn(Set.of(clientRegistrationProvider));

        // mock response from DCR with a new client ID
        ClientRegistrationResponse clientRegistrationResponse = new ClientRegistrationResponse();
        clientRegistrationResponse.setClientId("client-id-from-clientRegistration");
        when(clientRegistrationService.renewClientSecret(any(), any())).thenReturn(clientRegistrationResponse);

        applicationService.renewClientSecret(GraviteeContext.getExecutionContext(), APP);

        // check DCR service has been called
        verify(clientRegistrationService).renewClientSecret(GraviteeContext.getExecutionContext(), "{\"my\":\"payload\"}");

        // check application has been updated with new client ID
        verify(applicationRepository)
            .update(argThat(application -> application.getMetadata().get("client_id").equals("client-id-from-clientRegistration")));
    }

    private Application fakeApp() {
        Application app = new Application();
        app.setId(APP);
        app.setEnvironmentId(ENVIRONMENT_ID);
        return app;
    }
}
