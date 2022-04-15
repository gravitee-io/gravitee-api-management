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

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Rating;
import io.gravitee.rest.api.portal.rest.model.RatingInput;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiRatingResourceTest extends AbstractResourceTest {

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
        doReturn(mockApis)
            .when(apiService)
            .findPublishedByUser(eq(GraviteeContext.getExecutionContext()), any(), argThat(q -> singletonList(API).equals(q.getIds())));

        RatingEntity ratingEntity = new RatingEntity();
        ratingEntity.setId(RATING);
        ratingEntity.setComment(RATING);
        ratingEntity.setApi(API);
        ratingEntity.setRate(Integer.valueOf(1).byteValue());
        doReturn(ratingEntity).when(ratingService).findById(eq(GraviteeContext.getExecutionContext()), eq(RATING));
        doReturn(ratingEntity).when(ratingService).update(eq(GraviteeContext.getExecutionContext()), any());
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    public void shouldHaveApiNotFoundWhenUpdateWithFakeApiId() {
        RatingInput ratingInput = new RatingInput().comment(RATING).value(2);
        final String fakeId = "fake";
        final Response response = target(fakeId).path("ratings").path(RATING).request().put(Entity.json(ratingInput));
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
    public void shouldHaveRatingNotFoundWhenUpdateWithFakeRatingId() {
        RatingInput ratingInput = new RatingInput().comment(RATING).value(2);
        final String fakeId = "fake";
        final Response response = target(API).path("ratings").path(fakeId).request().put(Entity.json(ratingInput));
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
    public void shouldUpdateApiRating() {
        RatingInput ratingInput = new RatingInput().comment(RATING).value(2);
        Rating rating = new Rating();
        rating.setId(RATING);
        rating.setValue(2);

        doReturn(rating).when(ratingMapper).convert(eq(GraviteeContext.getExecutionContext()), any(), any());
        final Response response = target(API).path("ratings").path(RATING).request().put(Entity.json(ratingInput));
        Rating updatedRatingResponse = response.readEntity(Rating.class);
        assertNotNull(updatedRatingResponse);
        assertEquals(rating.getValue(), updatedRatingResponse.getValue());
    }

    @Test
    public void shouldHaveApiNotFoundWhenDeleteWithFakeApiId() {
        final String fakeId = "fake";
        final Response response = target(fakeId).path("ratings").path(RATING).request().delete();
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
    public void shouldHaveRatingNotFoundWhenDeleteWithFakeRatingId() {
        final String fakeId = "fake";
        final Response response = target(API).path("ratings").path(fakeId).request().delete();
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
    public void shouldDeleteApiRating() {
        final Response deleteResponse = target(API).path("ratings").path(RATING).request().delete();
        assertEquals(NO_CONTENT_204, deleteResponse.getStatus());
    }
}
