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
package io.gravitee.rest.api.portal.rest.resource.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Singleton
@Slf4j
public class ConsoleAuthenticationResource extends AbstractAuthenticationResource {

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

    @Autowired
    private AuthoritiesProvider authoritiesProvider;

    @GET
    public Response redirectTo(@QueryParam("token") String token, @Context final HttpServletResponse httpResponse) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(environment.getProperty("jwt.secret"));
            JWTVerifier jwtVerifier = JWT.require(algorithm).build();

            final DecodedJWT jwt = jwtVerifier.verify(token);

            // Initialize execution context from management API token.
            final String organizationId = jwt.getClaim(JWTHelper.Claims.ORG).asString();
            final String environmentId = GraviteeContext.getCurrentEnvironment() != null
                ? GraviteeContext.getCurrentEnvironment()
                : GraviteeContext.getDefaultEnvironment();

            ExecutionContext executionContext = new ExecutionContext(organizationId, environmentId);
            GraviteeContext.fromExecutionContext(executionContext);

            // Retrieve the user.
            final UserEntity user = userService.findById(executionContext, jwt.getSubject());

            //set user to Authentication Context
            final Set<GrantedAuthority> authorities = authoritiesProvider.retrieveAuthorities(user.getId(), organizationId, environmentId);

            UserDetails userDetails = new UserDetails(user.getId(), "", authorities);
            userDetails.setOrganizationId(user.getOrganizationId());
            userDetails.setEmail(user.getEmail());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, authorities));

            // connect the user
            super.connectUser(jwt.getSubject(), httpResponse);
            String url = installationAccessQueryService.getPortalUrl(environmentId);

            // Redirect the user.
            return Response.temporaryRedirect(new URI(url)).build();
        } catch (UserNotFoundException e) {
            log.error("Authentication failed", e);
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error occurred when trying to log user using external authentication provider.", e);
            return Response.serverError().build();
        } finally {
            GraviteeContext.cleanContext();
        }
    }
}
