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
package io.gravitee.plugin.console;

import io.gravitee.rest.api.management.rest.mapper.ObjectMapperResolver;
import io.gravitee.rest.api.management.rest.provider.BadRequestExceptionMapper;
import io.gravitee.rest.api.management.rest.provider.ByteArrayOutputStreamWriter;
import io.gravitee.rest.api.management.rest.provider.ConstraintValidationExceptionMapper;
import io.gravitee.rest.api.management.rest.provider.EnumParamConverterProvider;
import io.gravitee.rest.api.management.rest.provider.JsonMappingExceptionMapper;
import io.gravitee.rest.api.management.rest.provider.ManagementExceptionMapper;
import io.gravitee.rest.api.management.rest.provider.NotAllowedDomainExceptionMapper;
import io.gravitee.rest.api.management.rest.provider.NotAllowedExceptionMapper;
import io.gravitee.rest.api.management.rest.provider.NotFoundDomainExceptionMapper;
import io.gravitee.rest.api.management.rest.provider.NotFoundExceptionMapper;
import io.gravitee.rest.api.management.rest.provider.PayloadInputBodyReader;
import io.gravitee.rest.api.management.rest.provider.TechnicalDomainExceptionMapper;
import io.gravitee.rest.api.management.rest.provider.ThrowableMapper;
import io.gravitee.rest.api.management.rest.provider.UnrecognizedPropertyExceptionMapper;
import io.gravitee.rest.api.management.rest.provider.ValidationDomainExceptionMapper;
import io.gravitee.rest.api.rest.filter.GraviteeContextResponseFilter;
import io.gravitee.rest.api.rest.filter.GraviteeLicenseFilter;
import io.gravitee.rest.api.rest.filter.MaintenanceFilter;
import io.gravitee.rest.api.rest.filter.PermissionsFilter;
import io.gravitee.rest.api.rest.filter.SecurityContextFilter;
import io.gravitee.rest.api.rest.filter.UriBuilderRequestFilter;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConsoleExtensionApplication extends ResourceConfig {

    public ConsoleExtensionApplication() {
        register(ConsoleExtensionsResource.class);

        register(MultiPartFeature.class);
        register(PayloadInputBodyReader.class);
        register(ObjectMapperResolver.class);
        register(ManagementExceptionMapper.class);
        register(ConstraintValidationExceptionMapper.class);
        register(UnrecognizedPropertyExceptionMapper.class);
        register(ThrowableMapper.class);
        register(NotFoundExceptionMapper.class);
        register(NotAllowedExceptionMapper.class);
        register(BadRequestExceptionMapper.class);
        register(EnumParamConverterProvider.class);
        register(ValidationDomainExceptionMapper.class);
        register(NotAllowedDomainExceptionMapper.class);
        register(NotFoundDomainExceptionMapper.class);
        register(TechnicalDomainExceptionMapper.class);
        register(JsonMappingExceptionMapper.class);

        register(SecurityContextFilter.class);
        register(PermissionsFilter.class);
        register(GraviteeLicenseFilter.class);
        register(GraviteeContextResponseFilter.class);
        register(UriBuilderRequestFilter.class);
        register(MaintenanceFilter.class);
        register(ByteArrayOutputStreamWriter.class);
        register(JacksonFeature.class);
    }
}
