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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.gravitee.repository.management.model.ClientRegistrationProvider.AuditEvent.CLIENT_REGISTRATION_PROVIDER_UPDATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.configuration.application.registration.ClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.configuration.application.registration.InitialAccessTokenType;
import io.gravitee.rest.api.model.configuration.application.registration.UpdateClientRegistrationProviderEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.configuration.application.registration.ClientRegistrationProviderNotFoundException;
import io.gravitee.rest.api.service.impl.configuration.application.registration.ClientRegistrationServiceImpl;
import io.gravitee.rest.api.service.impl.configuration.application.registration.EmptyInitialAccessTokenException;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.register.ClientRegistrationResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientRegistrationService_UpdateTest {

    @InjectMocks
    private ClientRegistrationServiceImpl clientRegistrationService = new ClientRegistrationServiceImpl();

    @Mock
    private ClientRegistrationProviderRepository mockClientRegistrationProviderRepository;

    @Mock
    private AuditService mockAuditService;

    private final WireMockServer wireMockServer = new WireMockServer(Options.DYNAMIC_PORT);

    @Before
    public void setup() {
        wireMockServer.start();
    }

    @After
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void shouldUpdateProvider() throws TechnicalException {
        UpdateClientRegistrationProviderEntity providerPayload = new UpdateClientRegistrationProviderEntity();
        providerPayload.setName("name");
        providerPayload.setDiscoveryEndpoint("http://localhost:" + wireMockServer.port() + "/am");

        ClientRegistrationProvider existingPayload = new ClientRegistrationProvider();
        existingPayload.setId("CRP_ID");
        existingPayload.setEnvironmentId(GraviteeContext.getCurrentEnvironment());

        when(mockClientRegistrationProviderRepository.findById(eq(existingPayload.getId()))).thenReturn(Optional.of(existingPayload));

        wireMockServer.stubFor(
            get(urlEqualTo("/am"))
                .willReturn(aResponse().withBody("{\"token_endpoint\": \"tokenEp\",\"registration_endpoint\": \"registrationEp\"}"))
        );

        ClientRegistrationProvider providerUpdatedMock = new ClientRegistrationProvider();
        providerUpdatedMock.setId(existingPayload.getId());
        providerUpdatedMock.setName(existingPayload.getName());
        when(
            mockClientRegistrationProviderRepository.update(
                argThat(p ->
                    Objects.equals(p.getId(), existingPayload.getId()) &&
                    Objects.equals(p.getEnvironmentId(), GraviteeContext.getExecutionContext().getEnvironmentId()) &&
                    Objects.equals(p.getName(), providerPayload.getName()) &&
                    p.getUpdatedAt() != null
                )
            )
        )
            .thenReturn(providerUpdatedMock);

        ClientRegistrationProviderEntity providerUpdated = clientRegistrationService.update(
            GraviteeContext.getExecutionContext(),
            existingPayload.getId(),
            providerPayload
        );
        assertNotNull("Result is null", providerUpdated);

        verify(mockAuditService, times(1))
            .createAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                eq(CLIENT_REGISTRATION_PROVIDER_UPDATED),
                any(),
                any(),
                any()
            );
        verify(mockClientRegistrationProviderRepository, times(1)).update(any());
    }

    @Test(expected = ClientRegistrationProviderNotFoundException.class)
    public void shouldNotUpdateProviderBecauseDoesNotBelongToEnvironment() throws TechnicalException {
        UpdateClientRegistrationProviderEntity providerPayload = new UpdateClientRegistrationProviderEntity();
        providerPayload.setName("name");
        providerPayload.setDiscoveryEndpoint("http://localhost:" + wireMockServer.port() + "/am");

        ClientRegistrationProvider existingPayload = new ClientRegistrationProvider();
        existingPayload.setId("CRP_ID");
        existingPayload.setEnvironmentId("Another_environment");

        when(mockClientRegistrationProviderRepository.findById(eq(existingPayload.getId()))).thenReturn(Optional.of(existingPayload));

        clientRegistrationService.update(GraviteeContext.getExecutionContext(), existingPayload.getId(), providerPayload);

        verify(mockAuditService, never())
            .createAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                eq(CLIENT_REGISTRATION_PROVIDER_UPDATED),
                any(),
                any(),
                any()
            );
        verify(mockClientRegistrationProviderRepository, never()).update(any());
    }

    @Test(expected = EmptyInitialAccessTokenException.class)
    public void shouldThrowWithTypeInitialAccessTokenAndWithoutToken() throws TechnicalException {
        UpdateClientRegistrationProviderEntity providerPayload = new UpdateClientRegistrationProviderEntity();
        providerPayload.setInitialAccessTokenType(InitialAccessTokenType.INITIAL_ACCESS_TOKEN);

        ClientRegistrationProvider existingPayload = new ClientRegistrationProvider();
        existingPayload.setId("CRP_ID");
        existingPayload.setEnvironmentId(GraviteeContext.getCurrentEnvironment());

        when(mockClientRegistrationProviderRepository.findById(eq(existingPayload.getId()))).thenReturn(Optional.of(existingPayload));

        clientRegistrationService.update(GraviteeContext.getExecutionContext(), existingPayload.getId(), providerPayload);
    }

    @Test
    public void shouldUpdateApplication_withAdditionalClientMetadata() throws TechnicalException, JsonProcessingException {
        UpdateApplicationEntity updateApplicationEntity = new UpdateApplicationEntity();
        OAuthClientSettings oAuthClientSettings = new OAuthClientSettings();
        Map<String, String> additionalClientMetadata = new HashMap<>();
        additionalClientMetadata.put("policy_uri", "https://example.com/policy");
        oAuthClientSettings.setAdditionalClientMetadata(additionalClientMetadata);

        ApplicationSettings applicationSettings = new ApplicationSettings();
        applicationSettings.setOAuthClient(oAuthClientSettings);
        updateApplicationEntity.setSettings(applicationSettings);

        ClientRegistrationResponse existingPayload = new ClientRegistrationResponse();
        existingPayload.setId("CRP_ID");
        existingPayload.setRegistrationAccessToken("registrationAccessToken");
        existingPayload.setRegistrationClientUri("http://localhost:" + wireMockServer.port() + "/registration");

        wireMockServer.stubFor(
            put(urlEqualTo("/registration"))
                .willReturn(
                    aResponse()
                        .withBody(
                            "{\"client_id\": \"clientId\",\"client_secret\": \"clientSecret\", \"policy_uri\": \"https://example.com/policy\"}"
                        )
                )
        );

        ClientRegistrationProvider provider = new ClientRegistrationProvider();
        provider.setId(existingPayload.getId());
        provider.setName("name");
        provider.setDiscoveryEndpoint("http://localhost:" + wireMockServer.port() + "/am");

        when(mockClientRegistrationProviderRepository.findById(eq(existingPayload.getId()))).thenReturn(Optional.of(provider));
        when(mockClientRegistrationProviderRepository.findAllByEnvironment(eq(GraviteeContext.getExecutionContext().getEnvironmentId())))
            .thenReturn(newSet(provider));

        wireMockServer.stubFor(
            get(urlEqualTo("/am"))
                .willReturn(aResponse().withBody("{\"token_endpoint\": \"tokenEp\",\"registration_endpoint\": \"registrationEp\"}"))
        );

        ClientRegistrationProvider providerUpdatedMock = new ClientRegistrationProvider();
        providerUpdatedMock.setId(provider.getId());
        providerUpdatedMock.setName(provider.getName());
        when(
            mockClientRegistrationProviderRepository.update(
                argThat(p ->
                    p.getId().equals(provider.getId()) &&
                    p.getEnvironmentId().equals(GraviteeContext.getExecutionContext().getEnvironmentId()) &&
                    p.getName().equals(provider.getName()) &&
                    p.getUpdatedAt() != null
                )
            )
        )
            .thenReturn(providerUpdatedMock);

        ObjectMapper mapper = new ObjectMapper();

        ClientRegistrationResponse providerUpdated = clientRegistrationService.update(
            GraviteeContext.getExecutionContext(),
            mapper.writeValueAsString(existingPayload),
            updateApplicationEntity
        );
        assertNotNull("Result is null", providerUpdated);

        assertEquals("https://example.com/policy", providerUpdated.getPolicyUri());
    }
}
