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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.Rating;
import io.gravitee.rest.api.portal.rest.model.RatingInput;
import io.gravitee.rest.api.portal.rest.model.RatingsResponse;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiRatingsResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String RATING = "my-rating";

    private static final String FORBIDDEN_API = "my-forbidden-api";

    protected String contextPath() {
        return "apis/";
    }

    private ApiEntity mockApi;
    private ApiEntity forbiddenApi;

    @Before
    public void init() throws IOException {
        reset(apiService);
        reset(ratingService);
        reset(ratingMapper);
        
        mockApi = new ApiEntity();
        mockApi.setId(API);
        mockApi.setVisibility(Visibility.PUBLIC);
        doReturn(mockApi).when(apiService).findById(API);
        
        RatingEntity rating1 = new RatingEntity();
        RatingEntity rating2 = new RatingEntity();

        Page<RatingEntity> ratingEntityPage = new Page<RatingEntity>(Arrays.asList(rating1, rating2), 1, 2, 2);
        doReturn(ratingEntityPage).when(ratingService).findByApi(eq(API), any());
        
        forbiddenApi = new ApiEntity();
        forbiddenApi.setVisibility(Visibility.PRIVATE);
        doReturn(forbiddenApi).when(apiService).findById(FORBIDDEN_API);

        doReturn(new Rating()
                .id(RATING)
                .comment(RATING)
                .value(1)).when(ratingMapper).convert(any());
        
        RatingEntity createdRating = new RatingEntity();
        createdRating.setId(RATING);
        createdRating.setComment(RATING);
        createdRating.setRate((byte)1);
        doReturn(createdRating).when(ratingService).create(any());
    }

    
    @Test
    public void shouldGetForbiddenAccess() {
        final Response response = target(FORBIDDEN_API).path("ratings").request().get();

        assertEquals(FORBIDDEN_403, response.getStatus());
        
        final Error error = response.readEntity(Error.class);
        
        assertNotNull(error);
        assertEquals("403", error.getCode());
        assertEquals("io.gravitee.rest.api.service.exceptions.ForbiddenAccessException", error.getTitle());
        assertEquals("You do not have sufficient rights to access this resource", error.getDetail());
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
    public void shouldCreateApiRating() {
        RatingInput ratingInput = new RatingInput()
                .comment(RATING)
                .value(1)
                ;
        
        final Response response = target(API).path("ratings").request().post(Entity.json(ratingInput));
        assertEquals(OK_200, response.getStatus());

        final Rating ratingResponse = response.readEntity(Rating.class);
        assertNotNull(ratingResponse);
        assertEquals(RATING, ratingResponse.getComment());
        assertEquals(Integer.valueOf(1), ratingResponse.getValue());
        assertEquals(RATING, ratingResponse.getId());
        
    }
}
