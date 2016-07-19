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

import io.gravitee.management.model.analytics.*;
import io.gravitee.management.service.AnalyticsService;
import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.*;
import io.gravitee.repository.analytics.query.response.HealthResponse;
import io.gravitee.repository.analytics.query.response.HitsResponse;
import io.gravitee.repository.analytics.query.response.TopHitsResponse;
import io.gravitee.repository.analytics.query.response.histogram.Data;
import io.gravitee.repository.analytics.query.response.histogram.HistogramResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @Override
    public HistogramAnalytics apiHitsBy(String query, String key, String field, List<String> aggTypes, long from, long to, long interval) {
        try {
            return convert(analyticsRepository.query(query, key, field, aggTypes, from, to, interval), from, interval);
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for api hits by {}.", field, ex);
            return null;
        }
    }

    @Override
    public HitsAnalytics apiGlobalHits(String query, String key, long from, long to) {
        try {
            return convert(analyticsRepository.query(query, key, from, to));
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for api global hits.", ex);
            return null;
        }
    }

    @Override
    public TopHitsAnalytics apiTopHits(String query, String key, String field, long from, long to, int size) {
        try {
            return convert(analyticsRepository.query(query, key, field, from, to, size));
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for api top hits.", ex);
            return null;
        }
    }

    @Override
    public HistogramAnalytics apiKeyHits(String apiKey, long from, long to, long interval) {
        return apiKeyHits(HitsByApiKeyQuery.Type.HITS, apiKey, from, to, interval);
    }

    @Override
    public HistogramAnalytics apiKeyHitsByStatus(String apiKey, long from, long to, long interval) {
        return apiKeyHits(HitsByApiKeyQuery.Type.HITS_BY_STATUS, apiKey, from, to, interval);
    }

    @Override
    public HistogramAnalytics apiKeyHitsByLatency(String apiKey, long from, long to, long interval) {
        return apiKeyHits(HitsByApiKeyQuery.Type.HITS_BY_LATENCY, apiKey, from, to, interval);
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

    private HistogramAnalytics apiKeyHits(HitsByApiKeyQuery.Type type, String apiKey, long from, long to, long interval) {
        logger.debug("Run analytics query {} for API key '{}'", type, apiKey);

        try {
            return runHistoricalQuery(QueryBuilders.query()
                    .hitsByApiKey(apiKey)
                    .period(DateRangeBuilder.between(from, to))
                    .interval(IntervalBuilder.interval(interval))
                    .type(type)
                    .build(), from, interval);

        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for analytics data.", ex);
            return null;
        }
    }

    private HistogramAnalytics runHistoricalQuery(Query<HistogramResponse> query, long from, long interval) throws Exception {
        return convert(analyticsRepository.query(query), from, interval);
    }

    private HistogramAnalytics convert(HistogramResponse histogramResponse, long from, long interval) {
        HistogramAnalytics analytics = new HistogramAnalytics();

        analytics.setTimestamps(histogramResponse.timestamps());

        for(io.gravitee.repository.analytics.query.response.histogram.Bucket bucket : histogramResponse.values()) {
            Bucket analyticsBucket = convertBucket(analytics.getTimestamps(), from, interval, bucket);
            analytics.getValues().add(analyticsBucket);
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

        return  topHitsAnalytics;
    }
}
