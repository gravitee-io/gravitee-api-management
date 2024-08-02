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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageMediaEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.UploadUnauthorized;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PortalPageMediaResourceTest extends AbstractResourceTest {

    private static final String PAGE = "my-page";

    @Override
    protected String contextPath() {
        return "portal";
    }

    @Before
    public void init() {
        Mockito.reset(mediaService);
        GraviteeContext.cleanContext();
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldBeBadRequestIfContentLengthGreaterThanMediaMaxSize() throws IOException {
        StreamDataBodyPart filePart = new StreamDataBodyPart(
            "file",
            this.getClass().getResource("/media/logo.svg").openStream(),
            "logo.svg",
            MediaType.valueOf("image/svg+xml")
        );
        final MultiPart multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(filePart);
        final FormDataBodyPart fileNameBodyPart = new FormDataBodyPart("fileName", "logo.svg");
        multiPart.bodyPart(fileNameBodyPart);

        final PageEntity pageMock = new PageEntity();
        pageMock.setType("MARKDOWN");
        pageMock.setReferenceType(PageReferenceType.ENVIRONMENT.name());
        pageMock.setReferenceId(GraviteeContext.getCurrentEnvironment());

        doReturn(pageMock).when(pageService).findById(PAGE);

        when(httpServletRequest().getContentLength()).thenReturn(15);
        when(mediaService.getMediaMaxSize(any())).thenReturn(10L);

        final Response response = envTarget()
            .path("pages")
            .path(PAGE)
            .path("media")
            .request()
            .post(Entity.entity(multiPart, multiPart.getMediaType()));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        final UploadUnauthorized result = response.readEntity(UploadUnauthorized.class);
        assertThat(result.getMessage()).isEqualTo("Max size is " + 10L + "bytes. Actual size is " + 15 + "bytes.");
    }

    @Test
    public void shouldAttachMediaToPortalPage() throws IOException {
        StreamDataBodyPart filePart = new StreamDataBodyPart(
            "file",
            this.getClass().getResource("/media/logo.svg").openStream(),
            "logo.svg",
            MediaType.valueOf("image/svg+xml")
        );
        final MultiPart multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(filePart);
        final FormDataBodyPart fileNameBodyPart = new FormDataBodyPart("fileName", "logo.svg");
        multiPart.bodyPart(fileNameBodyPart);

        final PageEntity pageMock = new PageEntity();
        pageMock.setType("MARKDOWN");
        pageMock.setReferenceType(PageReferenceType.ENVIRONMENT.name());
        pageMock.setReferenceId(GraviteeContext.getCurrentEnvironment());

        doReturn(pageMock).when(pageService).findById(PAGE);

        when(httpServletRequest().getContentLength()).thenReturn(5);
        when(mediaService.getMediaMaxSize(any())).thenReturn(10L);

        final String mediaHash = "#MEDIA_HASH";
        when(mediaService.savePortalMedia(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(mediaHash);

        final Date attachedAt = new Date();
        PageEntity pageEntity = new PageEntity();
        PageMediaEntity createdMedia = new PageMediaEntity();
        createdMedia.setMediaHash(mediaHash);
        createdMedia.setAttachedAt(attachedAt);
        pageEntity.setAttachedMedia(List.of(createdMedia));
        when(pageService.attachMedia(eq(PAGE), eq(mediaHash), any())).thenReturn(Optional.of(pageEntity));

        final Response response = envTarget()
            .path("pages")
            .path(PAGE)
            .path("media")
            .request()
            .post(Entity.entity(multiPart, multiPart.getMediaType()));

        assertThat(response.getStatus()).isEqualTo(CREATED_201);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
            .isEqualTo(envTarget().path("pages").path(PAGE).path("media").getUri().toString());

        final MediaEntity result = response.readEntity(MediaEntity.class);
        assertThat(result.getHash()).isEqualTo(mediaHash);
        assertThat(result.getCreateAt()).isEqualTo(attachedAt);
        assertThat(result.getData()).isNull();
        assertThat(result.getType()).isEqualTo("image");
        assertThat(result.getSubType()).isEqualTo("svg+xml");
        assertThat(result.getFileName()).isEqualTo("logo.svg");
    }

    @Test
    public void shouldNotAttachMediaToApiPageBecausePageDoesNotBelongToApi() throws IOException {
        StreamDataBodyPart filePart = new StreamDataBodyPart(
            "file",
            this.getClass().getResource("/media/logo.svg").openStream(),
            "logo.svg",
            MediaType.valueOf("image/svg+xml")
        );
        final MultiPart multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(filePart);
        final FormDataBodyPart fileNameBodyPart = new FormDataBodyPart("fileName", "logo.svg");
        multiPart.bodyPart(fileNameBodyPart);

        final PageEntity pageMock = new PageEntity();
        pageMock.setType("MARKDOWN");
        pageMock.setReferenceType(PageReferenceType.ENVIRONMENT.name());
        pageMock.setReferenceId("Another_environment");

        doReturn(pageMock).when(pageService).findById(PAGE);

        final Response response = envTarget()
            .path("pages")
            .path(PAGE)
            .path("media")
            .request()
            .post(Entity.entity(multiPart, multiPart.getMediaType()));

        assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
    }
}
