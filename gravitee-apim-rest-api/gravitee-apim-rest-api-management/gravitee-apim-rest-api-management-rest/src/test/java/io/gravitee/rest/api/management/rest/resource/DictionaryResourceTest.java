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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.resource.param.LifecycleAction;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import org.junit.Test;

public class DictionaryResourceTest extends AbstractResourceTest {

    private static final String DICTIONARY_ID = "040f6a20-9fc2-429f-8f6a-209fc2629f8d";

    protected String contextPath() {
        return "configuration/dictionaries/" + DICTIONARY_ID;
    }

    @Test
    public void shouldStartDictionary() {
        DictionaryEntity dictionary = DictionaryEntity
            .builder()
            .id(DICTIONARY_ID)
            .updatedAt(new Date())
            .type(DictionaryType.DYNAMIC)
            .state(Lifecycle.State.STOPPED)
            .build();
        doReturn(dictionary).when(dictionaryService).findById(GraviteeContext.getExecutionContext(), DICTIONARY_ID);

        doReturn(dictionary).when(dictionaryService).start(GraviteeContext.getExecutionContext(), DICTIONARY_ID);

        final Response response = envTarget().queryParam("action", LifecycleAction.START).request().post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldStopDictionary() {
        DictionaryEntity dictionary = DictionaryEntity
            .builder()
            .id(DICTIONARY_ID)
            .updatedAt(new Date())
            .type(DictionaryType.DYNAMIC)
            .state(Lifecycle.State.STARTED)
            .build();
        doReturn(dictionary).when(dictionaryService).findById(GraviteeContext.getExecutionContext(), DICTIONARY_ID);

        doReturn(dictionary).when(dictionaryService).stop(GraviteeContext.getExecutionContext(), DICTIONARY_ID);

        final Response response = envTarget().queryParam("action", LifecycleAction.STOP).request().post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldReturnBadRequestWithInvalidLifecycleAction() {
        DictionaryEntity dictionary = DictionaryEntity
            .builder()
            .id(DICTIONARY_ID)
            .updatedAt(new Date())
            .type(DictionaryType.DYNAMIC)
            .state(Lifecycle.State.STARTED)
            .build();
        doReturn(dictionary).when(dictionaryService).findById(GraviteeContext.getExecutionContext(), DICTIONARY_ID);

        final Response response = envTarget().queryParam("action", "Bad action").request().post(null);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }
}
