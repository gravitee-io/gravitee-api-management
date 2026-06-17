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
package io.gravitee.gamma.rest.infra.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Adds {@code type=GROUP|LEAF} discriminators to analytics bucket nodes so Gamma responses match
 * the management v2 wire contract expected by dashboard widgets.
 *
 * @author GraviteeSource Team
 */
final class AnalyticsBucketTypeEnricher {

    private static final String TYPE = "type";
    private static final String BUCKETS = "buckets";
    private static final String METRICS = "metrics";
    private static final String GROUP = "GROUP";
    private static final String LEAF = "LEAF";

    private AnalyticsBucketTypeEnricher() {}

    static void enrichFacetsResponse(ObjectNode root) {
        enrichMetricBuckets(root, AnalyticsBucketTypeEnricher::enrichFacetBucket);
    }

    static void enrichTimeSeriesResponse(ObjectNode root) {
        enrichMetricBuckets(root, AnalyticsBucketTypeEnricher::enrichTimeSeriesBucket);
    }

    private static void enrichMetricBuckets(ObjectNode root, BucketEnricher enricher) {
        var metrics = root.get(METRICS);
        if (metrics == null || !metrics.isArray()) {
            return;
        }
        metrics.forEach(metric -> enrichBucketArray(metric.get(BUCKETS), enricher));
    }

    private static void enrichBucketArray(JsonNode buckets, BucketEnricher enricher) {
        if (buckets == null || !buckets.isArray()) {
            return;
        }
        buckets.forEach(bucket -> enricher.enrich(bucket));
    }

    private static void enrichFacetBucket(JsonNode bucket) {
        if (!(bucket instanceof ObjectNode object)) {
            return;
        }
        if (hasNestedBuckets(object)) {
            object.put(TYPE, GROUP);
            enrichBucketArray(object.get(BUCKETS), AnalyticsBucketTypeEnricher::enrichFacetBucket);
            return;
        }
        object.put(TYPE, LEAF);
    }

    private static void enrichTimeSeriesBucket(JsonNode bucket) {
        if (!(bucket instanceof ObjectNode object)) {
            return;
        }
        if (hasNestedBuckets(object)) {
            object.put(TYPE, GROUP);
            enrichBucketArray(object.get(BUCKETS), AnalyticsBucketTypeEnricher::enrichFacetBucket);
            return;
        }
        object.put(TYPE, LEAF);
    }

    private static boolean hasNestedBuckets(ObjectNode bucket) {
        var nested = bucket.get(BUCKETS);
        return nested != null && nested.isArray() && !nested.isEmpty();
    }

    @FunctionalInterface
    private interface BucketEnricher {
        void enrich(JsonNode bucket);
    }
}
