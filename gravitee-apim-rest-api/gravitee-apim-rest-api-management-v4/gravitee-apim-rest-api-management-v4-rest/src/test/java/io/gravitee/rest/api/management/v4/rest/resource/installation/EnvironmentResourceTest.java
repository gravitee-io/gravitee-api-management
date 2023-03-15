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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.gravitee.rest.api.management.v4.rest.model.Environment;
import io.gravitee.rest.api.management.v4.rest.model.ErrorEntity;
import io.gravitee.rest.api.management.v4.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

public class EnvironmentResourceTest extends AbstractResourceTest {

    public static final String FAKE_ENVIRONMENT_ID = "fake-environment";

    @Override
    protected String contextPath() {
        return "/environments";
    }

    @Before
    public void init() {
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentEnvironment(FAKE_ENVIRONMENT_ID);
    }

    @Test
    public void shouldReturnEnvironmentWithId() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId("my-env-id");
        env.setName("env-name");
        env.setOrganizationId("DEFAULT");
        env.setDescription("A nice description");
        env.setHrids(List.of("hrid-1"));
        env.setCockpitId("cockpit-id");
        env.setDomainRestrictions(List.of("restriction-1"));

        doReturn(env).when(environmentService).findById(eq("my-env-id"));

        final Response response = rootTarget("my-env-id").request().get();
        assertEquals(200, response.getStatus());

        Environment body = response.readEntity(Environment.class);
        assertNotNull(body);
        assertEquals("my-env-id", body.getId());
        assertEquals("env-name", body.getName());
        assertEquals("A nice description", body.getDescription());
    }

    @Test
    public void shouldReturn404WhenEnvNotFound() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId("my-env-id");

        doThrow(new EnvironmentNotFoundException("my-env-id")).when(environmentService).findById(eq("my-env-id"));

        final Response response = rootTarget("my-env-id").request().get();
        assertEquals(404, response.getStatus());

        var body = response.readEntity(ErrorEntity.class);
        assertNotNull(body);
        assertEquals(404, body.getHttpStatus());
    }
}
