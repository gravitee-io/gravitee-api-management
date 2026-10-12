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
package io.gravitee.apim.rest.api.mcp.security.config;

import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.rest.api.mcp.security.oauth.IdentityProviderTokenIntrospector;
import io.gravitee.apim.rest.api.mcp.security.oauth.McpAuthorizationServerResolver;
import io.gravitee.apim.rest.api.mcp.security.oauth.McpAuthorizationSettings;
import io.gravitee.apim.rest.api.mcp.security.oauth.McpMountResolver;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.builder.JerseyClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * OAuth on the MCP mount: which organization a URL serves, which authorization server protects it, and how its tokens
 * are checked.
 *
 * @author GraviteeSource Team
 */
@Configuration
public class McpOAuthConfiguration {

    @Bean
    public McpAuthorizationSettings mcpAuthorizationSettings(Environment environment) {
        return new McpAuthorizationSettings(environment);
    }

    @Bean
    public McpMountResolver mcpMountResolver(
        InstallationTypeDomainService installationTypeDomainService,
        AccessPointQueryService accessPointQueryService,
        OrganizationService organizationService,
        InstallationAccessQueryService installationAccessQueryService
    ) {
        return new McpMountResolver(
            installationTypeDomainService,
            accessPointQueryService,
            organizationService,
            installationAccessQueryService
        );
    }

    @Bean
    public McpAuthorizationServerResolver mcpAuthorizationServerResolver(
        McpAuthorizationSettings mcpAuthorizationSettings,
        SocialIdentityProviderService socialIdentityProviderService
    ) {
        return new McpAuthorizationServerResolver(mcpAuthorizationSettings, socialIdentityProviderService);
    }

    @Bean
    public IdentityProviderTokenIntrospector identityProviderTokenIntrospector(Environment environment) {
        return new IdentityProviderTokenIntrospector(JerseyClientBuilder.newBuilder(environment).build());
    }
}
