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
import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.portal.rest.mapper.ViewMapper;
import io.gravitee.rest.api.portal.rest.model.View;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.View.ALL_ID;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ViewsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private ViewService viewService;

    @Autowired
    private ViewMapper viewMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getViews(@BeanParam PaginationParam paginationParam) {
        final List<View> viewsList = viewService.findAll()
                .stream()
                .filter(v -> !ALL_ID.equals(v.getId()))
                .filter(v -> !v.isHidden())
                .sorted(Comparator.comparingInt(ViewEntity::getOrder))
                .map(v-> viewMapper.convert(v, uriInfo.getBaseUriBuilder()))
                .collect(Collectors.toList());
        
        return createListResponse(viewsList, paginationParam);
    }
    
    @Path("{viewId}")
    public ViewResource getViewResource() {
        return resourceContext.getResource(ViewResource.class);
    }
}
