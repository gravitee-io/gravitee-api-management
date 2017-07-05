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
package io.gravitee.management.rest.resource.auth;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.NewExternalUserEntity;
import io.gravitee.management.model.UpdateUserEntity;
import io.gravitee.management.security.authentication.AuthenticationProvider;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.swagger.annotations.Api;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/auth/oauth2")
@Api(tags = {"Authentication"})
public class OAuth2AuthenticationResource extends AbstractAuthenticationResource {

    @Inject
    @Named("oauth2")
    private AuthenticationProvider authenticationProvider;

    private Client client;

    public OAuth2AuthenticationResource() {
        this.client = ClientBuilder.newClient();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response oauth2(@Valid final Payload payload,
                           @Context final HttpServletRequest request) throws IOException {
        // Step 1. Exchange authorization code for access token.
        final MultivaluedStringMap accessData = new MultivaluedStringMap();
        accessData.add(CLIENT_ID_KEY, payload.getClientId());
        accessData.add(REDIRECT_URI_KEY, payload.getRedirectUri());
        accessData.add(CLIENT_SECRET, (String) authenticationProvider.configuration().get("clientSecret"));
        accessData.add(CODE_KEY, payload.getCode());
        accessData.add(GRANT_TYPE_KEY, AUTH_CODE);
        Response response = client.target((String) authenticationProvider.configuration().get("tokenEndpoint"))
                .request(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.form(accessData));
        accessData.clear();

        // Step 2. Retrieve profile information about the current user.
        final String accessToken = (String) getResponseEntity(response).get(
                (String) authenticationProvider.configuration().get("accessTokenProperty"));
        response = client
                .target((String) authenticationProvider.configuration().get("userInfoEndpoint"))
                .request(javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.AUTHORIZATION,
                        String.format(
                                (String) authenticationProvider.configuration().get("authorizationHeader"),
                                accessToken))
                .get();

        // Step 3. Process the authenticated user.
        final Map<String, Object> userInfo = getResponseEntity(response);
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return processUser(userInfo);
        }

        return Response.status(response.getStatusInfo()).build();
    }

    private Response processUser(final Map<String, Object> userInfo) {
        String username = (String) userInfo.get(authenticationProvider.configuration().get("mapping.email"));

        if (username == null) {
            throw new BadRequestException("No public email linked to your account");
        }

        try {
            userService.findByName(username);
        } catch (UserNotFoundException unfe) {
            final NewExternalUserEntity newUser = new NewExternalUserEntity();
            newUser.setUsername(username);
            newUser.setSource(AuthenticationSource.OAUTH2.getName());
            newUser.setSourceId((String) userInfo.get(authenticationProvider.configuration().get("mapping.id")));
            newUser.setLastname((String) userInfo.get(authenticationProvider.configuration().get("mapping.lastname")));
            newUser.setFirstname((String) userInfo.get(authenticationProvider.configuration().get("mapping.firstname")));
            newUser.setEmail(username);
            userService.create(newUser);
        }

        // User refresh
        UpdateUserEntity user = new UpdateUserEntity();
        user.setUsername(username);
        user.setPicture((String) userInfo.get(authenticationProvider.configuration().get("mapping.picture")));

        userService.update(user);

        return connectUser(username);
    }
}
