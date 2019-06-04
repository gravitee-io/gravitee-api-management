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

import static io.gravitee.rest.api.model.Visibility.PUBLIC;
import static java.util.stream.Collectors.toList;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.model.NewRatingEntity;
import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.mapper.RatingMapper;
import io.gravitee.rest.api.portal.rest.model.Rating;
import io.gravitee.rest.api.portal.rest.model.RatingInput;
import io.gravitee.rest.api.portal.rest.model.RatingsResponse;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
//APIPortal: only if ratings is activated ?
public class ApiRatingsResource extends AbstractResource {
    
    @Context
    private UriInfo uriInfo;
    
    @Autowired
    private RatingService ratingService;
    
    @Inject
    private RatingMapper ratingMapper;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiRatingsByApiId(@PathParam("apiId") String apiId, @DefaultValue(PAGE_QUERY_PARAM_DEFAULT) @QueryParam("page") Integer page, @DefaultValue(SIZE_QUERY_PARAM_DEFAULT) @QueryParam("size") Integer size) {
        final ApiEntity apiEntity = apiService.findById(apiId);
        if (PUBLIC.equals(apiEntity.getVisibility())) {
            final Page<RatingEntity> ratingEntityPage = ratingService.findByApi(apiId, new PageableBuilder().pageNumber(page).pageSize(size).build());
            
            List<Rating> ratings = ratingEntityPage.getContent().stream()
                    .map(ratingMapper::convert)
                    .collect(toList());
            
            int totalItems = ratings.size();
            
            //No Need to paginate since the entity list has been filtered yet
            //ratings = this.paginateResultList(ratings, page, size)           
            
            RatingsResponse response = new RatingsResponse()
                    .data(ratings)
                    .links(this.computePaginatedLinks(uriInfo, page, size, totalItems))
                    ;
            
            return Response.ok(response).build();
        }
        
        throw new ForbiddenAccessException();
        
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createApiRatingForApi(@PathParam("apiId") String apiId, @Valid RatingInput ratingInput) {
        NewRatingEntity rating = new NewRatingEntity();
        rating.setApi(apiId);
        rating.setComment(ratingInput.getComment());
        // APIPortal: missing 'Title' in ratingInput
        // rating.setTitle()
        rating.setRate(ratingInput.getValue().byteValue());
        
        RatingEntity createdRating = ratingService.create(rating);
        
        return Response
                .ok(ratingMapper.convert(createdRating))
                .build();
    }

}
