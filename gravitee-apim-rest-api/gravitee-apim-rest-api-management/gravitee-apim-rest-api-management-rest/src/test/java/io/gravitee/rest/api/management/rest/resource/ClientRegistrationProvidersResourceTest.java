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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.configuration.application.registration.ClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.configuration.application.registration.InitialAccessTokenType;
import io.gravitee.rest.api.model.configuration.application.registration.NewClientRegistrationProviderEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ClientRegistrationProvidersResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "configuration/applications/registration/providers";
    }

    @Test
    public void shouldCreateSubscription() {
        reset(clientRegistrationService);
        NewClientRegistrationProviderEntity newClientRegistrationProviderEntity = new NewClientRegistrationProviderEntity();
        newClientRegistrationProviderEntity.setName("my-client-registration-provider-name");
        newClientRegistrationProviderEntity.setDiscoveryEndpoint("my-client-registration-provider-discovery-endpoint");
        newClientRegistrationProviderEntity.setInitialAccessTokenType(InitialAccessTokenType.INITIAL_ACCESS_TOKEN);
        newClientRegistrationProviderEntity.setInitialAccessToken("my-client-registration-provider-initial-access-token");

        ClientRegistrationProviderEntity createdClientRegistrationProvider = new ClientRegistrationProviderEntity();
        createdClientRegistrationProvider.setId("my-client-registration-provider-id");
        when(clientRegistrationService.create(eq(GraviteeContext.getExecutionContext()), any()))
            .thenReturn(createdClientRegistrationProvider);

        final Response response = envTarget().request().post(Entity.json(newClientRegistrationProviderEntity));
        assertEquals(CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path("my-client-registration-provider-id").getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
    }
}
