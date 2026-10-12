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
package io.gravitee.apim.rest.api.mcp.security.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.am.AMIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.oidc.OIDCIdentityProviderEntity;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

/**
 * Asks an APIM identity provider's authorization server about the tokens sent to the MCP endpoint.
 *
 * <p>Unlike the login {@code exchange} resources, it fails closed: a token is accepted only when the response states
 * {@code "active": true}, lists the MCP resource in {@code aud} (RFC 8707) and carries a future {@code exp}.
 */
public class IdentityProviderTokenIntrospector {

    private static final String ACCESS_MANAGEMENT_DISCOVERY_PATH = "/oidc/.well-known/openid-configuration";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Client client;
    private final Map<String, String> issuers = new ConcurrentHashMap<>();

    public IdentityProviderTokenIntrospector(Client client) {
        this.client = client;
    }

    public static Optional<String> discoveryUrl(SocialIdentityProviderEntity identityProvider) {
        return switch (identityProvider) {
            case OIDCIdentityProviderEntity oidc -> Optional.ofNullable(oidc.getDiscoveryEndpoint());
            case AMIdentityProviderEntity accessManagement -> Optional.ofNullable(accessManagement.getDiscoveryEndpoint()).or(() ->
                Optional.of(accessManagement.getServerUrl() + ACCESS_MANAGEMENT_DISCOVERY_PATH)
            );
            default -> Optional.empty();
        };
    }

    public IntrospectedToken introspect(
        SocialIdentityProviderEntity identityProvider,
        ClientCredentials credentials,
        String token,
        String resource
    ) {
        var form = new MultivaluedHashMap<String, String>();
        form.add("token", token);
        form.add("token_type_hint", "access_token");
        JsonNode claims;
        try (
            var response = client
                .target(identityProvider.getTokenIntrospectionEndpoint())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.AUTHORIZATION, basic(credentials))
                .post(Entity.form(form))
        ) {
            if (response.getStatus() != 200) {
                throw new IdentityProviderException(
                    "Introspection at identity provider " + identityProvider.getId() + " answered " + response.getStatus()
                );
            }
            claims = JSON.readTree(response.readEntity(String.class));
        } catch (IOException | ProcessingException e) {
            throw new IdentityProviderException("Introspection at identity provider " + identityProvider.getId() + " failed", e);
        }
        return accept(identityProvider, claims, resource);
    }

    /** Read once per discovery document: an issuer that changes at the same URL needs a restart. */
    public String issuer(SocialIdentityProviderEntity identityProvider) {
        var url = discoveryUrl(identityProvider).orElseThrow(() ->
            new IdentityProviderException("Identity provider " + identityProvider.getId() + " has no discovery document")
        );
        return issuers.computeIfAbsent(url, this::readIssuer);
    }

    private String readIssuer(String discoveryUrl) {
        try (var response = client.target(discoveryUrl).request(MediaType.APPLICATION_JSON_TYPE).get()) {
            if (response.getStatus() != 200) {
                throw new IdentityProviderException("Discovery document at " + discoveryUrl + " answered " + response.getStatus());
            }
            var issuer = JSON.readTree(response.readEntity(String.class)).path("issuer");
            if (!issuer.isTextual() || issuer.asText().isBlank()) {
                throw new IdentityProviderException("Discovery document at " + discoveryUrl + " names no issuer");
            }
            return issuer.asText();
        } catch (IOException | ProcessingException e) {
            throw new IdentityProviderException("Cannot read the discovery document at " + discoveryUrl, e);
        }
    }

    private static IntrospectedToken accept(SocialIdentityProviderEntity identityProvider, JsonNode claims, String resource) {
        var source = "Identity provider " + identityProvider.getId();
        var active = claims.path("active");
        if (!active.isBoolean() || !active.booleanValue()) {
            throw new IdentityProviderException(source + " reported the token as inactive");
        }
        if (!audienceContains(claims.path("aud"), resource)) {
            throw new IdentityProviderException(source + " issued the token for another audience than " + resource);
        }
        var exp = claims.path("exp");
        if (!exp.canConvertToLong()) {
            throw new IdentityProviderException(source + " gave the token no expiry");
        }
        var expiresAt = Instant.ofEpochSecond(exp.asLong());
        if (!expiresAt.isAfter(TimeProvider.instantNow())) {
            throw new IdentityProviderException(source + " gave the token an expiry in the past");
        }
        return new IntrospectedToken(expiresAt, claims);
    }

    private static boolean audienceContains(JsonNode audience, String resource) {
        if (audience.isTextual()) {
            return audience.asText().equals(resource);
        }
        return (
            audience.isArray() &&
            StreamSupport.stream(audience.spliterator(), false).anyMatch(value -> value.isTextual() && value.asText().equals(resource))
        );
    }

    private static String basic(ClientCredentials credentials) {
        var pair = credentials.clientId() + ':' + credentials.clientSecret();
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }
}
