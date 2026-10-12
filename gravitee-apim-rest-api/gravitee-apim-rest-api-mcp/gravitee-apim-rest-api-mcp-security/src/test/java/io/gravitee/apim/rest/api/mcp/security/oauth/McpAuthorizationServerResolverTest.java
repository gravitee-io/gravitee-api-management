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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.model.configuration.identity.am.AMIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.github.GitHubIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.oidc.OIDCIdentityProviderEntity;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService.ActivationTarget;
import io.gravitee.rest.api.service.impl.configuration.identity.IdentityProviderNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class McpAuthorizationServerResolverTest {

    private static final ActivationTarget ACME = new ActivationTarget("acme", IdentityProviderActivationReferenceType.ORGANIZATION);
    private static final ClientCredentials CREDENTIALS = new ClientCredentials("mcp-acme", "secret-acme");

    private final SocialIdentityProviderService identityProviders = mock(SocialIdentityProviderService.class);
    private final McpAuthorizationServerResolver resolver = new McpAuthorizationServerResolver(
        new McpAuthorizationSettings(
            new MockEnvironment()
                .withProperty("http.api.mcp.authorization.organizations[0].orgId", "acme")
                .withProperty("http.api.mcp.authorization.organizations[0].identityProvider", "idp-acme")
                .withProperty("http.api.mcp.authorization.organizations[0].clientId", "mcp-acme")
                .withProperty("http.api.mcp.authorization.organizations[0].clientSecret", "secret-acme")
        ),
        identityProviders
    );

    @Test
    void should_resolve_the_access_management_domain_activated_on_the_organization() {
        var accessManagement = new AMIdentityProviderEntity("https://am.example.com/acme");
        when(identityProviders.findById("idp-acme", ACME)).thenReturn(accessManagement);

        assertThat(resolver.resolve("acme")).contains(new McpAuthorizationServer(accessManagement, CREDENTIALS));
    }

    @Test
    void should_resolve_an_openid_connect_provider_with_introspection_and_discovery() {
        var oidc = new OIDCIdentityProviderEntity();
        oidc.setTokenIntrospectionEndpoint("https://keycloak.example.com/introspect");
        oidc.setDiscoveryEndpoint("https://keycloak.example.com/.well-known/openid-configuration");
        when(identityProviders.findById("idp-acme", ACME)).thenReturn(oidc);

        assertThat(resolver.resolve("acme")).contains(new McpAuthorizationServer(oidc, CREDENTIALS));
    }

    @Test
    void should_stay_off_for_an_organization_without_settings() {
        assertThat(resolver.resolve("globex")).isEmpty();
    }

    @Test
    void should_stay_off_when_the_identity_provider_is_not_activated_on_the_organization() {
        when(identityProviders.findById(eq("idp-acme"), any())).thenThrow(new IdentityProviderNotFoundException("idp-acme"));

        assertThat(resolver.resolve("acme")).isEmpty();
    }

    @Test
    void should_stay_off_for_a_provider_that_cannot_issue_tokens_for_the_endpoint() {
        when(identityProviders.findById("idp-acme", ACME)).thenReturn(new GitHubIdentityProviderEntity());

        assertThat(resolver.resolve("acme")).isEmpty();
    }

    @Test
    void should_stay_off_for_an_openid_connect_provider_without_introspection() {
        var oidc = new OIDCIdentityProviderEntity();
        oidc.setDiscoveryEndpoint("https://keycloak.example.com/.well-known/openid-configuration");
        when(identityProviders.findById("idp-acme", ACME)).thenReturn(oidc);

        assertThat(resolver.resolve("acme")).isEmpty();
    }

    @Test
    void should_stay_off_for_an_openid_connect_provider_without_discovery() {
        var oidc = new OIDCIdentityProviderEntity();
        oidc.setTokenIntrospectionEndpoint("https://keycloak.example.com/introspect");
        when(identityProviders.findById("idp-acme", ACME)).thenReturn(oidc);

        assertThat(resolver.resolve("acme")).isEmpty();
    }
}
