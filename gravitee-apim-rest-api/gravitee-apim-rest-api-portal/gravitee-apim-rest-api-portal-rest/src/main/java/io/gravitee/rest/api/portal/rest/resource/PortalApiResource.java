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

import io.gravitee.apim.core.api.use_case.GetApiForPortalUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * The page of an API on one portal. Each read first requires the portal to list the API for the viewer, then answers as the unscoped
 * path does, so the unscoped rules still apply on top. The check sits in each method rather than in a sub-resource locator so that it
 * runs after {@link RequirePortalAuth}, like the unscoped reads.
 */
public class PortalApiResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GetApiForPortalUseCase getApiForPortalUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApi(@PathParam("apiId") String apiId, @QueryParam("include") List<String> include) {
        return resourceContext.getResource(ApiResource.class).getApi(apiId, include, true, PortalScope.fromPath(uriInfo));
    }

    @GET
    @Path("plans")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApiPlans(@PathParam("apiId") String apiId, @BeanParam PaginationParam paginationParam) {
        requireListed(apiId);
        return resourceContext.getResource(ApiPlansResource.class).getApiPlansByApiId(apiId, paginationParam);
    }

    @GET
    @Path("pages")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApiPages(
        @HeaderParam("Accept-Language") String acceptLang,
        @PathParam("apiId") String apiId,
        @BeanParam PaginationParam paginationParam,
        @QueryParam("homepage") Boolean homepage,
        @QueryParam("parent") String parent
    ) {
        requireListed(apiId);
        return resourceContext.getResource(ApiPagesResource.class).getPagesByApiId(acceptLang, apiId, paginationParam, homepage, parent);
    }

    @GET
    @Path("pages/{pageId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApiPage(
        @HeaderParam("Accept-Language") String acceptLang,
        @PathParam("apiId") String apiId,
        @PathParam("pageId") String pageId,
        @QueryParam("include") List<String> include
    ) {
        requireListed(apiId);
        return resourceContext.getResource(ApiPageResource.class).getPageByApiIdAndPageId(acceptLang, apiId, pageId, include);
    }

    @GET
    @Path("informations")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApiInformations(@Context Request request, @PathParam("apiId") String apiId) {
        requireListed(apiId);
        return resourceContext.getResource(ApiInformationsResource.class).getApiInformations(request, apiId);
    }

    @GET
    @Path("subscription-form")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getSubscriptionForm(@PathParam("apiId") String apiId) {
        requireListed(apiId);
        return resourceContext.getResource(ApiSubscriptionFormResource.class).getSubscriptionForm(apiId);
    }

    private void requireListed(String apiId) {
        var executionContext = GraviteeContext.getExecutionContext();
        var output = getApiForPortalUseCase.execute(
            new GetApiForPortalUseCase.Input(
                executionContext.getEnvironmentId(),
                executionContext.getOrganizationId(),
                apiId,
                getAuthenticatedUserOrNull(),
                PortalScope.fromPath(uriInfo)
            )
        );
        if (!output.visible()) {
            throw new ApiNotFoundException(apiId);
        }
    }
}
