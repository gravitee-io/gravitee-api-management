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

import static io.gravitee.definition.model.DefinitionContext.*;
import static jakarta.ws.rs.client.Entity.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.api.DefinitionContextEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class ApiDefinitionContextResourceTest extends AbstractResourceTest {

    private static final String API_ID = "d635e8d0-8b19-40f6-8cc3-e53633109e08";
    private static final String DEFINITION_CONTEXT_PATH = "definition-context";

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Test
    public void shouldReturn500IfTechnicalException() {
        doThrow(new TechnicalManagementException()).when(definitionContextService).setDefinitionContext(eq(API_ID), any());

        Response response = envTarget(API_ID).path(DEFINITION_CONTEXT_PATH).request().put(json(newKubernetesContext()));

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void shouldReturn403IfNotGranted() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        Response response = envTarget(API_ID).path(DEFINITION_CONTEXT_PATH).request().put(json(newKubernetesContext()));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldReturn200AndSetDefinitionContext() {
        Response response = envTarget(API_ID).path(DEFINITION_CONTEXT_PATH).request().put(json(newKubernetesContext()));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verify(definitionContextService)
            .setDefinitionContext(
                eq(API_ID),
                argThat(context -> {
                    assertThat(context).usingRecursiveComparison().isEqualTo(newKubernetesContext());
                    return true;
                })
            );
    }

    @Test
    public void shouldReturn400WithNullOrigin() {
        DefinitionContextEntity definitionContextEntity = new DefinitionContextEntity(null, MODE_FULLY_MANAGED, ORIGIN_KUBERNETES);
        Response response = envTarget(API_ID).path(DEFINITION_CONTEXT_PATH).request().put(json(definitionContextEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldReturn400WithUnknownOrigin() {
        DefinitionContextEntity definitionContextEntity = new DefinitionContextEntity("unknown", MODE_FULLY_MANAGED, ORIGIN_KUBERNETES);
        Response response = envTarget(API_ID).path(DEFINITION_CONTEXT_PATH).request().put(json(definitionContextEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldReturn400WithNullMode() {
        DefinitionContextEntity definitionContextEntity = new DefinitionContextEntity(ORIGIN_KUBERNETES, null, ORIGIN_KUBERNETES);
        Response response = envTarget(API_ID).path(DEFINITION_CONTEXT_PATH).request().put(json(definitionContextEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldReturn400WithUnknownMode() {
        DefinitionContextEntity definitionContextEntity = new DefinitionContextEntity(ORIGIN_KUBERNETES, "unknown", ORIGIN_KUBERNETES);
        Response response = envTarget(API_ID).path(DEFINITION_CONTEXT_PATH).request().put(json(definitionContextEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldReturn400WithNullSyncFrom() {
        DefinitionContextEntity definitionContextEntity = new DefinitionContextEntity(ORIGIN_KUBERNETES, MODE_FULLY_MANAGED, null);
        Response response = envTarget(API_ID).path(DEFINITION_CONTEXT_PATH).request().put(json(definitionContextEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldReturn400WithUnknownSyncFrom() {
        DefinitionContextEntity definitionContextEntity = new DefinitionContextEntity(ORIGIN_KUBERNETES, MODE_FULLY_MANAGED, "unknown");
        Response response = envTarget(API_ID).path(DEFINITION_CONTEXT_PATH).request().put(json(definitionContextEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    private static DefinitionContextEntity newKubernetesContext() {
        return new DefinitionContextEntity(ORIGIN_KUBERNETES, MODE_FULLY_MANAGED, ORIGIN_KUBERNETES);
    }
}
