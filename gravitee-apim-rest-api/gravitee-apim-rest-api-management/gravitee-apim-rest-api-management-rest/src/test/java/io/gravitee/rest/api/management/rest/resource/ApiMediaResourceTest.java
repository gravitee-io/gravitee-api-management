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
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiMediaNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.ws.rs.client.Invocation;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author GraviteeSource Team
 */
public class ApiMediaResourceTest extends AbstractResourceTest {

    private static final String API_ID = "42cf6d9f-de3d-4a99-a6f7-f6a7d2d81b39";
    private static final String MEDIA_HASH = "72A6AF6B72587FE1720AC31F802F9DD6";
    private static final String RESOURCE_PATH = "media";

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAdminAuthenticationFilter.class);
    }

    @Before
    public void init() {
        Mockito.reset(mediaService);
        Mockito.reset(configService);
        Mockito.reset(objectMapper);
    }

    @Test
    public void shouldReturn500IfTechnicalExceptionOnDeleteByHashAndApi() {
        when(mediaService.findByHashAndApiId(MEDIA_HASH, API_ID)).thenReturn(new MediaEntity());
        doThrow(TechnicalManagementException.class).when(mediaService).deleteByHashAndApi(MEDIA_HASH, API_ID);
        assertEquals(INTERNAL_SERVER_ERROR_500, mediaRequest().delete().getStatus());
    }

    @Test
    public void shouldReturn404IfMediaNotFoundOnDeleteByHashAndApi() {
        doThrow(new ApiMediaNotFoundException(MEDIA_HASH, API_ID)).when(mediaService).deleteByHashAndApi(MEDIA_HASH, API_ID);
        assertEquals(NOT_FOUND_404, mediaRequest().delete().getStatus());
    }

    @Test
    public void shouldReturn403IfNotGrantedOnDeleteByHashAndApi() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        assertEquals(FORBIDDEN_403, mediaRequest().delete().getStatus());
    }

    @Test
    public void shouldReturn204IfGrantedOnDeleteByHashAndApi() {
        assertEquals(NO_CONTENT_204, mediaRequest().delete().getStatus());
    }

    private Invocation.Builder mediaRequest() {
        return envTarget(API_ID).path(RESOURCE_PATH).path(MEDIA_HASH).request();
    }
}
