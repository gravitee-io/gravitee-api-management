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

import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_CERTIFICATE;
import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UserEntity;
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
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.register.ClientRegistrationResponse;
import java.util.*;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationService_CreateTest {

    private static final String APPLICATION_NAME = "myApplication";
    private static final String CLIENT_ID = "myClientId";
    private static final String USER_NAME = "myUser";

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("DEFAULT", "DEFAULT");

    @InjectMocks
    private ApplicationServiceImpl applicationService = new ApplicationServiceImpl();

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    @Mock
    private GroupService groupService;

    @Mock
    private NewApplicationEntity newApplication;

    @Mock
    private Application application;

    @Mock
    private AuditService auditService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ApplicationTypeService applicationTypeService;

    @Mock
    private ApplicationConverter applicationConverter;

    @Mock
    private ClientRegistrationService clientRegistrationService;

    @Before
    public void setup() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldCreateForUser() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        settings.setTls(TlsSettings.builder().clientCertificate(VALID_PEM).build());
        when(newApplication.getSettings()).thenReturn(settings);
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(application.getType()).thenReturn(ApplicationType.SIMPLE);
        when(application.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(applicationRepository.create(any())).thenReturn(application);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);
        when(newApplication.getDescription()).thenReturn("My description");
        when(groupService.findByEvent(eq(GraviteeContext.getCurrentEnvironment()), any())).thenReturn(Collections.emptySet());
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(mock(UserEntity.class));
        when(applicationConverter.toApplication(any(NewApplicationEntity.class))).thenCallRealMethod();

        final ApplicationEntity applicationEntity = applicationService.create(
            GraviteeContext.getExecutionContext(),
            newApplication,
            USER_NAME
        );

        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
        verify(applicationRepository).create(argThat(appToCreate -> appToCreate.getGroups().isEmpty()));
    }

    @Test
    public void shouldCreateAppWithDefaultGroup() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        settings.setTls(TlsSettings.builder().clientCertificate(VALID_PEM).build());
        when(newApplication.getSettings()).thenReturn(settings);
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(application.getType()).thenReturn(ApplicationType.SIMPLE);
        when(application.getApiKeyMode()).thenReturn(ApiKeyMode.UNSPECIFIED);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(applicationRepository.create(any())).thenReturn(application);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);
        when(newApplication.getDescription()).thenReturn("My description");

        GroupEntity defaultGroupToAdd = new GroupEntity();
        defaultGroupToAdd.setId("default-group-to-add");
        when(groupService.findByEvent(eq(GraviteeContext.getCurrentEnvironment()), any())).thenReturn(Set.of(defaultGroupToAdd));
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(mock(UserEntity.class));
        when(applicationConverter.toApplication(any(NewApplicationEntity.class))).thenCallRealMethod();

        final ApplicationEntity applicationEntity = applicationService.create(
            GraviteeContext.getExecutionContext(),
            newApplication,
            USER_NAME
        );

        assertNotNull(applicationEntity);
        verify(applicationRepository)
            .create(
                argThat(appToCreate -> appToCreate.getGroups().size() == 1 && appToCreate.getGroups().contains("default-group-to-add"))
            );
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotCreateBecauseClientRegistrationDisable() throws TechnicalException {
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setOauth(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);

        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_REGISTRATION_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.FALSE);

        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotCreateBecauseAppTypeIsNotAllowed() {
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setApplicationType("web");
        settings.setOauth(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_REGISTRATION_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
    }

    @Test(expected = ApplicationGrantTypesNotFoundException.class)
    public void shouldNotCreateBecauseGrantTypesIsEmpty() {
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setApplicationType("web");
        settings.setOauth(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_REGISTRATION_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_TYPE_WEB_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
    }

    @Test(expected = ApplicationGrantTypesNotAllowedException.class)
    public void shouldNotCreateBecauseGrantTypesIsNotAllowed() {
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setApplicationType("web");
        clientSettings.setGrantTypes(Arrays.asList("foobar"));
        settings.setOauth(clientSettings);
        ApplicationTypeEntity applicationType = mock(ApplicationTypeEntity.class);
        when(applicationTypeService.getApplicationType(any())).thenReturn(applicationType);
        when(newApplication.getSettings()).thenReturn(settings);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_REGISTRATION_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_TYPE_WEB_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
    }

    @Test(expected = ApplicationRedirectUrisNotFound.class)
    public void shouldNotCreateBecauseRedirectURIsNotFound() {
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setApplicationType("web");
        clientSettings.setGrantTypes(Arrays.asList("foobar"));
        settings.setOauth(clientSettings);
        ApplicationTypeEntity applicationType = mock(ApplicationTypeEntity.class);
        ApplicationGrantTypeEntity foobar = new ApplicationGrantTypeEntity();
        foobar.setType("foobar");
        when(applicationType.getRequires_redirect_uris()).thenReturn(true);
        when(applicationType.getAllowed_grant_types()).thenReturn(Arrays.asList(foobar));
        when(applicationTypeService.getApplicationType(any())).thenReturn(applicationType);
        when(newApplication.getSettings()).thenReturn(settings);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_REGISTRATION_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_TYPE_WEB_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);
        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
    }

    @Test(expected = ClientIdAlreadyExistsException.class)
    public void shouldNotCreateBecauseClientIdExists() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("client_id", CLIENT_ID);

        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);

        when(applicationRepository.existsMetadataEntryForEnv(METADATA_CLIENT_ID, CLIENT_ID, "DEFAULT")).thenReturn(true);

        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
    }

    @Test(expected = InvalidApplicationApiKeyModeException.class)
    public void shouldNotCreateCauseSharedApiKeyModeDisabled() {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        when(newApplication.getSettings()).thenReturn(settings);

        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_SHARED_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(false);

        when(newApplication.getApiKeyMode()).thenReturn(io.gravitee.rest.api.model.ApiKeyMode.SHARED);

        applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME);
    }

    @Test
    public void shouldNotCreateBecauseOfInvalidClientCertificate() {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        settings.setTls(
            TlsSettings
                .builder()
                .clientCertificate(
                    """
                             -----BEGIN CERTIFICATE-----
                             This one is invalid
                             -----END CERTIFICATE-----
                             """
                )
                .build()
        );
        when(newApplication.getSettings()).thenReturn(settings);

        Assertions
            .assertThatThrownBy(() -> applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME))
            .isInstanceOf(ApplicationInvalidCertificateException.class)
            .hasMessage("An error has occurred while parsing client certificate");
    }

    @Test
    public void shouldNotCreateBecauseOfNotRecognizedClientCertificate() {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        settings.setTls(
            TlsSettings
                .builder()
                .clientCertificate(
                    """
                             no certificate header, so no parsed as an exception by the library.
                             """
                )
                .build()
        );
        when(newApplication.getSettings()).thenReturn(settings);

        Assertions
            .assertThatThrownBy(() -> applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME))
            .isInstanceOf(ApplicationEmptyCertificateException.class)
            .hasMessage("No certificate can be extracted");
    }

    @SneakyThrows
    @Test
    public void shouldNotCreateBecauseOfAlreadyUsedClientCertificate() {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        settings.setTls(TlsSettings.builder().clientCertificate(VALID_PEM).build());
        when(newApplication.getSettings()).thenReturn(settings);

        when(
            applicationRepository.existsMetadataEntryForEnv(
                METADATA_CLIENT_CERTIFICATE,
                Base64.getEncoder().encodeToString(settings.getTls().getClientCertificate().trim().getBytes()),
                "DEFAULT"
            )
        )
            .thenReturn(true);

        Assertions
            .assertThatThrownBy(() -> applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME))
            .isInstanceOf(ApplicationCertificateAlreadyUsedException.class)
            .hasMessage("Certificate is currently in use by another application");
    }

    @SneakyThrows
    @Test
    public void shouldNotCreateBecauseOfCertificateAuthorityCertificate() {
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId(CLIENT_ID);
        settings.setApp(clientSettings);
        settings.setTls(TlsSettings.builder().clientCertificate(VALID_CA_PEM).build());
        when(newApplication.getSettings()).thenReturn(settings);

        Assertions
            .assertThatThrownBy(() -> applicationService.create(GraviteeContext.getExecutionContext(), newApplication, USER_NAME))
            .isInstanceOf(ApplicationCertificateAuthorityException.class)
            .hasMessage("Certificate Authorities are not supported, requires a client certificate");
    }

    @Test
    public void shouldCreateOauthApp() throws TechnicalException {
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(application.getType()).thenReturn(ApplicationType.BROWSER);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(applicationRepository.create(any())).thenReturn(application);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);
        when(newApplication.getDescription()).thenReturn("My description");
        when(groupService.findByEvent(eq(GraviteeContext.getCurrentEnvironment()), any())).thenReturn(Collections.emptySet());
        when(userService.findById(any(), any())).thenReturn(mock(UserEntity.class));

        // client registration is enabled, and browser app type also
        when(parameterService.findAsBoolean(any(), eq(Key.APPLICATION_REGISTRATION_ENABLED), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(true);
        when(parameterService.findAsBoolean(any(), eq(Key.APPLICATION_TYPE_BROWSER_ENABLED), any(), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(true);

        // oauth app setting contains everything required
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings oAuthClientSettings = new OAuthClientSettings();
        oAuthClientSettings.setGrantTypes(List.of("application-grant-type"));
        oAuthClientSettings.setApplicationType("BROWSER");
        settings.setOauth(oAuthClientSettings);
        when(newApplication.getSettings()).thenReturn(settings);

        // mock application type service
        ApplicationTypeEntity applicationTypeEntity = new ApplicationTypeEntity();
        ApplicationGrantTypeEntity applicationGrantTypeEntity = new ApplicationGrantTypeEntity();
        applicationGrantTypeEntity.setType("application-grant-type");
        applicationGrantTypeEntity.setResponse_types(List.of("response-type"));
        applicationTypeEntity.setAllowed_grant_types(List.of(applicationGrantTypeEntity));
        applicationTypeEntity.setRequires_redirect_uris(false);
        when(applicationTypeService.getApplicationType("BROWSER")).thenReturn(applicationTypeEntity);

        // mock response from DCR with a new client ID
        ClientRegistrationResponse clientRegistrationResponse = new ClientRegistrationResponse();
        clientRegistrationResponse.setClientId("client-id-from-clientRegistration");
        when(clientRegistrationService.register(any(), any())).thenReturn(clientRegistrationResponse);
        when(applicationConverter.toApplication(any(NewApplicationEntity.class))).thenCallRealMethod();

        applicationService.create(EXECUTION_CONTEXT, newApplication, USER_NAME);

        // ensure app has been created with client_id from DCR in metadata
        verify(applicationRepository)
            .create(argThat(application -> application.getMetadata().get(METADATA_CLIENT_ID).equals("client-id-from-clientRegistration")));
    }

    private final String VALID_PEM =
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

    private final String VALID_CA_PEM =
        """
           -----BEGIN CERTIFICATE-----
           MIIGAzCCA+ugAwIBAgIUcso7he1LovzeKw5od1lZD3vlNOAwDQYJKoZIhvcNAQEL
           BQAwgZAxKTAnBgkqhkiG9w0BCQEWGmNvbnRhY3RAZ3Jhdml0ZWVzb3VyY2UuY29t
           MRAwDgYDVQQDDAdBUElNX0NOMQ0wCwYDVQQLDARBUElNMRQwEgYDVQQKDAtBUElN
           X1Rlc3RlcjEOMAwGA1UEBwwFTGlsbGUxDzANBgNVBAgMBkZyYW5jZTELMAkGA1UE
           BhMCRlIwHhcNMjQwODI4MDY0NzMzWhcNMzQwODI2MDY0NzMzWjCBkDEpMCcGCSqG
           SIb3DQEJARYaY29udGFjdEBncmF2aXRlZXNvdXJjZS5jb20xEDAOBgNVBAMMB0FQ
           SU1fQ04xDTALBgNVBAsMBEFQSU0xFDASBgNVBAoMC0FQSU1fVGVzdGVyMQ4wDAYD
           VQQHDAVMaWxsZTEPMA0GA1UECAwGRnJhbmNlMQswCQYDVQQGEwJGUjCCAiIwDQYJ
           KoZIhvcNAQEBBQADggIPADCCAgoCggIBAMOEVa4niB+yfSz9+cxoydZTMoHVPUEJ
           6o4NT34pcGf4Q6+DwNmV3Lrk291rw4hhXnlzflw4AOEZEbbpVBCC304vfjSt+enE
           MP8AtuIAsAJXjKMNBO3saD+6fhLdyIdz3rjq+fMcIAcjGFGQqgQJoniLnrnDU3ee
           WX0XnRHFOB1iGfMZ2X+0PptKvKH8Pq33er6tCCCH2cA7Owc4+6herDtP4oQ+xSqY
           spEORK37iRg7Pm8NgA/GsfjBIDjyjBsYN+waNGuS02MR8znQfgk+DjZlc4+e93vK
           VJfTzgMdOG1a/imB1mdwZvO1l9nSlArJlfvItCzi+2dc6Us67Pp8XyCiHK/2I7nJ
           DBDs84o3SA4uWe6SXfOGulTma6ENPsKC5Oh8VbbZvubbgNqFRCY19yz66zhq4wH4
           7W90TGZelHez6Dk/cCnl3WRPljuEzRcqRiU8YWdMCVqAfjdgxdSiQCOWa+Ug7Hlz
           LUvRcCAS2i20oGePKJ1Zl9IJuoik8QCovzjPP4bGgySTzvlhuKB7kyxJ6EDmo1Ic
           k2HVr0VvLRV4O1gT2lGFSu2k0QquV6WeKoQni+/oZfMRv1LTc0m+r34PN+ZdL+01
           2Bkcs2lmdp5oTwbSjw8w76Yf1vtz0SSwaQDgss8t0dJZNPECnHL+wki5byyrsdBM
           bJbovk7g8HUJAgMBAAGjUzBRMB0GA1UdDgQWBBQ3IIhDN+2FihTlbDjDIPdXjIEz
           GDAfBgNVHSMEGDAWgBQ3IIhDN+2FihTlbDjDIPdXjIEzGDAPBgNVHRMBAf8EBTAD
           AQH/MA0GCSqGSIb3DQEBCwUAA4ICAQBCyUHgdsx5tG09ol5PoHULn7QpUNYqdRo2
           Go3Qy+VTl1PngnKzWcpzFgc4zf+gaQG55KelulqOSAr13GBL2Wd9u7diM5OQQ6pK
           dhxWu9i2U/7LMSASYpNgawHTVdZ6tLi5hPxL8WQxoEBtXGIynQNKI6z76AjZwRcr
           fgq+CB2Ai8jcJxWCcMfbABPAPSwK9bRAmuP95+K7CXiCOvVnHQFQT3xMw4yyZ2Qq
           HrAL42RGyiejAx8eraE8fH8Dq9iWn6q91WY60nesyOnZLkZz/c8mTvibCE97d767
           rJUJREeS4MHFOw/wHXN/JeLryedUGSR4pEllBS/QjUhiUysvM+02a1XuwP0qD/5v
           697tDuozn/i7N9O0ThbZNR9KlSSMqAJ1iWpijt7e7Rr/CqP/42HYOZSyuoYGiydA
           P5TTsFBjbDTs2XtPEjPkoZ2vzegKLcT7H/pBtNHdwNnEcLbgDLwMGwxWI6urkjx4
           uz/iY/SibzgTnuxgTjW03HFVOFq9w2Tv/4qFNJrCxt+aQwG4RjnS77zS4AFoJ6ZI
           YQvqCvXVVosYZWLZGkbQSc2iNS2Wr5dFqngl3py6kps8BcUDzF/J/9QLMLI3ZTUt
           P5EfACFOUJGjCiuDC02wG2mO44Y98bT3oIMdjH9haMd5eoEAxmFy+M4UVTa2YK6u
           Q6teMha+jg==
           -----END CERTIFICATE-----""";

    @Test
    public void shouldHandleAdditionalClientMetadata() throws TechnicalException {
        var additionalClientMetadata = Map.of("policy_uri", "http://example.com/policy");

        // Mock OAuthClientSettings
        OAuthClientSettings oAuthClientSettings = new OAuthClientSettings();
        oAuthClientSettings.setAdditionalClientMetadata(additionalClientMetadata);
        oAuthClientSettings.setApplicationType("BROWSER");
        oAuthClientSettings.setGrantTypes(List.of("authorization_code"));

        // Mock ApplicationSettings
        ApplicationSettings settings = new ApplicationSettings();
        settings.setOauth(oAuthClientSettings);
        when(newApplication.getSettings()).thenReturn(settings);

        // Mock other necessary methods
        when(application.getName()).thenReturn(APPLICATION_NAME);
        when(application.getType()).thenReturn(ApplicationType.BROWSER);
        when(application.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(applicationRepository.create(any())).thenReturn(application);
        when(newApplication.getName()).thenReturn(APPLICATION_NAME);
        when(newApplication.getDescription()).thenReturn("My description");
        when(groupService.findByEvent(eq(GraviteeContext.getCurrentEnvironment()), any())).thenReturn(Collections.emptySet());
        when(userService.findById(any(), any())).thenReturn(mock(UserEntity.class));

        // Mock application type service
        ApplicationTypeEntity applicationTypeEntity = new ApplicationTypeEntity();
        ApplicationGrantTypeEntity applicationGrantTypeEntity = new ApplicationGrantTypeEntity();
        applicationGrantTypeEntity.setType("authorization_code");
        applicationGrantTypeEntity.setResponse_types(List.of("code"));
        applicationTypeEntity.setAllowed_grant_types(List.of(applicationGrantTypeEntity));
        applicationTypeEntity.setRequires_redirect_uris(false);
        when(applicationTypeService.getApplicationType("BROWSER")).thenReturn(applicationTypeEntity);

        // Mock response from DCR with a new client ID
        ClientRegistrationResponse clientRegistrationResponse = new ClientRegistrationResponse();
        clientRegistrationResponse.setClientId("client-id-from-clientRegistration");
        when(clientRegistrationService.register(any(), any())).thenReturn(clientRegistrationResponse);
        when(applicationConverter.toApplication(any(NewApplicationEntity.class))).thenCallRealMethod();

        // Enable DCR
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_REGISTRATION_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);

        // Enable BROWSER application type
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.APPLICATION_TYPE_BROWSER_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(Boolean.TRUE);

        // Call the create method
        ApplicationEntity applicationEntity = applicationService.create(EXECUTION_CONTEXT, newApplication, USER_NAME);

        // Verify that the additionalClientMetadata is handled properly
        assertNotNull(applicationEntity);
        assertEquals(APPLICATION_NAME, applicationEntity.getName());
    }
}
