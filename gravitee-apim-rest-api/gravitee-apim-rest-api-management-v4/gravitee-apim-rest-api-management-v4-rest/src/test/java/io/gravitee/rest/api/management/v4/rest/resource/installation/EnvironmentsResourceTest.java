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
package io.gravitee.rest.api.management.v4.rest.resource.installation;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.management.v4.rest.model.Environment;
import io.gravitee.rest.api.management.v4.rest.model.ErrorEntity;
import io.gravitee.rest.api.management.v4.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "/environments";
    }

    @Before
    public void init() {
        reset(environmentService);
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldReturnEnvironments() {
        EnvironmentEntity env1 = new EnvironmentEntity();
        env1.setId("my-env-id");

        EnvironmentEntity env2 = new EnvironmentEntity();
        env2.setId("my-env-id-2");

        when(environmentService.findByOrganization(GraviteeContext.getCurrentOrganization())).thenReturn(List.of(env1, env2));

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var body = response.readEntity(Collection.class);
        assertNotNull(body);
        assertEquals(2, body.size());
    }

    @Test
    public void shouldReturnEmptyList() {
        when(environmentService.findByOrganization(GraviteeContext.getCurrentOrganization())).thenReturn(List.of());

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var body = response.readEntity(Collection.class);
        assertNotNull(body);
        assertEquals(0, body.size());
    }

    @Test
    public void shouldFailWithTechnicalException() {
        doThrow(new TechnicalManagementException("oops"))
            .when(environmentService)
            .findByOrganization(GraviteeContext.getCurrentOrganization());

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }
}
