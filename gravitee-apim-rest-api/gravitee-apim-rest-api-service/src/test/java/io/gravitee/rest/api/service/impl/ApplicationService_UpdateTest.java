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

import static io.gravitee.repository.management.model.ApiKeyMode.SHARED;
import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_CERTIFICATE;
import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_ID;
import static io.gravitee.repository.management.model.Application.METADATA_REGISTRATION_PAYLOAD;
import static io.gravitee.rest.api.model.ApiKeyMode.UNSPECIFIED;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.application.TlsSettings;
import io.gravitee.rest.api.model.configuration.application.ApplicationGrantTypeEntity;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypeEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
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
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.util.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import joptsimple.internal.Strings;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private PlanSearchService planSearchService;

    @Test
    public void shouldUpdate() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        settings.setTls(TlsSettings.builder().clientCertificate(VALID_PEM_1).build());

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
        lenient().when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        lenient()
            .when(existingApplication.getMetadata())
            .thenReturn(Map.of(METADATA_CLIENT_CERTIFICATE, Base64.getEncoder().encodeToString(VALID_PEM_1.getBytes())));

        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(updateApplication.getApiKeyMode()).thenReturn(io.gravitee.rest.api.model.ApiKeyMode.SHARED);

        final Application updatedApplication = mock(Application.class);
        when(updatedApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updatedApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(updatedApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(updatedApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(updatedApplication.getMetadata())
            .thenReturn(Map.of(METADATA_CLIENT_CERTIFICATE, Base64.getEncoder().encodeToString(VALID_PEM_2.getBytes())));

        when(applicationRepository.update(any())).thenReturn(updatedApplication);

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
        Assertions.assertThat(applicationEntity.getSettings().getTls().getClientCertificate()).isEqualTo(VALID_PEM_2);
    }

    @Test
    public void shouldUpdateForV4Api() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(Strings.EMPTY);
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

        String apiKeyPlanId = "apiKeyPlan";
        String pushPlanId = "pushPLan";
        SubscriptionEntity apiKeySubscription = new SubscriptionEntity();
        apiKeySubscription.setPlan(apiKeyPlanId);
        apiKeySubscription.setStatus(SubscriptionStatus.ACCEPTED);
        SubscriptionEntity pushSubscription = new SubscriptionEntity();
        pushSubscription.setPlan(pushPlanId);
        pushSubscription.setStatus(SubscriptionStatus.ACCEPTED);
        when(subscriptionService.findByApplicationAndPlan(GraviteeContext.getExecutionContext(), APPLICATION_ID, null))
            .thenReturn(Arrays.asList(apiKeySubscription, pushSubscription));

        io.gravitee.rest.api.model.v4.plan.PlanEntity apiKeyPlanEntity = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        apiKeyPlanEntity.setId(apiKeyPlanId);
        apiKeyPlanEntity.setSecurity(new PlanSecurity(PlanSecurityType.API_KEY.name(), ""));
        apiKeyPlanEntity.setMode(PlanMode.STANDARD);
        io.gravitee.rest.api.model.v4.plan.PlanEntity pushPlanEntity = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        pushPlanEntity.setId(pushPlanId);
        pushPlanEntity.setSecurity(null);
        pushPlanEntity.setMode(PlanMode.PUSH);
        when(planSearchService.findByIdIn(GraviteeContext.getExecutionContext(), Set.of(apiKeyPlanId, pushPlanId)))
            .thenReturn(Set.of(apiKeyPlanEntity, pushPlanEntity));

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

        verify(subscriptionService).findByApplicationAndPlan(any(ExecutionContext.class), eq(APPLICATION_ID), isNull());
        verify(planSearchService).findByIdIn(any(ExecutionContext.class), eq(Set.of(apiKeyPlanId, pushPlanId)));
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
        settings.setOauth(new OAuthClientSettings());
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
        // existing application has a SHARED API Key mode
        when(existingApplication.getApiKeyMode()).thenReturn(SHARED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));

        // updated application has a UNSPECIFIED API Key mode
        when(updateApplication.getApiKeyMode()).thenReturn(UNSPECIFIED);
        when(updateApplication.getSettings()).thenReturn(new ApplicationSettings());
        updateApplication.getSettings().setApp(new SimpleApplicationSettings());

        // this should throw exception cause API Key mode update is forbidden
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

        // existing application has a UNSPECIFIED API Key mode
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));

        // updated application has a SHARED API Key mode
        when(updateApplication.getApiKeyMode()).thenReturn(io.gravitee.rest.api.model.ApiKeyMode.SHARED);
        when(updateApplication.getSettings()).thenReturn(new ApplicationSettings());
        updateApplication.getSettings().setApp(new SimpleApplicationSettings());

        // this should throw exception cause shard API Key setting is disabled
        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
    }

    @Test(expected = ApplicationGrantTypesNotFoundException.class)
    public void should_throw_exception_cause_oAuth_client_settings_has_no_grant_types() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.BROWSER);

        // oauth app settings doesn't contain grant types
        ApplicationSettings settings = new ApplicationSettings();
        settings.setOauth(new OAuthClientSettings());
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
        oAuthClientSettings.setApplicationType(ApplicationType.BROWSER.name());
        settings.setOauth(oAuthClientSettings);
        when(updateApplication.getSettings()).thenReturn(settings);

        ApplicationTypeEntity applicationTypeEntity = new ApplicationTypeEntity();
        ApplicationGrantTypeEntity applicationGrantTypeEntity = new ApplicationGrantTypeEntity();
        applicationGrantTypeEntity.setType("application-grant-type");
        applicationGrantTypeEntity.setResponse_types(List.of("response-type"));
        applicationTypeEntity.setAllowed_grant_types(List.of(applicationGrantTypeEntity));
        applicationTypeEntity.setRequires_redirect_uris(false);
        when(applicationTypeService.getApplicationType(ApplicationType.BROWSER.name())).thenReturn(applicationTypeEntity);

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
        oAuthClientSettings.setApplicationType(ApplicationType.BROWSER.name());
        settings.setOauth(oAuthClientSettings);
        when(updateApplication.getSettings()).thenReturn(settings);

        ApplicationTypeEntity applicationTypeEntity = new ApplicationTypeEntity();
        ApplicationGrantTypeEntity applicationGrantTypeEntity = new ApplicationGrantTypeEntity();
        applicationGrantTypeEntity.setType("application-grant-type");
        applicationGrantTypeEntity.setResponse_types(List.of("response-type"));
        applicationTypeEntity.setAllowed_grant_types(List.of(applicationGrantTypeEntity));
        applicationTypeEntity.setRequires_redirect_uris(false);
        when(applicationTypeService.getApplicationType(ApplicationType.BROWSER.name())).thenReturn(applicationTypeEntity);

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

        SubscriptionEntity subscription1 = new SubscriptionEntity();
        subscription1.setId("sub-1");
        subscription1.setClientId("old id");

        SubscriptionEntity subscription2 = new SubscriptionEntity();
        subscription2.setId("sub-2");
        subscription2.setClientId("old id");

        List<SubscriptionEntity> subscriptions = List.of(subscription1, subscription2);
        when(
            subscriptionService.search(
                any(),
                argThat(criteria ->
                    criteria.getApplications().contains(APPLICATION_ID) &&
                    criteria
                        .getStatuses()
                        .containsAll(Set.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED, SubscriptionStatus.PENDING))
                )
            )
        )
            .thenReturn(subscriptions);

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        ArgumentCaptor<Consumer<Subscription>> subscriptionModifierCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(subscriptionService, times(2)).update(any(), any(UpdateSubscriptionEntity.class), subscriptionModifierCaptor.capture());

        // Verify the function called to modify the subscription has been modifying its client id
        Subscription fakeSubscription = new Subscription();
        subscriptionModifierCaptor.getValue().accept(fakeSubscription);
        Assertions.assertThat(fakeSubscription.getClientId()).isEqualTo(CLIENT_ID);
    }

    @Test
    public void should_throw_exception_on_update_client_certificate_of_subscriptions() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        settings.setApp(new SimpleApplicationSettings());
        settings.setTls(TlsSettings.builder().clientCertificate(VALID_PEM_1).build());

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(updateApplication.getSettings()).thenReturn(settings);

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");

        SubscriptionEntity subscription1 = new SubscriptionEntity();
        subscription1.setId("sub-1");
        subscription1.setClientCertificate("old cert");

        SubscriptionEntity subscription2 = new SubscriptionEntity();
        subscription2.setId("sub-2");
        subscription2.setClientCertificate("old cert");

        assertThrows(
            ClientCertificateChangeNotAllowedException.class,
            () -> {
                applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
            }
        );
    }

    @Test
    public void should_update_neither_client_id_or_client_certificate_of_subscriptions() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        settings.setTls(TlsSettings.builder().clientCertificate(VALID_PEM_1).build());

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(updateApplication.getSettings()).thenReturn(settings);

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");

        SubscriptionEntity subscription1 = new SubscriptionEntity();
        subscription1.setId("sub-1");
        subscription1.setClientId("old client id");
        subscription1.setClientCertificate("old cert");

        SubscriptionEntity subscription2 = new SubscriptionEntity();
        subscription2.setId("sub-2");
        subscription2.setClientId("old client id");
        subscription2.setClientCertificate("old cert");

        assertThrows(
            ClientCertificateChangeNotAllowedException.class,
            () -> {
                applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
            }
        );
    }

    @Test
    public void shouldNotUpdateWhenClientIdIsEmptyWithJWTPlan() {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(Strings.EMPTY);
        settings.setApp(clientSettings);
        when(updateApplication.getSettings()).thenReturn(settings);

        String jwtPlanId = "jwtPLan";
        String apiKeyPlanId = "apiKeyPlan";
        String oauthPlanId = "oauthPLan";
        SubscriptionEntity jwtSubscription = new SubscriptionEntity();
        jwtSubscription.setPlan(jwtPlanId);
        jwtSubscription.setStatus(SubscriptionStatus.ACCEPTED);
        SubscriptionEntity apiKeySubscription = new SubscriptionEntity();
        apiKeySubscription.setPlan(apiKeyPlanId);
        apiKeySubscription.setStatus(SubscriptionStatus.ACCEPTED);
        SubscriptionEntity oauthSubscription = new SubscriptionEntity();
        oauthSubscription.setPlan(oauthPlanId);
        oauthSubscription.setStatus(SubscriptionStatus.ACCEPTED);
        when(subscriptionService.findByApplicationAndPlan(GraviteeContext.getExecutionContext(), APPLICATION_ID, null))
            .thenReturn(Arrays.asList(jwtSubscription, apiKeySubscription, oauthSubscription));

        PlanEntity jwtPlanEntity = new PlanEntity();
        jwtPlanEntity.setId(jwtPlanId);
        jwtPlanEntity.setSecurity(PlanSecurityType.JWT);
        PlanEntity apiKeyPlanEntity = new PlanEntity();
        apiKeyPlanEntity.setId(apiKeyPlanId);
        apiKeyPlanEntity.setSecurity(PlanSecurityType.API_KEY);
        PlanEntity oauthPlanEntity = new PlanEntity();
        oauthPlanEntity.setId(oauthPlanId);
        oauthPlanEntity.setSecurity(PlanSecurityType.OAUTH2);
        when(planSearchService.findByIdIn(GraviteeContext.getExecutionContext(), Set.of(jwtPlanId, apiKeyPlanId, oauthPlanId)))
            .thenReturn(Set.of(jwtPlanEntity, apiKeyPlanEntity, oauthPlanEntity));

        assertThrows(
            ApplicationClientIdException.class,
            () -> applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication)
        );

        verify(subscriptionService).findByApplicationAndPlan(any(ExecutionContext.class), eq(APPLICATION_ID), isNull());
        verify(planSearchService).findByIdIn(any(ExecutionContext.class), eq(Set.of(jwtPlanId, apiKeyPlanId, oauthPlanId)));
    }

    @Test
    public void should_filter_subscriptions_when_validate_client_id() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(Strings.EMPTY);
        settings.setApp(clientSettings);
        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(updateApplication.getApiKeyMode()).thenReturn(io.gravitee.rest.api.model.ApiKeyMode.SHARED);

        String jwtPlanId = "jwtPLan";
        String apiKeyPlanId = "apiKeyPlan";
        SubscriptionEntity jwtSubscription = new SubscriptionEntity();
        jwtSubscription.setPlan(jwtPlanId);
        jwtSubscription.setStatus(SubscriptionStatus.CLOSED);
        SubscriptionEntity jwtSubscription2 = new SubscriptionEntity();
        jwtSubscription2.setPlan(jwtPlanId);
        jwtSubscription2.setStatus(SubscriptionStatus.REJECTED);
        SubscriptionEntity apiKeySubscription = new SubscriptionEntity();
        apiKeySubscription.setPlan(apiKeyPlanId);
        apiKeySubscription.setStatus(SubscriptionStatus.ACCEPTED);
        when(subscriptionService.findByApplicationAndPlan(GraviteeContext.getExecutionContext(), APPLICATION_ID, null))
            .thenReturn(Arrays.asList(jwtSubscription, apiKeySubscription, jwtSubscription2));

        PlanEntity jwtPlanEntity = new PlanEntity();
        jwtPlanEntity.setId(jwtPlanId);
        jwtPlanEntity.setSecurity(PlanSecurityType.JWT);
        PlanEntity apiKeyPlanEntity = new PlanEntity();
        apiKeyPlanEntity.setId(apiKeyPlanId);
        when(planSearchService.findByIdIn(GraviteeContext.getExecutionContext(), Set.of(apiKeyPlanId)))
            .thenReturn(Set.of(apiKeyPlanEntity));

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

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());

        verify(applicationRepository)
            .update(argThat(application -> APPLICATION_NAME.equals(application.getName()) && application.getUpdatedAt() != null));
        verify(subscriptionService).findByApplicationAndPlan(any(ExecutionContext.class), eq(APPLICATION_ID), isNull());
        verify(planSearchService).findByIdIn(any(ExecutionContext.class), eq(Set.of(apiKeyPlanId)));
    }

    @Test
    public void should_not_update_null_new_client_id() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
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
        SubscriptionEntity subscription1 = new SubscriptionEntity();
        subscription1.setId("sub-1");
        subscription1.setClientId("old id");

        SubscriptionEntity subscription2 = new SubscriptionEntity();
        subscription2.setId("sub-2");
        subscription2.setClientId("old id");

        List<SubscriptionEntity> subscriptions = List.of(subscription1, subscription2);
        when(
            subscriptionService.search(
                any(),
                argThat(criteria ->
                    criteria.getApplications().contains(APPLICATION_ID) &&
                    criteria
                        .getStatuses()
                        .containsAll(Set.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED, SubscriptionStatus.PENDING))
                )
            )
        )
            .thenReturn(subscriptions);

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        verify(subscriptionService, never()).update(any(), any(UpdateSubscriptionEntity.class), any());
    }

    @Test
    public void should_not_update_client_id_of_subscriptions_with_null_client_id() throws TechnicalException {
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
        when(
            subscriptionService.search(
                any(),
                argThat(criteria ->
                    criteria.getApplications().contains(APPLICATION_ID) &&
                    criteria
                        .getStatuses()
                        .containsAll(Set.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED, SubscriptionStatus.PENDING))
                )
            )
        )
            .thenReturn(subscriptions);

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        verify(subscriptionService, never()).update(any(), any(UpdateSubscriptionEntity.class), any());
    }

    private final String VALID_PEM_1 =
        """
              -----BEGIN CERTIFICATE-----
              MIIFxjCCA64CCQD9kAnHVVL02TANBgkqhkiG9w0BAQsFADCBozEsMCoGCSqGSIb3
              DQEJARYddW5pdC50ZXN0c0BncmF2aXRlZXNvdXJjZS5jb20xEzARBgNVBAMMCnVu
              aXQtdGVzdHMxFzAVBgNVBAsMDkdyYXZpdGVlU291cmNlMRcwFQYDVQQKDA5HcmF2
              aXRlZVNvdXJjZTEOMAwGA1UEBwwFTGlsbGUxDzANBgNVBAgMBkZyYW5jZTELMAkG
              A1UEBhMCRlIwIBcNMjExMDE5MTUyMDQxWhgPMjEyMTA5MjUxNTIwNDFaMIGjMSww
              KgYJKoZIhvcNAQkBFh11bml0LnRlc3RzQGdyYXZpdGVlc291cmNlLmNvbTETMBEG
              A1UEAwwKdW5pdC10ZXN0czEXMBUGA1UECwwOR3Jhdml0ZWVTb3VyY2UxFzAVBgNV
              BAoMDkdyYXZpdGVlU291cmNlMQ4wDAYDVQQHDAVMaWxsZTEPMA0GA1UECAwGRnJh
              bmNlMQswCQYDVQQGEwJGUjCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIB
              AOKxBeF33XOd5sVaHbavIGFU+DMTX+cqTbRiJQJqAlrrDeuPQ3YEfga7hpHHB3ev
              OjunNCBJp4p/6VsBhylqcqd8KU+xqQ/wvNsqzp/50ssMkud+0sbPFjjjxM1rDI9X
              JVCqGqa15jlKfylcOOggH6KAOugM4BquBjeTRH0mGv2MBgZvtKHAieW0gzPslXxp
              UZZZ+gvvSSLo7NkAv7awWKSoV+yMlXma0yX0ygAj14EK1AxhFLZFgWDm8Ex919ry
              rbcPV6tqUHjw7Us8cy8p/pqftOUnwyRQ4LmaSdqwESZmdU+GXNXq22sAB6rX0G7u
              tXmoXVwQVlD8kEb79JbbIEOfPvLATyr8VStCK5dSXyc/JuzDo7QCquQUdrGpWrSy
              wdKKbCbOWDStakmBTEkgB0Bqg6yWFrHjgj+rzNeWFvIoZA+sLV2UCrlhDQ8BUV9O
              PMdgGBMKu4TrdEezt1NqDHjvThC3c6quxixxmaO/K7YPncVzguypijw7U7yl8CkG
              DlUJ+rPddEgsQCf+1E6z/xIeh8sCEdLm6TN80Dsw1yTdwzhRO9KvVY/gjE/ZaUYL
              g8Z0Htjq6vvnMwvr4C/8ykRk9oMYlv3o52pXQEcsbiZYm7LCTwgCs6k7KEiaHUze
              ySEqlkqFC8PG2GzCC6dM50xYktbcmwC+mep7c6bTAsexAgMBAAEwDQYJKoZIhvcN
              AQELBQADggIBAIHpb9solYTIPszzgvw0S6BVBAGzARDNDSi/jj+4KXKlKxYvVvq+
              bTX7YE6rC/wFGpyCjwfoWzzIrfLiIcmVTfu1o13Y/B8IEP4WyiAYrGszLqbjy1wM
              cyfwaxYpP/XfIQgcP5idI6kAA7hbGrFrLijIcdfYhh4tr6dsjD81uNrsVhp+JcAV
              CPv2o5YeRSMFUJrImAU5s73yX/x6fb2nCUR6PIMiPm9gveIAuY2+L12NzIJUugwN
              EZjqCeOr52f/yDuA+pAvVCGnZSSdkVWUh02ZsPxM4TiRzmxSkM5ODb59XWHeoFT1
              yvKA2F7+WFAL2R8BhBoVlBp1hug33Mrsix7L6yG4G9Ljss9Y0pzEd4B+IFGbpMZN
              R4dqZGpKS0aiStnvnurXBVWwIcJ3kCaAl2OgXZO5ivi+iNIx8e5qtXqDCnnlpeGz
              1KVhzZaqND1I+X1JS6I/V/HiTsnuVdg5aBZPYbQI0QLSgB+0SOjmTlWzjyJEt0PS
              kyOEs4bB9CPf3JaWgB9aORczsgn/cz8S7kEc8JlXDflePiSl4QPWYbX05wY9l2lJ
              yzuug/vKMCWUq0cU2i8WSA02N0+tEm4hCNol04KLKa3MRAa/yOSmDIJ4z+2D/BSD
              FZHaYejhPQFZzv73SxOAu2QCaXH5vIBEDx4Mb+lvc4BukgeIT2Gyi2gg
              -----END CERTIFICATE-----
              """;

    private static final String VALID_PEM_2 =
        """
           -----BEGIN CERTIFICATE-----
           MIIFqDCCA5ACCQD8CdtUilB/LDANBgkqhkiG9w0BAQsFADCBlTEpMCcGCSqGSIb3
           DQEJARYaY29udGFjdEBncmF2aXRlZXNvdXJjZS5jb20xEjAQBgNVBAMMCWphZWdl
           ci1jYTENMAsGA1UECwwEQVBJTTEXMBUGA1UECgwOR3Jhdml0ZWVTb3VyY2UxDjAM
           BgNVBAcMBUxpbGxlMQ8wDQYDVQQIDAZGcmFuY2UxCzAJBgNVBAYTAkZSMB4XDTIz
           MDQxNzEyNDMwMFoXDTMzMDQxNDEyNDMwMFowgZUxKTAnBgkqhkiG9w0BCQEWGmNv
           bnRhY3RAZ3Jhdml0ZWVzb3VyY2UuY29tMRIwEAYDVQQDDAlqYWVnZXItY2ExDTAL
           BgNVBAsMBEFQSU0xFzAVBgNVBAoMDkdyYXZpdGVlU291cmNlMQ4wDAYDVQQHDAVM
           aWxsZTEPMA0GA1UECAwGRnJhbmNlMQswCQYDVQQGEwJGUjCCAiIwDQYJKoZIhvcN
           AQEBBQADggIPADCCAgoCggIBANMbbR6pXb+AOoAC0ymdypgAZNcRwkMBC1MrUeJp
           P+3G6LVP6PFaYiukhGlMuemtgDaPomsdnDDQHYF/WRe2BNI+LB0U/PQ8A7FSqnnN
           i2RCGbTvqk68kYrfCOfL14q3iBZNxydFEEoU7UGqdxo0RDbwF+oWcCE6RoFd+5nS
           GQycOdHpBm5omftvg7zBTWoBJNAifkLf+TcHB3nJLqq0wvUG7HnmHxIU8GJW7BA7
           4gEfgALEhra40tUar3g15IVbmLDHJaYwCpVz09na2A6kF0QqunkLoNC32MaajPZv
           ROjXJgTxhDHcPzBJO4y1MVTRKG+dtr/X5uLCad4ZMi2wWYgFXzjPK871OW5+eAKn
           rJEJDapaEuf9NX1AuWOLVVjDReBkfpvy7H1p+lQ9y0HNLOcMIdt1zLDsbSIySDAo
           A4eXnorTHWP3y3JkguFIDEo/FxSw0blvTqAQyAmb3FWJIrgJ+mU5VDeYMhoezbki
           RUFCQS7R33ns9A8Tw4kaOtLtc3Xrj8EBzpjT1ioygWnaQTrg7uj3CxLess+e71vJ
           X5n6M2UNV6dQf6izSisQx/XJeawuEZgkRtHs7ta52i0Xujje+XI09S6uavWWfgnj
           v/24vOyUq/Y21McrM1nWhrofk4JPLDp8zzOvuJRQyLJyWWxIveIMgowvgzVUHmvn
           ese9AgMBAAEwDQYJKoZIhvcNAQELBQADggIBAG8tLNCV5/xM/YGMgZgdIyZnxQJf
           4Zxfyg7NGiQKgPZOQ5lTaBlfbdRSdCywsb8yQmY4bv75Z3DB9wvyQG+cJvBGm7iR
           SMcS+VPZP8G+MimVuj9qUEcf5J9sqWfkRN32jwgbIAKtGFrJoMbNymoVX+Qvb1Jv
           voCrULP7lJrNMwnIIecB6MOazrM+spQdP4UgqixHorJI0bAKxdTNm8ZN3SRZzJ0Q
           f1Bvnjw7FU1G4s9JNnjNDu3S3zafq2cKkDWzE7ZstyAlKJrZwyWjyx2hEtPlKJ16
           XOPErC92+1r2yYA7Z9jYTt+42t/DUky4oIXDkmrF8k+DIciXgIg3O2f8S+JfsGyW
           NFg1N8Mpld9voItmBDlfahEq/RddMIGOrisl8d8oRB9SzONZG/leK06/ZCqeC2Mc
           T/AmHp7tnAhRNyKf0mw8yMQygpRHN9bQUXHfYgZGQ2hWW6AP+3URg8pyJmwsz9OA
           SIHa2KmTKy/R8ssOAh8jyzeMXhuesnv9zSV1zllbfBZ54+1EQVba3Pg2RRpsonN5
           Ya26HYHn2V4Y5PkE+YeKn9xcl/G+KWiymHpZ9cyUUE06ZBQY4Ha0vX67b0K5AYwZ
           vVJj7N1kwlzS2KIysLrefl7PUMikuuEopTH6Xmi9obDZfywPoK2LxUdnN3m8KmUe
           FPeI4PKEw5AUf97H
           -----END CERTIFICATE-----
           
           """;
}
