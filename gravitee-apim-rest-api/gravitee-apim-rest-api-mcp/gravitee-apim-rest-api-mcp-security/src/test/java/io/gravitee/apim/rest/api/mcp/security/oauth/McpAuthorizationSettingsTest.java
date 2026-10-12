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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class McpAuthorizationSettingsTest {

    @Test
    void should_read_one_entry_per_organization() {
        var environment = new MockEnvironment()
            .withProperty("http.api.mcp.authorization.organizations[0].orgId", "DEFAULT")
            .withProperty("http.api.mcp.authorization.organizations[0].identityProvider", "am-default")
            .withProperty("http.api.mcp.authorization.organizations[0].clientId", "mcp-default")
            .withProperty("http.api.mcp.authorization.organizations[0].clientSecret", "secret-default")
            .withProperty("http.api.mcp.authorization.organizations[1].orgId", "acme")
            .withProperty("http.api.mcp.authorization.organizations[1].identityProvider", "keycloak-acme")
            .withProperty("http.api.mcp.authorization.organizations[1].clientId", "mcp-acme")
            .withProperty("http.api.mcp.authorization.organizations[1].clientSecret", "secret-acme");

        var settings = new McpAuthorizationSettings(environment);

        assertThat(settings.of("DEFAULT")).contains(
            new McpAuthorization("am-default", new ClientCredentials("mcp-default", "secret-default"))
        );
        assertThat(settings.of("acme")).contains(new McpAuthorization("keycloak-acme", new ClientCredentials("mcp-acme", "secret-acme")));
        assertThat(settings.of("other")).isEmpty();
    }

    @Test
    void should_read_the_single_form_as_the_default_organization() {
        var environment = new MockEnvironment()
            .withProperty("http.api.mcp.authorization.identityProvider", "am-default")
            .withProperty("http.api.mcp.authorization.clientId", "mcp-default")
            .withProperty("http.api.mcp.authorization.clientSecret", "secret-default");

        var settings = new McpAuthorizationSettings(environment);

        assertThat(settings.of("DEFAULT")).contains(
            new McpAuthorization("am-default", new ClientCredentials("mcp-default", "secret-default"))
        );
    }

    @Test
    void should_have_no_entry_when_nothing_is_configured() {
        assertThat(new McpAuthorizationSettings(new MockEnvironment()).of("DEFAULT")).isEmpty();
    }

    @Test
    void should_refuse_an_entry_without_its_client_secret_and_name_the_key() {
        var environment = new MockEnvironment()
            .withProperty("http.api.mcp.authorization.organizations[0].orgId", "acme")
            .withProperty("http.api.mcp.authorization.organizations[0].identityProvider", "keycloak-acme")
            .withProperty("http.api.mcp.authorization.organizations[0].clientId", "mcp-acme");

        assertThatThrownBy(() -> new McpAuthorizationSettings(environment))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("http.api.mcp.authorization.organizations[0].clientSecret");
    }

    @Test
    void should_never_print_the_client_secret() {
        assertThat(new ClientCredentials("mcp-acme", "secret-acme").toString()).contains("mcp-acme").doesNotContain("secret-acme");
    }
}
