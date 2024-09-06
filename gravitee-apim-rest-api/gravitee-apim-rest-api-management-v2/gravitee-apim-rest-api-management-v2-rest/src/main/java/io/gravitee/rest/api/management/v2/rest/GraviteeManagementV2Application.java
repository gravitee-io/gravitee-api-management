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
package io.gravitee.rest.api.management.v2.rest;

import io.gravitee.rest.api.management.v2.rest.exceptionMapper.BadRequestExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.ConstraintValidationExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.JsonMappingExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.ManagementExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.NotFoundExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.PreconditionFailedExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.ThrowableMapper;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.UnrecognizedPropertyExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.ValidationExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.domain.NotAllowedDomainExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.domain.NotFoundDomainExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.domain.TechnicalDomainExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.domain.ValidationDomainExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.provider.ByteArrayOutputStreamWriter;
import io.gravitee.rest.api.management.v2.rest.provider.CommaSeparatedQueryParamConverterProvider;
import io.gravitee.rest.api.management.v2.rest.provider.ObjectMapperResolver;
import io.gravitee.rest.api.management.v2.rest.provider.YamlWriter;
import io.gravitee.rest.api.management.v2.rest.resource.OpenAPIResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApisResource;
import io.gravitee.rest.api.management.v2.rest.resource.application.ApplicationsResource;
import io.gravitee.rest.api.management.v2.rest.resource.asyncjob.AsyncJobsResource;
import io.gravitee.rest.api.management.v2.rest.resource.installation.EnvironmentsResource;
import io.gravitee.rest.api.management.v2.rest.resource.installation.GraviteeLicenseResource;
import io.gravitee.rest.api.management.v2.rest.resource.installation.OrganizationResource;
import io.gravitee.rest.api.management.v2.rest.resource.integration.IntegrationsResource;
import io.gravitee.rest.api.management.v2.rest.resource.plugin.ApiServicesResource;
import io.gravitee.rest.api.management.v2.rest.resource.plugin.EndpointsResource;
import io.gravitee.rest.api.management.v2.rest.resource.plugin.EntrypointsResource;
import io.gravitee.rest.api.management.v2.rest.resource.plugin.PoliciesResource;
import io.gravitee.rest.api.management.v2.rest.resource.plugin.ResourcesResource;
import io.gravitee.rest.api.management.v2.rest.resource.ui.ManagementUIResource;
import io.gravitee.rest.api.rest.filter.GraviteeContextResponseFilter;
import io.gravitee.rest.api.rest.filter.PermissionsFilter;
import io.gravitee.rest.api.rest.filter.SecurityContextFilter;
import io.gravitee.rest.api.rest.filter.UriBuilderRequestFilter;
import jakarta.inject.Inject;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeManagementV2Application extends ResourceConfig {

    @Inject
    public GraviteeManagementV2Application() {
        //Main resource
        register(ManagementUIResource.class);
        register(GraviteeLicenseResource.class); // Deprecated
        register(OrganizationResource.class);
        register(EnvironmentsResource.class);
        register(ApisResource.class);
        register(ApplicationsResource.class);

        // Resources deprecated at root level
        register(EndpointsResource.class);
        register(EntrypointsResource.class);
        register(ApiServicesResource.class);
        register(PoliciesResource.class);
        register(ResourcesResource.class);
        register(IntegrationsResource.class);
        register(AsyncJobsResource.class);

        register(MultiPartFeature.class);

        register(ObjectMapperResolver.class);

        register(ManagementExceptionMapper.class);
        register(ConstraintValidationExceptionMapper.class);
        register(UnrecognizedPropertyExceptionMapper.class);
        register(ThrowableMapper.class);
        register(NotFoundExceptionMapper.class);
        register(io.gravitee.rest.api.management.v2.rest.exceptionMapper.NotAllowedExceptionMapper.class);
        register(BadRequestExceptionMapper.class);
        register(PreconditionFailedExceptionMapper.class);
        register(ValidationExceptionMapper.class);
        register(JsonMappingExceptionMapper.class);

        register(ValidationDomainExceptionMapper.class);
        register(TechnicalDomainExceptionMapper.class);
        register(NotAllowedDomainExceptionMapper.class);
        register(NotFoundDomainExceptionMapper.class);

        register(CommaSeparatedQueryParamConverterProvider.class);

        register(SecurityContextFilter.class);
        register(PermissionsFilter.class);
        register(GraviteeContextResponseFilter.class);
        register(UriBuilderRequestFilter.class);
        register(ByteArrayOutputStreamWriter.class);
        register(YamlWriter.class);
        register(JacksonFeature.class);

        register(OpenAPIResource.class);

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
    }
}
