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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.RatingEntity;
import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.RatingAnswerNotFoundException;
import io.gravitee.rest.api.service.exceptions.RatingNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Collection;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiRatingAnswerResource extends AbstractResource {

    @Autowired
    private RatingService ratingService;

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.API_RATING_ANSWER, acls = RolePermissionAction.DELETE)
    })
    public Response deleteApiRatingAnswer(@PathParam("apiId") String apiId, @PathParam("ratingId") String ratingId, @PathParam("answerId") String answerId) {
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(getAuthenticatedUserOrNull());
        if (userApis.stream().anyMatch(a -> a.getId().equals(apiId))) {

            RatingEntity ratingEntity = ratingService.findById(ratingId);
            if (ratingEntity != null && ratingEntity.getId().equals(ratingId)) {

                if (ratingEntity.getAnswers().stream().anyMatch(answer -> answer.getId().equals(answerId))){
                    ratingService.deleteAnswer(ratingId, answerId);
                    return Response
                            .status(Status.NO_CONTENT)
                            .build();
                }

                throw new RatingAnswerNotFoundException(answerId, ratingId, apiId);
            }
            throw new RatingNotFoundException(ratingId, apiId);
        }
        throw new ApiNotFoundException(apiId);
    }
}
