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

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.enhancer.ViewEnhancer;
import io.gravitee.rest.api.portal.rest.mapper.ViewMapper;
import io.gravitee.rest.api.portal.rest.model.View;
import io.gravitee.rest.api.portal.rest.resource.param.ApisParam;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.ViewService;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ViewsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Context
    private UriInfo uriInfo;
    
    @Autowired
    private ViewService viewService;

    @Autowired
    private ViewMapper viewMapper;
    
    @Autowired
    private ViewEnhancer viewEnhancer;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getViews(@BeanParam PaginationParam paginationParam) {
        Set<ApiEntity> apis;
        if (isAuthenticated()) {
            apis = apiService.findByUser(getAuthenticatedUser(), null);
        } else {
            apis = apiService.findByVisibility(Visibility.PUBLIC);
        }
        
        List<View> viewsList = viewService.findAll()
                .stream()
                .filter(v -> !v.isHidden())
                .sorted(Comparator.comparingInt(ViewEntity::getOrder))
                .map(v -> viewEnhancer.enhance(apis).apply(v))
                .map(v-> viewMapper.convert(v, uriInfo.getBaseUriBuilder()))
                .collect(Collectors.toList());
        
        return createListResponse(viewsList, paginationParam, uriInfo);
    }


    
    @Path("{viewId}")
    public ViewResource getViewResource() {
        return resourceContext.getResource(ViewResource.class);
    }
}
