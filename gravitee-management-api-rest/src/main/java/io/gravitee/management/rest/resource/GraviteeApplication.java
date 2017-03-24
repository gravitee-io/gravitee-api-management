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
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeApplication extends ResourceConfig {

    public GraviteeApplication() {

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
}
