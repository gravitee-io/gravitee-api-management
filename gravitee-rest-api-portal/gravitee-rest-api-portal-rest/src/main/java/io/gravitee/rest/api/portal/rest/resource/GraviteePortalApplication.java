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
package io.gravitee.rest.api.portal.rest.resource;

import javax.inject.Inject;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import io.gravitee.rest.api.portal.rest.filter.GraviteeContextFilter;
import io.gravitee.rest.api.portal.rest.filter.SecurityContextFilter;
import io.gravitee.rest.api.portal.rest.mapper.ObjectMapperResolver;
import io.gravitee.rest.api.portal.rest.provider.BadRequestExceptionMapper;
import io.gravitee.rest.api.portal.rest.provider.ByteArrayOutputStreamWriter;
import io.gravitee.rest.api.portal.rest.provider.ConstraintValidationExceptionMapper;
import io.gravitee.rest.api.portal.rest.provider.ManagementExceptionMapper;
import io.gravitee.rest.api.portal.rest.provider.NotAllowedExceptionMapper;
import io.gravitee.rest.api.portal.rest.provider.NotFoundExceptionMapper;
import io.gravitee.rest.api.portal.rest.provider.QueryParamExceptionMapper;
import io.gravitee.rest.api.portal.rest.provider.ThrowableMapper;
import io.gravitee.rest.api.portal.rest.provider.UnrecognizedPropertyExceptionMapper;
import io.gravitee.rest.api.portal.rest.provider.UriBuilderRequestFilter;
import io.gravitee.rest.api.security.authentication.AuthenticationProviderManager;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteePortalApplication extends ResourceConfig {

    private final AuthenticationProviderManager authenticationProviderManager;

    @Inject
    public GraviteePortalApplication(AuthenticationProviderManager authenticationProviderManager) {
        this.authenticationProviderManager = authenticationProviderManager;

        //Main resource
        register(EnvironmentResource.class);
        register(OpenApiResource.class);
        
        register(MultiPartFeature.class);
        
        register(ObjectMapperResolver.class);
        register(ManagementExceptionMapper.class);
        register(ConstraintValidationExceptionMapper.class);
        register(UnrecognizedPropertyExceptionMapper.class);
        register(ThrowableMapper.class);
        register(NotFoundExceptionMapper.class);
        register(NotAllowedExceptionMapper.class);
        register(BadRequestExceptionMapper.class);
        register(QueryParamExceptionMapper.class);

        register(SecurityContextFilter.class);
        register(GraviteeContextFilter.class);
        register(UriBuilderRequestFilter.class);
        register(ByteArrayOutputStreamWriter.class);
        register(JacksonFeature.class);
        

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
    }
}
