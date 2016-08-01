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

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.analytics.Analytics;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.rest.resource.param.AnalyticsParam;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.AnalyticsService;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@ApiPermissionsRequired(ApiPermission.ANALYTICS)
public class ApiAnalyticsResource extends AbstractResource {

    @PathParam("api")
    private String api;

    @Inject
    private AnalyticsService analyticsService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response hits(@BeanParam AnalyticsParam analyticsParam) {
        analyticsParam.validate();

        Analytics analytics = null;

        switch(analyticsParam.getTypeParam().getValue()) {
            case HITS_BY:
                analytics = analyticsService.apiHitsBy(
                        analyticsParam.getQuery(),
                        analyticsParam.getKey(),
                        analyticsParam.getField(),
                        analyticsParam.getAggType(),
                        analyticsParam.getFrom(),
                        analyticsParam.getTo(),
                        analyticsParam.getInterval());
                break;
            case GLOBAL_HITS:
                analytics = analyticsService.apiGlobalHits(
                        analyticsParam.getQuery(),
                        analyticsParam.getKey(),
                        analyticsParam.getFrom(),
                        analyticsParam.getTo());
                break;
            case TOP_HITS:
                analytics = analyticsService.apiTopHits(
                        analyticsParam.getQuery(),
                        analyticsParam.getKey(),
                        analyticsParam.getField(),
                        analyticsParam.getFrom(),
                        analyticsParam.getTo(),
                        analyticsParam.getSize());
                break;
        }

        return Response.ok(analytics).build();
    }
}
