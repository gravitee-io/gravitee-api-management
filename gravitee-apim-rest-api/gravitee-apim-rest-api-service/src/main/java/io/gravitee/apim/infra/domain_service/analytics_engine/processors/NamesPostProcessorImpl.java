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

import io.gravitee.apim.core.analytics_engine.domain_service.NamesPostprocessor;
import io.gravitee.apim.core.analytics_engine.model.*;
import java.util.List;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NamesPostProcessorImpl implements NamesPostprocessor {

    @Override
    public FacetsResponse mapNames(MetricsContext context, List<FacetSpec.Name> facets, FacetsResponse response) {
        var mappedMetrics = response
            .metrics()
            .stream()
            .map(metricsFacetResponse -> mapFacetMetrics(context, facets, metricsFacetResponse))
            .toList();

        return new FacetsResponse(mappedMetrics);
    }

    MetricFacetsResponse mapFacetMetrics(MetricsContext context, List<FacetSpec.Name> facets, MetricFacetsResponse metric) {
        var mappedBuckets = this.mapFacetBuckets(context, facets, metric.buckets());
        return new MetricFacetsResponse(metric.metric(), mappedBuckets);
    }

    List<FacetBucketResponse> mapFacetBuckets(MetricsContext context, List<FacetSpec.Name> facets, List<FacetBucketResponse> buckets) {
        if (facets.isEmpty()) {
            return buckets;
        }

        return buckets
            .stream()
            .map(bucket -> mapFacetBucket(context, facets, bucket))
            .toList();
    }

    FacetBucketResponse mapFacetBucket(MetricsContext context, List<FacetSpec.Name> facets, FacetBucketResponse bucket) {
        var bucketName = switch (facets.getFirst()) {
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
    public TimeSeriesResponse mapNames(MetricsContext context, List<FacetSpec.Name> facets, TimeSeriesResponse response) {
        var mappedMetrics = response
            .metrics()
            .stream()
            .map(metricsFacetResponse -> mapTimeSeriesMetrics(context, facets, metricsFacetResponse))
            .toList();

        return new TimeSeriesResponse(mappedMetrics);
    }

    TimeSeriesMetricResponse mapTimeSeriesMetrics(MetricsContext context, List<FacetSpec.Name> facets, TimeSeriesMetricResponse metric) {
        var mappedBuckets = mapTimeSeriesBuckets(context, facets, metric.buckets());
        return new TimeSeriesMetricResponse(metric.name(), mappedBuckets);
    }

    List<TimeSeriesBucketResponse> mapTimeSeriesBuckets(
        MetricsContext context,
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

    TimeSeriesBucketResponse mapTimeSeriesBucket(MetricsContext context, List<FacetSpec.Name> facets, TimeSeriesBucketResponse bucket) {
        var updatedBuckets = bucket
            .buckets()
            .stream()
            .map(b -> mapFacetBucket(context, facets, b))
            .toList();
        return new TimeSeriesBucketResponse(bucket.key(), bucket.name(), bucket.timestamp(), updatedBuckets, bucket.measures());
    }
}
