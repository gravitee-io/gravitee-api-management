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

import io.gravitee.management.model.*;
import io.gravitee.management.model.analytics.query.LogQuery;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.log.*;
import io.gravitee.management.model.log.extended.Request;
import io.gravitee.management.model.log.extended.Response;
import io.gravitee.management.service.*;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.DateRangeBuilder;
import io.gravitee.repository.analytics.query.IntervalBuilder;
import io.gravitee.repository.analytics.query.QueryBuilders;
import io.gravitee.repository.analytics.query.tabular.TabularResponse;
import io.gravitee.repository.log.api.LogRepository;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    @Autowired
    private InstanceService instanceService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private SubscriptionService subscriptionService;

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
            logResponse.setLogs(response.getLogs().stream()
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
    public ApiRequest findApiLog(String id, Long timestamp) {
        try {
            return toApiRequest(logRepository.findById(id, timestamp));
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
            logResponse.setLogs(response.getLogs().stream()
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
    public ApplicationRequest findApplicationLog(String id, Long timestamp) {
        try {
            return toApplicationRequest(logRepository.findById(id, timestamp));
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

    private Function<String, Map<String, String>> getGatewayMetadata(String gateway) {
        return s -> {
            Map<String, String> metadata = new HashMap<>();

            Optional<InstanceListItem> instanceOptional = instanceService.findInstances(true).stream()
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
        };
    }

    private String getSubscription(io.gravitee.repository.log.model.ExtendedLog log) {
        if (log.getApiKey() != null) {
            try {
                ApiKeyEntity key = apiKeyService.findByKey(log.getApiKey());
                if (key != null) {
                    return key.getSubscription();
                }
            } catch (ApiKeyNotFoundException e) {
                // wrong apikey
            }
        } else if (log.getPlan() != null && log.getApplication() != null){
            PlanEntity plan = planService.findById(log.getPlan());
            if (!PlanSecurityType.API_KEY.equals(plan.getSecurity()) && !PlanSecurityType.KEY_LESS.equals(plan.getSecurity())) {
                Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApplicationAndPlan(log.getApplication(), log.getPlan());
                if (!subscriptions.isEmpty() && subscriptions.size() == 1) {
                    return subscriptions.iterator().next().getId();
                }
            }
        }
        return null;
    }

    private ApiRequestItem toApiRequestItem(io.gravitee.repository.log.model.Log log) {
        ApiRequestItem req = new ApiRequestItem();
        req.setId(log.getId());
        req.setTransactionId(log.getTransactionId());
        req.setApplication(log.getApplication());
        req.setMethod(log.getMethod());
        req.setPath(new QueryStringDecoder(log.getUri()).path());
        req.setPlan(log.getPlan());
        req.setResponseTime(log.getResponseTime());
        req.setStatus(log.getStatus());
        req.setTimestamp(log.getTimestamp());
        req.setEndpoint(log.getApiResponseTime() > 0);
        return req;
    }

    private ApplicationRequestItem toApplicationRequestItem(io.gravitee.repository.log.model.Log log) {
        ApplicationRequestItem req = new ApplicationRequestItem();
        req.setId(log.getId());
        req.setTransactionId(log.getTransactionId());
        req.setApi(log.getApi());
        req.setMethod(log.getMethod());
        req.setPath(new QueryStringDecoder(log.getUri()).path());
        req.setPlan(log.getPlan());
        req.setResponseTime(log.getResponseTime());
        req.setStatus(log.getStatus());
        req.setTimestamp(log.getTimestamp());
        return req;
    }

    private ApiRequest toApiRequest(io.gravitee.repository.log.model.ExtendedLog log) {
        ApiRequest req = new ApiRequest();
        req.setId(log.getId());
        req.setTransactionId(log.getTransactionId());
        req.setApplication(log.getApplication());
        req.setApiResponseTime(log.getApiResponseTime());
        req.setEndpoint(log.getEndpoint());
        req.setLocalAddress(log.getLocalAddress());
        req.setRemoteAddress(log.getRemoteAddress());
        req.setMethod(log.getMethod());
        req.setPath(new QueryStringDecoder(log.getUri()).path());
        req.setPlan(log.getPlan());
        req.setRequestContentLength(log.getRequestContentLength());
        req.setResponseContentLength(log.getResponseContentLength());
        req.setResponseTime(log.getResponseTime());
        req.setStatus(log.getStatus());
        req.setTenant(log.getTenant());
        req.setTimestamp(log.getTimestamp());
        req.setUri(log.getUri());
        req.setApiKey(log.getApiKey());
        req.setMessage(log.getMessage());
        req.setGateway(log.getGateway());
        req.setSubscription(getSubscription(log));
        req.setHost(log.getHost());

        req.setClientRequest(createRequest(log.getClientRequest()));
        req.setProxyRequest(createRequest(log.getProxyRequest()));
        req.setClientResponse(createResponse(log.getClientResponse()));
        req.setProxyResponse(createResponse(log.getProxyResponse()));

        Map<String, Map<String, String>> metadata = new HashMap<>();

        String application = log.getApplication();
        String plan = log.getPlan();
        String gateway = log.getGateway();


        if (application != null) {
            metadata.computeIfAbsent(application, getApplicationMetadata(application));
        }
        if (plan != null) {
            metadata.computeIfAbsent(plan, getPlanMetadata(plan));
        }
        if (gateway != null) {
            metadata.computeIfAbsent(gateway, getGatewayMetadata(gateway));
        }

        req.setMetadata(metadata);

        return req;
    }

    private Request createRequest(io.gravitee.repository.log.model.Request repoRequest) {
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

    private Response createResponse(io.gravitee.repository.log.model.Response repoResponse) {
        if (repoResponse == null) {
            return null;
        }

        Response response = new Response();
        response.setStatus(repoResponse.getStatus());
        response.setHeaders(repoResponse.getHeaders());
        response.setBody(repoResponse.getBody());

        return response;
    }

    private ApplicationRequest toApplicationRequest(io.gravitee.repository.log.model.ExtendedLog log) {
        ApplicationRequest req = new ApplicationRequest();
        req.setId(log.getId());
        req.setTransactionId(log.getTransactionId());
        req.setApi(log.getApi());
        req.setMethod(log.getMethod());
        req.setPath(new QueryStringDecoder(log.getUri()).path());
        req.setPlan(log.getPlan());
        req.setRequestContentLength(log.getRequestContentLength());
        req.setResponseContentLength(log.getResponseContentLength());
        req.setResponseTime(log.getResponseTime());
        req.setStatus(log.getStatus());
        req.setTimestamp(log.getTimestamp());
        req.setApiKey(log.getApiKey());
        req.setRequest(createRequest(log.getClientRequest()));
        req.setResponse(createResponse(log.getClientResponse()));
        req.setHost(log.getHost());

        Map<String, Map<String, String>> metadata = new HashMap<>();

        String api = log.getApi();
        String plan = log.getPlan();
        String gateway = log.getGateway();

        if (api != null) {
            metadata.computeIfAbsent(api, getAPIMetadata(api));
        }
        if (plan != null) {
            metadata.computeIfAbsent(plan, getPlanMetadata(plan));
        }
        if (gateway != null) {
            metadata.computeIfAbsent(gateway, getGatewayMetadata(gateway));
        }

        req.setMetadata(metadata);

        return req;
    }
}
