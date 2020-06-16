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
import io.gravitee.rest.api.model.FetcherEntity;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud [at] graviteesource [dot] com) 
 * @author GraviteeSource Team
 */
public class FetcherResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "fetchers/my-id";
    }

    @Test
    public void shoudGetNoFetcherFromUnknownId() {
        Mockito.reset(fetcherService);
        when(fetcherService.findById("my-id")).thenReturn(null);

        final Response response = envTarget().request().get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
        assertThat(response.readEntity(String.class)).isEqualTo("");

        verify(fetcherService, times(1)).findById("my-id");
        verify(fetcherService, times(0)).getSchema(anyString());
    }

    @Test
    public void shouldGetFetcherWithoutSchema() {
        Mockito.reset(fetcherService);
        FetcherEntity fetcherEntity = new FetcherEntity();
        fetcherEntity.setId("my-id");

        when(fetcherService.findById("my-id")).thenReturn(fetcherEntity);
        when(fetcherService.getSchema(anyString())).thenReturn("schema");

        final Response response = envTarget().request().get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        Object o = response.readEntity(Object.class);
        assertThat(o).isNotNull();
        assertThat(o).isInstanceOf(LinkedHashMap.class);
        LinkedHashMap<String, String> elt = (LinkedHashMap<String, String>) o;
        assertThat(elt).hasSize(1);
        assertThat(elt.get("id")).isEqualTo("my-id");

        verify(fetcherService, times(1)).findById("my-id");
        verify(fetcherService, times(0)).getSchema(anyString());
    }

    @Test
    public void shouldGetFetcherSchema() {
        Mockito.reset(fetcherService);
        FetcherEntity fetcherEntity = new FetcherEntity();
        fetcherEntity.setId("my-id");

        when(fetcherService.findById("my-id")).thenReturn(fetcherEntity);
        when(fetcherService.getSchema(anyString())).thenReturn("my-schema");

        final Response response = envTarget().path("schema").request().get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        String o = response.readEntity(String.class);
        assertThat(o).isNotNull();
        assertThat(o).isEqualTo("my-schema");

        verify(fetcherService, times(1)).findById("my-id");
        verify(fetcherService, times(1)).getSchema(anyString());
    }
}
