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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class ApiResource_ImagesTest extends ApiResourceTest {

    private static final String PICTURE =
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAMgAAADIAQAAAACFI5MzAAAAMklEQVR4Xu3JsQkAIBDAwN9/aSWNheACcilzs17NPU6EFCFFSBFShBQhRUgRUoQU+U82i451ljeAPcoAAAAASUVORK5CYII=";

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API;
    }

    @Test
    public void should_not_get_picture_with_insufficient_rights() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.READ)
            )
        )
            .thenReturn(false);
        final Response response = rootTarget("picture").request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_not_update_picture_with_insufficient_rights() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        )
            .thenReturn(false);
        final Response response = rootTarget("picture").request().put(Entity.text(""));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_get_api_picture() {
        InlinePictureEntity inlinePictureEntity = new InlinePictureEntity();
        inlinePictureEntity.setContent(PICTURE.getBytes());
        inlinePictureEntity.setType("image/jpeg");
        when(apiImagesService.getApiPicture(any(), any())).thenReturn(inlinePictureEntity);

        final Response response = rootTarget("picture").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        byte[] responseBody = response.readEntity(byte[].class);
        assertThat(responseBody).isEqualTo(PICTURE.getBytes());
    }

    @Test
    public void should_get_no_picture() {
        when(apiImagesService.getApiPicture(any(), any())).thenReturn(null);

        final Response response = rootTarget("picture").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        byte[] responseBody = response.readEntity(byte[].class);
        assertThat(responseBody).isEmpty();
    }

    @Test
    public void should_update_api_picture() {
        final Response response = rootTarget("picture").request().put(Entity.text(PICTURE));
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        verify(apiImagesService, times(1)).updateApiPicture(GraviteeContext.getExecutionContext(), API, PICTURE);
    }

    @Test
    public void should_delete_api_picture() {
        final Response response = rootTarget("picture").request().delete();
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        verify(apiImagesService, times(1)).updateApiPicture(GraviteeContext.getExecutionContext(), API, null);
    }

    @Test
    public void should_get_invalid_format_picture() {
        final Response response = rootTarget("picture").request().put(Entity.text(""));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        verifyNoInteractions(apiImagesService);
    }

    @Test
    public void should_not_get_background_with_insufficient_rights() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.READ)
            )
        )
            .thenReturn(false);
        final Response response = rootTarget("background").request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_not_update_background_with_insufficient_rights() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        )
            .thenReturn(false);
        final Response response = rootTarget("background").request().put(Entity.text(""));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_get_api_background() {
        InlinePictureEntity inlinePictureEntity = new InlinePictureEntity();
        inlinePictureEntity.setContent(PICTURE.getBytes());
        inlinePictureEntity.setType("image/jpeg");
        when(apiImagesService.getApiBackground(any(), any())).thenReturn(inlinePictureEntity);

        final Response response = rootTarget("background").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        byte[] responseBody = response.readEntity(byte[].class);
        assertThat(responseBody).isEqualTo(PICTURE.getBytes());
    }

    @Test
    public void should_get_no_background() {
        when(apiImagesService.getApiBackground(any(), any())).thenReturn(null);

        final Response response = rootTarget("background").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        byte[] responseBody = response.readEntity(byte[].class);
        assertThat(responseBody).isEmpty();
    }

    @Test
    public void should_update_api_background() {
        final Response response = rootTarget("background").request().put(Entity.text(PICTURE));
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        verify(apiImagesService, times(1)).updateApiBackground(GraviteeContext.getExecutionContext(), API, PICTURE);
    }

    @Test
    public void should_delete_api_background() {
        final Response response = rootTarget("background").request().delete();
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        verify(apiImagesService, times(1)).updateApiBackground(GraviteeContext.getExecutionContext(), API, null);
    }

    @Test
    public void should_get_invalid_format_background() {
        final Response response = rootTarget("background").request().put(Entity.text(""));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        verifyNoInteractions(apiImagesService);
    }
}
