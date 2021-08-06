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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.UpdateRatingEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.RatingMapper;
import io.gravitee.rest.api.portal.rest.model.RatingInput;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.RatingNotFoundException;
import java.util.Collection;
import java.util.Collections;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiRatingResource extends AbstractResource {

    @Autowired
    private RatingService ratingService;

    @Inject
    private RatingMapper ratingMapper;

    @Context
    private ResourceContext resourceContext;

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_RATING, acls = RolePermissionAction.DELETE) })
    public Response deleteApiRating(@PathParam("apiId") String apiId, @PathParam("ratingId") String ratingId) {
        // FIXME: are we sure we need to fetch the api while the permission system alreay allowed the user to delete the rating ?
        final ApiQuery apiQuery = new ApiQuery();
        apiQuery.setIds(Collections.singletonList(apiId));
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(getAuthenticatedUserOrNull(), apiQuery);
        if (userApis.stream().anyMatch(a -> a.getId().equals(apiId))) {
            RatingEntity ratingEntity = ratingService.findById(ratingId);

            if (ratingEntity != null && ratingEntity.getApi().equals(apiId)) {
                ratingService.delete(ratingId);
                return Response.status(Status.NO_CONTENT).build();
            }
            throw new RatingNotFoundException(ratingId, apiId);
        }
        throw new ApiNotFoundException(apiId);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_RATING, acls = RolePermissionAction.UPDATE) })
    public Response updateApiRating(
        @PathParam("apiId") String apiId,
        @PathParam("ratingId") String ratingId,
        @Valid RatingInput ratingInput
    ) {
        if (ratingInput == null) {
            throw new BadRequestException("Input must not be null.");
        }
        final ApiQuery apiQuery = new ApiQuery();
        apiQuery.setIds(Collections.singletonList(apiId));
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(getAuthenticatedUserOrNull(), apiQuery);
        if (userApis.stream().anyMatch(a -> a.getId().equals(apiId))) {
            RatingEntity ratingEntity = ratingService.findById(ratingId);
            if (ratingEntity != null && ratingEntity.getApi().equals(apiId)) {
                UpdateRatingEntity rating = new UpdateRatingEntity();
                rating.setId(ratingId);
                rating.setApi(apiId);
                rating.setComment(ratingInput.getComment());
                rating.setTitle(ratingInput.getTitle());
                rating.setRate(ratingInput.getValue().byteValue());

                RatingEntity updatedRating = ratingService.update(rating);

                return Response.status(Status.OK).entity(ratingMapper.convert(updatedRating, uriInfo)).build();
            }
            throw new RatingNotFoundException(ratingId, apiId);
        }
        throw new ApiNotFoundException(apiId);
    }

    @Path("/answers")
    public ApiRatingAnswersResource getApiRatingResource() {
        return resourceContext.getResource(ApiRatingAnswersResource.class);
    }
}
