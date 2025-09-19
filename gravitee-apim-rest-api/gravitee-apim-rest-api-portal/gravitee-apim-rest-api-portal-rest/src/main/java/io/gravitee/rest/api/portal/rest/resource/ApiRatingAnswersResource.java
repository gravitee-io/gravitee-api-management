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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.NewRatingAnswerEntity;
import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.RatingMapper;
import io.gravitee.rest.api.portal.rest.model.RatingAnswerInput;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.RatingNotFoundException;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiRatingAnswersResource extends AbstractResource {

    @Autowired
    private RatingService ratingService;

    @Inject
    private RatingMapper ratingMapper;

    @Context
    private ResourceContext resourceContext;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_RATING_ANSWER, acls = RolePermissionAction.CREATE) })
    public Response createApiRatingAnswer(
        @PathParam("apiId") String apiId,
        @PathParam("ratingId") String ratingId,
        @Valid RatingAnswerInput ratingAnswerInput
    ) {
        if (ratingAnswerInput == null) {
            throw new BadRequestException("Input must not be null.");
        }

        if (accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), apiId)) {
            RatingEntity ratingEntity = ratingService.findById(GraviteeContext.getExecutionContext(), ratingId);
            if (ratingEntity != null && ratingEntity.getApi().equals(apiId)) {
                NewRatingAnswerEntity ratingAnswerEntity = new NewRatingAnswerEntity();
                ratingAnswerEntity.setComment(ratingAnswerInput.getComment());
                ratingAnswerEntity.setRatingId(ratingId);
                RatingEntity updatedRating = ratingService.createAnswer(GraviteeContext.getExecutionContext(), ratingAnswerEntity);
                return Response.status(Status.CREATED)
                    .entity(ratingMapper.convert(GraviteeContext.getExecutionContext(), updatedRating, uriInfo))
                    .build();
            }
            throw new RatingNotFoundException(ratingId, apiId);
        }

        throw new ApiNotFoundException(apiId);
    }

    @Path("{answerId}")
    public ApiRatingAnswerResource getApiRatingAnswerResource() {
        return resourceContext.getResource(ApiRatingAnswerResource.class);
    }
}
