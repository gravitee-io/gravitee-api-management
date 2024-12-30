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

import static io.gravitee.rest.api.model.WorkflowType.REVIEW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.ApiFixtures;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.model.WorkflowReferenceType;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApiResource_StartTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_start";
    }

    @Override
    @BeforeEach
    public void init() throws TechnicalException {
        super.init();
        reset(apiLicenseService);
    }

    @Test
    public void should_not_start_api_with_insufficient_rights() {
        when(permissionService.hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_DEFINITION), eq(API), any()))
            .thenReturn(false);
        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_not_start_api_with_incorrect_if_match() {
        var apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        final Response response = rootTarget().request().header(HttpHeaders.IF_MATCH, "\"000\"").post(Entity.json(""));
        assertEquals(HttpStatusCode.PRECONDITION_FAILED_412, response.getStatus());

        var body = response.readEntity(String.class);
        assert (body.isEmpty());

        verify(apiStateServiceV4, never()).start(any(), any(), any());
    }

    @Test
    public void should_not_start_api_if_not_found() {
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API)))
            .thenThrow(new ApiNotFoundException(API));

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());

        var body = response.readEntity(Error.class);
        assertNotNull(body);

        verify(apiStateServiceV4, never()).start(any(), any(), any());
    }

    @Test
    public void should_not_start_api_if_archived() {
        var apiEntity = ApiFixtures
            .aModelHttpApiV4()
            .toBuilder()
            .id(API)
            .state(Lifecycle.State.STOPPED)
            .lifecycleState(ApiLifecycleState.ARCHIVED)
            .build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        var body = response.readEntity(Error.class);
        assertNotNull(body);
        assertEquals("Deleted API cannot be started", body.getMessage());

        verify(apiStateServiceV4, never()).start(any(), any(), any());
    }

    @Test
    public void should_not_start_api_with_failing_license_check() {
        var apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).state(Lifecycle.State.STOPPED).build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);
        doThrow(new ForbiddenFeatureException("apim-en-endpoint-kafka")).when(apiLicenseService).checkLicense(any(), anyString());
        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());

        var body = response.readEntity(Error.class);
        assertNotNull(body);
        assertEquals("Feature 'apim-en-endpoint-kafka' is not available with your license tier", body.getMessage());

        verify(apiStateServiceV4, never()).start(any(), any(), any());
    }

    @Test
    public void should_not_start_api_if_already_started() {
        var apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).state(Lifecycle.State.STARTED).build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        var body = response.readEntity(Error.class);
        assertNotNull(body);
        assertEquals("API is already started", body.getMessage());

        verify(apiStateServiceV4, never()).start(any(), any(), any());
    }

    @Test
    public void should_not_start_api_if_not_review_ok() {
        var apiEntity = ApiFixtures
            .aModelHttpApiV4()
            .toBuilder()
            .id(API)
            .state(Lifecycle.State.STOPPED)
            .workflowState(WorkflowState.IN_REVIEW)
            .build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        when(
            parameterService.findAsBoolean(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.API_REVIEW_ENABLED),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        )
            .thenReturn(true);

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        var body = response.readEntity(Error.class);
        assertNotNull(body);
        assertEquals("API cannot be started without being reviewed", body.getMessage());

        verify(apiStateServiceV4, never()).start(any(), any(), any());
    }

    @Test
    public void should_start_api() {
        var apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).state(Lifecycle.State.STOPPED).build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        when(apiStateServiceV4.start(eq(GraviteeContext.getExecutionContext()), eq(API), eq(USER_NAME))).thenReturn(apiEntity);

        when(
            parameterService.findAsBoolean(
                eq(GraviteeContext.getExecutionContext()),
                eq(Key.API_REVIEW_ENABLED),
                eq(ParameterReferenceType.ENVIRONMENT)
            )
        )
            .thenReturn(true);

        var workflowOk = new Workflow();
        workflowOk.setState("REVIEW_OK");

        var workflowList = new ArrayList<Workflow>();
        workflowList.add(workflowOk);

        when(workflowService.findByReferenceAndType(eq(WorkflowReferenceType.API), eq(API), eq(REVIEW))).thenReturn(workflowList);

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        assertEquals("\"" + apiEntity.getUpdatedAt().getTime() + "\"", response.getHeaders().get("Etag").get(0));

        verify(apiStateServiceV4, times(1)).start(eq(GraviteeContext.getExecutionContext()), eq(API), eq(USER_NAME));
    }
}
