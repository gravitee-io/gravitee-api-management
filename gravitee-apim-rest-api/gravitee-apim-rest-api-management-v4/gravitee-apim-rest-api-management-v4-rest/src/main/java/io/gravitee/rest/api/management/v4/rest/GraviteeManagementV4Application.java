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
package io.gravitee.rest.api.management.v4.rest;

import io.gravitee.rest.api.management.v4.rest.exceptionMapper.BadRequestExceptionMapper;
import io.gravitee.rest.api.management.v4.rest.exceptionMapper.ConstraintValidationExceptionMapper;
import io.gravitee.rest.api.management.v4.rest.exceptionMapper.ManagementExceptionMapper;
import io.gravitee.rest.api.management.v4.rest.exceptionMapper.NotAllowedExceptionMapper;
import io.gravitee.rest.api.management.v4.rest.exceptionMapper.NotFoundExceptionMapper;
import io.gravitee.rest.api.management.v4.rest.exceptionMapper.ThrowableMapper;
import io.gravitee.rest.api.management.v4.rest.exceptionMapper.UnrecognizedPropertyExceptionMapper;
import io.gravitee.rest.api.management.v4.rest.filter.GraviteeContextRequestFilter;
import io.gravitee.rest.api.management.v4.rest.filter.GraviteeContextResponseFilter;
import io.gravitee.rest.api.management.v4.rest.filter.SecurityContextFilter;
import io.gravitee.rest.api.management.v4.rest.filter.UriBuilderRequestFilter;
import io.gravitee.rest.api.management.v4.rest.provider.ByteArrayOutputStreamWriter;
import io.gravitee.rest.api.management.v4.rest.provider.ObjectMapperResolver;
import io.gravitee.rest.api.management.v4.rest.resource.OpenAPIResource;
import io.gravitee.rest.api.management.v4.rest.resource.api.ApiResource;
import io.gravitee.rest.api.management.v4.rest.resource.connector.EndpointsResource;
import io.gravitee.rest.api.management.v4.rest.resource.connector.EntrypointsResource;
import io.gravitee.rest.api.management.v4.rest.resource.installation.EnvironmentResource;
import io.gravitee.rest.api.management.v4.rest.resource.installation.OrganizationResource;
import javax.inject.Inject;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeManagementV4Application extends ResourceConfig {

    @Inject
    public GraviteeManagementV4Application() {
        //Main resource
        register(OrganizationResource.class);
        register(EnvironmentResource.class);
        register(ApiResource.class);
        register(EndpointsResource.class);
        register(EntrypointsResource.class);

        register(MultiPartFeature.class);

        register(ObjectMapperResolver.class);

        register(ManagementExceptionMapper.class);
        register(ConstraintValidationExceptionMapper.class);
        register(UnrecognizedPropertyExceptionMapper.class);
        register(ThrowableMapper.class);
        register(NotFoundExceptionMapper.class);
        register(NotAllowedExceptionMapper.class);
        register(BadRequestExceptionMapper.class);

        register(SecurityContextFilter.class);
        register(GraviteeContextRequestFilter.class);
        register(GraviteeContextResponseFilter.class);
        register(UriBuilderRequestFilter.class);
        register(ByteArrayOutputStreamWriter.class);
        register(JacksonFeature.class);

        register(OpenAPIResource.class);

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
    }
}
