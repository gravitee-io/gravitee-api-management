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

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

import java.io.ByteArrayOutputStream;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.enhancer.ViewEnhancer;
import io.gravitee.rest.api.portal.rest.mapper.ViewMapper;
import io.gravitee.rest.api.service.ViewService;

/**
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ViewResource extends AbstractResource {

    @Context
    private UriInfo uriInfo;
    
    @Autowired
    private ViewService viewService;

    @Autowired
    private ViewMapper viewMapper;

    @Autowired
    private ViewEnhancer viewEnhancer;

    @GET
    @Produces(APPLICATION_JSON)
    public Response get(@PathParam("viewId") String viewId) {
        ViewEntity view = viewService.findNotHiddenById(viewId);

        Set<ApiEntity> apis;
        if (isAuthenticated()) {
            apis = apiService.findByUser(getAuthenticatedUser(), null);
        } else {
            apis = apiService.findByVisibility(Visibility.PUBLIC);
        }
        view = viewEnhancer.enhance(apis).apply(view);
        
        return Response
                .ok(viewMapper.convert(view, uriInfo.getBaseUriBuilder()))
                .build();
    }


    @GET
    @Path("picture")
    public Response picture(
            @Context Request request,
            @PathParam("viewId") String viewId) {
        viewService.findNotHiddenById(viewId);

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        InlinePictureEntity image = viewService.getPicture(viewId);

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder
                    .cacheControl(cc)
                    .build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response
                .ok(baos)
                .cacheControl(cc)
                .tag(etag)
                .type(image.getType())
                .build();
    }

}
