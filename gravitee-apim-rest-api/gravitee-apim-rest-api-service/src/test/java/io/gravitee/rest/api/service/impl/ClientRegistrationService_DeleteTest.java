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

import static io.gravitee.repository.management.model.ClientRegistrationProvider.AuditEvent.CLIENT_REGISTRATION_PROVIDER_DELETED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import io.gravitee.rest.api.model.configuration.application.registration.NewClientRegistrationProviderEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.configuration.application.registration.ClientRegistrationProviderNotFoundException;
import io.gravitee.rest.api.service.impl.configuration.application.registration.ClientRegistrationServiceImpl;
import io.gravitee.rest.api.service.impl.configuration.application.registration.InvalidRenewClientSecretException;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientRegistrationService_DeleteTest {

    @InjectMocks
    private ClientRegistrationServiceImpl clientRegistrationService = new ClientRegistrationServiceImpl();

    @Mock
    private ClientRegistrationProviderRepository mockClientRegistrationProviderRepository;

    @Mock
    private AuditService mockAuditService;

    @Test
    public void shouldDeleteProvider() throws TechnicalException {
        ClientRegistrationProvider existingPayload = new ClientRegistrationProvider();
        existingPayload.setId("CRP_ID");
        when(mockClientRegistrationProviderRepository.findById(eq(existingPayload.getId()))).thenReturn(Optional.of(existingPayload));

        clientRegistrationService.delete(GraviteeContext.getExecutionContext(), existingPayload.getId());

        verify(mockAuditService, times(1))
            .createAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                eq(CLIENT_REGISTRATION_PROVIDER_DELETED),
                any(),
                any(),
                isNull()
            );
        verify(mockClientRegistrationProviderRepository, times(1)).delete(existingPayload.getId());
    }

    @Test(expected = ClientRegistrationProviderNotFoundException.class)
    public void shouldThrowClientRegistrationProviderNotFoundException() {
        clientRegistrationService.delete(GraviteeContext.getExecutionContext(), "providerId");
    }
}
