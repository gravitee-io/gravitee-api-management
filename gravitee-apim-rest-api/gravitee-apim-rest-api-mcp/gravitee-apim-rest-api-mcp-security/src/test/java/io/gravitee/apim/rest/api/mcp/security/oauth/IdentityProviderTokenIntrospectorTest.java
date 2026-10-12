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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.configuration.identity.am.AMIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.oidc.OIDCIdentityProviderEntity;
import io.gravitee.rest.api.service.builder.JerseyClientBuilder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

@WireMockTest
class IdentityProviderTokenIntrospectorTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");
    private static final String RESOURCE = "https://apim.example.com/mcp/organizations/acme";
    private static final ClientCredentials CREDENTIALS = new ClientCredentials("mcp-client", "mcp-secret");

    private final IdentityProviderTokenIntrospector introspector = new IdentityProviderTokenIntrospector(
        JerseyClientBuilder.newBuilder(new MockEnvironment()).build()
    );
    private OIDCIdentityProviderEntity identityProvider;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wireMock) {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
        identityProvider = new OIDCIdentityProviderEntity();
        identityProvider.setId("keycloak-acme");
        identityProvider.setTokenIntrospectionEndpoint(wireMock.getHttpBaseUrl() + "/introspect");
        identityProvider.setDiscoveryEndpoint(wireMock.getHttpBaseUrl() + "/.well-known/openid-configuration");
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Test
    void should_introspect_with_the_credentials_of_the_mcp_endpoint() {
        stubIntrospection("{\"active\":true,\"sub\":\"jane\",\"aud\":\"" + RESOURCE + "\",\"exp\":" + inOneHour() + "}");

        var token = introspector.introspect(identityProvider, CREDENTIALS, "the-token", RESOURCE);

        assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(token.claims().get("sub").asText()).isEqualTo("jane");
        verify(
            postRequestedFor(urlEqualTo("/introspect"))
                .withHeader("Authorization", equalTo("Basic " + base64("mcp-client:mcp-secret")))
                .withRequestBody(containing("token=the-token"))
        );
    }

    @Test
    void should_accept_an_audience_given_as_an_array() {
        stubIntrospection("{\"active\":true,\"aud\":[\"another\",\"" + RESOURCE + "\"],\"exp\":" + inOneHour() + "}");

        assertThat(introspector.introspect(identityProvider, CREDENTIALS, "the-token", RESOURCE).expiresAt()).isAfter(NOW);
    }

    @Test
    void should_reject_an_inactive_token() {
        stubIntrospection("{\"active\":false}");

        assertRejected("inactive");
    }

    @Test
    void should_reject_a_token_whose_activity_is_not_stated() {
        stubIntrospection("{\"aud\":\"" + RESOURCE + "\",\"exp\":" + inOneHour() + "}");

        assertRejected("inactive");
    }

    @Test
    void should_reject_a_token_issued_for_another_resource() {
        stubIntrospection("{\"active\":true,\"aud\":\"https://apim.example.com/mcp/organizations/globex\",\"exp\":" + inOneHour() + "}");

        assertRejected("audience");
    }

    @Test
    void should_reject_a_token_without_audience() {
        stubIntrospection("{\"active\":true,\"exp\":" + inOneHour() + "}");

        assertRejected("audience");
    }

    @Test
    void should_reject_a_token_without_expiry() {
        stubIntrospection("{\"active\":true,\"aud\":\"" + RESOURCE + "\"}");

        assertRejected("expiry");
    }

    @Test
    void should_reject_an_expired_token() {
        stubIntrospection("{\"active\":true,\"aud\":\"" + RESOURCE + "\",\"exp\":" + NOW.minusSeconds(1).getEpochSecond() + "}");

        assertRejected("expiry");
    }

    @Test
    void should_reject_a_token_the_identity_provider_could_not_introspect() {
        stubFor(post(urlEqualTo("/introspect")).willReturn(aResponse().withStatus(401).withBody("{\"error\":\"invalid_client\"}")));

        assertRejected("401");
    }

    @Test
    void should_read_the_issuer_once_from_the_discovery_document() {
        stubFor(
            get(urlEqualTo("/.well-known/openid-configuration")).willReturn(
                okJson("{\"issuer\":\"https://keycloak.example.com/realms/acme\"}")
            )
        );

        assertThat(introspector.issuer(identityProvider)).isEqualTo("https://keycloak.example.com/realms/acme");
        assertThat(introspector.issuer(identityProvider)).isEqualTo("https://keycloak.example.com/realms/acme");
        verify(1, getRequestedFor(urlEqualTo("/.well-known/openid-configuration")));
    }

    @Test
    void should_read_the_discovery_document_of_an_access_management_domain_under_its_oidc_path(WireMockRuntimeInfo wireMock) {
        var accessManagement = new AMIdentityProviderEntity(wireMock.getHttpBaseUrl() + "/acme");
        stubFor(
            get(urlEqualTo("/acme/oidc/.well-known/openid-configuration")).willReturn(
                okJson("{\"issuer\":\"https://am.example.com/acme/oidc\"}")
            )
        );

        assertThat(IdentityProviderTokenIntrospector.discoveryUrl(accessManagement)).contains(
            wireMock.getHttpBaseUrl() + "/acme/oidc/.well-known/openid-configuration"
        );
        assertThat(introspector.issuer(accessManagement)).isEqualTo("https://am.example.com/acme/oidc");
    }

    @Test
    void should_retry_the_discovery_document_after_a_failure() {
        stubFor(get(urlEqualTo("/.well-known/openid-configuration")).willReturn(aResponse().withStatus(503)));
        assertThatThrownBy(() -> introspector.issuer(identityProvider)).isInstanceOf(IdentityProviderException.class);

        stubFor(
            get(urlEqualTo("/.well-known/openid-configuration")).willReturn(
                okJson("{\"issuer\":\"https://keycloak.example.com/realms/acme\"}")
            )
        );

        assertThat(introspector.issuer(identityProvider)).isEqualTo("https://keycloak.example.com/realms/acme");
    }

    private void assertRejected(String reason) {
        assertThatThrownBy(() -> introspector.introspect(identityProvider, CREDENTIALS, "the-token", RESOURCE))
            .isInstanceOf(IdentityProviderException.class)
            .hasMessageContaining(reason)
            .hasMessageNotContaining("the-token");
    }

    private static void stubIntrospection(String body) {
        stubFor(post(urlEqualTo("/introspect")).willReturn(okJson(body)));
    }

    private static long inOneHour() {
        return NOW.plusSeconds(3600).getEpochSecond();
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
