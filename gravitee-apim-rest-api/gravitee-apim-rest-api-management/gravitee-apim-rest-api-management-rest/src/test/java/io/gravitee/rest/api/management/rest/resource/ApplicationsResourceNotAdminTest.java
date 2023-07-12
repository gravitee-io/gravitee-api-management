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

import io.gravitee.common.http.HttpStatusCode;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class ApplicationsResourceNotAdminTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "applications";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAdminAuthenticationFilter.class);
    }

    @Test
    public void shouldNotGetArchivedApplications() {
        final Response response = envTarget().queryParam("status", "ARCHIVED").request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldGetArchivedApplicationsPaged() {
        final Response response = envTarget("/_paged").queryParam("status", "ARCHIVED").request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }
}
