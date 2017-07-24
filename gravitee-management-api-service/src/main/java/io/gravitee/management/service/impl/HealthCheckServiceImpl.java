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
import io.gravitee.management.model.analytics.query.LogQuery;
import io.gravitee.management.model.healthcheck.ApiMetrics;
import io.gravitee.management.model.healthcheck.Log;
import io.gravitee.management.model.healthcheck.Logs;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.HealthCheckService;
import io.gravitee.management.service.InstanceService;
import io.gravitee.repository.healthcheck.api.HealthCheckRepository;
import io.gravitee.repository.healthcheck.query.Bucket;
import io.gravitee.repository.healthcheck.query.FieldBucket;
import io.gravitee.repository.healthcheck.query.QueryBuilders;
import io.gravitee.repository.healthcheck.query.availability.AvailabilityQuery;
import io.gravitee.repository.healthcheck.query.availability.AvailabilityResponse;
import io.gravitee.repository.healthcheck.query.log.LogsResponse;
import io.gravitee.repository.healthcheck.query.log.Step;
import io.gravitee.repository.healthcheck.query.responsetime.AverageResponseTimeQuery;
import io.gravitee.repository.healthcheck.query.responsetime.AverageResponseTimeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
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
    public Logs getLogs(String api, LogQuery query) {
        logger.debug("Run health logs query for API '{}'", api);

        try {
            LogsResponse response = healthCheckRepository.query(
                    QueryBuilders.logs()
                            .api(api)
                            .page(query.getPage())
                            .size(query.getSize())
                            .query(query.getQuery())
                            .build());

            return convert(response);
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while searching for health data.", ex);
            return null;
        }
    }

    private Logs convert(LogsResponse response) {
        Logs logsResponse = new Logs(response.getSize());

        // Transform repository logs
        logsResponse.setLogs(response.getLogs().stream()
                .map(this::toLog)
                .collect(Collectors.toList()));

        // Add metadata (only if they are results)
        if (response.getSize() > 0) {
            Map<String, Map<String, String>> metadata = new HashMap<>();

            logsResponse.getLogs().forEach(logItem -> {
                String gateway = logItem.getGateway();
                //String endpoint = logItem.getEndpoint();

                if (gateway != null) {
                    metadata.computeIfAbsent(gateway, this::getGatewayMetadata);
                }
                /*
                if (endpoint != null) {
                    metadata.computeIfAbsent(endpoint, edpt -> getEndpointMetadata(null, edpt));
                }
                */
            });

            logsResponse.setMetadata(metadata);
        }

        return logsResponse;
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

        if (repoLog.getSteps() != null) {
            log.setSteps(repoLog.getSteps().stream().map(repoStep -> {
                io.gravitee.management.model.healthcheck.Step step = new io.gravitee.management.model.healthcheck.Step();
                step.setMessage(repoStep.getMessage());
                step.setMethod(repoStep.getMethod());
                step.setStatusCode(repoStep.getStatusCode());
                step.setSuccess(repoStep.isSuccess());
                step.setUrl(repoStep.getUrl());
                return step;
            }).collect(Collectors.toList()));
        }

        return log;
    }

    private Map<String, String> getEndpointMetadata(ApiEntity api, String endpointName) {
        Map<String, String> metadata = new HashMap<>();

        Optional<Endpoint> endpointOpt = api.getProxy().getEndpoints()
                .stream()
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
