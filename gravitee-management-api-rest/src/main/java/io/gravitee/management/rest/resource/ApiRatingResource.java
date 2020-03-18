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
package io.gravitee.management.rest.resource;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.*;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.RatingService;
import io.gravitee.management.service.exceptions.UnauthorizedAccessException;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;

import static io.gravitee.management.model.Visibility.PUBLIC;
import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API", "Rating"})
public class ApiRatingResource extends AbstractResource {

    @Autowired
    private RatingService ratingService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Page<RatingEntity> list(@PathParam("api") String api, @Min(1) @QueryParam("pageNumber") int pageNumber, @QueryParam("pageSize") int pageSize) {
        final ApiEntity apiEntity = apiService.findById(api);
        if (PUBLIC.equals(apiEntity.getVisibility()) || hasPermission(RolePermission.API_RATING, api, RolePermissionAction.READ)) {
            final Page<RatingEntity> ratingEntityPage =
                    ratingService.findByApi(api, new PageableBuilder().pageNumber(pageNumber).pageSize(pageSize).build());
            final List<RatingEntity> filteredRatings =
                    ratingEntityPage.getContent().stream().map(ratingEntity -> filterPermission(api, ratingEntity)).collect(toList());
            return new Page<>(filteredRatings, ratingEntityPage.getPageNumber(), (int) ratingEntityPage.getPageElements(), ratingEntityPage.getTotalElements());
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    @Path("current")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RatingEntity getByApiAndUser(@PathParam("api") String api) {
        if (!isAuthenticated()) {
            return null;
        }
        final ApiEntity apiEntity = apiService.findById(api);
        if (PUBLIC.equals(apiEntity.getVisibility()) || hasPermission(RolePermission.API_RATING, api, RolePermissionAction.READ)) {
            return filterPermission(api, ratingService.findByApiForConnectedUser(api));
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    @Path("summary")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RatingSummaryEntity getSummaryByApi(@PathParam("api") String api) {
        final ApiEntity apiEntity = apiService.findById(api);
        if (PUBLIC.equals(apiEntity.getVisibility()) || hasPermission(RolePermission.API_RATING, api, RolePermissionAction.READ)) {
            return ratingService.findSummaryByApi(api);
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.API_RATING, acls = RolePermissionAction.CREATE)
    })
    public RatingEntity create(@PathParam("api") String api, @Valid @NotNull final NewRatingEntity rating) {
        rating.setApi(api);
        return filterPermission(api, ratingService.create(rating));
    }

    @Path("{rating}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.API_RATING, acls = RolePermissionAction.UPDATE)
    })
    public RatingEntity update(@PathParam("api") String api, @PathParam("rating") String rating, @Valid @NotNull final UpdateRatingEntity ratingEntity) {
        ratingEntity.setId(rating);
        ratingEntity.setApi(api);
        return filterPermission(api, ratingService.update(ratingEntity));
    }

    @Path("{rating}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.API_RATING, acls = RolePermissionAction.DELETE)
    })
    public void deleteRating(@PathParam("api") String api, @PathParam("rating") String rating) {
        ratingService.delete(rating);
    }

    @Path("{rating}/answers")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.API_RATING_ANSWER, acls = RolePermissionAction.CREATE)
    })
    public RatingEntity createAnswer(@PathParam("api") String api, @PathParam("rating") String rating, @Valid @NotNull final NewRatingAnswerEntity answer) {
        answer.setRatingId(rating);
        return filterPermission(api, ratingService.createAnswer(answer));
    }

    @Path("{rating}/answers/{answer}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.API_RATING_ANSWER, acls = RolePermissionAction.DELETE)
    })
    public void deleteAnswer(@PathParam("api") String api, @PathParam("rating") String rating, @PathParam("answer") String answer) {
        ratingService.deleteAnswer(rating, answer);
    }

    private RatingEntity filterPermission(final String api, final RatingEntity ratingEntity) {
        if (!hasPermission(RolePermission.API_RATING_ANSWER, api, RolePermissionAction.READ) && ratingEntity != null) {
            ratingEntity.setAnswers(null);
        }
        return ratingEntity;
    }
}
