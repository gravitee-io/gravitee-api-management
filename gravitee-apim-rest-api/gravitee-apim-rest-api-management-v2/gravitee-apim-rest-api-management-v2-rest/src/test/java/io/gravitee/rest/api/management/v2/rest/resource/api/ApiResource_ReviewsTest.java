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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.ApiFixtures;
import io.gravitee.rest.api.management.v2.rest.model.ApiReview;
import io.gravitee.rest.api.model.ReviewEntity;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class ApiResource_ReviewsTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/reviews";
    }

    @Test
    public void should_ask_review() {
        ApiReview apiReview = new ApiReview();
        apiReview.setMessage("My comment");

        var apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).updatedAt(new Date()).build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        when(
            apiWorkflowStateService.askForReview(eq(GraviteeContext.getExecutionContext()), eq(API), eq(USER_NAME), any(ReviewEntity.class))
        )
            .thenReturn(apiEntity);

        final Response response = rootTarget("_ask").request().post(Entity.json(apiReview));

        assertEquals(NO_CONTENT_204, response.getStatus());
        verify(apiWorkflowStateService)
            .askForReview(
                eq(GraviteeContext.getExecutionContext()),
                eq(API),
                eq(USER_NAME),
                argThat(reviewEntity -> reviewEntity.getMessage().equals(apiReview.getMessage()))
            );
    }

    @Test
    public void should_return_400_when_asking_review_still_in_progress() {
        ApiReview apiReview = new ApiReview();
        apiReview.setMessage("My comment");

        var apiEntity = ApiFixtures
            .aModelHttpApiV4()
            .toBuilder()
            .id(API)
            .workflowState(WorkflowState.IN_REVIEW)
            .updatedAt(new Date())
            .build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        final Response response = rootTarget("_ask").request().post(Entity.json(apiReview));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(apiWorkflowStateService, never()).askForReview(any(), any(), any(), any());
    }

    @Test
    public void should_return_400_when_API_is_ARCHIVED() {
        ApiReview apiReview = new ApiReview();
        apiReview.setMessage("My comment");

        var apiEntity = ApiFixtures
            .aModelHttpApiV4()
            .toBuilder()
            .id(API)
            .lifecycleState(ApiLifecycleState.ARCHIVED)
            .updatedAt(new Date())
            .build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        final Response response = rootTarget("_ask").request().post(Entity.json(apiReview));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(apiWorkflowStateService, never()).askForReview(any(), any(), any(), any());
    }

    @Test
    public void should_accept_review() {
        ApiReview apiReview = new ApiReview();
        apiReview.setMessage("My comment");

        var apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).updatedAt(new Date()).build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        when(
            apiWorkflowStateService.acceptReview(eq(GraviteeContext.getExecutionContext()), eq(API), eq(USER_NAME), any(ReviewEntity.class))
        )
            .thenReturn(apiEntity);

        final Response response = rootTarget("_accept").request().post(Entity.json(apiReview));

        assertEquals(NO_CONTENT_204, response.getStatus());
        verify(apiWorkflowStateService)
            .acceptReview(
                eq(GraviteeContext.getExecutionContext()),
                eq(API),
                eq(USER_NAME),
                argThat(reviewEntity -> reviewEntity.getMessage().equals(apiReview.getMessage()))
            );
    }

    @Test
    public void should_return_400_when_accept_review_on_DRAFT_API() {
        ApiReview apiReview = new ApiReview();
        apiReview.setMessage("My comment");

        var apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).workflowState(WorkflowState.DRAFT).updatedAt(new Date()).build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        final Response response = rootTarget("_accept").request().post(Entity.json(apiReview));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(apiWorkflowStateService, never()).askForReview(any(), any(), any(), any());
    }

    @Test
    public void should_reject_review() {
        ApiReview apiReview = new ApiReview();
        apiReview.setMessage("My comment");

        var apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).updatedAt(new Date()).build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        when(
            apiWorkflowStateService.rejectReview(eq(GraviteeContext.getExecutionContext()), eq(API), eq(USER_NAME), any(ReviewEntity.class))
        )
            .thenReturn(apiEntity);

        final Response response = rootTarget("_reject").request().post(Entity.json(apiReview));

        assertEquals(NO_CONTENT_204, response.getStatus());
        verify(apiWorkflowStateService)
            .rejectReview(
                eq(GraviteeContext.getExecutionContext()),
                eq(API),
                eq(USER_NAME),
                argThat(reviewEntity -> reviewEntity.getMessage().equals(apiReview.getMessage()))
            );
    }

    @Test
    public void should_return_400_when_reject_review_on_DRAFT_API() {
        ApiReview apiReview = new ApiReview();
        apiReview.setMessage("My comment");

        var apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).workflowState(WorkflowState.DRAFT).updatedAt(new Date()).build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        final Response response = rootTarget("_reject").request().post(Entity.json(apiReview));

        assertEquals(BAD_REQUEST_400, response.getStatus());
        verify(apiWorkflowStateService, never()).askForReview(any(), any(), any(), any());
    }
}
