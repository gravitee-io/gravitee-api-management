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
package io.gravitee.gamma.authz.rest;

import io.gravitee.gamma.authz.rest.exception.CascadeTooLargeExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.EntityNotFoundExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.ForbiddenAccessExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.IllegalArgumentExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.InvalidEntityIdExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.InvalidStatusTransitionExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.PolicyNotFoundExceptionMapper;
import io.gravitee.gamma.authz.rest.exception.UnauthorizedAccessExceptionMapper;
import io.gravitee.gamma.authz.rest.resource.EntitiesResource;
import io.gravitee.gamma.authz.rest.resource.HealthResource;
import io.gravitee.gamma.authz.rest.resource.PoliciesResource;
import io.gravitee.gamma.authz.rest.resource.SchemaResource;
import io.gravitee.rest.api.rest.filter.GraviteeContextResponseFilter;
import io.gravitee.rest.api.rest.filter.GraviteeLicenseFilter;
import io.gravitee.rest.api.rest.filter.MaintenanceFilter;
import io.gravitee.rest.api.rest.filter.PermissionsFilter;
import io.gravitee.rest.api.rest.filter.SecurityContextFilter;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class GammaAuthzApplication extends ResourceConfig {

    public GammaAuthzApplication() {
        register(PoliciesResource.class);
        register(EntitiesResource.class);
        register(SchemaResource.class);
        register(HealthResource.class);

        register(PolicyNotFoundExceptionMapper.class);
        register(EntityNotFoundExceptionMapper.class);
        register(CascadeTooLargeExceptionMapper.class);
        register(InvalidStatusTransitionExceptionMapper.class);
        register(InvalidEntityIdExceptionMapper.class);
        register(IllegalArgumentExceptionMapper.class);
        register(UnauthorizedAccessExceptionMapper.class);
        register(ForbiddenAccessExceptionMapper.class);

        register(SecurityContextFilter.class);
        register(PermissionsFilter.class);
        register(GraviteeLicenseFilter.class);
        register(GraviteeContextResponseFilter.class);
        register(MaintenanceFilter.class);

        register(JacksonFeature.class);
    }
}
