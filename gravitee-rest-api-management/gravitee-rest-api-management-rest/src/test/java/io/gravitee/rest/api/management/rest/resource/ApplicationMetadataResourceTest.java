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
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.NewApplicationMetadataEntity;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationMetadataResourceTest extends AbstractResourceTest {

    private static final String APPLICATION = "my-application";

    @Override
    protected String contextPath() {
        return "applications";
    }

    @Test
    public void shouldCreateMetadata() {
        Mockito.reset(applicationMetadataService);

        NewApplicationMetadataEntity newMetadata = new NewApplicationMetadataEntity();
        newMetadata.setName("my-metadata-name");

        ApplicationMetadataEntity createdMetadata = new ApplicationMetadataEntity();
        createdMetadata.setKey("my-metadata-id");
        when(applicationMetadataService.create(any())).thenReturn(createdMetadata);

        final Response response = envTarget().path(APPLICATION).path("metadata").request().post(Entity.json(newMetadata));
        assertEquals(CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path(APPLICATION).path("metadata").path("my-metadata-id").getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
    }
}
