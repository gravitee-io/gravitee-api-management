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
package io.gravitee.rest.api.management.v2.rest.resource.portal_pages;

import io.gravitee.apim.core.portal_page.use_case.GetHomepageUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalPagesMapper;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

public class PortalPagesResource extends AbstractResource {

    @Inject
    private GetHomepageUseCase getHomepageUseCase;

    @PathParam("envId")
    private String envId;

    @GET
    @Produces("application/json")
    @Path("/_homepage")
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.READ }) })
    public PortalPageResponse getPortalHomepage() {
        var input = new GetHomepageUseCase.Input(envId);
        var homepage = getHomepageUseCase.execute(input);

        return PortalPagesMapper.INSTANCE.map(homepage);
    }
}
