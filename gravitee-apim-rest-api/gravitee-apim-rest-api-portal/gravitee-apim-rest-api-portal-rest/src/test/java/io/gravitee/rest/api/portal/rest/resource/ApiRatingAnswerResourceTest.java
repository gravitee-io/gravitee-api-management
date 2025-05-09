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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.RatingAnswerEntity;
import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiRatingAnswerResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String RATING = "my-rating";
    private static final String ANSWER = "my-answer";

    protected String contextPath() {
        return "apis/";
    }

    @BeforeEach
    public void init() {
        resetAllMocks();

        ApiEntity mockApi = new ApiEntity();
        mockApi.setId(API);
        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(true);

        RatingEntity ratingEntity = new RatingEntity();
        ratingEntity.setId(RATING);
        ratingEntity.setComment(RATING);
        ratingEntity.setApi(API);
        ratingEntity.setRate(Integer.valueOf(1).byteValue());
        RatingAnswerEntity answer = new RatingAnswerEntity();
        answer.setId(ANSWER);
        ratingEntity.setAnswers(Arrays.asList(answer));
        doReturn(ratingEntity).when(ratingService).findById(eq(GraviteeContext.getExecutionContext()), eq(RATING));
        doReturn(ratingEntity).when(ratingService).createAnswer(eq(GraviteeContext.getExecutionContext()), any());

        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    public void shouldHaveApiNotFoundWhenDeleteWithFakeApi() {
        final String fakeId = "fake";
        final Response response = target(fakeId).path("ratings").path(RATING).path("answers").path(ANSWER).request().delete();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.api.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Api [" + fakeId + "] cannot be found.", error.getMessage());
    }

    @Test
    public void shouldHaveRatingNotFoundWhenDeleteWithFakeRating() {
        final String fakeId = "fake";
        final Response response = target(API).path("ratings").path(fakeId).path("answers").path(ANSWER).request().delete();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.rating.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Rating [" + fakeId + "] cannot be found on the api [" + API + "]", error.getMessage());
    }

    @Test
    public void shouldHaveRatingAnswerNotFoundWhenDeleteWithFakeId() {
        final String fakeId = "fake";
        final Response response = target(API).path("ratings").path(fakeId).path("answers").path(ANSWER).request().delete();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.rating.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Rating [" + fakeId + "] cannot be found on the api [" + API + "]", error.getMessage());
    }

    @Test
    public void shouldDeleteApiRatingAnswer() {
        final Response response = target(API).path("ratings").path(RATING).path("answers").path(ANSWER).request().delete();
        assertEquals(NO_CONTENT_204, response.getStatus());
    }
}
