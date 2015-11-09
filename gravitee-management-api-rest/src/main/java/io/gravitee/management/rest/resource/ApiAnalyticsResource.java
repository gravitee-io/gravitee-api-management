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

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.analytics.HistogramAnalytics;
import io.gravitee.management.rest.resource.param.AnalyticsParam;
import io.gravitee.management.service.AnalyticsService;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.PermissionService;
import io.gravitee.management.service.PermissionType;
import io.gravitee.management.service.exceptions.ApiNotFoundException;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiAnalyticsResource extends AbstractResource {

    @PathParam("apiName")
    private String apiName;

    @Inject
    private ApiService apiService;

    @Inject
    private PermissionService permissionService;

    @Inject
    private AnalyticsService analyticsService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response hits(@BeanParam AnalyticsParam analyticsParam) throws ApiNotFoundException {
        Optional<ApiEntity> api = apiService.findByName(apiName);

        if (! api.isPresent()) {
            throw new ApiNotFoundException(apiName);
        }

        permissionService.hasPermission(getAuthenticatedUser(), apiName, PermissionType.VIEW_API);

        HistogramAnalytics analytics = null;

        switch(analyticsParam.getTypeParam().getType()) {
            case HITS:
                analytics = analyticsService.apiHits(
                        apiName,
                        analyticsParam.getFrom(),
                        analyticsParam.getTo(),
                        analyticsParam.getInterval());
                break;
            case HITS_BY_LATENCY:
                analytics = analyticsService.apiHitsByLatency(
                        apiName,
                        analyticsParam.getFrom(),
                        analyticsParam.getTo(),
                        analyticsParam.getInterval());
                break;
            case HITS_BY_STATUS:
                analytics = analyticsService.apiHitsByStatus(
                        apiName,
                        analyticsParam.getFrom(),
                        analyticsParam.getTo(),
                        analyticsParam.getInterval());
                break;
            case HITS_BY_APIKEY:
                analytics = analyticsService.apiHitsByApiKey(
                        apiName,
                        analyticsParam.getFrom(),
                        analyticsParam.getTo(),
                        analyticsParam.getInterval());
                break;
        }

        return Response.ok(analytics).build();
    }
}
