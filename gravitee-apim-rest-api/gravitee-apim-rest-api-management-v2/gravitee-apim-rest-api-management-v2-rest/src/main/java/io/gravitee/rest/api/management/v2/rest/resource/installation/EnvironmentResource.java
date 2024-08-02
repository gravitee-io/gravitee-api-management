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
package io.gravitee.rest.api.management.v2.rest.resource.installation;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.EnvironmentMapper;
import io.gravitee.rest.api.management.v2.rest.model.Environment;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiMembersResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApisResource;
import io.gravitee.rest.api.management.v2.rest.resource.application.ApplicationsResource;
import io.gravitee.rest.api.management.v2.rest.resource.category.CategoriesResource;
import io.gravitee.rest.api.management.v2.rest.resource.category.CategoryResource;
import io.gravitee.rest.api.management.v2.rest.resource.environment.SharedPolicyGroupsResource;
import io.gravitee.rest.api.management.v2.rest.resource.group.GroupsResource;
import io.gravitee.rest.api.management.v2.rest.resource.ui.ThemesResource;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EnvironmentService environmentService;

    @Path("/apis")
    public ApisResource getApisResource() {
        return resourceContext.getResource(ApisResource.class);
    }

    @Path("/applications")
    public ApplicationsResource getApplicationsResource() {
        return resourceContext.getResource(ApplicationsResource.class);
    }

    @Path("/groups")
    public GroupsResource getGroupsResource() {
        return resourceContext.getResource(GroupsResource.class);
    }

    @Path("/ui/themes")
    public ThemesResource getThemesResource() {
        return resourceContext.getResource(ThemesResource.class);
    }

    @Path("/categories")
    public CategoriesResource getCategoriesResource() {
        return resourceContext.getResource(CategoriesResource.class);
    }

    @Path("/shared-policy-groups")
    public SharedPolicyGroupsResource getSharedPolicyGroupsResource() {
        return resourceContext.getResource(SharedPolicyGroupsResource.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Environment getEnvironment(@PathParam("envId") String envId) {
        return EnvironmentMapper.INSTANCE.map(environmentService.findByOrgAndIdOrHrid(GraviteeContext.getCurrentOrganization(), envId));
    }
}
