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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.infra.domain_service.analytics_engine.mapper.ApiMapper;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.analytics.engine.api.query.HttpStatusCodeGroups;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
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

    private static final Map<String, String> STATUS_CODE_GROUP_FROM_ES_KEY = HttpStatusCodeGroups.esBucketKeyToGroupLabel();

    private static final Map<String, String> HTTP_METHOD_BY_CODE = buildHttpMethodCodeMap();

    private static final Map<String, String> HTTP_STATUS_LABELS = Map.ofEntries(
        Map.entry("100", "100 Continue"),
        Map.entry("101", "101 Switching Protocols"),
        Map.entry("200", "200 OK"),
        Map.entry("201", "201 Created"),
        Map.entry("202", "202 Accepted"),
        Map.entry("204", "204 No Content"),
        Map.entry("301", "301 Moved Permanently"),
        Map.entry("302", "302 Found"),
        Map.entry("304", "304 Not Modified"),
        Map.entry("307", "307 Temporary Redirect"),
        Map.entry("308", "308 Permanent Redirect"),
        Map.entry("400", "400 Bad Request"),
        Map.entry("401", "401 Unauthorized"),
        Map.entry("403", "403 Forbidden"),
        Map.entry("404", "404 Not Found"),
        Map.entry("405", "405 Method Not Allowed"),
        Map.entry("408", "408 Request Timeout"),
        Map.entry("409", "409 Conflict"),
        Map.entry("413", "413 Payload Too Large"),
        Map.entry("415", "415 Unsupported Media Type"),
        Map.entry("422", "422 Unprocessable Entity"),
        Map.entry("429", "429 Too Many Requests"),
        Map.entry("500", "500 Internal Server Error"),
        Map.entry("501", "501 Not Implemented"),
        Map.entry("502", "502 Bad Gateway"),
        Map.entry("503", "503 Service Unavailable"),
        Map.entry("504", "504 Gateway Timeout")
    );

    private static Map<String, String> buildHttpMethodCodeMap() {
        var methods = HttpMethod.values();
        var map = new java.util.HashMap<String, String>(methods.length);
        for (var m : methods) {
            map.put(String.valueOf(m.code()), m.name());
        }
        return Map.copyOf(map);
    }

    private final ApiRepository apiRepository;
    private final ApplicationService applicationSearchService;

    @Override
    public FacetsResponse mapBucketNames(AnalyticsQueryContext context, List<FacetSpec.Name> facets, FacetsResponse response) {
        var buckets = response
            .metrics()
            .stream()
            .flatMap(r -> r.buckets().stream())
            .toList();

        var updatedContext = populateNames(context, facets, buckets);

        var mappedMetrics = response
            .metrics()
            .stream()
            .map(metricsFacetResponse -> mapFacetMetrics(updatedContext, facets, metricsFacetResponse))
            .toList();

        return new FacetsResponse(mappedMetrics);
    }

    MetricFacetsResponse mapFacetMetrics(AnalyticsQueryContext context, List<FacetSpec.Name> facets, MetricFacetsResponse metric) {
        var mappedBuckets = this.mapFacetBuckets(context, facets, metric.buckets());
        return new MetricFacetsResponse(metric.metric(), metric.unit(), mappedBuckets);
    }

    List<FacetBucketResponse> mapFacetBuckets(
        AnalyticsQueryContext context,
        List<FacetSpec.Name> facets,
        List<FacetBucketResponse> buckets
    ) {
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

    FacetBucketResponse mapFacetBucket(AnalyticsQueryContext context, List<FacetSpec.Name> facets, FacetBucketResponse bucket) {
        var bucketName = switch (facets.getFirst()) {
            case FacetSpec.Name.HTTP_STATUS_CODE_GROUP -> STATUS_CODE_GROUP_FROM_ES_KEY.getOrDefault(bucket.key(), bucket.key());
            case FacetSpec.Name.HTTP_METHOD -> HTTP_METHOD_BY_CODE.getOrDefault(bucket.key(), bucket.key());
            case FacetSpec.Name.HTTP_STATUS -> HTTP_STATUS_LABELS.getOrDefault(bucket.key(), bucket.key());
            case FacetSpec.Name.API -> {
                var name = context.apiNamesById().get(bucket.key());
                yield name != null ? name : bucket.key();
            }
            case FacetSpec.Name.APPLICATION -> {
                var name = context.applicationNamesById().get(bucket.key());
                yield name != null ? name : bucket.key();
            }
            default -> bucket.key();
        };

        var mappedBuckets = this.mapFacetBuckets(context, facets.subList(1, facets.size()), bucket.buckets());

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

        var updatedContext = populateNames(context, facets, buckets);

        var mappedMetrics = response
            .metrics()
            .stream()
            .map(metricsFacetResponse -> mapTimeSeriesMetrics(updatedContext, facets, metricsFacetResponse))
            .toList();

        return new TimeSeriesResponse(mappedMetrics);
    }

    private TimeSeriesMetricResponse mapTimeSeriesMetrics(
        AnalyticsQueryContext context,
        List<FacetSpec.Name> facets,
        TimeSeriesMetricResponse metric
    ) {
        var mappedBuckets = mapTimeSeriesBuckets(context, facets, metric.buckets());
        return new TimeSeriesMetricResponse(metric.name(), metric.unit(), mappedBuckets);
    }

    private List<TimeSeriesBucketResponse> mapTimeSeriesBuckets(
        AnalyticsQueryContext context,
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
        AnalyticsQueryContext context,
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

    private AnalyticsQueryContext populateNames(
        AnalyticsQueryContext context,
        List<FacetSpec.Name> facets,
        List<FacetBucketResponse> buckets
    ) {
        var updatedContext = context;

        if (updatedContext.apiNamesById().isEmpty()) {
            var apiIds = getIdsFromBuckets(FacetSpec.Name.API, facets, buckets);
            updatedContext = updatedContext.withApiNamesById(loadApiNames(apiIds));
        }

        if (updatedContext.applicationNamesById().isEmpty()) {
            var applicationIds = getIdsFromBuckets(FacetSpec.Name.APPLICATION, facets, buckets);
            updatedContext = updatedContext.withApplicationNamesById(loadApplicationNames(applicationIds));
        }

        return updatedContext;
    }

    Map<String, String> loadApiNames(Set<String> apiIds) {
        if (apiIds.isEmpty()) {
            return Map.of();
        }

        var criteria = new ApiCriteria.Builder().ids(apiIds).build();
        var apis = ApiMapper.INSTANCE.map(apiRepository.search(criteria, ApiFieldFilter.defaultFields()));

        return apis.stream().collect(Collectors.toMap(Api::getId, Api::getName));
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
