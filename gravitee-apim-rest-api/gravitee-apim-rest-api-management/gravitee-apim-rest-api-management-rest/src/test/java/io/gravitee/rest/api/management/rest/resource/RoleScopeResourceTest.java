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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.NewRoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RoleScopeResourceTest extends AbstractResourceTest {

    private static final String SCOPE = "API";

    @Inject
    private LicenseManager licenseManager;

    private License license;

    @Before
    public void init() {
        license = mock(License.class);
        when(licenseManager.getPlatformLicense()).thenReturn(license);
    }

    @Override
    protected String contextPath() {
        return "configuration/rolescopes/";
    }

    @Test
    public void should_forbid_create_role_without_custom_role_feature() {
        NewRoleEntity role = new NewRoleEntity();
        role.setName("sre");
        role.setScope(RoleScope.API);

        when(license.isFeatureEnabled("apim-custom-roles")).thenReturn(false);

        final Response response = envTarget(SCOPE).path("roles").request().post(Entity.json(role));

        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_allow_create_role_with_custom_role_feature() {
        when(license.isFeatureEnabled("apim-custom-roles")).thenReturn(true);
        when(roleService.create(any(), any())).thenReturn(new io.gravitee.rest.api.model.RoleEntity());

        NewRoleEntity role = new NewRoleEntity();
        role.setName("sre");
        role.setScope(RoleScope.API);

        final Response response = envTarget(SCOPE).path("roles").request().post(Entity.json(role));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }
}
