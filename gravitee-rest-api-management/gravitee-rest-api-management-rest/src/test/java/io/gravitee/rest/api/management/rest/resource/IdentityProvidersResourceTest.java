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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.configuration.identity.*;
import java.util.Collections;
import javax.management.relation.Role;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IdentityProvidersResourceTest extends AbstractResourceTest {

    private static final String ID = "my-idp-id";

    @Override
    protected String contextPath() {
        return "configuration/identities/";
    }

    @Before
    public void init() {
        reset(identityProviderService);

        IdentityProviderEntity createdIdentityProvider = new IdentityProviderEntity();
        createdIdentityProvider.setId(ID);
        doReturn(createdIdentityProvider).when(identityProviderService).create(any());

        IdentityProviderEntity updatedIdentityProvider = new IdentityProviderEntity();
        updatedIdentityProvider.setId(ID);
        doReturn(updatedIdentityProvider).when(identityProviderService).update(eq(ID), any());
    }

    @Test
    public void shouldCreate() {
        NewIdentityProviderEntity newIdentityProviderEntity = new NewIdentityProviderEntity();
        newIdentityProviderEntity.setName("my-idp-name");
        newIdentityProviderEntity.setType(IdentityProviderType.GITHUB);
        newIdentityProviderEntity.setConfiguration(Collections.emptyMap());

        final Response response = envTarget().request().post(Entity.json(newIdentityProviderEntity));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(envTarget().path(ID).getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    public void shouldUpdate() {
        UpdateIdentityProviderEntity updateIdentityProviderEntity = buildIdpForUpdate();

        final Response response = envTarget(ID).request().put(Entity.json(updateIdentityProviderEntity));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldNotUpdateBecauseOfGroupMappingEmpty() {
        UpdateIdentityProviderEntity updateIdentityProviderEntity = buildIdpForUpdate();
        final GroupMappingEntity groupMappingEntity = new GroupMappingEntity();
        groupMappingEntity.setCondition("true");
        updateIdentityProviderEntity.setGroupMappings(Collections.singletonList(groupMappingEntity));

        final Response response = envTarget(ID).request().put(Entity.json(updateIdentityProviderEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotUpdateBecauseOfRoleMappingEmpty() {
        UpdateIdentityProviderEntity updateIdentityProviderEntity = buildIdpForUpdate();
        final RoleMappingEntity roleMappingEntity = new RoleMappingEntity();
        roleMappingEntity.setCondition("true");
        updateIdentityProviderEntity.setRoleMappings(Collections.singletonList(roleMappingEntity));

        final Response response = envTarget(ID).request().put(Entity.json(updateIdentityProviderEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @NotNull
    private UpdateIdentityProviderEntity buildIdpForUpdate() {
        UpdateIdentityProviderEntity updateIdentityProviderEntity = new UpdateIdentityProviderEntity();
        updateIdentityProviderEntity.setName("my-idp-name");
        updateIdentityProviderEntity.setConfiguration(Collections.emptyMap());
        return updateIdentityProviderEntity;
    }
}
