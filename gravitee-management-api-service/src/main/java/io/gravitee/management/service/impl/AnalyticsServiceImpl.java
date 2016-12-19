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

import io.gravitee.common.data.domain.Order;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.analytics.*;
import io.gravitee.management.service.AnalyticsService;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.management.service.exceptions.ApplicationNotFoundException;
import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.response.HealthResponse;
import io.gravitee.repository.analytics.query.response.HitsResponse;
import io.gravitee.repository.analytics.query.response.TopHitsResponse;
import io.gravitee.repository.analytics.query.response.histogram.Data;
import io.gravitee.repository.analytics.query.response.histogram.HistogramResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApplicationService applicationService;

    @Override
    public HistogramAnalytics hitsBy(String query, String key, String field, List<String> aggTypes, long from, long to, long interval) {
        try {
            return convert(analyticsRepository.query(query, key, field, aggTypes, from, to, interval));
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for api hits by {}.", field, ex);
            return null;
        }
    }

    @Override
    public HitsAnalytics globalHits(String query, String key, long from, long to) {
        try {
            return convert(analyticsRepository.query(query, key, from, to));
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for api global hits.", ex);
            return null;
        }
    }

    @Override
    public TopHitsAnalytics topHits(String query, String key, String field, long from, long to, int size) {
        try {
            return convert(analyticsRepository.query(query, key, field, (Order) null, from, to, size));
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for api top hits.", ex);
            return null;
        }
    }

    @Override
    public TopHitsAnalytics topHits(String query, String key, String field, Order order, long from, long to, int size) {
        try {
            return convert(analyticsRepository.query(query, key, field, order, from, to, size));
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for api top hits.", ex);
            return null;
        }
    }

    @Override
    public HealthAnalytics health(String api, long from, long to, long interval) {
        logger.debug("Run health query for API '{}'", api);

        try {
            return convert(analyticsRepository.query(api, interval, from, to));
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for health data.", ex);
            return null;
        }
    }

    private HistogramAnalytics convert(HistogramResponse histogramResponse) {
        final HistogramAnalytics analytics = new HistogramAnalytics();
        final List<Long> timestamps = histogramResponse.timestamps();
        final long from = timestamps.get(0);
        final long interval = timestamps.get(1) - from;
        final long to = timestamps.get(timestamps.size() - 1);

        analytics.setTimestamp(new Timestamp(from, to, interval));

        for(io.gravitee.repository.analytics.query.response.histogram.Bucket bucket : histogramResponse.values()) {
            Bucket analyticsBucket = convertBucket(histogramResponse.timestamps(), from, interval, bucket);
            analytics.getValues().add(analyticsBucket);

            if (analyticsBucket.getName().equals("api-hits-by-application")) {
                // Prepare metadata
                Map<String, Map<String, String>> metadata = new HashMap<>();
                analyticsBucket.getBuckets().stream().map(Bucket::getName).forEach(app -> {
                    metadata.put(app, getApplicationMetadata(app));
                });

                analytics.setMetadata(metadata);
            } else if (analyticsBucket.getName().equals("application-hits-by-api")) {
                // Prepare metadata
                Map<String, Map<String, String>> metadata = new HashMap<>();
                analyticsBucket.getBuckets().stream().map(Bucket::getName).forEach(api -> {
                    metadata.put(api, getAPIMetadata(api));
                });

                analytics.setMetadata(metadata);
            }
        }
        return analytics;
    }

    private Bucket convertBucket(List<Long> timestamps, long from, long interval, io.gravitee.repository.analytics.query.response.histogram.Bucket bucket) {
        Bucket analyticsBucket = new Bucket();
        analyticsBucket.setName(bucket.name());

        for (io.gravitee.repository.analytics.query.response.histogram.Bucket childBucket : bucket.buckets()) {
            analyticsBucket.getBuckets().add(convertBucket(timestamps, from, interval, childBucket));
        }

        for (Map.Entry<String, List<Data>> dataBucket : bucket.data().entrySet()) {
            Bucket analyticsDataBucket = new Bucket();
            analyticsDataBucket.setName(dataBucket.getKey());

            long [] values = new long [timestamps.size()];
            for (Data data : dataBucket.getValue()) {
                values[(int) ((data.timestamp() - from) / interval)] = data.count();
            }

            analyticsDataBucket.setData(values);

            analyticsBucket.getBuckets().add(analyticsDataBucket);
        }

        return analyticsBucket;
    }

    private HealthAnalytics convert(HealthResponse response) {
        HealthAnalytics healthAnalytics = new HealthAnalytics();

        healthAnalytics.setTimestamps(response.timestamps());
        healthAnalytics.setBuckets(response.buckets());

        return healthAnalytics;
    }

    private HitsAnalytics convert(HitsResponse hitsResponse) {
        HitsAnalytics hitsAnalytics = new HitsAnalytics();
        hitsAnalytics.setName(hitsResponse.getName());
        hitsAnalytics.setHits(hitsResponse.getHits());

        return  hitsAnalytics;
    }

    private TopHitsAnalytics convert(TopHitsResponse topHitsResponse) {
        TopHitsAnalytics topHitsAnalytics = new TopHitsAnalytics();
        topHitsAnalytics.setName(topHitsResponse.getName());
        topHitsAnalytics.setValues(topHitsResponse.getValues());

        String queryName = topHitsResponse.getName();
        boolean api = false;
        if (queryName.contains("apis")) {
            api = true;
        } else if (queryName.contains("apps")) {
            api = false;
        }

        // Prepare metadata
        Map<String, Map<String, String>> metadata = new HashMap<>();
        if (topHitsResponse.getValues() != null) {
            for (String key : topHitsResponse.getValues().keySet()) {
                if (api) {
                    metadata.put(key, getAPIMetadata(key));
                } else {
                    metadata.put(key, getApplicationMetadata(key));
                }
            }
        }

        topHitsAnalytics.setMetadata(metadata);

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
        } catch (ApplicationNotFoundException anfe) {
            metadata.put("name", "Deleted application");
            metadata.put("deleted", "true");
        }

        return metadata;
    }
}
