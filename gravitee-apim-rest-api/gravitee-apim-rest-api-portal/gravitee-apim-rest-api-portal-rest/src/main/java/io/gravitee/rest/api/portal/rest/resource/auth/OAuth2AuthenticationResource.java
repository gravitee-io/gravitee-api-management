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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.configuration.identity.ClientAuthenticationMethod;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.portal.rest.model.PayloadInput;
import io.gravitee.rest.api.portal.rest.utils.BlindTrustManager;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.builder.JerseyClientBuilder;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import lombok.CustomLog;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Singleton
public class OAuth2AuthenticationResource extends AbstractAuthenticationResource {

    @Autowired
    private SocialIdentityProviderService socialIdentityProviderService;

    @Autowired
    private AuthoritiesProvider authoritiesProvider;

    private Client client;

    private static final String ACCESS_TOKEN_PROPERTY = "access_token";
    private static final String ID_TOKEN_PROPERTY = "id_token";
    private static final String ERROR_PROPERTY = "error";
    private static final String INVALID_CLIENT_ERROR = "invalid_client";
    private static final String PROVIDER_ERROR = "identity_provider_error";
    private static final String PROVIDER_UNAVAILABLE_ERROR = "identity_provider_unavailable";
    private static final String CLIENT_AUTHENTICATION_HINT =
        "The identity provider rejected the client credentials. Check that the tokenEndpointAuthMethod configured on " +
        "the identity provider matches the method the provider expects (client_secret_basic or client_secret_post); " +
        "the token and introspection endpoints must both accept it.";

    @PostConstruct
    public void initClient() throws NoSuchAlgorithmException, KeyManagementException {
        final boolean trustAllEnabled = environment.getProperty("security.trustAll", Boolean.class, false);
        final ClientBuilder builder = JerseyClientBuilder.newBuilder(environment);
        if (trustAllEnabled) {
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, new TrustManager[] { new BlindTrustManager() }, null);
            builder.sslContext(sc);
        }

        this.client = builder.build();
    }

    @POST
    @Path("_exchange")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenExchange(
        @PathParam(value = "identity") final String identity,
        @QueryParam(value = "token") final String token,
        @Context final HttpServletResponse servletResponse
    ) {
        SocialIdentityProviderEntity identityProvider = socialIdentityProviderService.findById(
            identity,
            new IdentityProviderActivationService.ActivationTarget(
                GraviteeContext.getCurrentEnvironment(),
                IdentityProviderActivationReferenceType.ENVIRONMENT
            )
        );

        if (identityProvider != null) {
            if (identityProvider.getTokenIntrospectionEndpoint() != null) {
                // Step1. Check the token by invoking the introspection endpoint
                final MultivaluedStringMap introspectData = new MultivaluedStringMap();
                introspectData.add(TOKEN, token);
                Invocation.Builder introspectRequest = client
                    //TODO: what is the correct introspection URL here ?
                    .target(identityProvider.getTokenIntrospectionEndpoint())
                    .request(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE);
                authenticateClient(identityProvider, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, introspectData, introspectRequest);
                Response response = introspectRequest.post(Entity.form(introspectData));
                introspectData.clear();

                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    JsonNode introspectPayload = response.readEntity(JsonNode.class);
                    boolean active = introspectPayload.path("active").asBoolean(true);

                    if (active) {
                        return authenticateUser(identityProvider, servletResponse, token, null, null);
                    } else {
                        log.info("Identity provider {} reported the exchanged token as inactive", identityProvider.getId());
                        return Response.status(Response.Status.UNAUTHORIZED).entity(introspectPayload).build();
                    }
                }

                String errorBody = getResponseEntityAsString(response);
                log.warn(
                    "Token introspection for identity provider {} at {} failed with status {}: {}\n{}",
                    identityProvider.getId(),
                    identityProvider.getTokenIntrospectionEndpoint(),
                    response.getStatus(),
                    response.getStatusInfo(),
                    errorBody
                );
                return clientAuthenticationFailure(response.getStatus(), errorBody);
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Token exchange is not supported for this identity provider")
                    .build();
            }
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response exchangeAuthorizationCode(
        @PathParam(value = "identity") String identity,
        @Valid @NotNull(message = "Input must not be null.") final PayloadInput payloadInput,
        @Context final HttpServletResponse servletResponse
    ) throws IOException {
        SocialIdentityProviderEntity identityProvider = socialIdentityProviderService.findById(
            identity,
            new IdentityProviderActivationService.ActivationTarget(
                GraviteeContext.getCurrentEnvironment(),
                IdentityProviderActivationReferenceType.ENVIRONMENT
            )
        );

        if (identityProvider != null) {
            // Step 1. Exchange authorization code for access token.
            final MultivaluedStringMap accessData = new MultivaluedStringMap();
            accessData.add(CLIENT_ID_KEY, payloadInput.getClientId());
            accessData.add(REDIRECT_URI_KEY, payloadInput.getRedirectUri());
            accessData.add(CODE_KEY, payloadInput.getCode());
            accessData.add(CODE_VERIFIER_KEY, payloadInput.getCodeVerifier());
            accessData.add(GRANT_TYPE_KEY, payloadInput.getGrantType());

            Invocation.Builder tokenRequest = client
                .target(identityProvider.getTokenEndpoint())
                .request(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE);
            authenticateClient(identityProvider, ClientAuthenticationMethod.CLIENT_SECRET_POST, accessData, tokenRequest);

            Response response = tokenRequest.post(Entity.form(accessData));
            accessData.clear();

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                final Map<String, Object> responseEntity = getResponseEntity(response);
                final String accessToken = (String) responseEntity.get(ACCESS_TOKEN_PROPERTY);
                final String idToken = (String) responseEntity.get(ID_TOKEN_PROPERTY);
                return authenticateUser(identityProvider, servletResponse, accessToken, idToken, payloadInput.getState());
            }

            String errorBody = getResponseEntityAsString(response);
            log.warn(
                "Authorization-code exchange for identity provider {} at {} failed with status {}: {}\n{}",
                identityProvider.getId(),
                identityProvider.getTokenEndpoint(),
                response.getStatus(),
                response.getStatusInfo(),
                errorBody
            );
            return clientAuthenticationFailure(response.getStatus(), errorBody);
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Applies the client credentials the way the identity provider expects to receive them. A provider that declares no
     * method keeps the behaviour this endpoint has always had, so existing configurations are unaffected; declaring one
     * makes the token and introspection calls agree, which they previously did not.
     */
    private void authenticateClient(
        SocialIdentityProviderEntity identityProvider,
        ClientAuthenticationMethod endpointDefault,
        MultivaluedStringMap form,
        Invocation.Builder request
    ) {
        ClientAuthenticationMethod method = identityProvider.getTokenEndpointAuthMethod() != null
            ? identityProvider.getTokenEndpointAuthMethod()
            : endpointDefault;

        // The null label makes this an enhanced switch (JLS 14.11.2), so the compiler requires it to be exhaustive:
        // adding a token_endpoint_auth_method constant such as none or private_key_jwt fails the build here instead of
        // silently doing nothing, or worse falling through to putting the secret in the body, on a credential path.
        switch (method) {
            case null -> throw new IllegalArgumentException("No client authentication method resolved for " + identityProvider.getId());
            case CLIENT_SECRET_BASIC -> request.header(HttpHeaders.AUTHORIZATION, basicAuthorization(identityProvider));
            case CLIENT_SECRET_POST -> {
                // The authorization-code flow already carries the client_id supplied by the caller. add() registers the
                // key even for a null value, so the presence of a value is what decides, not the presence of the key.
                if (form.getFirst(CLIENT_ID_KEY) == null) {
                    form.putSingle(CLIENT_ID_KEY, identityProvider.getClientId());
                }
                form.add(CLIENT_SECRET, identityProvider.getClientSecret());
            }
        }
    }

    private static String basicAuthorization(SocialIdentityProviderEntity identityProvider) {
        String credentials = identityProvider.getClientId() + ':' + identityProvider.getClientSecret();
        // Explicit charset: the platform default silently changes the bytes, and therefore the credential, per host
        return String.format("Basic %s", Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Reports a failed token or introspection call as an authentication failure carrying the provider's own error code,
     * naming the client authentication method when the provider rejected our credentials. Only the error code is
     * relayed, never the provider's raw response, which may describe its internals.
     */
    private Response clientAuthenticationFailure(int providerStatus, String providerResponseBody) {
        // A provider that is down and a provider that rejected our credentials are different problems, so they must not
        // arrive as the same error code. Without this, an outage reads as a configuration mistake.
        String error = providerStatus >= 500 ? PROVIDER_UNAVAILABLE_ERROR : PROVIDER_ERROR;
        try {
            Object providerError = getEntity(providerResponseBody).get(ERROR_PROPERTY);
            if (providerError instanceof String providerErrorCode && !providerErrorCode.isBlank()) {
                error = providerErrorCode;
            }
        } catch (IOException | RuntimeException e) {
            log.debug("Identity provider error response is not a JSON object", e);
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put(ERROR_PROPERTY, error);
        if (INVALID_CLIENT_ERROR.equals(error)) {
            payload.put("hint", CLIENT_AUTHENTICATION_HINT);
        }
        return Response.status(Response.Status.UNAUTHORIZED).entity(payload).build();
    }

    /**
     * Retrieve profile information about the authenticated oauth end-user and authenticate it in Gravitee.
     *
     * @return
     */
    private Response authenticateUser(
        final SocialIdentityProviderEntity socialProvider,
        final HttpServletResponse servletResponse,
        final String accessToken,
        final String idToken,
        final String state
    ) {
        // Step 2. Retrieve profile information about the authenticated end-user.
        Response response = client
            .target(socialProvider.getUserInfoEndpoint())
            .request(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, String.format(socialProvider.getAuthorizationHeader(), accessToken))
            .get();

        // Step 3. Process the authenticated user.
        final String userInfo = getResponseEntityAsString(response);
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return processUser(socialProvider, servletResponse, userInfo, state, accessToken, idToken);
        } else {
            log.error("User info failed with status {}: {}\n{}", response.getStatus(), response.getStatusInfo(), userInfo);
        }

        return Response.status(response.getStatusInfo()).build();
    }

    private Response processUser(
        final SocialIdentityProviderEntity socialProvider,
        final HttpServletResponse servletResponse,
        final String userInfo,
        final String state,
        final String accessToken,
        final String idToken
    ) {
        UserEntity user = userService.createOrUpdateUserFromSocialIdentityProvider(
            GraviteeContext.getExecutionContext(),
            socialProvider,
            userInfo,
            accessToken,
            idToken
        );
        String userId = user.getId();

        final Set<GrantedAuthority> authorities = authoritiesProvider.retrieveAuthorities(user.getId());

        //set user to Authentication Context
        UserDetails userDetails = new UserDetails(userId, "", authorities);
        userDetails.setEmail(user.getEmail());
        userDetails.setOrganizationId(user.getOrganizationId());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, authorities));

        return connectUser(userId, state, servletResponse, accessToken, idToken);
    }
}
