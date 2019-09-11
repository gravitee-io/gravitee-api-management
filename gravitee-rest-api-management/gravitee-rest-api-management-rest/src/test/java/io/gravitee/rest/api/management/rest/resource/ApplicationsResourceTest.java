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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.JerseySpringTest;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationsResourceTest extends AbstractResourceTest {

    protected String contextPath() {
        return "applications";
    }

    @Test
    public void shouldNotCreateApplication_noContent() {
        final Response response = target().request().post(null);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotCreateApplication_emptyName() {
        final NewApplicationEntity appEntity = new NewApplicationEntity();
        appEntity.setName("");
        appEntity.setDescription("my description");

        ApplicationEntity returnedApp = new ApplicationEntity();
        returnedApp.setId("my-beautiful-application");
        doReturn(returnedApp).when(applicationService).create(Mockito.any(NewApplicationEntity.class),
                Mockito.eq(JerseySpringTest.USER_NAME));

        final Response response = target().request().post(Entity.json(appEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }
}
