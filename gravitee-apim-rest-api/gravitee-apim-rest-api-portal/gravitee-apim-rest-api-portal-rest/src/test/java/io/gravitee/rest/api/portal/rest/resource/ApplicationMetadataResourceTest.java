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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.NewApplicationMetadataEntity;
import io.gravitee.rest.api.model.UpdateApplicationMetadataEntity;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationMetadataNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationMetadataResourceTest extends AbstractResourceTest {

    private static final String APPLICATION = "my-application";
    private static final String UNKNOWN_APPLICATION = "unknown-application";
    private static final String METADATA_1 = "my-metadata-1";
    private static final String METADATA_1_NAME = "my-metadata-1-name";
    private static final String METADATA_1_FORMAT = "BOOLEAN";
    private static final String METADATA_1_VALUE = "my-metadata-1-value";
    private static final String METADATA_1_DEFAULT_VALUE = "my-metadata-1-defaut-value";
    private static final String METADATA_2 = "my-metadata-2";
    private static final String UNKNOWN_METADATA = "unknown-metadata";

    @Override
    protected String contextPath() {
        return "applications/";
    }

    @Before
    public void init() {
        resetAllMocks();

        ApplicationMetadataEntity applicationMetadataEntity1 = new ApplicationMetadataEntity();
        applicationMetadataEntity1.setKey(METADATA_1);

        ApplicationMetadataEntity applicationMetadataEntity2 = new ApplicationMetadataEntity();
        applicationMetadataEntity2.setKey(METADATA_2);

        when(referenceMetadataMapper.convert(any())).thenCallRealMethod();
        when(referenceMetadataMapper.convert(any(), any())).thenCallRealMethod();
        when(referenceMetadataMapper.convert(any(), any(), any())).thenCallRealMethod();

        doReturn(Arrays.asList(applicationMetadataEntity1, applicationMetadataEntity2))
            .when(applicationMetadataService)
            .findAllByApplication(APPLICATION);
        doReturn(applicationMetadataEntity1).when(applicationMetadataService).findByIdAndApplication(METADATA_1, APPLICATION);
        doReturn(null).when(applicationMetadataService).findByIdAndApplication(METADATA_2, APPLICATION);

        when(applicationMetadataService.create(eq(GraviteeContext.getExecutionContext()), any()))
            .thenAnswer(
                invocation -> {
                    NewApplicationMetadataEntity newApplicationMetadataEntity = invocation.getArgument(1);
                    if (newApplicationMetadataEntity.getApplicationId().equals(UNKNOWN_APPLICATION)) {
                        throw new ApplicationNotFoundException(UNKNOWN_APPLICATION);
                    }
                    return applicationMetadataEntity1;
                }
            );
        when(applicationMetadataService.update(eq(GraviteeContext.getExecutionContext()), any()))
            .thenAnswer(
                invocation -> {
                    UpdateApplicationMetadataEntity updateApplicationMetadataEntity = invocation.getArgument(1);
                    if (updateApplicationMetadataEntity.getApplicationId().equals(UNKNOWN_APPLICATION)) {
                        throw new ApplicationNotFoundException(UNKNOWN_APPLICATION);
                    }
                    if (updateApplicationMetadataEntity.getKey().equals(UNKNOWN_METADATA)) {
                        throw new ApplicationMetadataNotFoundException(
                            updateApplicationMetadataEntity.getApplicationId(),
                            UNKNOWN_METADATA
                        );
                    }
                    return applicationMetadataEntity1;
                }
            );

        doThrow(ApplicationNotFoundException.class).when(applicationMetadataService).findAllByApplication(UNKNOWN_APPLICATION);
        doThrow(ApplicationNotFoundException.class).when(applicationMetadataService).findByIdAndApplication(any(), eq(UNKNOWN_APPLICATION));
        doThrow(ApplicationNotFoundException.class)
            .when(applicationMetadataService)
            .delete(eq(GraviteeContext.getExecutionContext()), any(), eq(UNKNOWN_APPLICATION));
        doThrow(ApplicationMetadataNotFoundException.class)
            .when(applicationMetadataService)
            .findByIdAndApplication(UNKNOWN_METADATA, APPLICATION);
        doThrow(ApplicationMetadataNotFoundException.class)
            .when(applicationMetadataService)
            .delete(eq(GraviteeContext.getExecutionContext()), eq(UNKNOWN_METADATA), any());
    }

    @Test
    public void shouldGetAllMetadata() {
        final Response response = target(APPLICATION).path("metadata").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ReferenceMetadataResponse metadataResponse = response.readEntity(ReferenceMetadataResponse.class);
        assertEquals(2, metadataResponse.getData().size());
        assertEquals(METADATA_1, metadataResponse.getData().get(0).getKey());
        assertEquals(METADATA_2, metadataResponse.getData().get(1).getKey());

        Links links = metadataResponse.getLinks();
        assertNotNull(links);
    }

    @Test
    public void shouldGetMetadataWithPaginatedLink() {
        final Response response = target(APPLICATION).path("metadata").queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ReferenceMetadataResponse metadataResponse = response.readEntity(ReferenceMetadataResponse.class);
        assertEquals(1, metadataResponse.getData().size());
        assertEquals(METADATA_2, metadataResponse.getData().get(0).getKey());

        Links links = metadataResponse.getLinks();
        assertNotNull(links);
    }

    @Test
    public void shouldNotGetMetadata() {
        final Response response = target(APPLICATION).path("metadata").queryParam("page", 10).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertEquals("errors.pagination.invalid", error.getCode());
        assertEquals("400", error.getStatus());
        assertEquals("Pagination is not valid", error.getMessage());
    }

    @Test
    public void shouldGetNoMetadataAndNoLink() {
        doReturn(Collections.emptyList()).when(applicationMetadataService).findAllByApplication(any());

        //Test with default limit
        final Response response = target(APPLICATION).path("metadata").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ReferenceMetadataResponse metadataResponse = response.readEntity(ReferenceMetadataResponse.class);
        assertEquals(0, metadataResponse.getData().size());

        Links links = metadataResponse.getLinks();
        assertNull(links);

        //Test with small limit
        final Response anotherResponse = target(APPLICATION).path("metadata").queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());

        metadataResponse = anotherResponse.readEntity(ReferenceMetadataResponse.class);
        assertEquals(0, metadataResponse.getData().size());

        links = metadataResponse.getLinks();
        assertNull(links);
    }

    @Test
    public void shouldGetMetadata() {
        final Response response = target(APPLICATION).path("metadata").path(METADATA_1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ReferenceMetadata responseMetadata = response.readEntity(ReferenceMetadata.class);
        assertNotNull(responseMetadata);
        assertEquals(METADATA_1, responseMetadata.getKey());
    }

    @Test
    public void shouldDeleteMetadata() {
        final Response response = target(APPLICATION).path("metadata").path(METADATA_1).request().delete();
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        Mockito.verify(applicationMetadataService).delete(GraviteeContext.getExecutionContext(), METADATA_1, APPLICATION);
    }

    @Test
    public void shouldCreateMetadata() {
        ReferenceMetadataInput metadataInput = new ReferenceMetadataInput()
            .name(METADATA_1_NAME)
            .defaultValue(METADATA_1_DEFAULT_VALUE)
            .format(ReferenceMetadataFormatType.valueOf(METADATA_1_FORMAT))
            .value(METADATA_1_VALUE);
        final Response response = target(APPLICATION).path("metadata").request().post(Entity.json(metadataInput));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(
            target(APPLICATION).path("metadata").path(METADATA_1).getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );

        ArgumentCaptor<NewApplicationMetadataEntity> newMetadataEntityCaptor = ArgumentCaptor.forClass(NewApplicationMetadataEntity.class);

        Mockito.verify(applicationMetadataService).create(eq(GraviteeContext.getExecutionContext()), newMetadataEntityCaptor.capture());
        final NewApplicationMetadataEntity newMetadataEntityCaptorValue = newMetadataEntityCaptor.getValue();
        assertEquals(APPLICATION, newMetadataEntityCaptorValue.getApplicationId());
        assertEquals(METADATA_1_NAME, newMetadataEntityCaptorValue.getName());
        assertEquals(METADATA_1_VALUE, newMetadataEntityCaptorValue.getValue());
        assertEquals(METADATA_1_DEFAULT_VALUE, newMetadataEntityCaptorValue.getDefaultValue());
        assertEquals(MetadataFormat.valueOf(METADATA_1_FORMAT), newMetadataEntityCaptorValue.getFormat());
    }

    @Test
    public void shouldUpdateMetadata() {
        ReferenceMetadataInput metadataInput = new ReferenceMetadataInput()
            .name(METADATA_1_NAME)
            .defaultValue(METADATA_1_DEFAULT_VALUE)
            .format(ReferenceMetadataFormatType.valueOf(METADATA_1_FORMAT))
            .value(METADATA_1_VALUE);
        final Response response = target(APPLICATION).path("metadata").path(METADATA_1).request().put(Entity.json(metadataInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<UpdateApplicationMetadataEntity> updateMetadataEntityCaptor = ArgumentCaptor.forClass(
            UpdateApplicationMetadataEntity.class
        );

        Mockito.verify(applicationMetadataService).update(eq(GraviteeContext.getExecutionContext()), updateMetadataEntityCaptor.capture());
        final UpdateApplicationMetadataEntity uodateMetadataEntityCaptorValue = updateMetadataEntityCaptor.getValue();
        assertEquals(APPLICATION, uodateMetadataEntityCaptorValue.getApplicationId());
        assertEquals(METADATA_1, uodateMetadataEntityCaptorValue.getKey());
        assertEquals(METADATA_1_NAME, uodateMetadataEntityCaptorValue.getName());
        assertEquals(METADATA_1_VALUE, uodateMetadataEntityCaptorValue.getValue());
        assertEquals(METADATA_1_DEFAULT_VALUE, uodateMetadataEntityCaptorValue.getDefaultValue());
        assertEquals(MetadataFormat.valueOf(METADATA_1_FORMAT), uodateMetadataEntityCaptorValue.getFormat());
    }

    //404 GET /metadata
    @Test
    public void shouldHaveNotFoundWhileGettingMetadata() {
        final Response response = target(UNKNOWN_APPLICATION).path("metadata").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    //404 POST /metadata
    @Test
    public void shouldHaveNotFoundWhileCreatingNewMetadataUnknownApplication() {
        final Response response = target(UNKNOWN_APPLICATION).path("metadata").request().post(Entity.json(new ReferenceMetadataInput()));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    //404 PUT /metadata/{metadataId}
    @Test
    public void shouldHaveNotFoundWhileUpdatingNewMetadataUnknownApplication() {
        final Response response = target(UNKNOWN_APPLICATION)
            .path("metadata")
            .path(METADATA_1)
            .request()
            .put(Entity.json(new ReferenceMetadataInput()));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldHaveNotFoundWhileUpdatingNewMetadataUnknownMetadata() {
        final Response response = target(APPLICATION)
            .path("metadata")
            .path(UNKNOWN_METADATA)
            .request()
            .put(Entity.json(new ReferenceMetadataInput()));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    //404 DELETE /metadata/{metadataId}
    @Test
    public void shouldHaveNotFoundWhileDeletingMetadataUnknonwnApplication() {
        final Response response = target(UNKNOWN_APPLICATION).path("metadata").path(METADATA_1).request().delete();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldHaveNotFoundWhileDeletingMetadataUnknownMetadata() {
        final Response response = target(APPLICATION).path("metadata").path(UNKNOWN_METADATA).request().delete();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    //404 GET /metadata/{metadataId}
    @Test
    public void shouldHaveNotFoundWhileGettingMetadataUnknownApplication() {
        final Response response = target(UNKNOWN_APPLICATION).path("metadata").path(METADATA_1).request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldHaveNotFoundWhileGettingMetadataUnknownMember() {
        final Response response = target(APPLICATION).path("metadata").path(UNKNOWN_METADATA).request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
}
