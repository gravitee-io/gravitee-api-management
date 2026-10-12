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

import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderType;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.ActivationTarget;
import io.gravitee.rest.api.service.impl.configuration.identity.IdentityProviderNotFoundException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

/**
 * Finds the authorization server of an organization's MCP URL. When the settings name none, or one that cannot protect
 * the endpoint, OAuth stays off for the organization and the endpoint keeps accepting APIM tokens only.
 */
@CustomLog
public class McpAuthorizationServerResolver {

    private final McpAuthorizationSettings settings;
    private final SocialIdentityProviderService identityProviders;
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    public McpAuthorizationServerResolver(McpAuthorizationSettings settings, SocialIdentityProviderService identityProviders) {
        this.settings = settings;
        this.identityProviders = identityProviders;
    }

    public Optional<McpAuthorizationServer> resolve(String organizationId) {
        var authorization = settings.of(organizationId);
        if (authorization.isEmpty()) {
            return Optional.empty();
        }
        var id = authorization.get().identityProvider();
        var target = new ActivationTarget(organizationId, IdentityProviderActivationReferenceType.ORGANIZATION);
        try {
            var identityProvider = identityProviders.findById(id, target);
            if (
                identityProvider == null ||
                (identityProvider.getType() != IdentityProviderType.GRAVITEEIO_AM &&
                    identityProvider.getType() != IdentityProviderType.OIDC)
            ) {
                return off(organizationId, "identity provider " + id + " is neither Gravitee AM nor OpenID Connect");
            }
            if (identityProvider.getTokenIntrospectionEndpoint() == null) {
                return off(organizationId, "identity provider " + id + " has no token introspection endpoint");
            }
            if (IdentityProviderTokenIntrospector.discoveryUrl(identityProvider).isEmpty()) {
                return off(organizationId, "identity provider " + id + " has no discovery endpoint");
            }
            return Optional.of(new McpAuthorizationServer(identityProvider, authorization.get().credentials()));
        } catch (IdentityProviderNotFoundException e) {
            return off(organizationId, "identity provider " + id + " is not activated on the organization");
        }
    }

    private Optional<McpAuthorizationServer> off(String organizationId, String reason) {
        if (reported.add(organizationId + '\n' + reason)) {
            log.warn("OAuth is off on the MCP endpoint of organization {}: {}", organizationId, reason);
        }
        return Optional.empty();
    }
}
