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

import com.fasterxml.jackson.databind.JavaType;
import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.common.util.Version;
import io.gravitee.management.rest.bind.AuthenticationBinder;
import io.gravitee.management.rest.filter.PermissionsFilter;
import io.gravitee.management.rest.filter.SecurityContextFilter;
import io.gravitee.management.rest.mapper.ObjectMapperResolver;
import io.gravitee.management.rest.provider.*;
import io.gravitee.management.rest.resource.auth.GitHubAuthenticationResource;
import io.gravitee.management.rest.resource.auth.GoogleAuthenticationResource;
import io.gravitee.management.rest.resource.auth.OAuth2AuthenticationResource;
import io.gravitee.management.rest.resource.search.SearchResource;
import io.gravitee.management.security.authentication.AuthenticationProvider;
import io.gravitee.management.security.authentication.AuthenticationProviderManager;
import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Model;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.Property;
import io.swagger.util.Json;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
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

        ModelConverters.getInstance().addConverter(new ModelConverter() {
            @Override
            public Property resolveProperty(Type type, ModelConverterContext context, Annotation[] annotations, Iterator<ModelConverter> chain) {
                final JavaType jType = Json.mapper().constructType(type);
                if (jType != null) {
                    final Class<?> cls = jType.getRawClass();
                    if (Date.class.isAssignableFrom(cls)) {
                        //DateTimeProperty property =
                        //        (DateTimeProperty) chain.next().resolveProperty(type, context, annotations, chain);
                        return new LongProperty();
                    }
                }

                return chain.hasNext() ?
                        chain.next().resolveProperty(type, context, annotations, chain)
                        : null;
            }

            @Override
            public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
                return chain.next().resolve(type, context, chain);
            }
        });
        register(ApisResource.class);
        register(ApplicationsResource.class);
        register(SubscriptionsResource.class);
        register(UsersResource.class);
        register(PoliciesResource.class);
        register(FetchersResource.class);
        register(ResourcesResource.class);
        register(InstancesResource.class);
        register(CurrentUserResource.class);
        register(PlatformResource.class);
        register(ConfigurationResource.class);
        register(GroupsResource.class);
        register(PortalResource.class);
        register(AuditResource.class);
        register(SearchResource.class);
        register(MessagesResource.class);

        // Dynamically register authentication endpoints
        register(new AuthenticationBinder(authenticationProviderManager));
        registerAuthenticationEndpoints();

        register(ObjectMapperResolver.class);
        register(ManagementExceptionMapper.class);
        register(UnrecognizedPropertyExceptionMapper.class);
        register(ThrowableMapper.class);
        register(NotFoundExceptionMapper.class);
        register(NotAllowedExceptionMapper.class);
        register(BadRequestExceptionMapper.class);

        register(SecurityContextFilter.class);
        register(PermissionsFilter.class);
        register(UriBuilderRequestFilter.class);
        register(ByteArrayOutputStreamWriter.class);
        register(JacksonFeature.class);

        register(ApiListingResource.class);
        register(SwaggerSerializers.class);

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
    }

    private void registerAuthenticationEndpoints() {
        registerAuthenticationEndpoint("google", GoogleAuthenticationResource.class);
        registerAuthenticationEndpoint("github", GitHubAuthenticationResource.class);
        registerAuthenticationEndpoint("oauth2", OAuth2AuthenticationResource.class);
    }

    private void registerAuthenticationEndpoint(String provider, Class resource) {
        if (authenticationProviderManager != null) {
            Optional<AuthenticationProvider> socialProvider = authenticationProviderManager.findIdentityProviderByType(provider);
            if (socialProvider.isPresent()) {
                Map<String, Object> configuration = socialProvider.get().configuration();
                String clientId = (String) EnvironmentUtils.get("clientId", configuration);
                String clientSecret = (String) EnvironmentUtils.get("clientSecret", configuration);

                if (clientId != null && !clientId.isEmpty() && clientSecret != null && !clientSecret.isEmpty()) {
                    register(resource);
                }
            }
        }
    }
}
