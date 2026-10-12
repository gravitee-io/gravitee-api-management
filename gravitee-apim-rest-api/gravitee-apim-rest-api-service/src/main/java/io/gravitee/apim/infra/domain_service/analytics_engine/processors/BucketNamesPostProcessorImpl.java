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
import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.analytics.engine.api.query.HttpStatusCodeGroups;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class BucketNamesPostProcessorImpl implements BucketNamesPostProcessor {

    private static final Map<String, String> STATUS_CODE_GROUP_FROM_ES_KEY = HttpStatusCodeGroups.esBucketKeyToFriendlyGroupLabel();

    private static final Map<String, String> HTTP_METHOD_BY_CODE = buildHttpMethodCodeMap();

    private static Map<String, String> buildHttpMethodCodeMap() {
        var methods = HttpMethod.values();
        var map = new java.util.HashMap<String, String>(methods.length);
        for (var m : methods) {
            map.put(String.valueOf(m.code()), m.name());
        }
        return Map.copyOf(map);
    }

    /**
     * Facets whose bucket keys are entity ids, named through the same resolver as the filter labels so a chart and a
     * filter chip always show the same name for the same id.
     */
    private static final Map<FacetSpec.Name, FilterSpec.Name> ID_FACETS = Map.of(
        FacetSpec.Name.API,
        FilterSpec.Name.API,
        FacetSpec.Name.APPLICATION,
        FilterSpec.Name.APPLICATION,
        FacetSpec.Name.PLAN,
        FilterSpec.Name.PLAN,
        FacetSpec.Name.API_PRODUCT,
        FilterSpec.Name.API_PRODUCT
    );

    private final FilterValueNameResolver filterValueNameResolver;

    @Override
    public FacetsResponse mapBucketNames(AnalyticsQueryContext context, List<FacetSpec.Name> facets, FacetsResponse response) {
        var buckets = response
            .metrics()
            .stream()
            .flatMap(r -> r.buckets().stream())
            .toList();

        var namesByFacet = loadNames(context, facets, buckets);

        var mappedMetrics = response
            .metrics()
            .stream()
            .map(metricsFacetResponse -> mapFacetMetrics(namesByFacet, facets, metricsFacetResponse))
            .toList();

        return new FacetsResponse(mappedMetrics);
    }

    MetricFacetsResponse mapFacetMetrics(
        Map<FacetSpec.Name, Map<String, String>> namesByFacet,
        List<FacetSpec.Name> facets,
        MetricFacetsResponse metric
    ) {
        var mappedBuckets = this.mapFacetBuckets(namesByFacet, facets, metric.buckets());
        return new MetricFacetsResponse(metric.metric(), metric.unit(), mappedBuckets, metric.ignoredFilters());
    }

    List<FacetBucketResponse> mapFacetBuckets(
        Map<FacetSpec.Name, Map<String, String>> namesByFacet,
        List<FacetSpec.Name> facets,
        List<FacetBucketResponse> buckets
    ) {
        if (facets.isEmpty()) {
            return buckets;
        }

        return buckets
            .stream()
            .map(bucket -> mapFacetBucket(namesByFacet, facets, bucket))
            .toList();
    }

    FacetBucketResponse mapFacetBucket(
        Map<FacetSpec.Name, Map<String, String>> namesByFacet,
        List<FacetSpec.Name> facets,
        FacetBucketResponse bucket
    ) {
        var facet = facets.getFirst();
        var bucketName = switch (facet) {
            case FacetSpec.Name.HTTP_STATUS_CODE_GROUP -> STATUS_CODE_GROUP_FROM_ES_KEY.getOrDefault(bucket.key(), bucket.key());
            case FacetSpec.Name.HTTP_METHOD -> HTTP_METHOD_BY_CODE.getOrDefault(bucket.key(), bucket.key());
            case FacetSpec.Name.HTTP_STATUS -> HttpStatusLabels.labelForCode(bucket.key());
            default -> namesByFacet.getOrDefault(facet, Map.of()).getOrDefault(bucket.key(), bucket.key());
        };

        var mappedBuckets = this.mapFacetBuckets(namesByFacet, facets.subList(1, facets.size()), bucket.buckets());

        return new FacetBucketResponse(bucket.key(), bucketName, mappedBuckets, bucket.measures());
    }

    @Override
    public TimeSeriesResponse mapBucketNames(AnalyticsQueryContext context, List<FacetSpec.Name> facets, TimeSeriesResponse response) {
        var buckets = response
            .metrics()
            .stream()
            .filter(timeSeriesMetricResponse -> timeSeriesMetricResponse.buckets() != null)
            .flatMap(timeSeriesMetricResponse -> timeSeriesMetricResponse.buckets().stream())
            .filter(timeSeriesBucketResponse -> timeSeriesBucketResponse.buckets() != null)
            .flatMap(timeSeriesBucketResponse -> timeSeriesBucketResponse.buckets().stream())
            .toList();

        var namesByFacet = loadNames(context, facets, buckets);

        var mappedMetrics = response
            .metrics()
            .stream()
            .map(metricsFacetResponse -> mapTimeSeriesMetrics(namesByFacet, facets, metricsFacetResponse))
            .toList();

        return new TimeSeriesResponse(mappedMetrics);
    }

    private TimeSeriesMetricResponse mapTimeSeriesMetrics(
        Map<FacetSpec.Name, Map<String, String>> namesByFacet,
        List<FacetSpec.Name> facets,
        TimeSeriesMetricResponse metric
    ) {
        var mappedBuckets = mapTimeSeriesBuckets(namesByFacet, facets, metric.buckets());
        return new TimeSeriesMetricResponse(metric.name(), metric.unit(), mappedBuckets, metric.ignoredFilters());
    }

    private List<TimeSeriesBucketResponse> mapTimeSeriesBuckets(
        Map<FacetSpec.Name, Map<String, String>> namesByFacet,
        List<FacetSpec.Name> filters,
        List<TimeSeriesBucketResponse> buckets
    ) {
        if (filters.isEmpty()) {
            return buckets;
        }

        return buckets
            .stream()
            .map(bucket -> mapTimeSeriesBucket(namesByFacet, filters, bucket))
            .toList();
    }

    private TimeSeriesBucketResponse mapTimeSeriesBucket(
        Map<FacetSpec.Name, Map<String, String>> namesByFacet,
        List<FacetSpec.Name> facets,
        TimeSeriesBucketResponse bucket
    ) {
        var updatedBuckets = bucket
            .buckets()
            .stream()
            .map(b -> mapFacetBucket(namesByFacet, facets, b))
            .toList();
        return new TimeSeriesBucketResponse(bucket.key(), bucket.name(), bucket.timestamp(), updatedBuckets, bucket.measures());
    }

    /**
     * Loads, once for the whole bucket tree, the names of every id-keyed facet of the request. Names already carried by
     * the context are reused rather than reloaded.
     */
    private Map<FacetSpec.Name, Map<String, String>> loadNames(
        AnalyticsQueryContext context,
        List<FacetSpec.Name> facets,
        List<FacetBucketResponse> buckets
    ) {
        var namesByFacet = new EnumMap<FacetSpec.Name, Map<String, String>>(FacetSpec.Name.class);

        ID_FACETS.forEach((facet, filter) -> {
            var ids = getIdsFromBuckets(facet, facets, buckets);
            if (!ids.isEmpty()) {
                var preloaded = preloadedNames(context, facet);
                namesByFacet.put(facet, preloaded.isEmpty() ? resolveNames(context, filter, ids) : preloaded);
            }
        });

        return namesByFacet;
    }

    private static Map<String, String> preloadedNames(AnalyticsQueryContext context, FacetSpec.Name facet) {
        return switch (facet) {
            case FacetSpec.Name.API -> context.apiNamesById();
            case FacetSpec.Name.APPLICATION -> context.applicationNamesById();
            default -> Map.of();
        };
    }

    /**
     * Names only enrich the response: a lookup that fails leaves the ids as names instead of failing the whole
     * analytics request.
     */
    private Map<String, String> resolveNames(AnalyticsQueryContext context, FilterSpec.Name filter, Set<String> ids) {
        try {
            return filterValueNameResolver.resolveNames(environmentIdOf(context), filter, List.copyOf(ids));
        } catch (RuntimeException e) {
            log.warn("Unable to resolve {} names for analytics buckets, keeping their ids: {}", filter, e.toString());
            log.debug("{} name resolution failure", filter, e);
            return Map.of();
        }
    }

    private static String environmentIdOf(AnalyticsQueryContext context) {
        var executionContext = context.executionContext();
        return executionContext != null && executionContext.hasEnvironmentId() ? executionContext.getEnvironmentId() : null;
    }

    private Set<String> getIdsFromBuckets(FacetSpec.Name target, List<FacetSpec.Name> facets, List<FacetBucketResponse> buckets) {
        if (facets.isEmpty()) {
            return Set.of();
        }

        if (facets.getFirst() == target) {
            return buckets.stream().map(FacetBucketResponse::key).collect(Collectors.toSet());
        }

        return getIdsFromBuckets(
            target,
            facets.subList(1, facets.size()),
            buckets
                .stream()
                .filter(b -> b.buckets() != null)
                .flatMap(b -> b.buckets().stream())
                .toList()
        );
    }
}
