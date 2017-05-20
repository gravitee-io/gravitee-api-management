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
package io.gravitee.management.rest.resource;

import io.gravitee.common.util.Version;
import io.gravitee.management.rest.filter.ApiPermissionFilter;
import io.gravitee.management.rest.filter.ApplicationPermissionFilter;
import io.gravitee.management.rest.filter.SecurityContextFilter;
import io.gravitee.management.rest.mapper.ObjectMapperResolver;
import io.gravitee.management.rest.provider.*;
import io.gravitee.management.rest.resource.auth.GitHubAuthenticationResource;
import io.gravitee.management.rest.resource.auth.GoogleAuthenticationResource;
import io.gravitee.management.security.authentication.AuthenticationProvider;
import io.gravitee.management.security.authentication.AuthenticationProviderManager;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeApplication extends ResourceConfig {

    private final AuthenticationProviderManager authenticationProviderManager;

    @Inject
    public GraviteeApplication(AuthenticationProviderManager authenticationProviderManager) {
        this.authenticationProviderManager = authenticationProviderManager;

        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion(Version.RUNTIME_VERSION.MAJOR_VERSION);
        beanConfig.setResourcePackage("io.gravitee.management.rest.resource");
        beanConfig.setTitle("Gravitee.io - Rest API");
        beanConfig.setScan(true);

        register(ApisResource.class);
        register(ApplicationsResource.class);
        register(SubscriptionsResource.class);
        register(UsersResource.class);
        register(PoliciesResource.class);
        register(FetchersResource.class);
        register(ResourcesResource.class);
        register(InstancesResource.class);
        register(UserResource.class);
        register(PlatformResource.class);
        register(ConfigurationResource.class);
        register(GroupsResource.class);
        register(PortalResource.class);

        // Dynamically register social authentication provider
        registerSocialProviders();

        register(ObjectMapperResolver.class);
        register(ManagementExceptionMapper.class);
        register(UnrecognizedPropertyExceptionMapper.class);
        register(ThrowableMapper.class);
        register(NotFoundExceptionMapper.class);
        register(BadRequestExceptionMapper.class);

        register(SecurityContextFilter.class);
        register(ApiPermissionFilter.class);
        register(ApplicationPermissionFilter.class);
        register(CorsResponseFilter.class);
        register(UriBuilderRequestFilter.class);
        register(ByteArrayOutputStreamWriter.class);
        register(JacksonFeature.class);

        register(ApiListingResource.class);
        register(SwaggerSerializers.class);

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
    }

    private void registerSocialProviders() {
        registerSocialProvider("google", GoogleAuthenticationResource.class);
        registerSocialProvider("github", GitHubAuthenticationResource.class);
    }

    private void registerSocialProvider(String provider, Class resource) {
        if (authenticationProviderManager != null) {
            Optional<AuthenticationProvider> socialProvider = authenticationProviderManager.findIdentityProviderByType(provider);
            if (socialProvider.isPresent()) {
                Map<String, Object> configuration = socialProvider.get().configuration();
                String clientId = (String) configuration.get("clientId");
                String clientSecret = (String) configuration.get("clientSecret");

                if (clientId != null && !clientId.isEmpty() && clientSecret != null && !clientSecret.isEmpty()) {
                    register(resource);
                }
            }
        }
    }
}
