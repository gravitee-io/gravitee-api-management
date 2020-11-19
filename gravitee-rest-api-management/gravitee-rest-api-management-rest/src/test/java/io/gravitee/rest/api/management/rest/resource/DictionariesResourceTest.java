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
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryType;
import io.gravitee.rest.api.model.configuration.dictionary.NewDictionaryEntity;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
public class DictionariesResourceTest extends AbstractResourceTest {

    protected String contextPath() {
        return "configuration/dictionaries";
    }

    @Test
    public void shouldNotCreateDictionary_noContent() {
        final Response response = target().request().post(null);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotCreateDictionary_emptyName() {
        reset(dictionaryService);

        final NewDictionaryEntity newDictionaryEntity = new NewDictionaryEntity();
        newDictionaryEntity.setName(null);
        newDictionaryEntity.setType(DictionaryType.MANUAL);

        DictionaryEntity returnedDictionary = new DictionaryEntity();
        returnedDictionary.setId("my-dictionary");
        doReturn(returnedDictionary).when(dictionaryService).create(any());

        final Response response = target().request().post(Entity.json(new NewDictionaryEntity()));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldCreateApi() {
        reset(dictionaryService);

        final NewDictionaryEntity newDictionaryEntity = new NewDictionaryEntity();
        newDictionaryEntity.setDescription("description");
        newDictionaryEntity.setName("my-dictionary-name");
        newDictionaryEntity.setType(DictionaryType.MANUAL);

        DictionaryEntity returnedDictionary = new DictionaryEntity();
        returnedDictionary.setId("my-dictionary");
        doReturn(returnedDictionary).when(dictionaryService).create(any());

        final Response response = target().request().post(Entity.json(newDictionaryEntity));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(target().path("my-dictionary").getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }
}
