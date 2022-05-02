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
import static io.gravitee.repository.management.model.ClientRegistrationProvider.AuditEvent.CLIENT_REGISTRATION_PROVIDER_CREATED;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import io.gravitee.rest.api.model.configuration.application.registration.ClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.configuration.application.registration.NewClientRegistrationProviderEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.configuration.application.registration.ClientRegistrationServiceImpl;
import java.util.Collections;
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
public class ClientRegistrationService_CreateTest {

    @InjectMocks
    private ClientRegistrationServiceImpl clientRegistrationService = new ClientRegistrationServiceImpl();

    @Mock
    private ClientRegistrationProviderRepository mockClientRegistrationProviderRepository;

    @Mock
    private AuditService mockAuditService;

    private WireMockServer wireMockServer = new WireMockServer();

    @Before
    public void setup() {
        wireMockServer.start();
    }

    @After
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void shouldCreateProvider() throws TechnicalException {
        NewClientRegistrationProviderEntity providerPayload = new NewClientRegistrationProviderEntity();
        providerPayload.setName("name");
        providerPayload.setDiscoveryEndpoint("http://localhost:8080/am");

        stubFor(
            get(urlEqualTo("/am"))
                .willReturn(aResponse().withBody("{\"token_endpoint\": \"tokenEp\",\"registration_endpoint\": \"registrationEp\"}"))
        );
        ClientRegistrationProvider providerCreatedMock = new ClientRegistrationProvider();
        when(
            mockClientRegistrationProviderRepository.create(
                argThat(
                    p ->
                        p.getEnvironmentId() == GraviteeContext.getExecutionContext().getEnvironmentId() &&
                        p.getName() == providerPayload.getName() &&
                        p.getCreatedAt() != null
                )
            )
        )
            .thenReturn(providerCreatedMock);

        ClientRegistrationProviderEntity providerCreated = clientRegistrationService.create(
            GraviteeContext.getExecutionContext(),
            providerPayload
        );
        assertNotNull("Result is null", providerCreated);

        verify(mockAuditService, times(1))
            .createAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                eq(CLIENT_REGISTRATION_PROVIDER_CREATED),
                any(),
                isNull(),
                any()
            );
        verify(mockClientRegistrationProviderRepository, times(1)).create(any());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotCreateMoreThanOneProvider() throws TechnicalException {
        when(mockClientRegistrationProviderRepository.findAll()).thenReturn(Collections.singleton(new ClientRegistrationProvider()));

        NewClientRegistrationProviderEntity providerPayload = new NewClientRegistrationProviderEntity();
        clientRegistrationService.create(GraviteeContext.getExecutionContext(), providerPayload);
    }
}
