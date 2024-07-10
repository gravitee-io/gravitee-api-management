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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.rest.api.portal.rest.filter.GraviteeContextRequestFilter;
import io.gravitee.rest.api.portal.rest.filter.GraviteeContextResponseFilter;
import io.gravitee.rest.api.portal.rest.filter.PermissionsFilter;
import io.gravitee.rest.api.portal.rest.filter.SecurityContextFilter;
import io.gravitee.rest.api.portal.rest.mapper.ObjectMapperResolver;
import io.gravitee.rest.api.portal.rest.provider.*;
import io.gravitee.rest.api.security.authentication.AuthenticationProviderManager;
import javax.inject.Inject;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

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
        register(EnvironmentsResource.class);
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
        register(JsonMappingExceptionMapper.class);

        register(SecurityContextFilter.class);
        register(GraviteeContextRequestFilter.class);
        register(GraviteeContextResponseFilter.class);
        register(PermissionsFilter.class);
        register(UriBuilderRequestFilter.class);
        register(ByteArrayOutputStreamWriter.class);
        register(JacksonFeature.class);

        register(PayloadInputBodyReader.class);

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
    }
}
