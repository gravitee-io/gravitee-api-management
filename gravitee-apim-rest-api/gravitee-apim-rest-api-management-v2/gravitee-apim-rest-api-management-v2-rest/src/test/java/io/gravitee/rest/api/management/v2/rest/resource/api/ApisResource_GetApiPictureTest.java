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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Date;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

public class ApisResource_GetApiPictureTest extends AbstractResourceTest {

    private static final String API_ID = "my-api";
    private static final String PICTURE = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkS";
    private static final String ENVIRONMENT = "fake-env";

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @Before
    public void init() {
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT);
        environment.setOrganizationId(ORGANIZATION);

        doReturn(environment).when(environmentService).findById(ENVIRONMENT);
        doReturn(environment).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);
    }

    @Test
    @DisplayName("Should get V4 API picture bytes array")
    public void should_get_V4_api_picture() {
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        api.setPicture(PICTURE);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setUpdatedAt(new Date());

        when(apiSearchServiceV4.findGenericById(any(), any())).thenReturn(api);

        InlinePictureEntity inlinePictureEntity = new InlinePictureEntity();
        inlinePictureEntity.setContent(PICTURE.getBytes());
        inlinePictureEntity.setType("image/jpeg");
        when(apiImagesService.getApiPicture(any(), any())).thenReturn(inlinePictureEntity);

        final Response response = rootTarget(API_ID).path("picture").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        byte[] responseBody = response.readEntity(byte[].class);
        assertThat(responseBody).isEqualTo(PICTURE.getBytes());
    }

    @Test
    @DisplayName("Should get V2 API picture bytes array")
    public void should_get_V2_api_picture() {
        io.gravitee.rest.api.model.api.ApiEntity api = new io.gravitee.rest.api.model.api.ApiEntity();
        api.setId(API_ID);
        api.setPicture(PICTURE);
        api.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        api.setUpdatedAt(new Date());

        when(apiSearchServiceV4.findGenericById(any(), any())).thenReturn(api);

        InlinePictureEntity inlinePictureEntity = new InlinePictureEntity();
        inlinePictureEntity.setContent(PICTURE.getBytes());
        inlinePictureEntity.setType("image/jpeg");
        when(apiImagesService.getApiPicture(any(), any())).thenReturn(inlinePictureEntity);

        final Response response = rootTarget(API_ID).path("picture").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        byte[] responseBody = response.readEntity(byte[].class);
        assertThat(responseBody).isEqualTo(PICTURE.getBytes());
    }

    @Test
    @DisplayName("Should get an OK response wihtout any content")
    public void should_get_nothing() {
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        api.setPicture(PICTURE);
        api.setUpdatedAt(new Date());
        when(apiSearchServiceV4.findGenericById(any(), any())).thenReturn(api);
        when(apiImagesService.getApiPicture(any(), any())).thenReturn(null);

        final Response response = rootTarget(API_ID).path("picture").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        byte[] responseBody = response.readEntity(byte[].class);
        assertThat(responseBody).isEmpty();
    }
}
