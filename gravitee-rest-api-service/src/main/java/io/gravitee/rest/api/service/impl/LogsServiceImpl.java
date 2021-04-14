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

import static io.gravitee.repository.log.model.Log.AuditEvent.LOG_READ;
import static io.gravitee.repository.management.model.Audit.AuditProperties.REQUEST_ID;
import static java.lang.System.lineSeparator;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.*;
import io.gravitee.repository.analytics.query.tabular.TabularResponse;
import io.gravitee.repository.log.api.LogRepository;
import io.gravitee.repository.log.model.ExtendedLog;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.log.*;
import io.gravitee.rest.api.model.log.extended.Request;
import io.gravitee.rest.api.model.log.extended.Response;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.exceptions.*;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class LogsServiceImpl implements LogsService {

    private final Logger logger = LoggerFactory.getLogger(LogsServiceImpl.class);

    private static final String UNKNOWN_SERVICE = "1";
    private static final String UNKNOWN_SERVICE_MAPPED = "?";

    private static final String METADATA_NAME = "name";
    private static final String METADATA_DELETED = "deleted";
    private static final String METADATA_UNKNOWN = "unknown";
    private static final String METADATA_VERSION = "version";
    private static final String METADATA_UNKNOWN_API_NAME = "Unknown API (not found)";
    private static final String METADATA_UNKNOWN_APPLICATION_NAME = "Unknown application (keyless)";
    private static final String METADATA_UNKNOWN_PLAN_NAME = "Unknown plan";
    private static final String METADATA_DELETED_API_NAME = "Deleted API";
    private static final String METADATA_DELETED_APPLICATION_NAME = "Deleted application";
    private static final String METADATA_DELETED_PLAN_NAME = "Deleted plan";

    private static final String RFC_3339_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final FastDateFormat dateFormatter = FastDateFormat.getInstance(RFC_3339_DATE_FORMAT);
    private static final char separator = ';';

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

    @Autowired
    private AuditService auditService;

    @Autowired
    private ParameterService parameterService;

    @Override
    public SearchLogResponse findByApi(String api, LogQuery query) {
        try {
            final String field = query.getField() == null ? "@timestamp" : query.getField();
            TabularResponse response = logRepository.query(
                QueryBuilders
                    .tabular()
                    .page(query.getPage())
                    .size(query.getSize())
                    .query(query.getQuery())
                    .sort(SortBuilder.on(field, query.isOrder() ? Order.ASC : Order.DESC, null))
                    .timeRange(DateRangeBuilder.between(query.getFrom(), query.getTo()), IntervalBuilder.interval(query.getInterval()))
                    .root("api", api)
                    .build()
            );

            SearchLogResponse<ApiRequestItem> logResponse = new SearchLogResponse<>(response.getSize());

            // Transform repository logs
            logResponse.setLogs(response.getLogs().stream().map(this::toApiRequestItem).collect(Collectors.toList()));

            // Add metadata (only if they are results)
            if (response.getSize() > 0) {
                Map<String, Map<String, String>> metadata = new HashMap<>();

                logResponse
                    .getLogs()
                    .forEach(
                        logItem -> {
                            String application = logItem.getApplication();
                            String plan = logItem.getPlan();

                            if (application != null) {
                                metadata.computeIfAbsent(application, getApplicationMetadata(application));
                            }
                            if (plan != null) {
                                metadata.computeIfAbsent(plan, getPlanMetadata(plan));
                            }
                        }
                    );

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
            final ExtendedLog log = logRepository.findById(id, timestamp);
            if (parameterService.findAsBoolean(Key.LOGGING_AUDIT_ENABLED)) {
                auditService.createApiAuditLog(log.getApi(), Collections.singletonMap(REQUEST_ID, id), LOG_READ, new Date(), null, null);
            }
            return toApiRequest(log);
        } catch (AnalyticsException ae) {
            logger.error("Unable to retrieve log: " + id, ae);
            throw new TechnicalManagementException("Unable to retrieve log: " + id, ae);
        }
    }

    @Override
    public SearchLogResponse findByApplication(String application, LogQuery query) {
        try {
            final String field = query.getField() == null ? "@timestamp" : query.getField();
            TabularResponse response = logRepository.query(
                QueryBuilders
                    .tabular()
                    .page(query.getPage())
                    .size(query.getSize())
                    .query(query.getQuery())
                    .sort(SortBuilder.on(field, query.isOrder() ? Order.ASC : Order.DESC, null))
                    .timeRange(DateRangeBuilder.between(query.getFrom(), query.getTo()), IntervalBuilder.interval(query.getInterval()))
                    .root("application", application)
                    .build()
            );

            SearchLogResponse<ApplicationRequestItem> logResponse = new SearchLogResponse<>(response.getSize());

            // Transform repository logs
            logResponse.setLogs(response.getLogs().stream().map(this::toApplicationRequestItem).collect(Collectors.toList()));

            // Add metadata (only if they are results)
            if (response.getSize() > 0) {
                Map<String, Map<String, String>> metadata = new HashMap<>();

                logResponse
                    .getLogs()
                    .forEach(
                        logItem -> {
                            String api = logItem.getApi();
                            String plan = logItem.getPlan();

                            if (api != null) {
                                metadata.computeIfAbsent(api, getAPIMetadata(api));
                            }
                            if (plan != null) {
                                metadata.computeIfAbsent(plan, getPlanMetadata(plan));
                            }
                        }
                    );

                logResponse.setMetadata(metadata);
            }

            return logResponse;
        } catch (AnalyticsException ae) {
            logger.error("Unable to retrieve logs: ", ae);
            throw new TechnicalManagementException("Unable to retrieve logs", ae);
        }
    }

    @Override
    public SearchLogResponse findPlatform(LogQuery query) {
        try {
            final String field = query.getField() == null ? "@timestamp" : query.getField();
            TabularResponse response = logRepository.query(
                QueryBuilders
                    .tabular()
                    .page(query.getPage())
                    .size(query.getSize())
                    .query(query.getQuery())
                    .sort(SortBuilder.on(field, query.isOrder() ? Order.ASC : Order.DESC, null))
                    .timeRange(DateRangeBuilder.between(query.getFrom(), query.getTo()), IntervalBuilder.interval(query.getInterval()))
                    //                            .root("application", application)
                    .build()
            );

            SearchLogResponse<PlatformRequestItem> logResponse = new SearchLogResponse<>(response.getSize());

            // Transform repository logs
            logResponse.setLogs(response.getLogs().stream().map(this::toPlatformRequestItem).collect(Collectors.toList()));

            // Add metadata (only if they are results)
            if (response.getSize() > 0) {
                Map<String, Map<String, String>> metadata = new HashMap<>();

                logResponse
                    .getLogs()
                    .forEach(
                        logItem -> {
                            String api = logItem.getApi();
                            String application = logItem.getApplication();
                            String plan = logItem.getPlan();

                            if (api != null) {
                                metadata.computeIfAbsent(api, getAPIMetadata(api));
                            }
                            if (application != null) {
                                metadata.computeIfAbsent(application, getApplicationMetadata(application));
                            }
                            if (plan != null) {
                                metadata.computeIfAbsent(plan, getPlanMetadata(plan));
                            }
                        }
                    );

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
            if (ae.getMessage().equals("Request [" + id + "] does not exist")) {
                throw new LogNotFoundException(id);
            }
            throw new TechnicalManagementException("Unable to retrieve log: " + id, ae);
        }
    }

    private Function<String, Map<String, String>> getAPIMetadata(String api) {
        return s -> {
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
        };
    }

    private Function<String, Map<String, String>> getApplicationMetadata(String application) {
        return s -> {
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
        };
    }

    private Function<String, Map<String, String>> getPlanMetadata(String plan) {
        return s -> {
            Map<String, String> metadata = new HashMap<>();
            try {
                if (plan.equals(UNKNOWN_SERVICE) || plan.equals(UNKNOWN_SERVICE_MAPPED)) {
                    metadata.put(METADATA_NAME, METADATA_UNKNOWN_PLAN_NAME);
                    metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
                } else {
                    PlanEntity planEntity = planService.findById(plan);
                    metadata.put(METADATA_NAME, planEntity.getName());
                }
            } catch (PlanNotFoundException anfe) {
                metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                metadata.put(METADATA_NAME, METADATA_DELETED_PLAN_NAME);
            }
            return metadata;
        };
    }

    private Function<String, Map<String, String>> getGatewayMetadata(String gateway) {
        return s -> {
            Map<String, String> metadata = new HashMap<>();

            try {
                InstanceEntity instance = instanceService.findById(gateway);
                metadata.put("hostname", instance.getHostname());
                metadata.put("ip", instance.getIp());
                if (instance.getTenant() != null) {
                    metadata.put("tenant", instance.getTenant());
                }
            } catch (InstanceNotFoundException infe) {
                metadata.put("deleted", "true");
            }

            return metadata;
        };
    }

    private String getSubscription(io.gravitee.repository.log.model.ExtendedLog log) {
        if ("API_KEY".equals(log.getSecurityType())) {
            try {
                ApiKeyEntity key = apiKeyService.findByKey(log.getSecurityToken());
                if (key != null) {
                    return key.getSubscription();
                }
            } catch (ApiKeyNotFoundException e) {
                // wrong apikey
            }
        } else if (log.getPlan() != null && log.getApplication() != null) {
            PlanEntity plan = planService.findById(log.getPlan());
            if (!PlanSecurityType.API_KEY.equals(plan.getSecurity()) && !PlanSecurityType.KEY_LESS.equals(plan.getSecurity())) {
                Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApplicationAndPlan(
                    log.getApplication(),
                    log.getPlan()
                );
                if (!subscriptions.isEmpty() && subscriptions.size() == 1) {
                    return subscriptions.iterator().next().getId();
                }
            }
        }
        return null;
    }

    @Override
    public String exportAsCsv(final SearchLogResponse searchLogResponse) {
        if (searchLogResponse.getLogs() == null || searchLogResponse.getLogs().isEmpty()) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Date");
        sb.append(separator);
        sb.append("Request Id");
        sb.append(separator);
        sb.append("Transaction Id");
        sb.append(separator);
        sb.append("Method");
        sb.append(separator);
        sb.append("Path");
        sb.append(separator);
        sb.append("Status");
        sb.append(separator);
        sb.append("Response Time");
        sb.append(separator);
        sb.append("Plan");
        sb.append(separator);
        final boolean userEnabled = parameterService.findAsBoolean(Key.LOGGING_USER_DISPLAYED);

        //get the first item to define the type of export
        if (searchLogResponse.getLogs().get(0) instanceof ApiRequestItem) {
            if (userEnabled) {
                sb.append("User");
                sb.append(separator);
            }
            sb.append("Application");
            sb.append(lineSeparator());

            for (final Object log : searchLogResponse.getLogs()) {
                final ApiRequestItem apiLog = (ApiRequestItem) log;
                processLine(
                    searchLogResponse,
                    sb,
                    apiLog.getTimestamp(),
                    apiLog.getId(),
                    apiLog.getTransactionId(),
                    apiLog.getMethod(),
                    apiLog.getPath(),
                    apiLog.getStatus(),
                    apiLog.getResponseTime(),
                    apiLog.getPlan(),
                    userEnabled,
                    apiLog.getUser()
                );
                final Object application = searchLogResponse.getMetadata().get(apiLog.getApplication());
                sb.append(getName(application));
                sb.append(lineSeparator());
            }
        } else if (searchLogResponse.getLogs().get(0) instanceof ApplicationRequestItem) {
            sb.append("API");
            sb.append(lineSeparator());

            for (final Object log : searchLogResponse.getLogs()) {
                final ApplicationRequestItem applicationLog = (ApplicationRequestItem) log;
                processLine(
                    searchLogResponse,
                    sb,
                    applicationLog.getTimestamp(),
                    applicationLog.getId(),
                    applicationLog.getTransactionId(),
                    applicationLog.getMethod(),
                    applicationLog.getPath(),
                    applicationLog.getStatus(),
                    applicationLog.getResponseTime(),
                    applicationLog.getPlan(),
                    false,
                    applicationLog.getUser()
                );
                final Object api = searchLogResponse.getMetadata().get(applicationLog.getApi());
                sb.append(getName(api));
                sb.append(lineSeparator());
            }
        } else if (searchLogResponse.getLogs().get(0) instanceof PlatformRequestItem) {
            if (userEnabled) {
                sb.append("User");
                sb.append(separator);
            }
            sb.append("API");
            sb.append(separator);
            sb.append("Application");
            sb.append(lineSeparator());

            for (final Object log : searchLogResponse.getLogs()) {
                final PlatformRequestItem platformLog = (PlatformRequestItem) log;
                processLine(
                    searchLogResponse,
                    sb,
                    platformLog.getTimestamp(),
                    platformLog.getId(),
                    platformLog.getTransactionId(),
                    platformLog.getMethod(),
                    platformLog.getPath(),
                    platformLog.getStatus(),
                    platformLog.getResponseTime(),
                    platformLog.getPlan(),
                    userEnabled,
                    platformLog.getUser()
                );
                final Object api = searchLogResponse.getMetadata().get(platformLog.getApi());
                sb.append(getName(api));
                sb.append(separator);
                final Object application = searchLogResponse.getMetadata().get(platformLog.getApplication());
                sb.append(getName(application));
                sb.append(lineSeparator());
            }
        }
        return sb.toString();
    }

    private void processLine(
        SearchLogResponse searchLogResponse,
        StringBuilder sb,
        long timestamp,
        String id,
        String transactionId,
        HttpMethod method,
        String path,
        int status,
        long responseTime,
        String plan,
        boolean userEnabled,
        String user
    ) {
        sb.append(dateFormatter.format(timestamp));
        sb.append(separator);
        sb.append(id);
        sb.append(separator);
        sb.append(transactionId);
        sb.append(separator);
        sb.append(method);
        sb.append(separator);
        sb.append(path);
        sb.append(separator);
        sb.append(status);
        sb.append(separator);
        sb.append(responseTime);
        sb.append(separator);
        sb.append(getName(searchLogResponse.getMetadata().get(plan)));
        sb.append(separator);
        if (userEnabled) {
            sb.append(user);
            sb.append(separator);
        }
    }

    private String getName(Object map) {
        return map == null ? "" : ((Map) map).get("name").toString();
    }

    private PlatformRequestItem toPlatformRequestItem(io.gravitee.repository.log.model.Log log) {
        PlatformRequestItem req = new PlatformRequestItem();
        req.setId(log.getId());
        req.setTransactionId(log.getTransactionId());
        req.setApi(log.getApi());
        req.setApplication(log.getApplication());
        req.setMethod(log.getMethod());
        req.setPath(new QueryStringDecoder(log.getUri()).toString());
        req.setPlan(log.getPlan());
        req.setResponseTime(log.getResponseTime());
        req.setStatus(log.getStatus());
        req.setTimestamp(log.getTimestamp());
        req.setEndpoint(log.getEndpoint() != null);
        req.setUser(log.getUser());
        return req;
    }

    private ApiRequestItem toApiRequestItem(io.gravitee.repository.log.model.Log log) {
        ApiRequestItem req = new ApiRequestItem();
        req.setId(log.getId());
        req.setTransactionId(log.getTransactionId());
        req.setApplication(log.getApplication());
        req.setMethod(log.getMethod());
        req.setPath(new QueryStringDecoder(log.getUri()).toString());
        req.setPlan(log.getPlan());
        req.setResponseTime(log.getResponseTime());
        req.setStatus(log.getStatus());
        req.setTimestamp(log.getTimestamp());
        req.setEndpoint(log.getEndpoint() != null);
        req.setUser(log.getUser());
        return req;
    }

    private ApplicationRequestItem toApplicationRequestItem(io.gravitee.repository.log.model.Log log) {
        ApplicationRequestItem req = new ApplicationRequestItem();
        req.setId(log.getId());
        req.setTransactionId(log.getTransactionId());
        req.setApi(log.getApi());
        req.setMethod(log.getMethod());
        req.setPath(new QueryStringDecoder(log.getUri()).toString());
        req.setPlan(log.getPlan());
        req.setResponseTime(log.getResponseTime());
        req.setStatus(log.getStatus());
        req.setTimestamp(log.getTimestamp());
        req.setUser(log.getUser());
        return req;
    }

    private ApiRequest toApiRequest(io.gravitee.repository.log.model.ExtendedLog log) {
        ApiRequest req = new ApiRequest();
        req.setId(log.getId());
        req.setApi(log.getApi());
        req.setTransactionId(log.getTransactionId());
        req.setApplication(log.getApplication());
        req.setApiResponseTime(log.getApiResponseTime());
        req.setEndpoint(log.getEndpoint());
        req.setLocalAddress(log.getLocalAddress());
        req.setRemoteAddress(log.getRemoteAddress());
        req.setMethod(log.getMethod());
        req.setPath(new QueryStringDecoder(log.getUri()).toString());
        req.setPlan(log.getPlan());
        req.setRequestContentLength(log.getRequestContentLength());
        req.setResponseContentLength(log.getResponseContentLength());
        req.setResponseTime(log.getResponseTime());
        req.setStatus(log.getStatus());
        req.setTenant(log.getTenant());
        req.setTimestamp(log.getTimestamp());
        req.setUri(log.getUri());
        req.setMessage(log.getMessage());
        req.setGateway(log.getGateway());
        req.setSubscription(getSubscription(log));
        req.setHost(log.getHost());
        req.setSecurityType(log.getSecurityType());
        req.setSecurityToken(log.getSecurityToken());

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
        req.setUser(log.getUser());

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
        req.setUri(log.getUri());
        req.setPath(new QueryStringDecoder(log.getUri()).toString());
        req.setPlan(log.getPlan());
        req.setRequestContentLength(log.getRequestContentLength());
        req.setResponseContentLength(log.getResponseContentLength());
        req.setResponseTime(log.getResponseTime());
        req.setStatus(log.getStatus());
        req.setTimestamp(log.getTimestamp());
        req.setRequest(createRequest(log.getClientRequest()));
        req.setResponse(createResponse(log.getClientResponse()));
        req.setHost(log.getHost());
        req.setSecurityType(log.getSecurityType());
        req.setSecurityToken(log.getSecurityToken());

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
        req.setUser(log.getUser());

        return req;
    }
}
