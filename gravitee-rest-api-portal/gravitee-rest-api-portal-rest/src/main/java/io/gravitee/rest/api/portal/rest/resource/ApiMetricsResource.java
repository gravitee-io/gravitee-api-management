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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.healthcheck.query.availability.AvailabilityQuery.Field;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.analytics.query.StatsAnalytics;
import io.gravitee.rest.api.model.analytics.query.StatsQuery;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.model.ApiMetrics;
import io.gravitee.rest.api.service.AnalyticsService;
import io.gravitee.rest.api.service.HealthCheckService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiMetricsResource extends AbstractResource {

    @Inject
    private SubscriptionService subscriptionService;
    @Inject
    private AnalyticsService analyticsService;
    @Inject
    private HealthCheckService healthCheckService;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getApiMetricsByApiId(@Context Request request, @PathParam("apiId") String apiId) {
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(getAuthenticatedUserOrNull());
        if (userApis.stream().anyMatch(a -> a.getId().equals(apiId))) {
            Number healthRatio = getHealthRatio(apiId);
            Number nbHits = getNbHits(apiId);
            Number subscribers = getApiNbSubscribers(apiId);

            ApiMetrics metrics = new ApiMetrics();
            metrics.setHealth(healthRatio);
            metrics.setHits(nbHits);
            metrics.setSubscribers(subscribers);

            return Response.ok(metrics).build();
        }
        throw new ApiNotFoundException(apiId);
    }

    private Number getHealthRatio(String apiId) {
        io.gravitee.rest.api.model.healthcheck.ApiMetrics<?> apiAvailability = healthCheckService.getAvailability(apiId,
                Field.ENDPOINT.name());
        if (apiAvailability != null) {
            Map<String, Double> globalAvailability = apiAvailability.getGlobal();
            if (globalAvailability != null) {
                try {
                    return BigDecimal.valueOf(globalAvailability.get("1w")).divide(BigDecimal.valueOf(100), 4,
                            RoundingMode.DOWN);
                } catch (NumberFormatException nfe) {
                    return null;
                }
            }
        }
        return null;
    }

    private Number getNbHits(String apiId) {
        StatsQuery query = new StatsQuery();
        Instant now = Instant.now();
        query.setFrom(now.minus(7, ChronoUnit.DAYS).toEpochMilli());
        query.setTo(now.toEpochMilli());
        query.setInterval(43200000);
        query.setRootField("api");
        query.setRootIdentifier(apiId);
        query.setField("response-time");

        try {
            final StatsAnalytics analytics = analyticsService.execute(query);
            if (analytics != null) {
                return analytics.getCount();
            }
        } catch (final Exception e) {
            // do nothing as the analytics errors should not break the portal
        }
        return null;
    }

    private Number getApiNbSubscribers(String apiId) {
        // find all subscribed apis
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApi(apiId);
        subscriptionQuery.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));

        // group by apis
        Collection<SubscriptionEntity> searchResult = subscriptionService.search(subscriptionQuery);
        if (searchResult != null) {
            return searchResult.stream()
                    .collect(Collectors.groupingBy(SubscriptionEntity::getApi, Collectors.counting())).get(apiId);
        }
        return null;
    }
}
