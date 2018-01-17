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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.PlanEntity;
import io.gravitee.management.model.TenantEntity;
import io.gravitee.management.model.analytics.*;
import io.gravitee.management.model.analytics.query.CountQuery;
import io.gravitee.management.model.analytics.query.DateHistogramQuery;
import io.gravitee.management.model.analytics.query.GroupByQuery;
import io.gravitee.management.service.*;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.*;
import io.gravitee.repository.analytics.query.count.CountResponse;
import io.gravitee.repository.analytics.query.groupby.GroupByQueryBuilder;
import io.gravitee.repository.analytics.query.groupby.GroupByResponse;
import io.gravitee.repository.analytics.query.response.histogram.Data;
import io.gravitee.repository.analytics.query.response.histogram.DateHistogramResponse;
import io.gravitee.repository.management.model.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AnalyticsServiceImpl implements AnalyticsService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private static final String APPLICATION_KEYLESS = "1";

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
            throw new TechnicalManagementException("Unable to calculate analytics", ae);
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
            throw new TechnicalManagementException("Unable to calculate analytics", ae);
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
                GroupByQuery.Order order = query.getOrder();
                queryBuilder.sort(SortBuilder.on(
                        order.getField(),
                        order.isOrder() ? Order.ASC : Order.DESC,
                        (order.getType() == null) ? SortType.AVG : SortType.valueOf(order.getType().toUpperCase())));
            }

            GroupByResponse response = analyticsRepository.query(queryBuilder.build());
            return convert(response);
        } catch (AnalyticsException ae) {
            logger.error("Unable to calculate analytics: ", ae);
            throw new TechnicalManagementException("Unable to calculate analytics", ae);
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

        if (analyticsBucket.getField().equals("application")) {
            // Prepare metadata
            Map<String, Map<String, String>> metadata = new HashMap<>();
            bucket.data().keySet().stream().forEach(app -> {
                metadata.put(app, getApplicationMetadata(app));
            });

            analyticsBucket.setMetadata(metadata);
        } else if (analyticsBucket.getField().equals("api")) {
            // Prepare metadata
            Map<String, Map<String, String>> metadata = new HashMap<>();
            bucket.data().keySet().stream().forEach(api -> {
                metadata.put(api, getAPIMetadata(api));
            });

            analyticsBucket.setMetadata(metadata);
        } else if (analyticsBucket.getField().equals("tenant")) {
            // Prepare metadata
            Map<String, Map<String, String>> metadata = new HashMap<>();
            bucket.data().keySet().stream().forEach(tenant -> {
                metadata.put(tenant, getTenantMetadata(tenant));
            });

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

    private HitsAnalytics convert(CountResponse countResponse) {
        HitsAnalytics hitsAnalytics = new HitsAnalytics();
//        hitsAnalytics.setName(countResponse.getName());
        hitsAnalytics.setHits(countResponse.getCount());

        return  hitsAnalytics;
    }

    private TopHitsAnalytics convert(GroupByResponse groupByResponse) {
        TopHitsAnalytics topHitsAnalytics = new TopHitsAnalytics();

        // Set results
        topHitsAnalytics.setValues(
            groupByResponse.values()
                    .stream()
                    .collect(Collectors.toMap(
                            GroupByResponse.Bucket::name, GroupByResponse.Bucket::value,
                            (v1,v2) ->{ throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));},
                            LinkedHashMap::new)));

        String fieldName = groupByResponse.getField();

        if (fieldName != null && !fieldName.isEmpty()) {

            // Prepare metadata
            Map<String, Map<String, String>> metadata = new HashMap<>();
            if (topHitsAnalytics.getValues() != null) {
                for (String key : topHitsAnalytics.getValues().keySet()) {
                    switch(fieldName) {
                        case "api": metadata.put(key, getAPIMetadata(key)); break;
                        case "application": metadata.put(key, getApplicationMetadata(key)); break;
                        case "plan": metadata.put(key, getPlanMetadata(key)); break;
                        case "tenant": metadata.put(key, getTenantMetadata(key)); break;
                        case "geoip.country_iso_code": metadata.put(key, getCountryName(key)); break;
                        default:
                            metadata.put(key, getGenericMetadata(key)); break;

                    }
                }
            }

            topHitsAnalytics.setMetadata(metadata);
        }

        return  topHitsAnalytics;
    }

    private Map<String, String> getAPIMetadata(String api) {
        Map<String, String> metadata = new HashMap<>();

        try {
            ApiEntity apiEntity = apiService.findById(api);
            metadata.put("name", apiEntity.getName());
            metadata.put("version", apiEntity.getVersion());
        } catch (ApiNotFoundException anfe) {
            metadata.put("name", "Deleted API");
            metadata.put("deleted", "true");
        }

        return metadata;
    }

    private Map<String, String> getApplicationMetadata(String application) {
        Map<String, String> metadata = new HashMap<>();

        try {
            ApplicationEntity applicationEntity = applicationService.findById(application);
            metadata.put("name", applicationEntity.getName());
            if (ApplicationStatus.ARCHIVED.toString().equals(applicationEntity.getStatus())) {
                metadata.put("deleted", "true");
            }
        } catch (ApplicationNotFoundException anfe) {
            metadata.put("deleted", "true");
            if (application.equals(APPLICATION_KEYLESS)) {
                metadata.put("name", "Unknown application (keyless)");
            } else {
                metadata.put("name", "Deleted application");
            }
        }

        return metadata;
    }

    private Map<String, String> getPlanMetadata(String plan) {
        Map<String, String> metadata = new HashMap<>();

        try {
            PlanEntity planEntity = planService.findById(plan);
            metadata.put("name", planEntity.getName());
        } catch (PlanNotFoundException anfe) {
            metadata.put("deleted", "true");
        }

        return metadata;
    }

    private Map<String, String> getTenantMetadata(String tenant) {
        Map<String, String> metadata = new HashMap<>();

        try {
            TenantEntity tenantEntity = tenantService.findById(tenant);
            metadata.put("name", tenantEntity.getName());
        } catch (TenantNotFoundException tnfe) {
            metadata.put("deleted", "true");
        }

        return metadata;
    }

    private Map<String, String> getCountryName(String country_iso) {
        Map<String, String> metadata = new HashMap<>();

        metadata.put("name", (new Locale("", country_iso)).getDisplayCountry(Locale.UK));

        return metadata;
    }

    private Map<String, String> getGenericMetadata(String value) {
        Map<String, String> metadata = new HashMap<>();

        metadata.put("name", value);

        return metadata;
    }

}
