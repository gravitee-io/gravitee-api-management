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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.management.rest.model.TokenEntity;
import io.gravitee.rest.api.management.rest.resource.auth.AbstractAuthenticationResource;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.exceptions.InvalidTokenException;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Singleton
@Slf4j
@Path("/redirect")
public class PortalRedirectResource extends AbstractAuthenticationResource {

    public static final String PROPERTY_HTTP_API_PORTAL_PROXY_PATH = "installation.api.proxyPath.portal";
    public static final String PROPERTY_HTTP_API_PORTAL_ENTRYPOINT = "http.api.portal.entrypoint";
    private static final String PORTAL_NEXT_VERSION = "next";
    private static final String PORTAL_NEXT_QUERY_PARAM = "version=next&";

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

    @GET
    public Response redirectToPortal(@Context final HttpServletRequest httpServletRequest, @QueryParam("version") final String version) {
        try {
            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UserEntity user = userService.findById(GraviteeContext.getExecutionContext(), userDetails.getUsername());

            TokenEntity tokenEntity = generateToken(user, 30);
            String environmentId = GraviteeContext.getCurrentEnvironment() != null
                ? GraviteeContext.getCurrentEnvironment()
                : GraviteeContext.getDefaultEnvironment();
            String url = installationAccessQueryService.getPortalAPIUrl(environmentId);
            if (url == null) {
                ServerHttpRequest request = new ServletServerHttpRequest(httpServletRequest);
                UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(request)
                    .replacePath(getPortalProxyPath())
                    .replaceQuery(null)
                    .build();
                url = uriComponents.toUriString();
            }
            var versionQueryParam = version != null && version.equals(PORTAL_NEXT_VERSION) ? PORTAL_NEXT_QUERY_PARAM : "";
            // Redirect the user.
            URI location = new URI(
                "%s/environments/%s/auth/console?%stoken=%s".formatted(url, environmentId, versionQueryParam, tokenEntity.getToken())
            );
            return Response.temporaryRedirect(location).build();
        } catch (InvalidTokenException | UserNotFoundException e) {
            log.error("Authentication failed", e);
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error occurred when trying to log user using external authentication provider.", e);
            return Response.serverError().build();
        } finally {
            GraviteeContext.cleanContext();
        }
    }

    private String getPortalProxyPath() {
        String entrypoint = environment.getProperty(PROPERTY_HTTP_API_PORTAL_ENTRYPOINT, "/portal");
        return environment.getProperty(PROPERTY_HTTP_API_PORTAL_PROXY_PATH, entrypoint);
    }
}
