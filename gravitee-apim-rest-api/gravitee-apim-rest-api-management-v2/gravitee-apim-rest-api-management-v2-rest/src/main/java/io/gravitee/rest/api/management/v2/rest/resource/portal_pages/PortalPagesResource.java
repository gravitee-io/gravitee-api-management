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

import static java.util.Optional.ofNullable;

import io.gravitee.apim.core.portal_page.model.ExpandsViewContext;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.core.portal_page.use_case.GetPortalPageUseCase;
import io.gravitee.apim.core.portal_page.use_case.UpdatePortalPageUseCase;
import io.gravitee.apim.core.portal_page.use_case.UpdatePortalPageViewPublicationStatusUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.PortalPagesMapper;
import io.gravitee.rest.api.management.v2.rest.model.PatchPortalPage;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageWithDetails;
import io.gravitee.rest.api.management.v2.rest.model.PortalPagesResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PortalPagesResource extends AbstractResource {

    @Inject
    private GetPortalPageUseCase getPortalPageUseCase;

    @Inject
    private UpdatePortalPageUseCase updatePortalPageUseCase;

    @Inject
    UpdatePortalPageViewPublicationStatusUseCase updatePortalPageViewPublicationStatusUseCase;

    @PathParam("envId")
    private String envId;

    @GET
    @Produces("application/json")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = { RolePermissionAction.READ }) })
    public PortalPagesResponse getPortalPages(@QueryParam("type") String type, @QueryParam("expands") List<String> expands) {
        GetPortalPageUseCase.Input input = toGetPortalPageInput(envId, type, expands);
        var page = getPortalPageUseCase.execute(input);

        return PortalPagesMapper.INSTANCE.map(page);
    }

    private GetPortalPageUseCase.Input toGetPortalPageInput(String envId, String type, List<String> expands) {
        var pageType = Arrays.stream(PortalViewContext.values())
            .filter(pcv -> pcv.name().equalsIgnoreCase(type))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown page type: " + type));
        var expandsEnum = ofNullable(expands).orElseGet(Collections::emptyList).stream().map(ExpandsViewContext::fromValue).toList();
        return new GetPortalPageUseCase.Input(envId, pageType, expandsEnum);
    }

    @PATCH
    @Produces("application/json")
    @Consumes("application/json")
    @Path("/{pageId}")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public PortalPageWithDetails patchPortalPage(@PathParam("pageId") String pageId, PatchPortalPage patchPortalPage) {
        var input = new UpdatePortalPageUseCase.Input(envId, pageId, patchPortalPage.getContent());
        var updatedHomepage = updatePortalPageUseCase.execute(input);

        return PortalPagesMapper.INSTANCE.map(updatedHomepage.portalPage());
    }

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("/{pageId}/_publish")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public PortalPageWithDetails publishPortalPage(@PathParam("pageId") String pageId) {
        var input = new UpdatePortalPageViewPublicationStatusUseCase.Input(PageId.of(pageId), true);
        var updatedHomepage = updatePortalPageViewPublicationStatusUseCase.execute(input);

        return PortalPagesMapper.INSTANCE.map(updatedHomepage.portalPage());
    }

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("/{pageId}/_unpublish")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public PortalPageWithDetails unpublishPortalPage(@PathParam("pageId") String pageId) {
        var input = new UpdatePortalPageViewPublicationStatusUseCase.Input(PageId.of(pageId), false);
        var updatedHomepage = updatePortalPageViewPublicationStatusUseCase.execute(input);
        return PortalPagesMapper.INSTANCE.map(updatedHomepage.portalPage());
    }
}
