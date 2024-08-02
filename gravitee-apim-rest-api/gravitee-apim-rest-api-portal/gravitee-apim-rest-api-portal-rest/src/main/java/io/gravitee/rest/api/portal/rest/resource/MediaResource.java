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
import io.gravitee.rest.api.model.MediaEntity;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MediaResource extends AbstractResource {

    @Inject
    private MediaService mediaService;

    @GET
    @Path("{mediaHash}")
    @Produces({ MediaType.WILDCARD, MediaType.APPLICATION_JSON })
    public Response getPortalMedia(@Context Request request, @PathParam("mediaHash") String mediaHash) {
        MediaEntity mediaEntity = mediaService.findByHash(GraviteeContext.getExecutionContext(), mediaHash, true);

        if (mediaEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return createMediaResponse(request, mediaHash, mediaEntity);
    }
}
