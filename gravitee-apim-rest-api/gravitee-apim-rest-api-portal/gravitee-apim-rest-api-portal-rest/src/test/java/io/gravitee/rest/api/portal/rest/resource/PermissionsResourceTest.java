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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PermissionsResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String APPLICATION = "my-app";

    protected String contextPath() {
        return "permissions/";
    }

    @BeforeEach
    public void init() {
        resetAllMocks();
        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(true);

        ApplicationListItem mockAppListItem = new ApplicationListItem();
        mockAppListItem.setId(APPLICATION);
        Set<ApplicationListItem> mockApps = new HashSet<>(Arrays.asList(mockAppListItem));
        doReturn(mockApps).when(applicationService).findByUser(eq(GraviteeContext.getExecutionContext()), any());

        ApplicationEntity mockAppEntity = new ApplicationEntity();
        mockAppEntity.setId(APPLICATION);

        doReturn(mockAppEntity).when(applicationService).findById(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldHaveBadRequestExceptionWithoutQueryParam() {
        final Response response = target().request().get();
        assertEquals(BAD_REQUEST_400, response.getStatus());
        List<Error> errors = response.readEntity(ErrorResponse.class).getErrors();
        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.unexpected", error.getCode());
        assertEquals("One of the two parameters appId or applicationId must not be null.", error.getMessage());
    }

    @Test
    public void shouldHaveApiNotFoundExceptionWithFakeApiId() {
        final Response response = target().queryParam("apiId", "fake").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());
        List<Error> errors = response.readEntity(ErrorResponse.class).getErrors();
        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.api.notFound", error.getCode());
        assertEquals("Api [fake] cannot be found.", error.getMessage());
    }

    @Test
    public void shouldHavePermissionsWithApiId() {
        final Response response = target().queryParam("apiId", API).request().get();
        assertEquals(OK_200, response.getStatus());
        final Map permissionsResponse = response.readEntity(Map.class);
        assertNotNull(permissionsResponse);
    }

    @Test
    public void shouldHaveApplicationNotFoundExceptionWithFakeApplicationId() {
        final Response response = target().queryParam("applicationId", "fake").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());
        List<Error> errors = response.readEntity(ErrorResponse.class).getErrors();
        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.application.notFound", error.getCode());
        assertEquals("Application [fake] cannot be found.", error.getMessage());
    }

    @Test
    public void shouldHavePermissionsWithApplicationId() {
        final Response response = target().queryParam("applicationId", APPLICATION).request().get();
        assertEquals(OK_200, response.getStatus());
        final Map permissionsResponse = response.readEntity(Map.class);
        assertNotNull(permissionsResponse);
    }
}
