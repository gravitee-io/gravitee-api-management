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
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.configuration.identity.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IdentityProvidersResourceTest extends AbstractResourceTest {

    private static final String ID = "my-idp-id";

    @Inject
    private LicenseManager licenseManager;

    @Override
    protected String contextPath() {
        return "configuration/identities/";
    }

    private License license;

    @BeforeEach
    public void init() {
        reset(identityProviderService);

        license = mock(License.class);
        when(licenseManager.getPlatformLicense()).thenReturn(license);

        IdentityProviderEntity createdIdentityProvider = new IdentityProviderEntity();
        createdIdentityProvider.setId(ID);
        doReturn(createdIdentityProvider).when(identityProviderService).create(eq(GraviteeContext.getExecutionContext()), any());

        IdentityProviderEntity updatedIdentityProvider = new IdentityProviderEntity();
        updatedIdentityProvider.setId(ID);
        doReturn(updatedIdentityProvider).when(identityProviderService).update(eq(GraviteeContext.getExecutionContext()), eq(ID), any());
    }

    @Test
    public void should_allow_create_idp_github_without_checking_feature() {
        NewIdentityProviderEntity newIdentityProviderEntity = new NewIdentityProviderEntity();
        newIdentityProviderEntity.setName("my-idp-name");
        newIdentityProviderEntity.setType(IdentityProviderType.GITHUB);
        newIdentityProviderEntity.setConfiguration(Collections.emptyMap());

        final Response response = envTarget().request().post(Entity.json(newIdentityProviderEntity));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(envTarget().path(ID).getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));

        verify(license, never()).isFeatureEnabled("apim-openid-connect-sso");
    }

    @Test
    public void should_allow_create_idp_oidc_with_openid_connect_feature() {
        when(license.isFeatureEnabled("apim-openid-connect-sso")).thenReturn(true);

        NewIdentityProviderEntity newIdentityProviderEntity = new NewIdentityProviderEntity();
        newIdentityProviderEntity.setName("my-idp-name");
        newIdentityProviderEntity.setType(IdentityProviderType.OIDC);
        newIdentityProviderEntity.setConfiguration(Collections.emptyMap());

        final Response response = envTarget().request().post(Entity.json(newIdentityProviderEntity));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(envTarget().path(ID).getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    public void should_forbid_create_idp_oidc_without_openid_connect_feature() {
        when(license.isFeatureEnabled("apim-openid-connect-sso")).thenReturn(false);

        NewIdentityProviderEntity newIdentityProviderEntity = new NewIdentityProviderEntity();
        newIdentityProviderEntity.setName("my-idp-name");
        newIdentityProviderEntity.setType(IdentityProviderType.OIDC);
        newIdentityProviderEntity.setConfiguration(Collections.emptyMap());

        final Response response = envTarget().request().post(Entity.json(newIdentityProviderEntity));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_omit_client_secret_from_get_response() {
        Map<String, Object> configuration = configurationWithSecret();
        IdentityProviderEntity entity = new IdentityProviderEntity();
        entity.setId(ID);
        entity.setOrganization(GraviteeContext.getCurrentOrganization());
        entity.setConfiguration(configuration);
        when(identityProviderService.findById(ID)).thenReturn(entity);

        final Response response = envTarget(ID).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertClientSecretOmitted(response, configuration);
    }

    @Test
    public void should_omit_client_secret_from_create_response() {
        Map<String, Object> configuration = configurationWithSecret();
        IdentityProviderEntity created = new IdentityProviderEntity();
        created.setId(ID);
        created.setConfiguration(configuration);
        doReturn(created).when(identityProviderService).create(eq(GraviteeContext.getExecutionContext()), any());

        NewIdentityProviderEntity newIdentityProviderEntity = new NewIdentityProviderEntity();
        newIdentityProviderEntity.setName("my-idp-name");
        newIdentityProviderEntity.setType(IdentityProviderType.GITHUB);
        newIdentityProviderEntity.setConfiguration(Collections.emptyMap());

        final Response response = envTarget().request().post(Entity.json(newIdentityProviderEntity));

        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertClientSecretOmitted(response, configuration);
    }

    @Test
    public void should_omit_client_secret_from_update_response() {
        Map<String, Object> configuration = configurationWithSecret();
        IdentityProviderEntity updated = new IdentityProviderEntity();
        updated.setId(ID);
        updated.setConfiguration(configuration);
        doReturn(updated).when(identityProviderService).update(eq(GraviteeContext.getExecutionContext()), eq(ID), any());

        final Response response = envTarget(ID).request().put(Entity.json(buildIdpForUpdate()));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertClientSecretOmitted(response, configuration);
    }

    @Test
    public void should_update_idp() {
        UpdateIdentityProviderEntity updateIdentityProviderEntity = buildIdpForUpdate();

        final Response response = envTarget(ID).request().put(Entity.json(updateIdentityProviderEntity));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void should_not_update_idp_because_when_group_mapping_is_empty() {
        UpdateIdentityProviderEntity updateIdentityProviderEntity = buildIdpForUpdate();
        final GroupMappingEntity groupMappingEntity = new GroupMappingEntity();
        groupMappingEntity.setCondition("true");
        updateIdentityProviderEntity.setGroupMappings(Collections.singletonList(groupMappingEntity));

        final Response response = envTarget(ID).request().put(Entity.json(updateIdentityProviderEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void should_not_update_idp_because_when_role_mapping_is_empty() {
        UpdateIdentityProviderEntity updateIdentityProviderEntity = buildIdpForUpdate();
        final RoleMappingEntity roleMappingEntity = new RoleMappingEntity();
        roleMappingEntity.setCondition("true");
        updateIdentityProviderEntity.setRoleMappings(Collections.singletonList(roleMappingEntity));

        final Response response = envTarget(ID).request().put(Entity.json(updateIdentityProviderEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    private static Map<String, Object> configurationWithSecret() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("clientId", "client-id");
        configuration.put("clientSecret", "stored-secret");
        return configuration;
    }

    private static void assertClientSecretOmitted(Response response, Map<String, Object> storedConfiguration) {
        JsonNode configuration = response.readEntity(JsonNode.class).get("configuration");
        assertEquals("client-id", configuration.get("clientId").asText());
        assertFalse(configuration.has("clientSecret"));
        assertEquals("stored-secret", storedConfiguration.get("clientSecret"));
    }

    @NotNull
    private UpdateIdentityProviderEntity buildIdpForUpdate() {
        UpdateIdentityProviderEntity updateIdentityProviderEntity = new UpdateIdentityProviderEntity();
        updateIdentityProviderEntity.setName("my-idp-name");
        updateIdentityProviderEntity.setConfiguration(Collections.emptyMap());
        return updateIdentityProviderEntity;
    }
}
