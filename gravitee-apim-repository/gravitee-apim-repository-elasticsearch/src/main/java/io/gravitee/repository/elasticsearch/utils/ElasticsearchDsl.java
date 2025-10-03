package io.gravitee.repository.elasticsearch.utils;

public final class ElasticsearchDsl {

    private ElasticsearchDsl() {}

    // Top-level keys and shared structural fields
    public static final class Keys {

        public static final String QUERY = "query";
        public static final String BOOL = "bool";
        public static final String FILTER = "filter";
        public static final String AGGS = "aggs";
        public static final String SIZE = "size";
        public static final String TRACK_TOTAL_HITS = "track_total_hits";
        public static final String HITS = "hits";
        public static final String SOURCE = "_source";
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

        private Query() {}
    }

    // Aggregation keywords (only unique to aggs)
    public static final class Aggs {

        public static final String COMPOSITE = "composite";
        public static final String SOURCES = "sources";
        public static final String TOP_HITS = "top_hits";
        public static final String TOP_METRICS = "top_metrics";
        public static final String METRICS = "metrics";
        public static final String SORT = "sort";
        public static final String DATE_HISTOGRAM = "date_histogram";
        public static final String FIXED_INTERVAL = "fixed_interval";
        public static final String MIN_DOC_COUNT = "min_doc_count";
        public static final String EXTENDED_BOUNDS = "extended_bounds";
        public static final String MIN = "min";
        public static final String MAX = "max";

        private Aggs() {}
    }

    // Sort keywords
    public static final class Sort {

        public static final String ORDER = "order";
        public static final String ASC = "asc";
        public static final String DESC = "desc";

        private Sort() {}
    }

    // Stable application names and tokens (prefixes/suffixes chosen by us)
    public static final class Names {

        public static final String LATEST_BUCKET_PREFIX = "latest_";
        public static final String DELTA_BUCKET_SUFFIX = "_delta";
        public static final String BY_DIMENSIONS = "by_dimensions";
        public static final String PER_INTERVAL = "per_interval";
        public static final String START_BUCKET_PREFIX = "start_";
        public static final String END_BUCKET_PREFIX = "end_";
        public static final String MILLISECONDS = "ms";
        public static final String TOP = "top";

        private Names() {}
    }
}
