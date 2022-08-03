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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.ApiKeyMode.SHARED;
import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_ID;
import static io.gravitee.repository.management.model.Application.METADATA_REGISTRATION_PAYLOAD;
import static io.gravitee.rest.api.model.ApiKeyMode.UNSPECIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.configuration.application.ApplicationGrantTypeEntity;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypeEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.configuration.application.ClientRegistrationService;
import io.gravitee.rest.api.service.converter.ApplicationConverter;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.ClientIdAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.register.ClientRegistrationResponse;
import java.util.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_UpdateTest {

    private static final String APPLICATION_ID = "id-app";
    private static final String APPLICATION_NAME = "myApplication";
    private static final String USER_NAME = "myUser";
    private static final String CLIENT_ID = "myClientId";

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
    private UpdateApplicationEntity updateApplication;

    @Mock
    private Application existingApplication;

    @Mock
    private AuditService auditService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ClientRegistrationService clientRegistrationService;

    @Mock
    private ApplicationConverter applicationConverter;

    @Mock
    private ApplicationTypeService applicationTypeService;

    @Test
    public void shouldUpdate() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);

        // 'Shared API KEY' setting is enabled, allows to update to SHARED mode
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_SHARED_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(true);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);

        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(updateApplication.getApiKeyMode()).thenReturn(io.gravitee.rest.api.model.ApiKeyMode.SHARED);

        when(applicationRepository.update(any())).thenReturn(existingApplication);

        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        final ApplicationEntity applicationEntity = applicationService.update(
            GraviteeContext.getExecutionContext(),
            APPLICATION_ID,
            updateApplication
        );

        verify(applicationRepository)
            .update(argThat(application -> APPLICATION_NAME.equals(application.getName()) && application.getUpdatedAt() != null));

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test(expected = ApplicationNotFoundException.class)
    public void shouldNotUpdateBecauseNotFound() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        when(updateApplication.getSettings()).thenReturn(settings);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(applicationRepository.update(any())).thenThrow(TechnicalException.class);
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
    }

    @Test
    public void shouldUpdateBecauseSameApplication() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getId()).thenReturn(APPLICATION_ID);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(METADATA_CLIENT_ID, CLIENT_ID);
        when(existingApplication.getMetadata()).thenReturn(metadata);

        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        when(updateApplication.getSettings()).thenReturn(settings);

        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(applicationRepository.update(any())).thenReturn(existingApplication);
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        final ApplicationEntity applicationEntity = applicationService.update(
            GraviteeContext.getExecutionContext(),
            APPLICATION_ID,
            updateApplication
        );

        verify(applicationRepository)
            .update(argThat(application -> APPLICATION_NAME.equals(application.getName()) && application.getUpdatedAt() != null));

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test(expected = ClientIdAlreadyExistsException.class)
    public void shouldNotUpdateBecauseDifferentApplication() throws TechnicalException {
        Application other = mock(Application.class);
        when(other.getId()).thenReturn("other-app");

        Map<String, String> metadata = new HashMap<>();
        metadata.put(METADATA_CLIENT_ID, CLIENT_ID);
        when(other.getMetadata()).thenReturn(metadata);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(applicationRepository.findAllByEnvironment("DEFAULT", ApplicationStatus.ACTIVE)).thenReturn(Sets.newSet(other));

        when(existingApplication.getId()).thenReturn(APPLICATION_ID);

        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);

        when(updateApplication.getSettings()).thenReturn(settings);

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_exception_cause_client_registration_is_disabled() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.BROWSER);

        // oauth app settings
        ApplicationSettings settings = new ApplicationSettings();
        settings.setoAuthClient(new OAuthClientSettings());
        when(updateApplication.getSettings()).thenReturn(settings);

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

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
    }

    @Test(expected = InvalidApplicationApiKeyModeException.class)
    public void should_throw_exception_trying_to_update_apiKeyMode_shared() throws TechnicalException {
        // existing application has a SHARED api key mode
        when(existingApplication.getApiKeyMode()).thenReturn(SHARED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));

        // updated application has a UNSPECIFIED api key mode
        when(updateApplication.getApiKeyMode()).thenReturn(UNSPECIFIED);
        when(updateApplication.getSettings()).thenReturn(new ApplicationSettings());
        updateApplication.getSettings().setApp(new SimpleApplicationSettings());

        // this should throw exception cause API key mode update is forbidden
        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
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
        )
            .thenReturn(false);

        // existing application has a UNSPECIFIED api key mode
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));

        // updated application has a SHARED api key mode
        when(updateApplication.getApiKeyMode()).thenReturn(io.gravitee.rest.api.model.ApiKeyMode.SHARED);
        when(updateApplication.getSettings()).thenReturn(new ApplicationSettings());
        updateApplication.getSettings().setApp(new SimpleApplicationSettings());

        // this should throw exception cause shard API key setting is disabled
        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
    }

    @Test(expected = ApplicationGrantTypesNotFoundException.class)
    public void should_throw_exception_cause_oAuth_client_settings_has_no_grant_types() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.BROWSER);

        // oauth app settings doesn't contain grant types
        ApplicationSettings settings = new ApplicationSettings();
        settings.setoAuthClient(new OAuthClientSettings());
        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getSettings()).thenReturn(settings);

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

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
    }

    @Test
    public void should_update_with_oauth2_clientId_from_client_registration_service() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(existingApplication.getType()).thenReturn(ApplicationType.BROWSER);
        when(existingApplication.getMetadata()).thenReturn(Map.of(METADATA_CLIENT_ID, "my-previous-client-id"));
        when(existingApplication.getMetadata()).thenReturn(Map.of(METADATA_REGISTRATION_PAYLOAD, "{}"));
        when(applicationRepository.update(any())).thenReturn(existingApplication);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
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

        // oauth app settings contains everything required
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings oAuthClientSettings = new OAuthClientSettings();
        oAuthClientSettings.setGrantTypes(List.of("application-grant-type"));
        oAuthClientSettings.setApplicationType("application-type");
        settings.setoAuthClient(oAuthClientSettings);
        when(updateApplication.getSettings()).thenReturn(settings);

        ApplicationTypeEntity applicationTypeEntity = new ApplicationTypeEntity();
        ApplicationGrantTypeEntity applicationGrantTypeEntity = new ApplicationGrantTypeEntity();
        applicationGrantTypeEntity.setType("application-grant-type");
        applicationGrantTypeEntity.setResponse_types(List.of("response-type"));
        applicationTypeEntity.setAllowed_grant_types(List.of(applicationGrantTypeEntity));
        applicationTypeEntity.setRequires_redirect_uris(false);
        when(applicationTypeService.getApplicationType("application-type")).thenReturn(applicationTypeEntity);

        // mock response from DCR with a new client ID
        ClientRegistrationResponse clientRegistrationResponse = new ClientRegistrationResponse();
        clientRegistrationResponse.setClientId("client-id-from-clientRegistration");
        when(clientRegistrationService.update(any(), any(), same(updateApplication))).thenReturn(clientRegistrationResponse);
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        // ensure application has been updated with the new client_id from DCR
        verify(applicationRepository)
            .update(argThat(application -> application.getMetadata().get(METADATA_CLIENT_ID).equals("client-id-from-clientRegistration")));
    }

    @Test
    public void should_update_with_previous_oAuth2_clientId_when_registration_service_fails() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(existingApplication.getType()).thenReturn(ApplicationType.BROWSER);
        when(existingApplication.getMetadata())
            .thenReturn(Map.of(METADATA_REGISTRATION_PAYLOAD, "{}", METADATA_CLIENT_ID, "my-previous-client-id"));
        when(applicationRepository.update(any())).thenReturn(existingApplication);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
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

        // oauth app settings contains everything required
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings oAuthClientSettings = new OAuthClientSettings();
        oAuthClientSettings.setGrantTypes(List.of("application-grant-type"));
        oAuthClientSettings.setApplicationType("application-type");
        settings.setoAuthClient(oAuthClientSettings);
        when(updateApplication.getSettings()).thenReturn(settings);

        ApplicationTypeEntity applicationTypeEntity = new ApplicationTypeEntity();
        ApplicationGrantTypeEntity applicationGrantTypeEntity = new ApplicationGrantTypeEntity();
        applicationGrantTypeEntity.setType("application-grant-type");
        applicationGrantTypeEntity.setResponse_types(List.of("response-type"));
        applicationTypeEntity.setAllowed_grant_types(List.of(applicationGrantTypeEntity));
        applicationTypeEntity.setRequires_redirect_uris(false);
        when(applicationTypeService.getApplicationType("application-type")).thenReturn(applicationTypeEntity);

        // DCR throws exception
        when(clientRegistrationService.update(any(), any(), same(updateApplication))).thenThrow(RuntimeException.class);
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        // ensure application has been updated, but kept the previous client_id
        verify(applicationRepository)
            .update(argThat(application -> application.getMetadata().get(METADATA_CLIENT_ID).equals("my-previous-client-id")));
    }

    @Test
    public void should_update_client_id_of_subscriptions() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(updateApplication.getSettings()).thenReturn(settings);
        when(applicationRepository.update(any())).thenReturn(existingApplication);

        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        List<SubscriptionEntity> subscriptions = List.of(mock(SubscriptionEntity.class), mock(SubscriptionEntity.class));
        when(subscriptionService.search(any(), argThat(criteria -> criteria.getApplications().contains(APPLICATION_ID))))
            .thenReturn(subscriptions);

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        verify(subscriptionService, times(2)).update(any(), any(UpdateSubscriptionEntity.class), eq(CLIENT_ID));
    }
}
