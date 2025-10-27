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
package io.gravitee.repository.elasticsearch.utils;

public final class ElasticsearchDsl {

    private ElasticsearchDsl() {}

    // Top-level keys and shared structural fields
    public static final class Keys {

        public static final String QUERY = "query";
        public static final String BOOL = "bool";
        public static final String SHOULD = "should";
        public static final String FILTER = "filter";
        public static final String AGGS = "aggs";
        public static final String SIZE = "size";
        public static final String TRACK_TOTAL_HITS = "track_total_hits";
        public static final String TIMESTAMP = "@timestamp";
        public static final String KEY = "key";
        public static final String DOC_COUNT = "doc_count";

        private Keys() {}
    }

    // Shared tokens used in both query and aggs sections
    public static final class Tokens {

        public static final String FIELD = "field";
        public static final String TERMS = "terms";

        private Tokens() {}
    }

    // Query operators (only unique to query)
    public static final class Query {

        public static final String TERM = "term";
        public static final String RANGE = "range";
        public static final String EXISTS = "exists";
        public static final String GTE = "gte";
        public static final String LTE = "lte";
        public static final String LT = "lt";

        private Query() {}
    }

    // Aggregation keywords (only unique to aggs)
    public static final class Aggs {

        public static final String COMPOSITE = "composite";
        public static final String SOURCES = "sources";
        public static final String TOP_METRICS = "top_metrics";
        public static final String METRICS = "metrics";
        public static final String SORT = "sort";
        public static final String DATE_HISTOGRAM = "date_histogram";
        public static final String FIXED_INTERVAL = "fixed_interval";
        public static final String MIN_DOC_COUNT = "min_doc_count";
        public static final String MINIMUM_SHOULD_MATCH = "minimum_should_match";
        public static final String EXTENDED_BOUNDS = "extended_bounds";
        public static final String MIN = "min";
        public static final String MAX = "max";
        public static final String BUCKETS = "buckets";
        public static final String MISSING_BUCKET = "missing_bucket";

        private Aggs() {}
    }

    // Sort keywords
    public static final class Sort {

        public static final String DESC = "desc";

        private Sort() {}
    }

    // Stable application names (prefixes/suffixes chosen by us)
    public static final class Names {

        public static final String LATEST_VALUE_PREFIX = "latest_";
        public static final String BY_DIMENSIONS = "by_dimensions";
        public static final String PER_INTERVAL = "per_interval";
        public static final String START_BUCKET_PREFIX = "start_";
        public static final String END_BUCKET_PREFIX = "end_";
        public static final String MILLISECONDS = "ms";
        public static final String TOP = "top";
        public static final String BEFORE_START = "before_start_time";
        public static final String END_IN_RANGE = "end_in_range";
        // Added for trend naming conventions
        public static final String MAX_PREFIX = "max_";

        private Names() {}
    }
}
