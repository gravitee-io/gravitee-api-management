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
import io.gravitee.management.model.ApiKeyEntity;
import io.gravitee.management.model.analytics.HistogramAnalytics;
import io.gravitee.management.model.permissions.ApplicationPermission;
import io.gravitee.management.rest.resource.param.AnalyticsParam;
import io.gravitee.management.rest.security.ApplicationPermissionsRequired;
import io.gravitee.management.service.AnalyticsService;
import io.gravitee.management.service.ApiKeyService;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.management.service.exceptions.NoValidApiKeyException;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@ApplicationPermissionsRequired(ApplicationPermission.MANAGE_API_KEYS)
@Api(tags = {"Application"})
public class ApiKeyAnalyticsResource extends AbstractResource {

    @PathParam("application")
    private String application;

    @PathParam("key")
    private String key;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private AnalyticsService analyticsService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response applicationAnalytics(@BeanParam AnalyticsParam analyticsParam) throws ApiNotFoundException {
        Optional<ApiKeyEntity> apiKeyEntity = apiKeyService.getCurrent(application, null);

        if (! apiKeyEntity.isPresent()) {
            throw new NoValidApiKeyException();
        }

        analyticsParam.validate();

        HistogramAnalytics analytics = null;

        switch(analyticsParam.getTypeParam().getValue()) {
            case HITS:
                analytics = analyticsService.apiKeyHits(
                        key,
                        analyticsParam.getFrom(),
                        analyticsParam.getTo(),
                        analyticsParam.getInterval());
                break;
            case HITS_BY_LATENCY:
                analytics = analyticsService.apiKeyHitsByLatency(
                        key,
                        analyticsParam.getFrom(),
                        analyticsParam.getTo(),
                        analyticsParam.getInterval());
                break;
            case HITS_BY_STATUS:
                analytics = analyticsService.apiKeyHitsByStatus(
                        key,
                        analyticsParam.getFrom(),
                        analyticsParam.getTo(),
                        analyticsParam.getInterval());
                break;
        }

        return Response.ok(analytics).build();
    }
}
