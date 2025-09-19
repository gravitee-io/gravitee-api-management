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

import static java.util.stream.Collectors.toList;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.NewRatingEntity;
import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.RatingMapper;
import io.gravitee.rest.api.portal.rest.model.Rating;
import io.gravitee.rest.api.portal.rest.model.RatingInput;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiRatingsResource extends AbstractResource {

    @Autowired
    private RatingService ratingService;

    @Inject
    private RatingMapper ratingMapper;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApiRatingsByApiId(
        @PathParam("apiId") String apiId,
        @BeanParam PaginationParam paginationParam,
        @QueryParam("mine") Boolean mine,
        @QueryParam("order") String order
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), apiId)) {
            List<Rating> ratings;
            if (mine != null && mine) {
                RatingEntity ratingEntity = ratingService.findByApiForConnectedUser(executionContext, apiId);
                if (ratingEntity != null) {
                    ratings = Arrays.asList(ratingMapper.convert(executionContext, ratingEntity, uriInfo));
                } else {
                    ratings = Collections.emptyList();
                }
            } else {
                final List<RatingEntity> ratingEntities = ratingService.findByApi(executionContext, apiId);
                Stream<Rating> ratingStream = ratingEntities
                    .stream()
                    .map((RatingEntity ratingEntity) -> ratingMapper.convert(executionContext, ratingEntity, uriInfo));
                if (order != null) {
                    ratingStream = ratingStream.sorted(new RatingComparator(order));
                }

                ratings = ratingStream.collect(toList());
            }
            return createListResponse(executionContext, ratings, paginationParam, true);
        }
        throw new ApiNotFoundException(apiId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_RATING, acls = RolePermissionAction.CREATE) })
    public Response createApiRating(@PathParam("apiId") String apiId, @Valid RatingInput ratingInput) {
        if (ratingInput == null) {
            throw new BadRequestException("Input must not be null.");
        }
        final ApiQuery apiQuery = new ApiQuery();
        apiQuery.setIds(Collections.singletonList(apiId));
        if (accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), apiId)) {
            NewRatingEntity rating = new NewRatingEntity();
            rating.setApi(apiId);
            rating.setComment(ratingInput.getComment());
            rating.setTitle(ratingInput.getTitle());
            rating.setRate(ratingInput.getValue().byteValue());
            RatingEntity createdRating = ratingService.create(GraviteeContext.getExecutionContext(), rating);

            return Response.status(Status.CREATED)
                .entity(ratingMapper.convert(GraviteeContext.getExecutionContext(), createdRating, uriInfo))
                .build();
        }
        throw new ApiNotFoundException(apiId);
    }

    @Path("{ratingId}")
    public ApiRatingResource getApiRatingResource() {
        return resourceContext.getResource(ApiRatingResource.class);
    }

    public class RatingComparator implements Comparator<Rating> {

        private final List<String> orders;

        public RatingComparator(String order) {
            this.orders = new ArrayList<>();
            if (order != null) {
                this.orders.addAll(Arrays.asList(order.split(",")));
            }
        }

        @Override
        public int compare(Rating r1, Rating r2) {
            int compare = 0;
            for (String order : this.orders) {
                compare = compare(r1, r2, order);
                if (compare != 0) {
                    break;
                }
            }
            return compare;
        }

        private int compare(Rating r1, Rating r2, String order) {
            int compare = 0;
            if (order.contains("value")) {
                compare = r1.getValue().compareTo(r2.getValue());
            }
            if (order.contains("date")) {
                compare = r1.getDate().compareTo(r2.getDate());
            }

            if (order.contains("answers")) {
                compare = Integer.compare(r1.getAnswers().size(), r2.getAnswers().size());
            }

            if (!order.startsWith("-")) {
                compare = compare * -1;
            }
            return compare;
        }
    }
}
