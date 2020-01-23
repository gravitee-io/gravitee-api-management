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

import static io.gravitee.common.http.HttpStatusCode.NOT_MODIFIED_304;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.eclipse.jetty.http.HttpHeader;
import org.junit.Test;
import org.mockito.Mockito;

import io.gravitee.rest.api.model.InlinePictureEntity;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PictureManagementTest {


    AbstractResource pictureResourceForTest = new AbstractResource() {};

    @Test
    public void testPictureResponse() throws IOException, URISyntaxException {
        Request request = Mockito.mock(Request.class);
        doReturn(null).when(request).evaluatePreconditions(any(EntityTag.class));

        InlinePictureEntity mockImage = new InlinePictureEntity();
        byte[] imageContent = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("media/logo.svg").toURI()));
        mockImage.setContent(imageContent);
        mockImage.setType("image/svg");

        Response response = pictureResourceForTest.createPictureResponse(request, mockImage);

        assertEquals(OK_200, response.getStatus());

        MultivaluedMap<String, Object> headers = response.getHeaders();
        MediaType mediaType = (MediaType) headers.getFirst(HttpHeader.CONTENT_TYPE.asString());
        String etag = ((EntityTag) headers.getFirst("ETag")).getValue();

        assertEquals(mockImage.getType(), mediaType.toString());

        ByteArrayOutputStream baos = (ByteArrayOutputStream)response.getEntity();
        byte[] fileContent = baos.toByteArray();
        assertTrue(Arrays.equals(fileContent, imageContent));

        String expectedTag = Integer.toString(new String(fileContent).hashCode());
        assertEquals(expectedTag, etag);


        // test Cache
        ResponseBuilder responseBuilder = Response.notModified();
        doReturn(responseBuilder).when(request).evaluatePreconditions(any(EntityTag.class));

        final Response cachedResponse = pictureResourceForTest.createPictureResponse(request, mockImage);
        assertEquals(NOT_MODIFIED_304, cachedResponse.getStatus());
    }

}
