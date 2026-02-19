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
import io.gravitee.rest.api.model.settings.ConsoleConfigEntity;
import io.gravitee.rest.api.model.settings.Enabled;
import io.gravitee.rest.api.model.settings.UserGroup;
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
import jakarta.ws.rs.BadRequestException;
import java.util.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import joptsimple.internal.Strings;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
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
    private static final String NEW_APPLICATION_NAME = "newApplication";

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

    @Mock
    private ConfigService configService;

    @Mock
    private GroupService groupService;

    @Mock
    private io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService clientCertificateCrudService;

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
        ).thenReturn(true);

        ConsoleConfigEntity config = getConsoleConfigEntity(true);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(config);
        lenient().when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);

        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(updateApplication.getApiKeyMode()).thenReturn(io.gravitee.rest.api.model.ApiKeyMode.SHARED);
        when(updateApplication.getGroups()).thenReturn(Set.of("group1", "group2"));

        final Application updatedApplication = mock(Application.class);
        when(updatedApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updatedApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(updatedApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(updatedApplication.getGroups()).thenReturn(Set.of("group1", "group2"));
        when(updatedApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);

        when(applicationRepository.update(any())).thenReturn(updatedApplication);

        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(getPrimaryOwner()));
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        // Mock the certificate service to return the certificate
        io.gravitee.apim.core.application_certificate.model.ClientCertificate mockCert =
            io.gravitee.apim.core.application_certificate.model.ClientCertificate.builder()
                .id("cert-id")
                .applicationId(APPLICATION_ID)
                .name("cert-name")
                .createdAt(new java.util.Date())
                .updatedAt(new java.util.Date())
                .certificate(VALID_PEM_1)
                .status(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.ACTIVE)
                .build();
        when(
            clientCertificateCrudService.findByApplicationIdAndStatuses(
                any(),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class)
            )
        ).thenReturn(java.util.Set.of(mockCert));

        final ApplicationEntity applicationEntity = applicationService.update(
            GraviteeContext.getExecutionContext(),
            APPLICATION_ID,
            updateApplication
        );

        verify(applicationRepository).update(
            argThat(application -> APPLICATION_NAME.equals(application.getName()) && application.getUpdatedAt() != null)
        );

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }

    @Test
    public void shouldThrowExceptionWhenUserGroupsRequiredButNotPresent() {
        ApplicationSettings settings = new ApplicationSettings();
        ConsoleConfigEntity config = getConsoleConfigEntity(true);
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(Strings.EMPTY);
        settings.setApp(clientSettings);
        mockSubscriptions();
        mockPlans();
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        when(configService.getConsoleConfig(executionContext)).thenReturn(config);

        Exception exception = assertThrows(BadRequestException.class, () ->
            applicationService.update(executionContext, APPLICATION_ID, updateApplication)
        );

        assertEquals(
            "Updating an application is not allowed as at least one group is required on the application.",
            exception.getMessage()
        );
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
        ).thenReturn(true);

        mockSubscriptions();
        mockPlans();
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

        MembershipEntity po = getPrimaryOwner();
        ConsoleConfigEntity config = getConsoleConfigEntity(false);

        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(config);
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        final ApplicationEntity applicationEntity = applicationService.update(
            GraviteeContext.getExecutionContext(),
            APPLICATION_ID,
            updateApplication
        );

        verify(applicationRepository).update(
            argThat(application -> APPLICATION_NAME.equals(application.getName()) && application.getUpdatedAt() != null)
        );

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());

        verify(subscriptionService).findByApplicationAndPlan(any(ExecutionContext.class), eq(APPLICATION_ID), isNull());
        verify(planSearchService).findByIdIn(any(ExecutionContext.class), eq(Set.of("apiKeyPlan", "pushPLan")));
    }

    private void mockSubscriptions() {
        SubscriptionEntity apiKeySubscription = new SubscriptionEntity();
        apiKeySubscription.setPlan("apiKeyPlan");
        apiKeySubscription.setStatus(SubscriptionStatus.ACCEPTED);
        SubscriptionEntity pushSubscription = new SubscriptionEntity();
        pushSubscription.setPlan("pushPLan");
        pushSubscription.setStatus(SubscriptionStatus.ACCEPTED);
        when(subscriptionService.findByApplicationAndPlan(GraviteeContext.getExecutionContext(), APPLICATION_ID, null)).thenReturn(
            Arrays.asList(apiKeySubscription, pushSubscription)
        );
    }

    private void mockPlans() {
        io.gravitee.rest.api.model.v4.plan.PlanEntity apiKeyPlanEntity = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        apiKeyPlanEntity.setId("apiKeyPlan");
        apiKeyPlanEntity.setSecurity(new PlanSecurity(PlanSecurityType.API_KEY.name(), ""));
        apiKeyPlanEntity.setMode(PlanMode.STANDARD);
        io.gravitee.rest.api.model.v4.plan.PlanEntity pushPlanEntity = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        pushPlanEntity.setId("pushPLan");
        pushPlanEntity.setSecurity(null);
        pushPlanEntity.setMode(PlanMode.PUSH);
        when(planSearchService.findByIdIn(GraviteeContext.getExecutionContext(), Set.of("apiKeyPlan", "pushPLan"))).thenReturn(
            Set.of(apiKeyPlanEntity, pushPlanEntity)
        );
    }

    @Test(expected = ApplicationNotFoundException.class)
    public void shouldNotUpdateBecauseNotFound() throws TechnicalException {
        ConsoleConfigEntity config = getConsoleConfigEntity(false);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());
        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(config);

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);

        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);
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

        ConsoleConfigEntity config = getConsoleConfigEntity(false);
        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(config);

        MembershipEntity po = getPrimaryOwner();
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        final ApplicationEntity applicationEntity = applicationService.update(
            GraviteeContext.getExecutionContext(),
            APPLICATION_ID,
            updateApplication
        );

        verify(applicationRepository).update(
            argThat(application -> APPLICATION_NAME.equals(application.getName()) && application.getUpdatedAt() != null)
        );

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
        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);

        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);
        when(updateApplication.getSettings()).thenReturn(settings);

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
    }

    @Test(expected = IllegalStateException.class)
    public void should_throw_exception_cause_client_registration_is_disabled() throws TechnicalException {
        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.BROWSER);
        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);
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
        ).thenReturn(false);

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

        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);
        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);

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
        ).thenReturn(false);

        // existing application has a UNSPECIFIED API Key mode
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));

        // updated application has a SHARED API Key mode
        when(updateApplication.getApiKeyMode()).thenReturn(io.gravitee.rest.api.model.ApiKeyMode.SHARED);
        when(updateApplication.getSettings()).thenReturn(new ApplicationSettings());
        updateApplication.getSettings().setApp(new SimpleApplicationSettings());

        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);
        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);

        // this should throw exception cause shard API Key setting is disabled
        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);
    }

    @Test(expected = ApplicationGrantTypesNotFoundException.class)
    public void should_throw_exception_cause_oAuth_client_settings_has_no_grant_types() throws TechnicalException {
        ConsoleConfigEntity config = getConsoleConfigEntity(false);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.BROWSER);
        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(config);
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
        ).thenReturn(true);

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

        MembershipEntity po = getPrimaryOwner();
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        // client registration is enabled
        when(
            parameterService.findAsBoolean(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.APPLICATION_REGISTRATION_ENABLED),
                any(),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        ).thenReturn(true);

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
        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);

        when(applicationTypeService.getApplicationType(ApplicationType.BROWSER.name())).thenReturn(applicationTypeEntity);
        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);
        // mock response from DCR with a new client ID
        ClientRegistrationResponse clientRegistrationResponse = new ClientRegistrationResponse();
        clientRegistrationResponse.setClientId("client-id-from-clientRegistration");
        when(clientRegistrationService.update(any(), any(), same(updateApplication))).thenReturn(clientRegistrationResponse);
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        // ensure application has been updated with the new client_id from DCR
        verify(applicationRepository).update(
            argThat(application -> application.getMetadata().get(METADATA_CLIENT_ID).equals("client-id-from-clientRegistration"))
        );
    }

    @Test
    public void should_update_with_previous_oAuth2_clientId_when_registration_service_fails() throws TechnicalException {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(existingApplication.getType()).thenReturn(ApplicationType.BROWSER);
        when(existingApplication.getMetadata()).thenReturn(
            Map.of(METADATA_REGISTRATION_PAYLOAD, "{}", METADATA_CLIENT_ID, "my-previous-client-id")
        );
        when(applicationRepository.update(any())).thenReturn(existingApplication);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = getPrimaryOwner();
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        // client registration is enabled
        when(
            parameterService.findAsBoolean(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.APPLICATION_REGISTRATION_ENABLED),
                any(),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        ).thenReturn(true);

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
        ConsoleConfigEntity config = getConsoleConfigEntity(false);

        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(config);
        when(applicationTypeService.getApplicationType(ApplicationType.BROWSER.name())).thenReturn(applicationTypeEntity);

        // DCR throws exception
        when(clientRegistrationService.update(any(), any(), same(updateApplication))).thenThrow(RuntimeException.class);
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        // ensure application has been updated, but kept the previous client_id
        verify(applicationRepository).update(
            argThat(application -> application.getMetadata().get(METADATA_CLIENT_ID).equals("my-previous-client-id"))
        );
    }

    @Test
    public void should_update_client_id_and_application_name_of_subscriptions() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getName()).thenReturn(NEW_APPLICATION_NAME);
        when(applicationRepository.update(any())).thenReturn(existingApplication);

        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = getPrimaryOwner();
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        SubscriptionEntity subscription1 = new SubscriptionEntity();
        subscription1.setId("sub-1");
        subscription1.setClientId("old id");
        subscription1.setApplicationName(APPLICATION_NAME);

        SubscriptionEntity subscription2 = new SubscriptionEntity();
        subscription2.setId("sub-2");
        subscription2.setClientId("old id");
        subscription2.setApplicationName(APPLICATION_NAME);
        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);

        List<SubscriptionEntity> subscriptions = List.of(subscription1, subscription2);
        when(
            subscriptionService.search(
                any(),
                argThat(
                    criteria ->
                        criteria.getApplications().contains(APPLICATION_ID) &&
                        criteria
                            .getStatuses()
                            .containsAll(Set.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED, SubscriptionStatus.PENDING))
                )
            )
        ).thenReturn(subscriptions);
        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        ArgumentCaptor<Consumer<Subscription>> subscriptionModifierCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(subscriptionService, times(2)).update(any(), any(UpdateSubscriptionEntity.class), subscriptionModifierCaptor.capture());

        // Verify the function called to modify the subscription has been modifying its client id
        Subscription fakeSubscription = new Subscription();
        subscriptionModifierCaptor.getValue().accept(fakeSubscription);
        Assertions.assertThat(fakeSubscription.getClientId()).isEqualTo(CLIENT_ID);
        Assertions.assertThat(fakeSubscription.getApplicationName()).isEqualTo(NEW_APPLICATION_NAME);
    }

    @Test
    public void should_update_client_certificate_via_crud_service() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        settings.setApp(new SimpleApplicationSettings());
        settings.setTls(TlsSettings.builder().clientCertificate(VALID_PEM_1).build());
        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);

        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();
        when(applicationRepository.update(any())).thenReturn(existingApplication);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = getPrimaryOwner();
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        // No existing certificate - this is a new certificate
        when(
            clientCertificateCrudService.findByApplicationIdAndStatuses(
                any(),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class)
            )
        ).thenReturn(java.util.Set.of());

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        // Verify that a new certificate was created
        verify(clientCertificateCrudService).create(eq(APPLICATION_ID), any());
    }

    @Test
    public void should_update_certificate_and_expire_old_one() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        settings.setTls(TlsSettings.builder().clientCertificate(VALID_PEM_1).build());
        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);

        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();
        when(applicationRepository.update(any())).thenReturn(existingApplication);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = getPrimaryOwner();
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        // Existing certificate with different content
        io.gravitee.apim.core.application_certificate.model.ClientCertificate existingCert =
            io.gravitee.apim.core.application_certificate.model.ClientCertificate.builder()
                .id("old-cert-id")
                .applicationId(APPLICATION_ID)
                .name("old-cert-name")
                .createdAt(new java.util.Date())
                .updatedAt(new java.util.Date())
                .certificate("old certificate content")
                .status(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.ACTIVE)
                .build();
        when(
            clientCertificateCrudService.findByApplicationIdAndStatuses(
                any(),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class)
            )
        ).thenReturn(java.util.Set.of(existingCert));

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        // Verify that the old certificate was expired and a new one was created
        verify(clientCertificateCrudService).update(eq("old-cert-id"), any());
        verify(clientCertificateCrudService).create(eq(APPLICATION_ID), any());
    }

    @Test
    public void should_create_multiple_certificates_from_list() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        settings.setApp(new SimpleApplicationSettings());
        settings.setTls(
            TlsSettings.builder()
                .clientCertificates(
                    java.util.List.of(
                        new io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate("cert-1", null, null, VALID_PEM_1),
                        new io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate(
                            "cert-2",
                            null,
                            null,
                            "another-pem-content"
                        )
                    )
                )
                .build()
        );
        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);

        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();
        when(applicationRepository.update(any())).thenReturn(existingApplication);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = getPrimaryOwner();
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        // No existing certificates
        when(
            clientCertificateCrudService.findByApplicationIdAndStatuses(
                any(),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class)
            )
        ).thenReturn(java.util.Set.of());

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        // Verify both certificates were created
        verify(clientCertificateCrudService, times(2)).create(eq(APPLICATION_ID), any());
    }

    @Test
    public void should_keep_existing_cert_and_add_new_one() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        settings.setApp(new SimpleApplicationSettings());
        settings.setTls(
            TlsSettings.builder()
                .clientCertificates(
                    java.util.List.of(
                        new io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate("existing", null, null, "existing-pem"),
                        new io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate("new-cert", null, null, "new-pem")
                    )
                )
                .build()
        );
        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);

        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();
        when(applicationRepository.update(any())).thenReturn(existingApplication);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = getPrimaryOwner();
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        // One existing cert that matches one of the new ones
        io.gravitee.apim.core.application_certificate.model.ClientCertificate existingCert =
            io.gravitee.apim.core.application_certificate.model.ClientCertificate.builder()
                .id("existing-cert-id")
                .applicationId(APPLICATION_ID)
                .name("existing")
                .createdAt(new java.util.Date())
                .certificate("existing-pem")
                .status(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.ACTIVE)
                .build();
        when(
            clientCertificateCrudService.findByApplicationIdAndStatuses(
                any(),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class)
            )
        ).thenReturn(java.util.Set.of(existingCert));

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        // Only the new cert should be created, existing one kept
        verify(clientCertificateCrudService, times(1)).create(eq(APPLICATION_ID), any());
        // Existing cert should NOT be expired
        verify(clientCertificateCrudService, never()).update(eq("existing-cert-id"), any());
    }

    private static @NonNull MembershipEntity getPrimaryOwner() {
        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        po.setMemberType(MembershipMemberType.USER);
        po.setReferenceId(APPLICATION_ID);
        po.setReferenceType(MembershipReferenceType.APPLICATION);
        po.setRoleId("APPLICATION_PRIMARY_OWNER");
        return po;
    }

    @Test
    public void should_revoke_removed_cert() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        settings.setApp(new SimpleApplicationSettings());
        // Only keep one cert out of two
        settings.setTls(
            TlsSettings.builder()
                .clientCertificates(
                    java.util.List.of(
                        new io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate("kept", null, null, "kept-pem")
                    )
                )
                .build()
        );
        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);

        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();
        when(applicationRepository.update(any())).thenReturn(existingApplication);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = getPrimaryOwner();
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        // Two existing certs - one will be kept, one removed
        io.gravitee.apim.core.application_certificate.model.ClientCertificate keptCert =
            io.gravitee.apim.core.application_certificate.model.ClientCertificate.builder()
                .id("kept-cert-id")
                .applicationId(APPLICATION_ID)
                .name("kept")
                .createdAt(new java.util.Date())
                .certificate("kept-pem")
                .status(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.ACTIVE)
                .build();
        io.gravitee.apim.core.application_certificate.model.ClientCertificate removedCert =
            io.gravitee.apim.core.application_certificate.model.ClientCertificate.builder()
                .id("removed-cert-id")
                .applicationId(APPLICATION_ID)
                .name("removed")
                .createdAt(new java.util.Date())
                .certificate("removed-pem")
                .status(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.ACTIVE)
                .build();
        when(
            clientCertificateCrudService.findByApplicationIdAndStatuses(
                any(),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class)
            )
        ).thenReturn(java.util.Set.of(keptCert, removedCert));

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        // Verify the removed cert is revoked (status set to REVOKED)
        ArgumentCaptor<io.gravitee.apim.core.application_certificate.model.ClientCertificate> certCaptor = ArgumentCaptor.forClass(
            io.gravitee.apim.core.application_certificate.model.ClientCertificate.class
        );
        verify(clientCertificateCrudService).update(eq("removed-cert-id"), certCaptor.capture());
        io.gravitee.apim.core.application_certificate.model.ClientCertificate revokedCert = certCaptor.getValue();
        Assertions.assertThat(revokedCert.getEndsAt()).isBeforeOrEqualTo(new Date());

        // Kept cert should NOT be touched
        verify(clientCertificateCrudService, never()).update(eq("kept-cert-id"), any());
        // No new certs to create
        verify(clientCertificateCrudService, never()).create(any(), any());
    }

    @Test
    public void should_expire_removed_cert() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        settings.setApp(new SimpleApplicationSettings());
        settings.setTls(
            TlsSettings.builder()
                .clientCertificates(
                    java.util.List.of(
                        new io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate("kept", null, null, "kept-pem")
                    )
                )
                .build()
        );
        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);

        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(updateApplication.getSettings()).thenReturn(settings);
        when(updateApplication.getName()).thenReturn(APPLICATION_NAME);
        when(updateApplication.getDescription()).thenReturn("My description");
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();
        when(applicationRepository.update(any())).thenReturn(existingApplication);
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = getPrimaryOwner();
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));

        // Two existing certs - one will be kept, one removed
        io.gravitee.apim.core.application_certificate.model.ClientCertificate keptCert =
            io.gravitee.apim.core.application_certificate.model.ClientCertificate.builder()
                .id("kept-cert-id")
                .applicationId(APPLICATION_ID)
                .name("kept")
                .createdAt(new java.util.Date())
                .certificate("kept-pem")
                .status(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.ACTIVE)
                .build();
        io.gravitee.apim.core.application_certificate.model.ClientCertificate removedCert =
            io.gravitee.apim.core.application_certificate.model.ClientCertificate.builder()
                .id("removed-cert-id")
                .applicationId(APPLICATION_ID)
                .name("removed")
                .createdAt(new java.util.Date())
                .certificate("removed-pem")
                .status(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.ACTIVE)
                .build();
        when(
            clientCertificateCrudService.findByApplicationIdAndStatuses(
                any(),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class),
                any(io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus.class)
            )
        ).thenReturn(java.util.Set.of(keptCert, removedCert));

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        // Removed cert should be expired
        verify(clientCertificateCrudService).update(eq("removed-cert-id"), any());
        // Kept cert should NOT be expired
        verify(clientCertificateCrudService, never()).update(eq("kept-cert-id"), any());
        // No new certs to create
        verify(clientCertificateCrudService, never()).create(any(), any());
    }

    private static @NotNull ConsoleConfigEntity getConsoleConfigEntity(boolean enabled) {
        ConsoleConfigEntity consoleConfig = new ConsoleConfigEntity();
        UserGroup userGroup = new UserGroup();
        userGroup.setRequired(new Enabled(enabled));
        consoleConfig.setUserGroup(userGroup);
        return consoleConfig;
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
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        when(subscriptionService.findByApplicationAndPlan(executionContext, APPLICATION_ID, null)).thenReturn(
            Arrays.asList(jwtSubscription, apiKeySubscription, oauthSubscription)
        );

        PlanEntity jwtPlanEntity = new PlanEntity();
        jwtPlanEntity.setId(jwtPlanId);
        jwtPlanEntity.setSecurity(PlanSecurityType.JWT);
        PlanEntity apiKeyPlanEntity = new PlanEntity();
        apiKeyPlanEntity.setId(apiKeyPlanId);
        apiKeyPlanEntity.setSecurity(PlanSecurityType.API_KEY);
        PlanEntity oauthPlanEntity = new PlanEntity();
        oauthPlanEntity.setId(oauthPlanId);
        oauthPlanEntity.setSecurity(PlanSecurityType.OAUTH2);
        when(planSearchService.findByIdIn(executionContext, Set.of(jwtPlanId, apiKeyPlanId, oauthPlanId))).thenReturn(
            Set.of(jwtPlanEntity, apiKeyPlanEntity, oauthPlanEntity)
        );

        assertThrows(ApplicationClientIdException.class, () ->
            applicationService.update(executionContext, APPLICATION_ID, updateApplication)
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
        when(subscriptionService.findByApplicationAndPlan(GraviteeContext.getExecutionContext(), APPLICATION_ID, null)).thenReturn(
            Arrays.asList(jwtSubscription, apiKeySubscription, jwtSubscription2)
        );

        PlanEntity jwtPlanEntity = new PlanEntity();
        jwtPlanEntity.setId(jwtPlanId);
        jwtPlanEntity.setSecurity(PlanSecurityType.JWT);
        PlanEntity apiKeyPlanEntity = new PlanEntity();
        apiKeyPlanEntity.setId(apiKeyPlanId);
        when(planSearchService.findByIdIn(GraviteeContext.getExecutionContext(), Set.of(apiKeyPlanId))).thenReturn(
            Set.of(apiKeyPlanEntity)
        );

        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_SHARED_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(true);

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getName()).thenReturn(APPLICATION_NAME);
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(existingApplication.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);

        when(applicationRepository.update(any())).thenReturn(existingApplication);

        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = getPrimaryOwner();
        ConsoleConfigEntity consoleConfig = getConsoleConfigEntity(false);

        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(consoleConfig);
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        final ApplicationEntity applicationEntity = applicationService.update(
            GraviteeContext.getExecutionContext(),
            APPLICATION_ID,
            updateApplication
        );

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());

        verify(applicationRepository).update(
            argThat(application -> APPLICATION_NAME.equals(application.getName()) && application.getUpdatedAt() != null)
        );
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

        MembershipEntity po = getPrimaryOwner();
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();
        SubscriptionEntity subscription1 = new SubscriptionEntity();
        subscription1.setId("sub-1");
        subscription1.setClientId("old id");

        SubscriptionEntity subscription2 = new SubscriptionEntity();
        subscription2.setId("sub-2");
        subscription2.setClientId("old id");
        ConsoleConfigEntity config = getConsoleConfigEntity(false);

        List<SubscriptionEntity> subscriptions = List.of(subscription1, subscription2);
        when(
            subscriptionService.search(
                any(),
                argThat(
                    criteria ->
                        criteria.getApplications().contains(APPLICATION_ID) &&
                        criteria
                            .getStatuses()
                            .containsAll(Set.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED, SubscriptionStatus.PENDING))
                )
            )
        ).thenReturn(subscriptions);
        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(config);

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        verify(subscriptionService, never()).update(any(), any(UpdateSubscriptionEntity.class), any());
    }

    @Test
    public void should_not_update_client_id_of_subscriptions_with_null_client_id() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        ConsoleConfigEntity config = getConsoleConfigEntity(false);

        when(configService.getConsoleConfig(GraviteeContext.getExecutionContext())).thenReturn(config);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(existingApplication));
        when(existingApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(existingApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(updateApplication.getSettings()).thenReturn(settings);
        when(applicationRepository.update(any())).thenReturn(existingApplication);

        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(mock(RoleEntity.class));

        MembershipEntity po = getPrimaryOwner();
        when(membershipService.getMembershipsByReferencesAndRole(any(), any(), any())).thenReturn(Collections.singleton(po));
        when(applicationConverter.toApplication(any(UpdateApplicationEntity.class))).thenCallRealMethod();

        List<SubscriptionEntity> subscriptions = List.of(mock(SubscriptionEntity.class), mock(SubscriptionEntity.class));
        when(
            subscriptionService.search(
                any(),
                argThat(
                    criteria ->
                        criteria.getApplications().contains(APPLICATION_ID) &&
                        criteria
                            .getStatuses()
                            .containsAll(Set.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED, SubscriptionStatus.PENDING))
                )
            )
        ).thenReturn(subscriptions);

        applicationService.update(GraviteeContext.getExecutionContext(), APPLICATION_ID, updateApplication);

        verify(subscriptionService, never()).update(any(), any(UpdateSubscriptionEntity.class), any());
    }

    private static final String VALID_PEM_1 = """
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
}
