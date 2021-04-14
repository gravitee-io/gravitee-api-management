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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderType;
import io.gravitee.rest.api.model.configuration.identity.NewIdentityProviderEntity;
import java.util.Collections;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IdentityProvidersResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "configuration/identities";
    }

    @Test
    public void shouldCreate() {
        reset(identityProviderService);

        NewIdentityProviderEntity newIdentityProviderEntity = new NewIdentityProviderEntity();
        newIdentityProviderEntity.setName("my-idp-name");
        newIdentityProviderEntity.setType(IdentityProviderType.GITHUB);
        newIdentityProviderEntity.setConfiguration(Collections.emptyMap());

        IdentityProviderEntity createdIdentityProvider = new IdentityProviderEntity();
        createdIdentityProvider.setId("my-idp-id");
        doReturn(createdIdentityProvider).when(identityProviderService).create(any());

        final Response response = target().request().post(Entity.json(newIdentityProviderEntity));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(target().path("my-idp-id").getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }
}
