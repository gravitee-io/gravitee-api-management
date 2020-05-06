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
package io.gravitee.rest.api.service.impl;

import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.*;
import io.gravitee.repository.analytics.query.AggregationType;
import io.gravitee.repository.analytics.query.count.CountResponse;
import io.gravitee.repository.analytics.query.groupby.GroupByQueryBuilder;
import io.gravitee.repository.analytics.query.groupby.GroupByResponse;
import io.gravitee.repository.analytics.query.response.histogram.Data;
import io.gravitee.repository.analytics.query.response.histogram.DateHistogramResponse;
import io.gravitee.repository.analytics.query.stats.StatsResponse;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.TenantEntity;
import io.gravitee.rest.api.model.analytics.*;
import io.gravitee.rest.api.model.analytics.query.*;
import io.gravitee.rest.api.model.analytics.query.DateHistogramQuery;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.exceptions.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AnalyticsServiceImpl implements AnalyticsService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private static final String UNKNOWN_SERVICE = "1";
    private static final String UNKNOWN_SERVICE_MAPPED = "?";

    private static final String METADATA_NAME = "name";
    private static final String METADATA_DELETED = "deleted";
    private static final String METADATA_UNKNOWN = "unknown";
    private static final String METADATA_VERSION = "version";
    private static final String METADATA_UNKNOWN_API_NAME = "Unknown API (not found)";
    private static final String METADATA_UNKNOWN_APPLICATION_NAME = "Unknown application (keyless)";
    private static final String METADATA_DELETED_API_NAME = "Deleted API";
    private static final String METADATA_DELETED_APPLICATION_NAME = "Deleted application";
    private static final String METADATA_DELETED_TENANT_NAME = "Deleted tenant";
    private static final String METADATA_DELETED_PLAN_NAME = "Deleted plan";

    private static final String FIELD_API = "api";
    private static final String FIELD_APPLICATION = "application";
    private static final String FIELD_TENANT = "tenant";
    private static final String FIELD_PLAN = "plan";
    private static final String FIELD_GEOIP_COUNTRY_ISO_CODE = "geoip.country_iso_code";

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private PlanService planService;

    @Autowired
    private TenantService tenantService;

    @Override
    public StatsAnalytics execute(final StatsQuery query) {
        try {
            final StatsResponse response = analyticsRepository.query(
                    QueryBuilders.stats()
                            .query(query.getQuery())
                            .timeRange(
                                    DateRangeBuilder.between(query.getFrom(), query.getTo()),
                                    IntervalBuilder.interval(query.getInterval())
                            )
                            .root(query.getRootField(), query.getRootIdentifier())
                            .field(query.getField())
                            .build());

            return convert(response, query);
        } catch (AnalyticsException ae) {
            logger.error("Unable to calculate analytics: ", ae);
            throw new AnalyticsCalculateException("Unable to calculate analytics");
        }
    }

    @Override
    public HitsAnalytics execute(CountQuery query) {
        try {
            CountResponse response = analyticsRepository.query(
                    QueryBuilders.count()
                            .query(query.getQuery())
                            .timeRange(
                                    DateRangeBuilder.between(query.getFrom(), query.getTo()),
                                    IntervalBuilder.interval(query.getInterval())
                            )
                            .root(query.getRootField(), query.getRootIdentifier())
                            .build());

            return convert(response);
        } catch (AnalyticsException ae) {
            logger.error("Unable to calculate analytics: ", ae);
            throw new AnalyticsCalculateException("Unable to calculate analytics");
        }
    }

    @Override
    public HistogramAnalytics execute(DateHistogramQuery query) {
        try {
            DateHistogramQueryBuilder queryBuilder = QueryBuilders.dateHistogram()
                    .query(query.getQuery())
                    .timeRange(
                            DateRangeBuilder.between(query.getFrom(), query.getTo()),
                            IntervalBuilder.interval(query.getInterval())
                    )
                    .root(query.getRootField(), query.getRootIdentifier());

            if (query.getAggregations() != null) {
                query.getAggregations().stream()
                        .forEach(aggregation ->
                                queryBuilder.aggregation(
                                        AggregationType.valueOf(aggregation.type().name()), aggregation.field()));
            }

            DateHistogramResponse response = analyticsRepository.query(queryBuilder.build());
            return convert(response);
        } catch (AnalyticsException ae) {
            logger.error("Unable to calculate analytics: ", ae);
            throw new AnalyticsCalculateException("Unable to calculate analytics");
        }
    }

    @Override
    public TopHitsAnalytics execute(GroupByQuery query) {
        try {
            GroupByQueryBuilder queryBuilder = QueryBuilders.groupBy()
                    .query(query.getQuery())
                    .timeRange(
                            DateRangeBuilder.between(query.getFrom(), query.getTo()),
                            IntervalBuilder.interval(query.getInterval())
                    )
                    .root(query.getRootField(), query.getRootIdentifier())
                    .field(query.getField());

            if (query.getGroups() != null) {
                query.getGroups().forEach(queryBuilder::range);
            }

            if (query.getOrder() != null) {
                final GroupByQuery.Order order = query.getOrder();
                queryBuilder.sort(SortBuilder.on(
                        order.getField(),
                        order.isOrder() ? Order.ASC : Order.DESC,
                        (order.getType() == null) ? SortType.AVG : SortType.valueOf(order.getType().toUpperCase())));
            }

            GroupByResponse response = analyticsRepository.query(queryBuilder.build());
            return convert(response);
        } catch (AnalyticsException ae) {
            logger.error("Unable to calculate analytics: ", ae);
            throw new AnalyticsCalculateException("Unable to calculate analytics");
        }
    }

    private HistogramAnalytics convert(DateHistogramResponse histogramResponse) {
        final HistogramAnalytics analytics = new HistogramAnalytics();
        final List<Long> timestamps = histogramResponse.timestamps();
        if (timestamps != null && timestamps.size() > 1) {
            final long from = timestamps.get(0);
            final long interval = timestamps.get(1) - from;
            final long to = timestamps.get(timestamps.size() - 1);

            analytics.setTimestamp(new Timestamp(from, to, interval));

            List<Bucket> buckets = new ArrayList<>(histogramResponse.values().size());
            for (io.gravitee.repository.analytics.query.response.histogram.Bucket bucket : histogramResponse.values()) {
                Bucket analyticsBucket = convertBucket(histogramResponse.timestamps(), from, interval, bucket);
                buckets.add(analyticsBucket);
            }
            analytics.setValues(buckets);

        }
        return analytics;
    }

    private Bucket convertBucket(List<Long> timestamps, long from, long interval, io.gravitee.repository.analytics.query.response.histogram.Bucket bucket) {
        Bucket analyticsBucket = new Bucket();
        analyticsBucket.setName(bucket.name());
        analyticsBucket.setField(bucket.field());

        List<Bucket> childBuckets = new ArrayList<>();

        for (io.gravitee.repository.analytics.query.response.histogram.Bucket childBucket : bucket.buckets()) {
            childBuckets.add(convertBucket(timestamps, from, interval, childBucket));
        }

        if (FIELD_APPLICATION.equals(analyticsBucket.getField())) {
            // Prepare metadata
            Map<String, Map<String, String>> metadata = new HashMap<>();
            bucket.data().keySet().forEach(app -> metadata.put(app, getApplicationMetadata(app)));

            analyticsBucket.setMetadata(metadata);
        } else if (FIELD_API.equals(analyticsBucket.getField())) {
            // Prepare metadata
            Map<String, Map<String, String>> metadata = new HashMap<>();
            bucket.data().keySet().forEach(api -> metadata.put(api, getAPIMetadata(api)));

            analyticsBucket.setMetadata(metadata);
        } else if (FIELD_TENANT.equals(analyticsBucket.getField())) {
            // Prepare metadata
            Map<String, Map<String, String>> metadata = new HashMap<>();
            bucket.data().keySet().forEach(tenant -> metadata.put(tenant, getTenantMetadata(tenant)));

            analyticsBucket.setMetadata(metadata);
        }

        for (Map.Entry<String, List<Data>> dataBucket : bucket.data().entrySet()) {
            Bucket analyticsDataBucket = new Bucket();
            analyticsDataBucket.setName(dataBucket.getKey());

            Number [] values = new Number [timestamps.size()];
            for (int i = 0; i <timestamps.size(); i++) {
                values[i] = 0;
            }
            for (Data data : dataBucket.getValue()) {
                values[(int) ((data.timestamp() - from) / interval)] = data.value();
            }

            analyticsDataBucket.setData(values);
            childBuckets.add(analyticsDataBucket);
        }
        analyticsBucket.setBuckets(childBuckets);

        return analyticsBucket;
    }

    private StatsAnalytics convert(final StatsResponse statsResponse, final StatsQuery query) {
        final StatsAnalytics statsAnalytics = new StatsAnalytics();
        statsAnalytics.setAvg(statsResponse.getAvg());
        statsAnalytics.setCount(statsResponse.getCount());
        statsAnalytics.setMax(statsResponse.getMax());
        statsAnalytics.setMin(statsResponse.getMin());
        statsAnalytics.setSum(statsResponse.getSum());
        if (statsResponse.getCount() != null) {
            final long numberOfSeconds = (query.getTo() - query.getFrom()) / 1000;
            statsAnalytics.setRps(statsResponse.getCount() / numberOfSeconds);
            statsAnalytics.setRpm(statsResponse.getCount() / numberOfSeconds * 60);
            statsAnalytics.setRph(statsResponse.getCount() / numberOfSeconds * 3600);
        }
        return statsAnalytics;
    }

    private HitsAnalytics convert(CountResponse countResponse) {
        HitsAnalytics hitsAnalytics = new HitsAnalytics();
        hitsAnalytics.setHits(countResponse.getCount());
        return hitsAnalytics;
    }

    private TopHitsAnalytics convert(GroupByResponse groupByResponse) {
        TopHitsAnalytics topHitsAnalytics = new TopHitsAnalytics();

        // Set results
        topHitsAnalytics.setValues(
            groupByResponse.values()
                    .stream()
                    .collect(Collectors.toMap(
                            // https://stackoverflow.com/questions/5525795/does-javascript-guarantee-object-property-order/5525820#5525820
                            // because javascript does not preserve the order, we have to convert all "1" keys to a non int value
                            bucket -> UNKNOWN_SERVICE.equals(bucket.name()) ? UNKNOWN_SERVICE_MAPPED : bucket.name(),
                            GroupByResponse.Bucket::value,
                            (v1,v2) ->{ throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));},
                            LinkedHashMap::new)));

        String fieldName = groupByResponse.getField();

        if (fieldName != null && !fieldName.isEmpty()) {

            // Prepare metadata
            Map<String, Map<String, String>> metadata = new HashMap<>();
            if (topHitsAnalytics.getValues() != null) {
                int i = 0;
                for (String key : topHitsAnalytics.getValues().keySet()) {
                    switch(fieldName) {
                        case FIELD_API: metadata.put(key, getAPIMetadata(key)); break;
                        case FIELD_APPLICATION: metadata.put(key, getApplicationMetadata(key)); break;
                        case FIELD_PLAN: metadata.put(key, getPlanMetadata(key)); break;
                        case FIELD_TENANT: metadata.put(key, getTenantMetadata(key)); break;
                        case FIELD_GEOIP_COUNTRY_ISO_CODE: metadata.put(key, getCountryName(key)); break;
                        default:
                            metadata.put(key, getGenericMetadata(key)); break;
                    }
                    metadata.get(key).put("order", String.valueOf(i));
                    i++;
                }
            }

            topHitsAnalytics.setMetadata(metadata);
        }

        return topHitsAnalytics;
    }

    private Map<String, String> getAPIMetadata(String api) {
        Map<String, String> metadata = new HashMap<>();

        try {
            if (api.equals(UNKNOWN_SERVICE) || api.equals(UNKNOWN_SERVICE_MAPPED)) {
                metadata.put(METADATA_NAME, METADATA_UNKNOWN_API_NAME);
                metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
            } else {
                ApiEntity apiEntity = apiService.findById(api);
                metadata.put(METADATA_NAME, apiEntity.getName());
                metadata.put(METADATA_VERSION, apiEntity.getVersion());
                if (ApiLifecycleState.ARCHIVED.equals(apiEntity.getLifecycleState())) {
                    metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                }
            }
        } catch (ApiNotFoundException anfe) {
            metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
            metadata.put(METADATA_NAME, METADATA_DELETED_API_NAME);
        }

        return metadata;
    }

    private Map<String, String> getApplicationMetadata(String application) {
        Map<String, String> metadata = new HashMap<>();

        try {
            if (application.equals(UNKNOWN_SERVICE) || application.equals(UNKNOWN_SERVICE_MAPPED)) {
                metadata.put(METADATA_NAME, METADATA_UNKNOWN_APPLICATION_NAME);
                metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
            } else {
                ApplicationEntity applicationEntity = applicationService.findById(application);
                metadata.put(METADATA_NAME, applicationEntity.getName());
                if (ApplicationStatus.ARCHIVED.toString().equals(applicationEntity.getStatus())) {
                    metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                }
            }
        } catch (ApplicationNotFoundException anfe) {
            metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
            metadata.put(METADATA_NAME, METADATA_DELETED_APPLICATION_NAME);
        }

        return metadata;
    }

    private Map<String, String> getPlanMetadata(String plan) {
        Map<String, String> metadata = new HashMap<>();

        try {
            PlanEntity planEntity = planService.findById(plan);
            metadata.put(METADATA_NAME, planEntity.getName());
        } catch (PlanNotFoundException anfe) {
            metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
            metadata.put(METADATA_NAME, METADATA_DELETED_PLAN_NAME);
        }

        return metadata;
    }

    private Map<String, String> getTenantMetadata(String tenant) {
        Map<String, String> metadata = new HashMap<>();

        try {
            TenantEntity tenantEntity = tenantService.findById(tenant);
            metadata.put(METADATA_NAME, tenantEntity.getName());
        } catch (TenantNotFoundException tnfe) {
            metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
            metadata.put(METADATA_NAME, METADATA_DELETED_TENANT_NAME);
        }

        return metadata;
    }

    private Map<String, String> getCountryName(String country_iso) {
        Map<String, String> metadata = new HashMap<>();

        metadata.put(METADATA_NAME, (new Locale("", country_iso)).getDisplayCountry(Locale.UK));

        return metadata;
    }

    private Map<String, String> getGenericMetadata(String value) {
        Map<String, String> metadata = new HashMap<>();

        metadata.put(METADATA_NAME, value);

        return metadata;
    }
}
