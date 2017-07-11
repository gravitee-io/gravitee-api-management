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
import io.gravitee.management.model.analytics.query.LogQuery;
import io.gravitee.management.model.log.*;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.LogsService;
import io.gravitee.management.service.PlanService;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.management.service.exceptions.ApplicationNotFoundException;
import io.gravitee.management.service.exceptions.PlanNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.DateRangeBuilder;
import io.gravitee.repository.analytics.query.IntervalBuilder;
import io.gravitee.repository.analytics.query.QueryBuilders;
import io.gravitee.repository.analytics.query.tabular.TabularResponse;
import io.gravitee.repository.log.api.LogRepository;
import io.gravitee.repository.management.model.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class LogsServiceImpl implements LogsService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(LogsServiceImpl.class);

    private static final String APPLICATION_KEYLESS = "1";

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private PlanService planService;

    @Override
    public SearchLogResponse findByApi(String api, LogQuery query) {
        try {
            TabularResponse response = logRepository.query(
                    QueryBuilders.tabular()
                            .page(query.getPage())
                            .size(query.getSize())
                            .query(query.getQuery())
                            .timeRange(
                                    DateRangeBuilder.between(query.getFrom(), query.getTo()),
                                    IntervalBuilder.interval(query.getInterval())
                            )
                            .root("api", api)
                            .build());

            SearchLogResponse<ApiRequestItem> logResponse = new SearchLogResponse<>(response.getSize());

            // Transform repository logs
            logResponse.setLogs(response.getRequests().stream()
                    .map(this::toApiRequestItem)
                    .collect(Collectors.toList()));

            // Add metadata (only if they are results)
            if (response.getSize() > 0) {
                Map<String, Map<String, String>> metadata = new HashMap<>();

                logResponse.getLogs().forEach(logItem -> {
                    String application = logItem.getApplication();
                    String plan = logItem.getPlan();

                    if (application != null) {
                        metadata.computeIfAbsent(application, getApplicationMetadata(application));
                    }
                    if (plan != null) {
                        metadata.computeIfAbsent(plan, getPlanMetadata(plan));
                    }
                });

                logResponse.setMetadata(metadata);
            }

            return logResponse;
        } catch (AnalyticsException ae) {
            logger.error("Unable to retrieve logs: ", ae);
            throw new TechnicalManagementException("Unable to retrieve logs", ae);
        }
    }

    @Override
    public ApiRequest findApiLog(String id) {
        try {
            return toApiRequest(logRepository.findById(id));
        } catch (AnalyticsException ae) {
            logger.error("Unable to retrieve log: " + id, ae);
            throw new TechnicalManagementException("Unable to retrieve log: " + id, ae);
        }
    }

    @Override
    public SearchLogResponse findByApplication(String application, LogQuery query) {
        try {
            TabularResponse response = logRepository.query(
                    QueryBuilders.tabular()
                            .page(query.getPage())
                            .size(query.getSize())
                            .query(query.getQuery())
                            .timeRange(
                                    DateRangeBuilder.between(query.getFrom(), query.getTo()),
                                    IntervalBuilder.interval(query.getInterval())
                            )
                            .root("application", application)
                            .build());

            SearchLogResponse<ApplicationRequestItem> logResponse = new SearchLogResponse<>(response.getSize());

            // Transform repository logs
            logResponse.setLogs(response.getRequests().stream()
                    .map(this::toApplicationRequestItem)
                    .collect(Collectors.toList()));

            // Add metadata (only if they are results)
            if (response.getSize() > 0) {
                Map<String, Map<String, String>> metadata = new HashMap<>();

                logResponse.getLogs().forEach(logItem -> {
                    String api = logItem.getApi();
                    String plan = logItem.getPlan();

                    if (api != null) {
                        metadata.computeIfAbsent(api, getAPIMetadata(api));
                    }
                    if (plan != null) {
                        metadata.computeIfAbsent(plan, getPlanMetadata(plan));
                    }
                });

                logResponse.setMetadata(metadata);
            }

            return logResponse;
        } catch (AnalyticsException ae) {
            logger.error("Unable to retrieve logs: ", ae);
            throw new TechnicalManagementException("Unable to retrieve logs", ae);
        }
    }

    @Override
    public ApplicationRequest findApplicationLog(String id) {
        try {
            return toApplicationRequest(logRepository.findById(id));
        } catch (AnalyticsException ae) {
            logger.error("Unable to retrieve log: " + id, ae);
            throw new TechnicalManagementException("Unable to retrieve log: " + id, ae);
        }
    }

    private Function<String, Map<String, String>> getAPIMetadata(String api) {
        return s -> {
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
        };
    }

    private Function<String, Map<String, String>> getApplicationMetadata(String application) {
        return s -> {
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
        };
    }

    private Function<String, Map<String, String>> getPlanMetadata(String plan) {
        return s -> {
            Map<String, String> metadata = new HashMap<>();

            try {
                PlanEntity planEntity = planService.findById(plan);
                metadata.put("name", planEntity.getName());
            } catch (PlanNotFoundException anfe) {
                metadata.put("deleted", "true");
            }

            return metadata;
        };
    }

    private ApiRequestItem toApiRequestItem(io.gravitee.repository.log.model.Request request) {
        ApiRequestItem req = new ApiRequestItem();
        req.setId(request.getId());
        req.setTransactionId(request.getTransactionId());
        req.setApplication(request.getApplication());
        req.setMethod(request.getMethod());
        req.setPath(request.getPath());
        req.setPlan(request.getPlan());
        req.setResponseTime(request.getResponseTime());
        req.setStatus(request.getStatus());
        req.setTimestamp(request.getTimestamp());
        return req;
    }

    private ApplicationRequestItem toApplicationRequestItem(io.gravitee.repository.log.model.Request request) {
        ApplicationRequestItem req = new ApplicationRequestItem();
        req.setId(request.getId());
        req.setTransactionId(request.getTransactionId());
        req.setApi(request.getApi());
        req.setMethod(request.getMethod());
        req.setPath(request.getPath());
        req.setPlan(request.getPlan());
        req.setResponseTime(request.getResponseTime());
        req.setStatus(request.getStatus());
        req.setTimestamp(request.getTimestamp());
        return req;
    }

    private ApiRequest toApiRequest(io.gravitee.repository.log.model.Request request) {
        ApiRequest req = new ApiRequest();
        req.setId(request.getId());
        req.setTransactionId(request.getTransactionId());
        req.setApplication(request.getApplication());
        req.setApiResponseTime(request.getApiResponseTime());
        req.setEndpoint(request.getEndpoint());
        req.setLocalAddress(request.getLocalAddress());
        req.setRemoteAddress(request.getRemoteAddress());
        req.setMethod(request.getMethod());
        req.setPath(request.getPath());
        req.setPlan(request.getPlan());
        req.setRequestContentLength(request.getRequestContentLength());
        req.setResponseContentLength(request.getResponseContentLength());
        req.setResponseTime(request.getResponseTime());
        req.setStatus(request.getStatus());
        req.setTenant(request.getTenant());
        req.setTimestamp(request.getTimestamp());
        req.setUri(request.getUri());
        req.setUser(request.getUser());
        req.setApiKey(request.getApiKey());
        req.setClientRequestHeaders(request.getClientRequestHeaders());
        req.setClientResponseHeaders(request.getClientResponseHeaders());
        req.setProxyRequestHeaders(request.getProxyRequestHeaders());
        req.setProxyResponseHeaders(request.getProxyResponseHeaders());
        req.setMessage(request.getMessage());
        req.setGateway(request.getGateway());
        return req;
    }

    private ApplicationRequest toApplicationRequest(io.gravitee.repository.log.model.Request request) {
        ApplicationRequest req = new ApplicationRequest();
        req.setId(request.getId());
        req.setTransactionId(request.getTransactionId());
        req.setApi(request.getApi());
        req.setMethod(request.getMethod());
        req.setPath(request.getPath());
        req.setPlan(request.getPlan());
        req.setRequestContentLength(request.getRequestContentLength());
        req.setResponseContentLength(request.getResponseContentLength());
        req.setResponseTime(request.getResponseTime());
        req.setStatus(request.getStatus());
        req.setTimestamp(request.getTimestamp());
        req.setUser(request.getUser());
        req.setApiKey(request.getApiKey());
        req.setClientRequestHeaders(request.getClientRequestHeaders());
        req.setClientResponseHeaders(request.getClientResponseHeaders());
        return req;
    }
}
