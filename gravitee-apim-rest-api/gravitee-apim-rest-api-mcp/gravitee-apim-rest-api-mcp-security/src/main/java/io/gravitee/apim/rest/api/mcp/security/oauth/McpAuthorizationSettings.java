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

import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.env.Environment;

/**
 * The OAuth settings of the MCP mount, per organization, read from {@code gravitee.yml} like the standalone console
 * URLs: a list under {@code organizations}, or a single entry for the default organization.
 */
public final class McpAuthorizationSettings {

    static final String PREFIX = "http.api.mcp.authorization.";

    private final Map<String, McpAuthorization> byOrganization;

    public McpAuthorizationSettings(Environment environment) {
        this.byOrganization = load(environment);
    }

    public Optional<McpAuthorization> of(String organizationId) {
        return Optional.ofNullable(byOrganization.get(organizationId));
    }

    private static Map<String, McpAuthorization> load(Environment environment) {
        var result = new HashMap<String, McpAuthorization>();
        if (environment.containsProperty(PREFIX + "organizations[0].orgId")) {
            for (int index = 0; environment.containsProperty(PREFIX + "organizations[" + index + "].orgId"); index++) {
                var prefix = PREFIX + "organizations[" + index + "].";
                result.put(required(environment, prefix + "orgId"), entry(environment, prefix));
            }
        } else if (environment.containsProperty(PREFIX + "identityProvider")) {
            result.put(GraviteeContext.getDefaultOrganization(), entry(environment, PREFIX));
        }
        return Map.copyOf(result);
    }

    private static McpAuthorization entry(Environment environment, String prefix) {
        return new McpAuthorization(
            required(environment, prefix + "identityProvider"),
            new ClientCredentials(required(environment, prefix + "clientId"), required(environment, prefix + "clientSecret"))
        );
    }

    private static String required(Environment environment, String key) {
        var value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " is required to protect the MCP endpoint with OAuth.");
        }
        return value;
    }
}
