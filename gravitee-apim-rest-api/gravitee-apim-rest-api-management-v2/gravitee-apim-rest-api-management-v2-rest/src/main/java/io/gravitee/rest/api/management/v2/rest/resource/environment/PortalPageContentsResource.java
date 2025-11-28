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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;

import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.use_case.GetPortalPageContentUseCase;
import io.gravitee.apim.core.portal_page.use_case.UpdatePortalPageContentUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalPageContentMapper;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 * @author GraviteeSource Team
 */
public class PortalPageContentsResource extends AbstractResource {

    @Inject
    private GetPortalPageContentUseCase getPortalPageContentUseCase;

    @Inject
    private UpdatePortalPageContentUseCase updatePortalPageContentUseCase;

    @GET
    @Path("/{portalPageContentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.READ) })
    public io.gravitee.rest.api.management.v2.rest.model.PortalPageContent getPortalPageContent(
        @PathParam("portalPageContentId") String portalPageContentId
    ) {
        try {
            var result = getPortalPageContentUseCase.execute(
                new GetPortalPageContentUseCase.Input(PortalPageContentId.of(portalPageContentId))
            );

            return PortalPageContentMapper.INSTANCE.map(result.content());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid content ID format: " + portalPageContentId);
        }
    }

    @PUT
    @Path("/{portalPageContentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.UPDATE) })
    public io.gravitee.rest.api.management.v2.rest.model.PortalPageContent updatePortalPageContent(
        @PathParam("portalPageContentId") String portalPageContentId,
        @Valid io.gravitee.rest.api.management.v2.rest.model.UpdatePortalPageContent updatePortalPageContent
    ) {
        final var executionContext = getExecutionContext();
        var result = updatePortalPageContentUseCase.execute(
            new UpdatePortalPageContentUseCase.Input(
                executionContext.getOrganizationId(),
                executionContext.getEnvironmentId(),
                portalPageContentId,
                PortalPageContentMapper.INSTANCE.map(updatePortalPageContent)
            )
        );

        return PortalPageContentMapper.INSTANCE.map(result.portalPageContent());
    }
}
