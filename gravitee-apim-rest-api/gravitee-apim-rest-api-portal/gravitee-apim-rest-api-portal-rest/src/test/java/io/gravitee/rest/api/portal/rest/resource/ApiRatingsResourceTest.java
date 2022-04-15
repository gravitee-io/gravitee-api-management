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

import static io.gravitee.common.http.HttpStatusCode.*;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiRatingUnavailableException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiRatingsResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String RATING = "my-rating";

    protected String contextPath() {
        return "apis/";
    }

    @Before
    public void init() throws IOException {
        resetAllMocks();

        ApiEntity mockApi = new ApiEntity();
        mockApi.setId(API);
        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(mockApi));
        doReturn(mockApis)
            .when(apiService)
            .findPublishedByUser(eq(GraviteeContext.getExecutionContext()), any(), argThat(q -> singletonList(API).equals(q.getIds())));

        RatingEntity rating1 = new RatingEntity();
        RatingEntity rating2 = new RatingEntity();

        List<RatingEntity> ratingEntityPage = Arrays.asList(rating1, rating2);
        doReturn(ratingEntityPage).when(ratingService).findByApi(eq(GraviteeContext.getExecutionContext()), eq(API));

        Rating rating = new Rating();
        rating.setId(RATING);
        rating.setComment(RATING);
        rating.setValue(1);

        doReturn(rating).when(ratingMapper).convert(eq(GraviteeContext.getExecutionContext()), any(), any());

        RatingEntity createdRating = new RatingEntity();
        createdRating.setId(RATING);
        createdRating.setComment(RATING);
        createdRating.setRate((byte) 1);
        doReturn(createdRating).when(ratingService).create(eq(GraviteeContext.getExecutionContext()), any());
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    public void shouldNotFoundWhileGettingApiRatings() {
        //init
        ApiEntity userApi = new ApiEntity();
        userApi.setId("1");
        doReturn(emptySet())
            .when(apiService)
            .findPublishedByUser(eq(GraviteeContext.getExecutionContext()), any(), argThat(q -> singletonList(API).equals(q.getIds())));

        //test
        final Response response = target(API).path("ratings").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.api.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Api [" + API + "] can not be found.", error.getMessage());
    }

    @Test
    public void shouldGetServiceUnavailable() {
        doThrow(ApiRatingUnavailableException.class).when(ratingService).create(eq(GraviteeContext.getExecutionContext()), any());

        RatingInput ratingInput = new RatingInput().comment(RATING).value(1);

        final Response response = target(API).path("ratings").request().post(Entity.json(ratingInput));
        assertEquals(SERVICE_UNAVAILABLE_503, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.rating.disabled", error.getCode());
        assertEquals("503", error.getStatus());
        assertEquals("API rating service is unavailable.", error.getMessage());
    }

    @Test
    public void shouldGetApiRatings() {
        final Response response = target(API).path("ratings").request().get();

        assertEquals(OK_200, response.getStatus());

        final RatingsResponse ratingsResponse = response.readEntity(RatingsResponse.class);
        List<Rating> ratings = ratingsResponse.getData();
        assertNotNull(ratings);
        assertEquals(2, ratings.size());
    }

    @Test
    public void shouldGetMineApiRatings() {
        final Response response = target(API).path("ratings").queryParam("mine", true).request().get();

        assertEquals(OK_200, response.getStatus());

        final RatingsResponse ratingsResponse = response.readEntity(RatingsResponse.class);
        List<Rating> ratings = ratingsResponse.getData();
        assertNotNull(ratings);
        assertEquals(0, ratings.size());
    }

    @Test
    public void shouldCreateApiRating() {
        RatingInput ratingInput = new RatingInput().comment(RATING).value(1);

        final Response response = target(API).path("ratings").request().post(Entity.json(ratingInput));
        assertEquals(CREATED_201, response.getStatus());

        final Rating ratingResponse = response.readEntity(Rating.class);
        assertNotNull(ratingResponse);
        assertEquals(RATING, ratingResponse.getComment());
        assertEquals(Integer.valueOf(1), ratingResponse.getValue());
        assertEquals(RATING, ratingResponse.getId());
    }

    @Test
    public void shouldNotFoundWhileCreatingApiRatings() {
        //init
        ApiEntity userApi = new ApiEntity();
        userApi.setId("1");
        doReturn(emptySet())
            .when(apiService)
            .findPublishedByUser(eq(GraviteeContext.getExecutionContext()), any(), argThat(q -> singletonList(API).equals(q.getIds())));

        //test
        RatingInput ratingInput = new RatingInput().comment(RATING).value(1);

        final Response response = target(API).path("ratings").request().post(Entity.json(ratingInput));
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.api.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Api [" + API + "] can not be found.", error.getMessage());
    }

    @Test
    public void shouldBadRequestWhileCreatingApiRatings() {
        //init
        ApiEntity userApi = new ApiEntity();
        userApi.setId("1");
        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(userApi));
        doReturn(mockApis).when(apiService).findPublishedByUser(eq(GraviteeContext.getExecutionContext()), any());

        //test

        final Response response = target(API).path("ratings").request().post(Entity.json(null));
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
}
