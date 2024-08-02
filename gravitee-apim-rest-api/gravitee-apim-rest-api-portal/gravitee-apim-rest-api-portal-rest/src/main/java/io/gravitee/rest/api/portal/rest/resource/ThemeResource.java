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

import io.gravitee.apim.core.theme.use_case.GetCurrentThemeUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.theme.ThemeType;
import io.gravitee.rest.api.model.theme.portal.ThemeEntity;
import io.gravitee.rest.api.portal.rest.mapper.ThemeMapper;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.ThemeService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ThemeTypeNotSupportedException;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;

public class ThemeResource extends AbstractResource {

    @Autowired
    ThemeService themeService;

    @Autowired
    ThemeMapper themeMapper;

    @Inject
    GetCurrentThemeUseCase getCurrentThemeUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalTheme(@QueryParam("type") ThemeType themeType) {
        if (Objects.isNull(themeType)) {
            throw new ThemeTypeNotSupportedException();
        }
        var result = getCurrentThemeUseCase
            .execute(
                GetCurrentThemeUseCase.Input
                    .builder()
                    .type(io.gravitee.apim.core.theme.model.ThemeType.valueOf(themeType.name()))
                    .executionContext(GraviteeContext.getExecutionContext())
                    .build()
            )
            .result();

        String themeURL = PortalApiLinkHelper.themeURL(uriInfo.getBaseUriBuilder(), result.getId());
        return Response.ok(themeMapper.convert(result, themeURL)).build();
    }

    @GET
    @Path("{themeId}/logo")
    public Response getLogo(@PathParam("themeId") String id, @Context Request request) {
        return this.buildPictureResponse(themeService.getLogo(GraviteeContext.getExecutionContext(), id), request);
    }

    @GET
    @Path("{themeId}/optionalLogo")
    public Response getOptionalLogo(@PathParam("themeId") String id, @Context Request request) {
        PictureEntity optionalLogo = themeService.getOptionalLogo(GraviteeContext.getExecutionContext(), id);
        if (optionalLogo != null) {
            return this.buildPictureResponse(optionalLogo, request);
        }
        return this.getLogo(id, request);
    }

    @GET
    @Path("{themeId}/backgroundImage")
    public Response getBackgroundImage(@PathParam("themeId") String id, @Context Request request) {
        return this.buildPictureResponse(themeService.getBackgroundImage(GraviteeContext.getExecutionContext(), id), request);
    }

    @GET
    @Path("{themeId}/favicon")
    public Response getFavicon(@PathParam("themeId") String id, @Context Request request) {
        return this.buildPictureResponse(themeService.getFavicon(GraviteeContext.getExecutionContext(), id), request);
    }

    private Response buildPictureResponse(PictureEntity picture, @Context Request request) {
        if (picture == null) {
            return Response.ok().build();
        }

        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity) picture).getUrl())).build();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        InlinePictureEntity image = (InlinePictureEntity) picture;

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder.cacheControl(cc).build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response.ok().entity(baos).cacheControl(cc).tag(etag).type(image.getType()).build();
    }
}
