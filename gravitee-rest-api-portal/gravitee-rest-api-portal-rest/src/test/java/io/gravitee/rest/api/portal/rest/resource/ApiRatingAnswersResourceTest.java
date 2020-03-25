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

import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Error;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.*;

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiRatingAnswersResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String RATING = "my-rating";

    protected String contextPath() {
        return "apis/";
    }

    @Before
    public void init() {
        resetAllMocks();

        ApiEntity mockApi = new ApiEntity();
        mockApi.setId(API);
        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(mockApi));
        doReturn(mockApis).when(apiService).findPublishedByUser(any());

        RatingEntity ratingEntity = new RatingEntity();
        ratingEntity.setId(RATING);
        ratingEntity.setApi(API);
        ratingEntity.setComment(RATING);
        ratingEntity.setRate(Integer.valueOf(1).byteValue());
        doReturn(ratingEntity).when(ratingService).findById(eq(RATING));
        doReturn(ratingEntity).when(ratingService).createAnswer(any());

    }

    @Test
    public void shouldHaveBadRequestWhenCreateApiRatingAnswerWithoutInput(){
        final Response response = target(API).path("ratings").path(RATING).path("answers").request().post(null);
        assertEquals(BAD_REQUEST_400, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.unexpected", error.getCode());
        assertEquals("400", error.getStatus());
        assertEquals("Input must not be null.", error.getMessage());
    }

    @Test
    public void shouldHaveApiNotFoundWhenCreateApiRatingAnswerWithFakeApiId() {
        RatingAnswerInput ratingAnswerInput = new RatingAnswerInput().comment(RATING);

        final String fakeId = "fake";
        final Response response = target(fakeId).path("ratings").path(RATING).path("answers").request().post(Entity.json(ratingAnswerInput));
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.api.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Api [" + fakeId + "] can not be found.", error.getMessage());
    }

    @Test
    public void shouldHaveRatingNotFoundWhenCreateApiRatingAnswerWithFakeRatingId() {
        RatingAnswerInput ratingAnswerInput = new RatingAnswerInput().comment(RATING);
        final String fakeId = "fake";
        final Response response = target(API).path("ratings").path(fakeId).path("answers").request().post(Entity.json(ratingAnswerInput));
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.rating.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Rating [" + fakeId + "] can not be found on the api [" + API + "]", error.getMessage());
    }

    @Test
    public void shouldCreateApiRatingAnswer() {
        RatingAnswerInput ratingAnswerInput = new RatingAnswerInput().comment(RATING);
        Rating rating = new Rating();
        rating.setId(RATING);
        rating.setValue(2);

        doReturn(rating).when(ratingMapper).convert(any(), any());
        final Response response = target(API).path("ratings").path(RATING).path("answers").request().post(Entity.json(ratingAnswerInput));
        Rating updatedRatingResponse = response.readEntity(Rating.class);
        assertNotNull(updatedRatingResponse);
        assertEquals(rating.getValue(), updatedRatingResponse.getValue());
    }

}
