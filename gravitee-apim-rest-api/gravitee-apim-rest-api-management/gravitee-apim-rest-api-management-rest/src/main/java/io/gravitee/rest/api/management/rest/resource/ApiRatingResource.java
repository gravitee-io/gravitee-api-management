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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.rest.api.model.Visibility.PUBLIC;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "API Ratings")
public class ApiRatingResource extends AbstractResource {

    @Inject
    private RatingService ratingService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @Parameter(name = "api", hidden = true)
    private String api;

    @GET
    @Operation(summary = "List ratings for an API")
    @Produces(MediaType.APPLICATION_JSON)
    public Page<RatingEntity> getApiRating(@Min(1) @QueryParam("pageNumber") int pageNumber, @QueryParam("pageSize") int pageSize) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final ApiEntity apiEntity = apiService.findById(executionContext, api);
        if (PUBLIC.equals(apiEntity.getVisibility()) || hasPermission(RolePermission.API_RATING, api, RolePermissionAction.READ)) {
            final Page<RatingEntity> ratingEntityPage = ratingService.findByApi(
                executionContext,
                api,
                new PageableBuilder().pageNumber(pageNumber).pageSize(pageSize).build()
            );
            final List<RatingEntity> filteredRatings = ratingEntityPage
                .getContent()
                .stream()
                .map(ratingEntity -> filterPermission(api, ratingEntity))
                .collect(toList());
            return new Page<>(
                filteredRatings,
                ratingEntityPage.getPageNumber(),
                (int) ratingEntityPage.getPageElements(),
                ratingEntityPage.getTotalElements()
            );
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    @Path("current")
    @GET
    @Operation(summary = "Retrieve current rating for an API provided by the authenticated user")
    @Produces(MediaType.APPLICATION_JSON)
    public RatingEntity getApiRatingByApiAndUser() {
        if (!isAuthenticated()) {
            return null;
        }
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final ApiEntity apiEntity = apiService.findById(executionContext, api);
        if (PUBLIC.equals(apiEntity.getVisibility()) || hasPermission(RolePermission.API_RATING, api, RolePermissionAction.READ)) {
            return filterPermission(api, ratingService.findByApiForConnectedUser(executionContext, api));
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    @Path("summary")
    @Operation(summary = "Get the rating summary for an API")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RatingSummaryEntity getApiRatingSummaryByApi() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final ApiEntity apiEntity = apiService.findById(executionContext, api);
        if (PUBLIC.equals(apiEntity.getVisibility()) || hasPermission(RolePermission.API_RATING, api, RolePermissionAction.READ)) {
            return ratingService.findSummaryByApi(executionContext, api);
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create a new rating for an API",
        description = "User must have the API_RATING[CREATE] permission to use this service"
    )
    @Permissions({ @Permission(value = RolePermission.API_RATING, acls = RolePermissionAction.CREATE) })
    public RatingEntity createApiRating(@Valid @NotNull final NewRatingEntity rating) {
        rating.setApi(api);
        return filterPermission(api, ratingService.create(GraviteeContext.getExecutionContext(), rating));
    }

    @Path("{rating}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update an existing rating for an API",
        description = "User must have the API_RATING[UPDATE] permission to use this service"
    )
    @Permissions({ @Permission(value = RolePermission.API_RATING, acls = RolePermissionAction.UPDATE) })
    public RatingEntity updateApiRating(@PathParam("rating") String rating, @Valid @NotNull final UpdateRatingEntity ratingEntity) {
        ratingEntity.setId(rating);
        ratingEntity.setApi(api);
        return filterPermission(api, ratingService.update(GraviteeContext.getExecutionContext(), ratingEntity));
    }

    @Path("{rating}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete an existing rating for an API",
        description = "User must have the API_RATING[DELETE] permission to use this service"
    )
    @Permissions({ @Permission(value = RolePermission.API_RATING, acls = RolePermissionAction.DELETE) })
    public void deleteApiRating(@PathParam("rating") String rating) {
        ratingService.delete(GraviteeContext.getExecutionContext(), rating);
    }

    @Path("{rating}/answers")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create an answer to a rating for an API",
        description = "User must have the API_RATING_ANSWER[CREATE] permission to use this service"
    )
    @Permissions({ @Permission(value = RolePermission.API_RATING_ANSWER, acls = RolePermissionAction.CREATE) })
    public RatingEntity createApiRatingAnswer(@PathParam("rating") String rating, @Valid @NotNull final NewRatingAnswerEntity answer) {
        answer.setRatingId(rating);
        return filterPermission(api, ratingService.createAnswer(GraviteeContext.getExecutionContext(), answer));
    }

    @Path("{rating}/answers/{answer}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete an answer to a rating for an API",
        description = "User must have the API_RATING_ANSWER[DELETE] permission to use this service"
    )
    @Permissions({ @Permission(value = RolePermission.API_RATING_ANSWER, acls = RolePermissionAction.DELETE) })
    public void deleteApiRatingAnswer(@PathParam("rating") String rating, @PathParam("answer") String answer) {
        ratingService.deleteAnswer(GraviteeContext.getExecutionContext(), rating, answer);
    }

    private RatingEntity filterPermission(final String api, final RatingEntity ratingEntity) {
        if (!hasPermission(RolePermission.API_RATING_ANSWER, api, RolePermissionAction.READ) && ratingEntity != null) {
            ratingEntity.setAnswers(null);
        }
        return ratingEntity;
    }
}
