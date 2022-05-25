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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.gravitee.repository.management.model.ClientRegistrationProvider.AuditEvent.CLIENT_REGISTRATION_PROVIDER_UPDATED;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import io.gravitee.rest.api.model.configuration.application.registration.ClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.configuration.application.registration.InitialAccessTokenType;
import io.gravitee.rest.api.model.configuration.application.registration.UpdateClientRegistrationProviderEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.configuration.application.registration.ClientRegistrationServiceImpl;
import io.gravitee.rest.api.service.impl.configuration.application.registration.EmptyInitialAccessTokenException;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.event.annotation.AfterTestMethod;

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
                argThat(
                    p ->
                        p.getId() == existingPayload.getId() &&
                        p.getEnvironmentId() == GraviteeContext.getExecutionContext().getEnvironmentId() &&
                        p.getName() == providerPayload.getName() &&
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

    @Test(expected = EmptyInitialAccessTokenException.class)
    public void shouldThrowWithTypeInitialAccessTokenAndWithoutToken() throws TechnicalException {
        UpdateClientRegistrationProviderEntity providerPayload = new UpdateClientRegistrationProviderEntity();
        providerPayload.setInitialAccessTokenType(InitialAccessTokenType.INITIAL_ACCESS_TOKEN);

        ClientRegistrationProvider existingPayload = new ClientRegistrationProvider();
        existingPayload.setId("CRP_ID");

        when(mockClientRegistrationProviderRepository.findById(eq(existingPayload.getId()))).thenReturn(Optional.of(existingPayload));

        clientRegistrationService.update(GraviteeContext.getExecutionContext(), existingPayload.getId(), providerPayload);
    }
}
