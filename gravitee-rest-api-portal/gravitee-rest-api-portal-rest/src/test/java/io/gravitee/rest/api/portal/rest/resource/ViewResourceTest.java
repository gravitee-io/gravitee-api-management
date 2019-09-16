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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NOT_MODIFIED_304;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.http.HttpHeader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.View;
import io.gravitee.rest.api.service.exceptions.ViewNotFoundException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ViewResourceTest extends AbstractResourceTest {

    private static final String VIEW_ID = "my-view-id";
    private static final String UNKNOWN_VIEW = "unknown";

    @Override
    protected String contextPath() {
        return "views/";
    }

    private InlinePictureEntity mockImage;
    private byte[] apiLogoContent;

    @Before
    public void init() throws IOException, URISyntaxException {
        resetAllMocks();
        
        ViewEntity viewEntity = new ViewEntity();
        viewEntity.setId(VIEW_ID);
        viewEntity.setHidden(false);
        doReturn(viewEntity).when(viewService).findNotHiddenById(VIEW_ID);
        
        Set<ApiEntity> mockApis = new HashSet<>();
        doReturn(mockApis).when(apiService).findByUser(any(), any());
        
        Function<ViewEntity, ViewEntity> identity = (v) -> v;
        doReturn(identity).when(viewEnhancer).enhance(any());
        
        Mockito.when(viewMapper.convert(any(), any())).thenCallRealMethod();

        mockImage = new InlinePictureEntity();
        apiLogoContent = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("media/logo.svg").toURI()));
        mockImage.setContent(apiLogoContent);
        mockImage.setType("image/svg");
        doReturn(mockImage).when(viewService).getPicture(VIEW_ID);

    }

    @Test
    public void shouldGetView() {
        final Response response = target(VIEW_ID).request().get();
        assertEquals(OK_200, response.getStatus());

        Mockito.verify(viewService).findNotHiddenById(VIEW_ID);
        Mockito.verify(apiService).findByUser(USER_NAME, null);
        Mockito.verify(viewEnhancer).enhance(any());
        Mockito.verify(viewMapper).convert(any(), any());

        final View responseView = response.readEntity(View.class);
        assertNotNull(responseView);
        
    }

    @Test
    public void shouldNotGetView() {
        doThrow(new ViewNotFoundException(UNKNOWN_VIEW)).when(viewService).findNotHiddenById(UNKNOWN_VIEW);

        final Response response = target(UNKNOWN_VIEW).request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());
        
        final Error error = response.readEntity(Error.class);
        assertNotNull(error);
        assertEquals("404", error.getCode());
        assertEquals("io.gravitee.rest.api.service.exceptions.ViewNotFoundException", error.getTitle());
        assertEquals("View ["+UNKNOWN_VIEW+"] can not be found.", error.getDetail());
    }
    
    @Test
    public void shouldGetViewPicture() throws IOException {
        final Response response = target(VIEW_ID).path("picture").request().get();
        assertEquals(OK_200, response.getStatus());

        MultivaluedMap<String, Object> headers = response.getHeaders();
        String contentType = (String) headers.getFirst(HttpHeader.CONTENT_TYPE.asString());
        String etag = (String) headers.getFirst("ETag");

        assertEquals(mockImage.getType(), contentType);

        File result = response.readEntity(File.class);
        byte[] fileContent = Files.readAllBytes(Paths.get(result.getAbsolutePath()));
        assertTrue(Arrays.equals(fileContent, apiLogoContent));
        
        String expectedTag = '"'+Integer.toString(new String(fileContent).hashCode())+'"';
        assertEquals(expectedTag, etag);
        
        
        // test Cache
        final Response cachedResponse = target(VIEW_ID).path("picture").request().header(HttpHeader.IF_NONE_MATCH.asString(), etag).get();
        assertEquals(NOT_MODIFIED_304, cachedResponse.getStatus());
    }
    
}
