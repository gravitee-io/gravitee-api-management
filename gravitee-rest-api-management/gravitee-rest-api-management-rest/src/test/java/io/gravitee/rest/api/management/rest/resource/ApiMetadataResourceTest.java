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

import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.NewApiMetadataEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiMetadataResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";

    @Override
    protected String contextPath() {
        return "apis";
    }

    @Before
    public void init() {
        Mockito.reset(apiMetadataService, searchEngineService);
    }

    @Test
    public void shouldCreateMetadata() {
        NewApiMetadataEntity newMetadata = new NewApiMetadataEntity();
        newMetadata.setName("my-metadata-name");

        ApiMetadataEntity createdMetadata = new ApiMetadataEntity();
        createdMetadata.setKey("my-metadata-id");
        when(apiMetadataService.create(any())).thenReturn(createdMetadata);

        final Response response = envTarget().path(API).path("metadata").request().post(Entity.json(newMetadata));
        assertEquals(CREATED_201, response.getStatus());
        assertEquals(envTarget().path(API).path("metadata").path("my-metadata-id").getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
        verify(searchEngineService, times(1)).index(any(), eq(false));
    }

    @Test
    public void shouldDeleteMetadata() {

        String metadata = "my-metadata";

        Response response =
                envTarget()
                .path(API)
                .path("metadata")
                .path(metadata)
                .request()
                .delete();
        assertEquals(NO_CONTENT_204, response.getStatus());
        verify(searchEngineService, times(1)).index(any(), eq(false));
    }
}
