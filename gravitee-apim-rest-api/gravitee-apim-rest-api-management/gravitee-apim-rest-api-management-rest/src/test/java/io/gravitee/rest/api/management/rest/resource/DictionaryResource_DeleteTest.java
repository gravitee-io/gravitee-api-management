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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import javax.ws.rs.core.Response;
import org.junit.Test;

public class DictionaryResource_DeleteTest extends AbstractResourceTest {

    private static final String DICTIONARY_ID = "dictionaryId";

    protected String contextPath() {
        return "configuration/dictionaries/";
    }

    @Test
    public void shouldDeleteDictionary() {
        final Response response = envTarget(DICTIONARY_ID).request().delete();
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        verify(dictionaryService, times(1)).delete(eq(DICTIONARY_ID));
    }
}
