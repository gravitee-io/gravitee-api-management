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

import io.gravitee.definition.model.Endpoint;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.InstanceListItem;
import io.gravitee.management.model.analytics.Analytics;
import io.gravitee.management.model.analytics.HistogramAnalytics;
import io.gravitee.management.model.analytics.Timestamp;
import io.gravitee.management.model.analytics.query.DateHistogramQuery;
import io.gravitee.management.model.analytics.query.LogQuery;
import io.gravitee.management.model.healthcheck.*;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.HealthCheckService;
import io.gravitee.management.service.InstanceService;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.AggregationType;
import io.gravitee.repository.analytics.query.DateRangeBuilder;
import io.gravitee.repository.analytics.query.IntervalBuilder;
import io.gravitee.repository.analytics.query.response.histogram.Data;
import io.gravitee.repository.healthcheck.api.HealthCheckRepository;
import io.gravitee.repository.healthcheck.query.Bucket;
import io.gravitee.repository.healthcheck.query.DateHistogramQueryBuilder;
import io.gravitee.repository.healthcheck.query.FieldBucket;
import io.gravitee.repository.healthcheck.query.QueryBuilders;
import io.gravitee.repository.healthcheck.query.availability.AvailabilityQuery;
import io.gravitee.repository.healthcheck.query.availability.AvailabilityResponse;
import io.gravitee.repository.healthcheck.query.log.ExtendedLog;
import io.gravitee.repository.healthcheck.query.log.LogsResponse;
import io.gravitee.repository.healthcheck.query.response.histogram.DateHistogramResponse;
import io.gravitee.repository.healthcheck.query.responsetime.AverageResponseTimeQuery;
import io.gravitee.repository.healthcheck.query.responsetime.AverageResponseTimeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HealthCheckServiceImpl implements HealthCheckService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    @Autowired
    private HealthCheckRepository healthCheckRepository;

    @Autowired
    private ApiService apiService;

    @Autowired
    private InstanceService instanceService;

    @Override
    public Analytics query(final DateHistogramQuery query) {
        try {
            final DateHistogramQueryBuilder queryBuilder = QueryBuilders.dateHistogram()
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

            return convert(healthCheckRepository.query(queryBuilder.build()));
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

            List<io.gravitee.management.model.analytics.Bucket> buckets = new ArrayList<>(histogramResponse.values().size());
            for (io.gravitee.repository.analytics.query.response.histogram.Bucket bucket : histogramResponse.values()) {
                io.gravitee.management.model.analytics.Bucket analyticsBucket = convertBucket(histogramResponse.timestamps(), from, interval, bucket);
                buckets.add(analyticsBucket);
            }
            analytics.setValues(buckets);

        }
        return analytics;
    }

    private io.gravitee.management.model.analytics.Bucket convertBucket(List<Long> timestamps, long from, long interval, io.gravitee.repository.analytics.query.response.histogram.Bucket bucket) {
        io.gravitee.management.model.analytics.Bucket analyticsBucket = new io.gravitee.management.model.analytics.Bucket();
        analyticsBucket.setName(bucket.name());
        analyticsBucket.setField(bucket.field());

        List<io.gravitee.management.model.analytics.Bucket> childBuckets = new ArrayList<>();

        for (io.gravitee.repository.analytics.query.response.histogram.Bucket childBucket : bucket.buckets()) {
            childBuckets.add(convertBucket(timestamps, from, interval, childBucket));
        }

        for (Map.Entry<String, List<Data>> dataBucket : bucket.data().entrySet()) {
            io.gravitee.management.model.analytics.Bucket analyticsDataBucket = new io.gravitee.management.model.analytics.Bucket();
            analyticsDataBucket.setName(dataBucket.getKey());

            final Number [] values = new Number [timestamps.size()];
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

    @Override
    public ApiMetrics getAvailability(String api, String field) {
        logger.debug("Run health availability query for API '{}'", api);

        try {
            ApiEntity apiEntity = apiService.findById(api);

            AvailabilityResponse response = healthCheckRepository.query(
                    QueryBuilders.availability()
                            .api(api)
                            .field(AvailabilityQuery.Field.valueOf(field))
                            .build());

            return convert(apiEntity, response.getEndpointAvailabilities(), field);
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for health data.", ex);
            return null;
        }
    }

    @Override
    public ApiMetrics getResponseTime(String api, String field) {
        logger.debug("Run health response-time query for API '{}'", api);

        try {
            ApiEntity apiEntity = apiService.findById(api);

            AverageResponseTimeResponse response = healthCheckRepository.query(
                    QueryBuilders.responseTime()
                            .api(api)
                            .field(AverageResponseTimeQuery.Field.valueOf(field))
                            .build());

            return convert(apiEntity, response.getEndpointResponseTimes(), field);
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for health data.", ex);
            return null;
        }
    }

    @Override
    public SearchLogResponse findByApi(String api, LogQuery query, Boolean transition) {
        logger.debug("Run health logs query for API '{}'", api);

        try {
            LogsResponse response = healthCheckRepository.query(
                    QueryBuilders.logs()
                            .api(api)
                            .page(query.getPage())
                            .size(query.getSize())
                            .query(query.getQuery())
                            .transition(transition)
                            .build());

            return convert(response);
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for health data.", ex);
            return null;
        }
    }

    @Override
    public Log findLog(String id) {
        try {
            ExtendedLog log = healthCheckRepository.findById(id);
            return toLog(log);
        } catch (AnalyticsException ae) {
            logger.error("An unexpected error occurs while searching for health data.", ae);
            return null;
        }
    }

    private SearchLogResponse convert(LogsResponse response) {
        SearchLogResponse searchLogResponseResponse = new SearchLogResponse(response.getSize());

        // Transform repository logs
        searchLogResponseResponse.setLogs(response.getLogs().stream()
                .map(this::toLog)
                .collect(Collectors.toList()));

        // Add metadata (only if they are results)
        if (response.getSize() > 0) {
            Map<String, Map<String, String>> metadata = new HashMap<>();

            searchLogResponseResponse.getLogs().forEach(logItem -> {
                String gateway = logItem.getGateway();
                if (gateway != null) {
                    metadata.computeIfAbsent(gateway, this::getGatewayMetadata);
                }
            });

            searchLogResponseResponse.setMetadata(metadata);
        }

        return searchLogResponseResponse;
    }

    private <T extends Number> ApiMetrics<T> convert(ApiEntity api, List<FieldBucket<T>> response, String field) {
        ApiMetrics<T> apiMetrics = new ApiMetrics<>();

        // Set endpoint availability (unknown endpoints are removed)
        Map<String, Map<String, T>> buckets = new HashMap<>();

        response
                .forEach(new Consumer<FieldBucket<T>>() {
                            @Override
                            public void accept(FieldBucket<T> bucket) {
                                Map<String, T> bucketMetrics = bucket.getValues()
                                        .stream()
                                        .collect(Collectors.toMap(Bucket::getKey, Bucket::getValue));

                                buckets.put(bucket.getName(), bucketMetrics);
                            }
                        });
        apiMetrics.setBuckets(buckets);

        if (!apiMetrics.getBuckets().isEmpty()) {
            Map<String, Double> values = new HashMap<>();

            apiMetrics.getBuckets().values().forEach(new Consumer<Map<String, T>>() {
                @Override
                public void accept(Map<String, T> stringTMap) {
                    stringTMap.entrySet().forEach(new Consumer<Map.Entry<String, T>>() {
                        @Override
                        public void accept(Map.Entry<String, T> stringTEntry) {
                            Number value = stringTEntry.getValue();
                            Double total = values.getOrDefault(stringTEntry.getKey(), 0d);
                            total += value.doubleValue();
                            values.put(stringTEntry.getKey(), total);
                        }
                    });
                }
            });

            values.entrySet().forEach(new Consumer<Map.Entry<String, Double>>() {
                @Override
                public void accept(Map.Entry<String, Double> stringDoubleEntry) {
                    values.put(stringDoubleEntry.getKey(), stringDoubleEntry.getValue() / apiMetrics.getBuckets().size());
                }
            });

            apiMetrics.setGlobal(values);
        }

        // Prepare metadata
        Map<String, Map<String, String>> metadata = new HashMap<>();

        apiMetrics.getBuckets().keySet().forEach(new Consumer<String>() {
            @Override
            public void accept(String name) {
                if (field.equalsIgnoreCase("endpoint")) {
                    metadata.put(name, getEndpointMetadata(api, name));
                } else if (field.equalsIgnoreCase("gateway")) {
                    metadata.put(name, getGatewayMetadata(name));
                }
            }
        });

        apiMetrics.setMetadata(metadata);

        return apiMetrics;
    }

    private Log toLog(io.gravitee.repository.healthcheck.query.log.Log repoLog) {
        Log log = new Log();
        log.setId(repoLog.getId());
        log.setTimestamp(repoLog.getTimestamp());
        log.setAvailable(repoLog.isAvailable());
        log.setSuccess(repoLog.isSuccess());
        log.setEndpoint(repoLog.getEndpoint());
        log.setGateway(repoLog.getGateway());
        log.setResponseTime(repoLog.getResponseTime());
        log.setState(repoLog.getState());

        Request request = new Request();
        request.setMethod(repoLog.getMethod());
        request.setUri(repoLog.getUri());
        log.setRequest(request);

        Response response = new Response();
        response.setStatus(repoLog.getStatus());
        log.setResponse(response);

        return log;
    }

    private Log toLog(io.gravitee.repository.healthcheck.query.log.ExtendedLog repoLog) {
        Log log = new Log();
        log.setId(repoLog.getId());
        log.setTimestamp(repoLog.getTimestamp());
        log.setAvailable(repoLog.isAvailable());
        log.setSuccess(repoLog.isSuccess());
        log.setEndpoint(repoLog.getEndpoint());
        log.setGateway(repoLog.getGateway());
        log.setResponseTime(repoLog.getResponseTime());
        log.setState(repoLog.getState());
        log.setMessage(repoLog.getSteps().get(0).getMessage());
        log.setRequest(createRequest(repoLog.getSteps().get(0).getRequest()));
        log.setResponse(createResponse(repoLog.getSteps().get(0).getResponse()));

        return log;
    }

    private Request createRequest(io.gravitee.repository.healthcheck.query.log.Request repoRequest) {
        if (repoRequest == null) {
            return null;
        }

        Request request = new Request();
        request.setUri(repoRequest.getUri());
        request.setMethod(repoRequest.getMethod());
        request.setHeaders(repoRequest.getHeaders());
        request.setBody(repoRequest.getBody());

        return request;
    }

    private Response createResponse(io.gravitee.repository.healthcheck.query.log.Response repoResponse) {
        if (repoResponse == null) {
            return null;
        }

        Response response = new Response();
        response.setStatus(repoResponse.getStatus());
        response.setHeaders(repoResponse.getHeaders());
        response.setBody(repoResponse.getBody());

        return response;
    }

    private Map<String, String> getEndpointMetadata(ApiEntity api, String endpointName) {
        Map<String, String> metadata = new HashMap<>();

        Optional<Endpoint> endpointOpt = api.getProxy()
                .getGroups()
                .stream()
                .flatMap(group -> group.getEndpoints().stream())
                .filter(endpoint -> endpoint.getName().equalsIgnoreCase(endpointName))
                .findFirst();

        if (endpointOpt.isPresent()) {
            metadata.put("target", endpointOpt.get().getTarget());
        } else {
            metadata.put("deleted", "true");
        }

        return metadata;
    }

    private Map<String, String> getGatewayMetadata(String gateway) {
        Map<String, String> metadata = new HashMap<>();

        Optional<InstanceListItem> instanceOptional = instanceService.findInstances(false).stream()
                .filter(instanceListItem -> instanceListItem.getId().equals(gateway))
                .findFirst();

        if (instanceOptional.isPresent()) {
            metadata.put("hostname", instanceOptional.get().getHostname());
            metadata.put("ip", instanceOptional.get().getIp());
            if (instanceOptional.get().getTenant() != null) {
                metadata.put("tenant", instanceOptional.get().getTenant());
            }
        } else {
            metadata.put("deleted", "true");
        }

        return metadata;
    }
}
