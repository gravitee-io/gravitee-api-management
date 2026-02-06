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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import io.gravitee.apim.core.analytics_engine.domain_service.BucketNamesPostProcessor;
import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class BucketNamesPostProcessorImpl implements BucketNamesPostProcessor {

    private static final String UNKNOWN_APPLICATION = "Unknown";

    private static final Map<String, String> STATUS_CODE_GROUP_FROM_ES_KEY = Map.of(
        "100-199",
        "1XX",
        "200-299",
        "2XX",
        "300-399",
        "3XX",
        "400-499",
        "4XX",
        "500-599",
        "5XX"
    );

    private final ApplicationService applicationSearchService;

    @Override
    public FacetsResponse mapBucketNames(UserContext context, List<FacetSpec.Name> facets, FacetsResponse response) {
        var buckets = response
            .metrics()
            .stream()
            .flatMap(r -> r.buckets().stream())
            .toList();

        var applications = populateApplicationNames(facets, buckets);
        var updatedContext = context.withApplicationNameById(applications);

        var mappedMetrics = response
            .metrics()
            .stream()
            .map(metricsFacetResponse -> mapFacetMetrics(updatedContext, facets, metricsFacetResponse))
            .toList();

        return new FacetsResponse(mappedMetrics);
    }

    MetricFacetsResponse mapFacetMetrics(UserContext context, List<FacetSpec.Name> facets, MetricFacetsResponse metric) {
        var mappedBuckets = this.mapFacetBuckets(context, facets, metric.buckets());
        return new MetricFacetsResponse(metric.metric(), mappedBuckets);
    }

    List<FacetBucketResponse> mapFacetBuckets(UserContext context, List<FacetSpec.Name> facets, List<FacetBucketResponse> buckets) {
        if (facets.isEmpty()) {
            return buckets;
        }

        return buckets
            .stream()
            .map(bucket -> mapFacetBucket(context, facets, bucket))
            .toList();
    }

    Map<String, String> loadApplicationNames(Set<String> applicationIds) {
        if (applicationIds.isEmpty()) {
            return Map.of();
        }

        var applications = getApplicationNamesByIds(applicationIds);
        applications.put("1", UNKNOWN_APPLICATION);

        return applications;
    }

    Map<String, String> getApplicationNamesByIds(Set<String> applicationIds) {
        var query = new ApplicationQuery();
        query.setIds(applicationIds);

        var content = applicationSearchService.search(GraviteeContext.getExecutionContext(), query, null, null).getContent();

        return content.stream().collect(Collectors.toMap(ApplicationListItem::getId, ApplicationListItem::getName));
    }

    FacetBucketResponse mapFacetBucket(UserContext context, List<FacetSpec.Name> facets, FacetBucketResponse bucket) {
        var bucketName = switch (facets.getFirst()) {
            case FacetSpec.Name.HTTP_STATUS_CODE_GROUP -> STATUS_CODE_GROUP_FROM_ES_KEY.getOrDefault(bucket.key(), bucket.key());
            case FacetSpec.Name.API -> context
                .apiNameById()
                .map(apis -> apis.get(bucket.key()))
                .orElse(bucket.key());
            case FacetSpec.Name.APPLICATION -> context
                .applicationNameById()
                .map(applications -> applications.get(bucket.key()))
                .orElse(bucket.key());
            default -> bucket.key();
        };

        var mappedBuckets = this.mapFacetBuckets(context, facets.subList(1, facets.size()), bucket.buckets());

        return new FacetBucketResponse(bucket.key(), bucketName, mappedBuckets, bucket.measures());
    }

    @Override
    public TimeSeriesResponse mapBucketNames(UserContext context, List<FacetSpec.Name> facets, TimeSeriesResponse response) {
        var buckets = response
            .metrics()
            .stream()
            .filter(timeSeriesMetricResponse -> timeSeriesMetricResponse.buckets() != null)
            .flatMap(timeSeriesMetricResponse -> timeSeriesMetricResponse.buckets().stream())
            .filter(timeSeriesBucketResponse -> timeSeriesBucketResponse.buckets() != null)
            .flatMap(timeSeriesBucketResponse -> timeSeriesBucketResponse.buckets().stream())
            .toList();

        var applications = populateApplicationNames(facets, buckets);
        var updatedContext = context.withApplicationNameById(applications);

        var mappedMetrics = response
            .metrics()
            .stream()
            .map(metricsFacetResponse -> mapTimeSeriesMetrics(updatedContext, facets, metricsFacetResponse))
            .toList();

        return new TimeSeriesResponse(mappedMetrics);
    }

    private TimeSeriesMetricResponse mapTimeSeriesMetrics(
        UserContext context,
        List<FacetSpec.Name> facets,
        TimeSeriesMetricResponse metric
    ) {
        var mappedBuckets = mapTimeSeriesBuckets(context, facets, metric.buckets());
        return new TimeSeriesMetricResponse(metric.name(), mappedBuckets);
    }

    private List<TimeSeriesBucketResponse> mapTimeSeriesBuckets(
        UserContext context,
        List<FacetSpec.Name> filters,
        List<TimeSeriesBucketResponse> buckets
    ) {
        if (filters.isEmpty()) {
            return buckets;
        }

        return buckets
            .stream()
            .map(bucket -> mapTimeSeriesBucket(context, filters, bucket))
            .toList();
    }

    private TimeSeriesBucketResponse mapTimeSeriesBucket(
        UserContext context,
        List<FacetSpec.Name> facets,
        TimeSeriesBucketResponse bucket
    ) {
        var updatedBuckets = bucket
            .buckets()
            .stream()
            .map(b -> mapFacetBucket(context, facets, b))
            .toList();
        return new TimeSeriesBucketResponse(bucket.key(), bucket.name(), bucket.timestamp(), updatedBuckets, bucket.measures());
    }

    private Map<String, String> populateApplicationNames(List<FacetSpec.Name> facets, List<FacetBucketResponse> buckets) {
        var applicationIds = getApplicationIdsFromBuckets(facets, buckets);
        return loadApplicationNames(applicationIds);
    }

    private Set<String> getApplicationIdsFromBuckets(List<FacetSpec.Name> facets, List<FacetBucketResponse> buckets) {
        if (facets.isEmpty()) {
            return Set.of();
        }

        if (facets.getFirst() == FacetSpec.Name.APPLICATION) {
            return buckets.stream().map(FacetBucketResponse::key).collect(Collectors.toSet());
        }

        return getApplicationIdsFromBuckets(
            facets.subList(1, facets.size()),
            buckets
                .stream()
                .filter(b -> b.buckets() != null)
                .flatMap(b -> b.buckets().stream())
                .toList()
        );
    }
}
