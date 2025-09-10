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

import static io.gravitee.common.http.HttpStatusCode.*;
import static jakarta.ws.rs.client.Entity.entity;
import static java.util.Base64.getEncoder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.management.rest.model.ErrorEntity;
import io.gravitee.rest.api.management.rest.resource.param.LifecycleAction;
import io.gravitee.rest.api.management.rest.resource.param.ReviewAction;
import io.gravitee.rest.api.model.ApiStateEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String UNKNOWN_API = "unknown";
    private static final MediaType IMAGE_SVG_XML_TYPE = MediaType.valueOf("image/svg+xml");
    private ApiEntity mockApi;
    private UpdateApiEntity updateApiEntity;

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Before
    public void init() {
        reset(apiService);
        GraviteeContext.cleanContext();

        mockApi = new ApiEntity();
        mockApi.setDefinitionContext(new DefinitionContext());
        mockApi.setId(API);
        mockApi.setName(API);
        mockApi.setVersion("1");
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/test")));
        mockApi.setProxy(proxy);
        mockApi.setUpdatedAt(new Date());
        doReturn(mockApi).when(apiService).findById(GraviteeContext.getExecutionContext(), API);
        doThrow(ApiNotFoundException.class).when(apiService).findById(GraviteeContext.getExecutionContext(), UNKNOWN_API);

        updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setDescription("toto");
        updateApiEntity.setVisibility(Visibility.PUBLIC);
        updateApiEntity.setName(API);
        updateApiEntity.setVersion("v1");
        proxy = new Proxy();
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/test")));
        updateApiEntity.setProxy(proxy);
        updateApiEntity.setLifecycleState(ApiLifecycleState.CREATED);
        doReturn(mockApi).when(apiService).update(eq(GraviteeContext.getExecutionContext()), eq(API), any(), eq(true));
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldGetApi() {
        final Response response = envTarget(API).request().get();

        assertEquals(OK_200, response.getStatus());

        final ApiEntity responseApi = response.readEntity(ApiEntity.class);
        assertNotNull(responseApi);
        assertEquals(API, responseApi.getName());
    }

    @Test
    public void shouldNotGetApiBecauseNotFound() {
        final Response response = envTarget(UNKNOWN_API).request().get();

        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldStartApi() {
        mockApi.setState(Lifecycle.State.STOPPED);
        doReturn(mockApi).when(apiService).start(eq(GraviteeContext.getExecutionContext()), eq(API), any());

        final Response response = envTarget(API).queryParam("action", LifecycleAction.START).request().post(null);

        assertEquals(NO_CONTENT_204, response.getStatus());

        verify(apiService).start(GraviteeContext.getExecutionContext(), API, "UnitTests");
    }

    @Test
    public void shouldNotStartApiBecauseNotFound() {
        mockApi.setState(Lifecycle.State.STOPPED);
        doReturn(mockApi).when(apiService).start(eq(GraviteeContext.getExecutionContext()), eq(API), any());
        final Response response = envTarget(UNKNOWN_API).queryParam("action", LifecycleAction.START).request().post(null);

        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldReturnBadRequestWithInvalidAction() {
        mockApi.setState(Lifecycle.State.STOPPED);
        doReturn(mockApi).when(apiService).start(eq(GraviteeContext.getExecutionContext()), eq(API), any());

        final Response response = envTarget(API).queryParam("action", "Soo bad action").request().post(null);

        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldStopApi() {
        mockApi.setState(Lifecycle.State.STARTED);
        doReturn(mockApi).when(apiService).stop(eq(GraviteeContext.getExecutionContext()), eq(API), any());

        final Response response = envTarget(API).queryParam("action", LifecycleAction.STOP).request().post(null);

        assertEquals(NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void shouldNotStopApiBecauseNotFound() {
        final Response response = envTarget(UNKNOWN_API).queryParam("action", LifecycleAction.STOP).request().post(null);

        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldUpdateApi() {
        final Response response = envTarget(API).request().put(Entity.json(updateApiEntity));

        assertEquals(response.readEntity(String.class), OK_200, response.getStatus());
    }

    @Test
    public void shouldUpdateApi_ImageWithUpperCaseType_issue4086() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream("/images/4086_jpeg.b64");
        String picture = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        updateApiEntity.setPicture(picture);
        final Response response = envTarget(API).request().put(Entity.json(updateApiEntity));

        assertEquals(response.readEntity(String.class), OK_200, response.getStatus());
    }

    @Test
    public void shouldNotUpdateApiBecauseTooLargePicture() {
        updateApiEntity.setPicture("data:image/png;base64," + randomAlphanumeric(1_000_000));
        final Response response = envTarget(API).request().put(Entity.json(updateApiEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        final String message = response.readEntity(String.class);
        assertTrue(message, message.contains("Invalid image format"));
    }

    @Test
    public void shouldNotUpdateApiBecauseNotAValidImage() {
        updateApiEntity.setPicture(getEncoder().encodeToString("<script>alert('XSS')</script>".getBytes()));
        final Response response = envTarget(API).request().put(Entity.json(updateApiEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        final String message = response.readEntity(String.class);
        assertTrue(message, message.contains("Invalid image format"));
    }

    @Test
    public void shouldAccessToApiState_asAdmin() {
        final Response response = envTarget(API + "/state").request().get();

        assertEquals(OK_200, response.getStatus());
        ApiStateEntity stateEntity = response.readEntity(ApiStateEntity.class);
        assertNotNull(stateEntity);
        assertEquals(API, stateEntity.getApiId());
    }

    @Test
    public void shouldNotUpdateApiBecauseSVGImage() {
        updateApiEntity.setPicture(
            "data:image/svg+xml;base64,PGh0bWw+CjxoZWFkPjwvaGVhZD4KPGJvZHk+Cjxzb21ldGhpbmc6c2NyaXB0IHhtbG5zOnNvbWV0aGluZz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94aHRtbCI+YWxlcnQoMSk8L3NvbWV0aGluZzpzY3JpcHQ+CjwvYm9keT4KPC9odG1sPg=="
        );
        final Response response = envTarget(API).request().put(Entity.json(updateApiEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        final String message = response.readEntity(String.class);
        assertTrue(message, message.contains("Invalid image format"));
    }

    @Test
    public void shouldNotUpdateApiBecauseNotAnImage() {
        updateApiEntity.setPicture(
            "data:text/plain;base64,PGh0bWw+CjxoZWFkPjwvaGVhZD4KPGJvZHk+Cjxzb21ldGhpbmc6c2NyaXB0IHhtbG5zOnNvbWV0aGluZz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94aHRtbCI+YWxlcnQoMSk8L3NvbWV0aGluZzpzY3JpcHQ+CjwvYm9keT4KPC9odG1sPg=="
        );
        final Response response = envTarget(API).request().put(Entity.json(updateApiEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        final String message = response.readEntity(String.class);
        assertTrue(message, message.contains("Invalid image format"));
    }

    @Test
    public void shouldNotUpdateApiBecauseTooLargeBackground() {
        updateApiEntity.setBackground("data:image/png;base64," + randomAlphanumeric(1_000_000));
        final Response response = envTarget(API).request().put(Entity.json(updateApiEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        final String message = response.readEntity(String.class);
        assertTrue(message, message.contains("Invalid image format"));
    }

    @Test
    public void shouldNotUpdateApiBackgroundBecauseNotAValidImage() {
        updateApiEntity.setBackground(getEncoder().encodeToString("<script>alert('XSS')</script>".getBytes()));
        final Response response = envTarget(API).request().put(Entity.json(updateApiEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        final String message = response.readEntity(String.class);
        assertTrue(message, message.contains("Invalid image format"));
    }

    @Test
    public void shouldNotUpdateApiBackgroundBecauseSVGImage() {
        updateApiEntity.setBackground(
            "data:image/svg+xml;base64,PGh0bWw+CjxoZWFkPjwvaGVhZD4KPGJvZHk+Cjxzb21ldGhpbmc6c2NyaXB0IHhtbG5zOnNvbWV0aGluZz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94aHRtbCI+YWxlcnQoMSk8L3NvbWV0aGluZzpzY3JpcHQ+CjwvYm9keT4KPC9odG1sPg=="
        );
        final Response response = envTarget(API).request().put(Entity.json(updateApiEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        final String message = response.readEntity(String.class);
        assertTrue(message, message.contains("Invalid image format"));
    }

    @Test
    public void shouldNotUpdateApiBackgroundBecauseNotAnImage() {
        updateApiEntity.setBackground(
            "data:text/plain;base64,PGh0bWw+CjxoZWFkPjwvaGVhZD4KPGJvZHk+Cjxzb21ldGhpbmc6c2NyaXB0IHhtbG5zOnNvbWV0aGluZz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94aHRtbCI+YWxlcnQoMSk8L3NvbWV0aGluZzpzY3JpcHQ+CjwvYm9keT4KPC9odG1sPg=="
        );
        final Response response = envTarget(API).request().put(Entity.json(updateApiEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        final String message = response.readEntity(String.class);
        assertTrue(message, message.contains("Invalid image format"));
    }

    @Test
    public void shouldDeployApi() {
        ApiDeploymentEntity deployEntity = new ApiDeploymentEntity();
        deployEntity.setDeploymentLabel("label");

        mockApi.setState(Lifecycle.State.STARTED);
        mockApi.setUpdatedAt(new Date());
        doReturn(mockApi)
            .when(apiService)
            .deploy(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(EventType.PUBLISH_API), any());

        final Response response = envTarget(API + "/deploy").request().post(Entity.json(deployEntity));

        assertEquals(OK_200, response.getStatus());

        verify(apiService, times(1)).deploy(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(EventType.PUBLISH_API), any());
    }

    @Test
    public void shouldNotDeployApi() {
        ApiDeploymentEntity deployEntity = new ApiDeploymentEntity();
        deployEntity.setDeploymentLabel("label_too_long_because_more_than_32_chars");

        mockApi.setState(Lifecycle.State.STARTED);
        mockApi.setUpdatedAt(new Date());
        doReturn(mockApi)
            .when(apiService)
            .deploy(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(EventType.PUBLISH_API), any());

        final Response response = envTarget(API + "/deploy").request().post(Entity.json(deployEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());

        verify(apiService, times(0)).deploy(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(EventType.PUBLISH_API), any());
    }

    public void shouldUploadApiMedia() {
        StreamDataBodyPart filePart = new StreamDataBodyPart(
            "file",
            this.getClass().getResourceAsStream("/media/logo.svg"),
            "logo.svg",
            MediaType.valueOf("image/svg+xml")
        );
        final MultiPart multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(filePart);
        final Response response = envTarget(API + "/media/upload").request().post(entity(multiPart, multiPart.getMediaType()));
        assertEquals(OK_200, response.getStatus());
    }

    public void shouldNotUploadApiMediaBecauseWrongMediaType() {
        StreamDataBodyPart filePart = new StreamDataBodyPart(
            "file",
            this.getClass().getResourceAsStream("/media/logo.svg"),
            "logo.svg",
            MediaType.APPLICATION_OCTET_STREAM_TYPE
        );
        final MultiPart multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(filePart);
        final Response response = envTarget(API + "/media/upload").request().post(entity(multiPart, multiPart.getMediaType()));
        assertEquals(BAD_REQUEST_400, response.getStatus());
        final String message = response.readEntity(String.class);
        assertTrue(message, message.contains("File format unauthorized"));
    }

    @Test
    public void shouldNotUploadApiMediaBecauseXSS1() {
        shouldNotUpdateApiMediaBecauseXSS("hacked1.svg");
    }

    @Test
    public void shouldNotUploadApiMediaBecauseXSS2() {
        shouldNotUpdateApiMediaBecauseXSS("hacked2.svg");
    }

    @Test
    public void shouldNotUploadApiMediaBecauseXSS3() {
        shouldNotUpdateApiMediaBecauseXSS("hacked3.svg");
    }

    @Test
    public void shouldNotUploadApiMediaBecauseXSS4() {
        shouldNotUpdateApiMediaBecauseXSS("hacked4.svg");
    }

    private void shouldNotUpdateApiMediaBecauseXSS(final String fileName) {
        final StreamDataBodyPart filePart = new StreamDataBodyPart(
            "file",
            this.getClass().getResourceAsStream("/media/" + fileName),
            fileName,
            IMAGE_SVG_XML_TYPE
        );
        final MultiPart multiPart = new MultiPart(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(filePart);
        final Response response = envTarget(API + "/media/upload").request().post(entity(multiPart, multiPart.getMediaType()));
        assertEquals(BAD_REQUEST_400, response.getStatus());
        final String message = response.readEntity(String.class);
        assertTrue(message, message.contains("Invalid image format"));
    }

    @Test
    public void shouldImportApiPathMappingsFromPage() {
        when(
            apiService.importPathMappingsFromPage(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                eq("Foo"),
                eq(DefinitionVersion.valueOfLabel("1.0.0"))
            )
        )
            .thenReturn(mockApi);

        final Response response = envTarget(API + "/import-path-mappings").queryParam("page", "Foo").request().post(null);
        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void shouldImportApiPathMappingsFromPageWithDefinitionVersion() {
        when(
            apiService.importPathMappingsFromPage(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                eq("Foo"),
                eq(DefinitionVersion.valueOfLabel("2.0.0"))
            )
        )
            .thenReturn(mockApi);

        final Response response = envTarget(API + "/import-path-mappings")
            .queryParam("page", "Foo")
            .queryParam("definitionVersion", "2.0.0")
            .request()
            .post(null);
        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void shouldAskForReview() {
        doReturn(mockApi).when(apiService).askForReview(eq(GraviteeContext.getExecutionContext()), eq(API), any(), any());
        final Response response = envTarget(API + "/reviews").queryParam("action", ReviewAction.ASK).request().post(null);

        assertEquals(NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void shouldAcceptReview() {
        doReturn(mockApi).when(apiService).acceptReview(eq(GraviteeContext.getExecutionContext()), eq(API), any(), any());
        final Response response = envTarget(API + "/reviews").queryParam("action", ReviewAction.ACCEPT).request().post(null);

        assertEquals(NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void shouldRejectReview() {
        doReturn(mockApi).when(apiService).rejectReview(eq(GraviteeContext.getExecutionContext()), eq(API), any(), any());
        final Response response = envTarget(API + "/reviews").queryParam("action", ReviewAction.REJECT).request().post(null);

        assertEquals(NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void shouldReturnBadRequestWithInvalidReviewsAction() {
        final Response response = envTarget(API + "/reviews").queryParam("action", "Soo bad action").request().post(null);

        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldImportApiFromURL() {
        reset(apiDuplicatorService);

        ApiEntity updatedApi = new ApiEntity();
        updatedApi.setId("my-api-id");
        updatedApi.setUpdatedAt(new Date());
        updatedApi.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        doReturn(updatedApi)
            .when(apiDuplicatorService)
            .updateWithImportedDefinition(eq(GraviteeContext.getExecutionContext()), any(), any());

        final Response response = envTarget()
            .path(API + "/import-url")
            .queryParam("definitionVersion", "2.0.0")
            .request()
            .put(Entity.text("http://localhost:8080/api/my-api-id"));

        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void shouldImportApiFromURLWithDeprecatedEndpoint() {
        reset(apiDuplicatorService);

        ApiEntity updatedApi = new ApiEntity();
        updatedApi.setId("my-api-id");
        updatedApi.setUpdatedAt(new Date());
        updatedApi.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        doReturn(updatedApi)
            .when(apiDuplicatorService)
            .updateWithImportedDefinition(eq(GraviteeContext.getExecutionContext()), any(), any());

        final Response response = envTarget()
            .path(API + "/import")
            .queryParam("definitionVersion", "2.0.0")
            .request()
            .put(Entity.text("http://localhost:8080/api/my-api-id"));

        assertEquals(OK_200, response.getStatus());
    }

    @Test
    public void shouldReportCrdExportErrors() {
        reset(apiDuplicatorService);

        doThrow(new TechnicalManagementException("Unable to export as JSON"))
            .when(apiExportService)
            .exportAsCustomResourceDefinition(any(), any(), any());

        final Response response = envTarget().path(API + "/crd").request().get();

        assertEquals(INTERNAL_SERVER_ERROR_500, response.getStatus());

        ErrorEntity errorEntity = response.readEntity(ErrorEntity.class);

        assertEquals("Unable to export as JSON", errorEntity.getMessage());
        assertEquals("unexpected", errorEntity.getTechnicalCode());
    }

    @Test
    public void shouldTriggerCrdExportFileDownload() {
        reset(apiDuplicatorService);

        doReturn("some-yaml").when(apiExportService).exportAsCustomResourceDefinition(any(), any(), any());

        final Response response = envTarget().path(API + "/crd").request().get();

        assertEquals(OK_200, response.getStatus());

        assertEquals("application/yaml", response.getHeaders().getFirst("Content-Type"));
        assertEquals("attachment;filename=my-api-1.yml", response.getHeaders().getFirst("Content-Disposition"));
    }

    @Test
    public void shouldRollbackApi_withV2Payload() {
        //given
        final String v2Payload =
            """
        {
          "id": "my-api",
          "name": "My Api",
          "version": "v1.0",
          "description": "rollback test for v2",
          "visibility": "PUBLIC",
          "proxy": {
            "virtual_hosts": [ { "path": "/test" } ],
            "groups": []
          },
          "plans": [],
          "flows": [],
          "paths": {}
        }
        """;

        ApiEntity rollbacked = new ApiEntity();
        rollbacked.setId(API);
        rollbacked.setName(API);
        rollbacked.setUpdatedAt(new Date());
        doReturn(rollbacked).when(apiService).rollback(eq(GraviteeContext.getExecutionContext()), eq(API), any());

        // when
        Response response = envTarget(API + "/rollback").request().post(Entity.json(v2Payload));

        // then
        assertEquals(OK_200, response.getStatus());
        verify(apiService, times(1)).rollback(eq(GraviteeContext.getExecutionContext()), eq(API), any());
        ApiEntity body = response.readEntity(ApiEntity.class);
        assertNotNull(body);
        assertEquals(API, body.getId());
    }

    @Test
    public void shouldReturnBadRequest_onV4PayloadForRollback() {
        //given
        final String v4Payload =
            """
        {
          "id": "my-api",
          "name": "v4 api",
          "version": "1.0",
          "description": "v4 api description",
          "visibility": "PRIVATE",
          "definitionVersion": "4.0.0",
          "proxy": {
            "virtual_hosts": [ { "path": "/" } ],
            "groups": []
          },
          "plans": [],
          "flows": [],
          "paths": {}
        }
        """;

        // when
        Response response = envTarget(API + "/rollback").request().post(Entity.json(v4Payload));

        // then
        assertEquals(BAD_REQUEST_400, response.getStatus());

        try {
            ErrorEntity err = response.readEntity(ErrorEntity.class);
            assertNotNull(err);
            System.out.println(err.getMessage());
            assertEquals("Detected a v4 API definition. Please use the migration tool instead of rollback.", err.getMessage());
        } catch (Exception ignore) {
            String msg = response.readEntity(String.class);
            assertTrue(msg.toLowerCase().contains("v4"));
        }

        verify(apiService, never()).rollback(any(), any(), any());
    }
}
