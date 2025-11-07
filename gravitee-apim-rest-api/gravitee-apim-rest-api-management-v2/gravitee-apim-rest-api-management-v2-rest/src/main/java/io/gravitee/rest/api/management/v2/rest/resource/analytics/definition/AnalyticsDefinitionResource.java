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
package io.gravitee.rest.api.management.v2.rest.resource.analytics.definition;

import io.gravitee.apim.core.analytics_engine.use_case.GetApiMetricSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFacetSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFilterSpecUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.AnalyticsDefinitionMapper;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ApiSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricSpecsResponse;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public class AnalyticsDefinitionResource {

    @Inject
    GetApiSpecUseCase getApiSpec;

    @Inject
    GetApiMetricSpecUseCase getApiMetricSpec;

    @Inject
    GetMetricFacetSpecUseCase getMetricFacetSpec;

    @Inject
    GetMetricFilterSpecUseCase getMetricFilterSpec;

    @Path("/apis")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public ApiSpecsResponse getApiSpecs() {
        return AnalyticsDefinitionMapper.INSTANCE.toApiSpecsResponse(getApiSpec.execute().specs());
    }

    @Path("/apis/{apiName}/metrics")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public MetricSpecsResponse getApiMetricSpecs(@PathParam("apiName") String apiName) {
        return AnalyticsDefinitionMapper.INSTANCE.toMetricSpecsResponse(
            getApiMetricSpec.execute(new GetApiMetricSpecUseCase.Input(apiName)).specs()
        );
    }

    @Path("/metrics/{metricName}/facets")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public FacetSpecsResponse getMetricFacetSpecs(@PathParam("metricName") String metricName) {
        return AnalyticsDefinitionMapper.INSTANCE.toFacetSpecsResponse(
            getMetricFacetSpec.execute(new GetMetricFacetSpecUseCase.Input(metricName)).specs()
        );
    }

    @Path("/metrics/{metricName}/filters")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public FilterSpecsResponse getMetricFilterSpecs(@PathParam("metricName") String metricName) {
        return AnalyticsDefinitionMapper.INSTANCE.toFilterSpecsResponse(
            getMetricFilterSpec.execute(new GetMetricFilterSpecUseCase.Input(metricName)).specs()
        );
    }
}
