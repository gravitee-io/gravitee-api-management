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
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import java.util.Collection;
import java.util.Collections;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

public class ApiMediaResource extends AbstractResource {

    @Inject
    private ApiService apiService;

    @Inject
    private MediaService mediaService;

    @GET
    @Path("{mediaHash}")
    @Produces({ MediaType.WILDCARD, MediaType.APPLICATION_JSON })
    public Response getApiMedia(@Context Request request, @PathParam("apiId") String apiId, @PathParam("mediaHash") String mediaHash) {
        final ApiQuery apiQuery = new ApiQuery();
        apiQuery.setIds(Collections.singletonList(apiId));
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(getAuthenticatedUserOrNull(), apiQuery);
        if (userApis.stream().anyMatch(a -> a.getId().equals(apiId))) {
            MediaEntity mediaEntity = mediaService.findByHashAndApi(mediaHash, apiId, true);

            if (mediaEntity == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return createMediaResponse(request, mediaHash, mediaEntity);
        }
        throw new ApiNotFoundException(apiId);
    }
}
