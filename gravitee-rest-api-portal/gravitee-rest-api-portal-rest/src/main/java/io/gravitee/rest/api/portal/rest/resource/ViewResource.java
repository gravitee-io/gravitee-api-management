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

import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.mapper.ViewMapper;
import io.gravitee.rest.api.service.ViewService;

import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import java.util.Set;

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ViewResource extends AbstractResource {

    @Autowired
    private ViewService viewService;

    @Autowired
    private ViewMapper viewMapper;

    @GET
    @Produces(APPLICATION_JSON)
    public Response get(@PathParam("viewId") String viewId) {
        ViewEntity view = viewService.findNotHiddenById(viewId);

        Set<ApiEntity> apis = apiService.findPublishedByUser(getAuthenticatedUserOrNull());
        view.setTotalApis(viewService.getTotalApisByView(apis, view));

        return Response
                .ok(viewMapper.convert(view, uriInfo.getBaseUriBuilder()))
                .build();
    }


    @GET
    @Path("picture")
    public Response picture(@Context Request request, @PathParam("viewId") String viewId) {
        viewService.findNotHiddenById(viewId);

        InlinePictureEntity image = viewService.getPicture(viewId);

        return createPictureResponse(request, image);
    }

}
