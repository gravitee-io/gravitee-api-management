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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.gravitee.repository.management.model.ClientRegistrationProvider.AuditEvent.CLIENT_REGISTRATION_PROVIDER_CREATED;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import io.gravitee.rest.api.model.configuration.application.registration.ClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.configuration.application.registration.InitialAccessTokenType;
import io.gravitee.rest.api.model.configuration.application.registration.NewClientRegistrationProviderEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.configuration.application.registration.ClientRegistrationServiceImpl;
import io.gravitee.rest.api.service.impl.configuration.application.registration.EmptyInitialAccessTokenException;
import io.gravitee.rest.api.service.impl.configuration.application.registration.InvalidRenewClientSecretException;
import java.util.Collections;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ClientRegistrationService_CreateTest {

    @InjectMocks
    private ClientRegistrationServiceImpl clientRegistrationService = new ClientRegistrationServiceImpl();

    @Mock
    private ClientRegistrationProviderRepository mockClientRegistrationProviderRepository;

    @Mock
    private AuditService mockAuditService;

    private final WireMockServer wireMockServer = new WireMockServer(Options.DYNAMIC_PORT);

    @BeforeEach
    public void setup() {
        wireMockServer.start();
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void shouldCreateProvider() throws TechnicalException {
        NewClientRegistrationProviderEntity providerPayload = new NewClientRegistrationProviderEntity();
        providerPayload.setName("name");
        providerPayload.setDiscoveryEndpoint("http://localhost:" + wireMockServer.port() + "/am");

        wireMockServer.stubFor(
            get(urlEqualTo("/am")).willReturn(
                aResponse().withBody("{\"token_endpoint\": \"tokenEp\",\"registration_endpoint\": \"registrationEp\"}")
            )
        );
        ClientRegistrationProvider providerCreatedMock = new ClientRegistrationProvider();
        when(
            mockClientRegistrationProviderRepository.create(
                argThat(
                    p ->
                        Objects.equals(p.getEnvironmentId(), GraviteeContext.getExecutionContext().getEnvironmentId()) &&
                        Objects.equals(p.getName(), providerPayload.getName()) &&
                        p.getCreatedAt() != null
                )
            )
        ).thenReturn(providerCreatedMock);

        ClientRegistrationProviderEntity providerCreated = clientRegistrationService.create(
            GraviteeContext.getExecutionContext(),
            providerPayload
        );
        assertNotNull(providerCreated, "Result is null");

        verify(mockAuditService, times(1)).createAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData -> auditLogData.getEvent().equals(CLIENT_REGISTRATION_PROVIDER_CREATED) && auditLogData.getOldValue() == null
            )
        );
        verify(mockClientRegistrationProviderRepository, times(1)).create(any());
    }

    @Test
    public void shouldNotCreateMoreThanOneProvider() throws TechnicalException {
        assertThrows(IllegalStateException.class, () -> {
            when(
                mockClientRegistrationProviderRepository.findAllByEnvironment(eq(GraviteeContext.getExecutionContext().getEnvironmentId()))
            ).thenReturn(Collections.singleton(new ClientRegistrationProvider()));

            NewClientRegistrationProviderEntity providerPayload = new NewClientRegistrationProviderEntity();
            clientRegistrationService.create(GraviteeContext.getExecutionContext(), providerPayload);
        });
    }

    @Test
    public void shouldThrowWithTypeInitialAccessTokenAndWithoutToken() {
        assertThrows(EmptyInitialAccessTokenException.class, () -> {
            NewClientRegistrationProviderEntity providerPayload = new NewClientRegistrationProviderEntity();
            providerPayload.setInitialAccessTokenType(InitialAccessTokenType.INITIAL_ACCESS_TOKEN);
            clientRegistrationService.create(GraviteeContext.getExecutionContext(), providerPayload);
        });
    }

    @Test
    public void shouldThrowWithBadRenewClientSecretMethod() {
        assertThrows(InvalidRenewClientSecretException.class, () -> {
            NewClientRegistrationProviderEntity providerPayload = new NewClientRegistrationProviderEntity();
            providerPayload.setName("name");
            providerPayload.setRenewClientSecretSupport(true);
            providerPayload.setRenewClientSecretMethod("DELETE");
            clientRegistrationService.create(GraviteeContext.getExecutionContext(), providerPayload);
        });
    }

    @Test
    public void shouldThrowWithBadRenewClientSecretEndpoint() {
        assertThrows(InvalidRenewClientSecretException.class, () -> {
            NewClientRegistrationProviderEntity providerPayload = new NewClientRegistrationProviderEntity();
            providerPayload.setName("name");
            providerPayload.setRenewClientSecretSupport(true);
            providerPayload.setRenewClientSecretMethod("POST");
            providerPayload.setRenewClientSecretEndpoint("notStartWithHttp");
            clientRegistrationService.create(GraviteeContext.getExecutionContext(), providerPayload);
        });
    }
}
