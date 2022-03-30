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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationResourceAdminTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "applications/my-app/";
    }

    @Test
    public void shouldRestoreApplication() {
        reset(applicationService);

        ApplicationEntity restored = new ApplicationEntity();
        restored.setId("my-beautiful-application");
        doReturn(restored).when(applicationService).restore(eq(GraviteeContext.getExecutionContext()), any());

        final Response response = envTarget("_restore").request().post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertEquals(response.readEntity(ApplicationEntity.class).getId(), "my-beautiful-application");
    }
}
