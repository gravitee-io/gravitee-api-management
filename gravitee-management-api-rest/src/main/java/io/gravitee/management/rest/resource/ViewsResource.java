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

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.*;
import io.gravitee.management.rest.enhancer.ViewEnhancer;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ViewsResource extends AbstractResource  {

    @Autowired
    private ViewService viewService;

    @Autowired
    private ViewEnhancer viewEnhancer;

    @Autowired
    private ApiService apiService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ViewEntity> list()  {
        Set<ApiEntity> apis;
        if (isAdmin()) {
            apis = apiService.findAll();
        } else if (isAuthenticated()) {
            apis = apiService.findByUser(getAuthenticatedUsername());
        } else {
            apis = apiService.findByVisibility(Visibility.PUBLIC);
        }

        return viewService.findAll()
                .stream()
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .map(v -> viewEnhancer.enhance(apis).apply(v))
                .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<ViewEntity> create(@Valid @NotNull final List<NewViewEntity> views) {
        return viewService.create(views);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<ViewEntity> update(@Valid @NotNull final List<UpdateViewEntity> views) {
        return viewService.update(views);
    }

    @Path("{view}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public void delete(@PathParam("view") String view) {
        viewService.delete(view);
    }
}
