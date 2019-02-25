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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.management.model.FetcherEntity;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;
/**
 * @author Nicolas GERAUD (nicolas.geraud [at] graviteesource [dot] com) 
 * @author GraviteeSource Team
 */
public class FetchersResourceTest extends AbstractResourceTest {
    protected String contextPath() {
        return "fetchers";
    }

    @Test
    public void shoudGetNoFetchersFromEmptyList() {
        Mockito.reset(fetcherService);
        when(fetcherService.findAll(false)).thenReturn(Collections.emptySet());

        final Response response = target().request().get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(response.readEntity(Set.class)).isEmpty();

        verify(fetcherService, times(1)).findAll(false);
        verify(fetcherService, times(0)).getSchema(anyString());
    }

    @Test
    public void shouldGetFetcherWithoutSchema() {
        Mockito.reset(fetcherService);
        FetcherEntity fetcherEntity = new FetcherEntity();
        fetcherEntity.setId("my-id");

        when(fetcherService.findAll(false)).thenReturn(Collections.singleton(fetcherEntity));
        when(fetcherService.getSchema(anyString())).thenReturn("schema");

        final Response response = target().request().get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        Set set = response.readEntity(Set.class);
        assertThat(set).isNotEmpty();
        assertThat(set).hasSize(1);
        Object o = set.iterator().next();
        assertThat(o).isNotNull();
        assertThat(o).isInstanceOf(LinkedHashMap.class);
        LinkedHashMap<String, String> elt = (LinkedHashMap<String, String>)o;
        assertThat(elt).hasSize(1);
        assertThat(elt.get("id")).isEqualTo("my-id");

        verify(fetcherService, times(1)).findAll(false);
        verify(fetcherService, times(0)).getSchema(anyString());
    }

    @Test
    public void shouldGetFetcherWithExpandSchema() {
        Mockito.reset(fetcherService);
        FetcherEntity fetcherEntity = new FetcherEntity();
        fetcherEntity.setId("my-id");

        when(fetcherService.findAll(false)).thenReturn(Collections.singleton(fetcherEntity));
        when(fetcherService.getSchema(anyString())).thenReturn("my-schema");

        final Response response = target().queryParam("expand", "schema").request().get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        Set set = response.readEntity(Set.class);
        assertThat(set).isNotEmpty();
        assertThat(set).hasSize(1);
        Object o = set.iterator().next();
        assertThat(o).isNotNull();
        assertThat(o).isInstanceOf(LinkedHashMap.class);
        LinkedHashMap<String, String> elt = (LinkedHashMap<String, String>)o;
        assertThat(elt).hasSize(2);
        assertThat(elt.get("id")).isEqualTo("my-id");
        assertThat(elt.get("schema")).isEqualTo("my-schema");

        verify(fetcherService, times(1)).findAll(false);
        verify(fetcherService, times(1)).getSchema(anyString());
    }

    @Test
    public void shouldGetFetcherWithUnknownExpand() {
        Mockito.reset(fetcherService);
        FetcherEntity fetcherEntity = new FetcherEntity();
        fetcherEntity.setId("my-id");

        when(fetcherService.findAll(false)).thenReturn(Collections.singleton(fetcherEntity));
        when(fetcherService.getSchema(anyString())).thenReturn("my-schema");

        final Response response = target().queryParam("expand", "unknown").request().get();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        Set set = response.readEntity(Set.class);
        assertThat(set).isNotEmpty();
        assertThat(set).hasSize(1);
        Object o = set.iterator().next();
        assertThat(o).isNotNull();
        assertThat(o).isInstanceOf(LinkedHashMap.class);
        LinkedHashMap<String, String> elt = (LinkedHashMap<String, String>)o;
        assertThat(elt).hasSize(1);
        assertThat(elt.get("id")).isEqualTo("my-id");

        verify(fetcherService, times(1)).findAll(false);
        verify(fetcherService, times(0)).getSchema(anyString());
    }
}
