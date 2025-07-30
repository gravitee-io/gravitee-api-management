/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.repository.analytics.query.DateRangeBuilder;
import io.gravitee.repository.analytics.query.IntervalBuilder;
import io.gravitee.repository.analytics.query.Order;
import io.gravitee.repository.analytics.query.QueryBuilders;
import io.gravitee.repository.analytics.query.SortBuilder;
import io.gravitee.repository.analytics.query.tabular.TabularResponse;
import io.gravitee.repository.log.api.LogRepository;
import io.gravitee.repository.log.model.ExtendedLog;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.InstanceEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.log.ApiRequest;
import io.gravitee.rest.api.model.log.ApiRequestItem;
import io.gravitee.rest.api.model.log.ApplicationRequest;
import io.gravitee.rest.api.model.log.ApplicationRequestItem;
import io.gravitee.rest.api.model.log.PlatformRequestItem;
import io.gravitee.rest.api.model.log.SearchLogResponse;
import io.gravitee.rest.api.model.log.extended.Request;
import io.gravitee.rest.api.model.log.extended.Response;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiKeyNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.InstanceNotFoundException;
import io.gravitee.rest.api.service.exceptions.LogNotFoundException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class LogsServiceImpl implements LogsService {

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
    private static final CsvUtils csvUtils = new CsvUtils(separator);

    @Lazy
    @Autowired
    private LogRepository logRepository;

    @Autowired
    private ApiSearchService apiSearchService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private PlanSearchService planSearchService;

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
    public SearchLogResponse<ApiRequestItem> findByApi(final ExecutionContext executionContext, String api, LogQuery query) {
        try {
            final String field = query.getField() == null ? "@timestamp" : query.getField();
            TabularResponse response = logRepository.query(
                executionContext.getQueryContext(),
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

            if (response == null) {
                return new SearchLogResponse<>(0);
            }

            SearchLogResponse<ApiRequestItem> logResponse = new SearchLogResponse<>(response.getSize());

            // Transform repository logs
            logResponse.setLogs(response.getLogs().stream().map(this::toApiRequestItem).collect(Collectors.toList()));

            // Add metadata (only if they are results)
            if (response.getSize() > 0) {
                Map<String, Map<String, String>> metadata = new HashMap<>();

                logResponse
                    .getLogs()
                    .forEach(logItem -> {
                        String application = logItem.getApplication();
                        String plan = logItem.getPlan();

                        if (application != null) {
                            metadata.computeIfAbsent(application, getApplicationMetadata(executionContext, application));
                        }
                        if (plan != null) {
                            metadata.computeIfAbsent(plan, getPlanMetadata(executionContext, plan));
                        }
                    });

                logResponse.setMetadata(metadata);
            }

            return logResponse;
        } catch (AnalyticsException ae) {
            throw new TechnicalManagementException(String.format("Unable to retrieve logs for API %s", api), ae);
        }
    }

    @Override
    public ApiRequest findApiLog(final ExecutionContext executionContext, String id, Long timestamp) {
        try {
            final ExtendedLog log = logRepository.findById(executionContext.getQueryContext(), id, timestamp);
            if (log == null) {
                return null;
            }

            // Check that request log belongs to the current environment
            apiSearchService.findRepositoryApiById(executionContext, log.getApi());

            if (parameterService.findAsBoolean(executionContext, Key.LOGGING_AUDIT_ENABLED, ParameterReferenceType.ORGANIZATION)) {
                auditService.createApiAuditLog(
                    executionContext,
                    log.getApi(),
                    Collections.singletonMap(REQUEST_ID, id),
                    LOG_READ,
                    new Date(),
                    null,
                    null
                );
            }
            return toApiRequest(executionContext, log);
        } catch (AnalyticsException ae) {
            throw new TechnicalManagementException(String.format("Unable to retrieve log: %s", id), ae);
        } catch (ApiNotFoundException anfe) {
            log.warn("Requested log [{}] is not attached to environment [{}]", id, executionContext.getEnvironmentId(), anfe);
            throw new LogNotFoundException(id);
        }
    }

    @Override
    public SearchLogResponse<ApplicationRequestItem> findByApplication(
        ExecutionContext executionContext,
        String application,
        LogQuery query
    ) {
        try {
            final String field = query.getField() == null ? "@timestamp" : query.getField();
            TabularResponse response = logRepository.query(
                executionContext.getQueryContext(),
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

            if (response == null) {
                return new SearchLogResponse<>(0);
            }

            SearchLogResponse<ApplicationRequestItem> logResponse = new SearchLogResponse<>(response.getSize());

            // Transform repository logs
            logResponse.setLogs(response.getLogs().stream().map(this::toApplicationRequestItem).collect(Collectors.toList()));

            // Add metadata (only if they are results)
            if (response.getSize() > 0) {
                Map<String, Map<String, String>> metadata = new HashMap<>();

                logResponse
                    .getLogs()
                    .forEach(logItem -> {
                        String api = logItem.getApi();
                        String plan = logItem.getPlan();

                        if (api != null) {
                            metadata.computeIfAbsent(api, getAPIMetadata(executionContext, api));
                        }
                        if (plan != null) {
                            metadata.computeIfAbsent(plan, getPlanMetadata(executionContext, plan));
                        }
                    });

                logResponse.setMetadata(metadata);
            }

            return logResponse;
        } catch (AnalyticsException ae) {
            throw new TechnicalManagementException("Unable to retrieve logs", ae);
        }
    }

    @Override
    public SearchLogResponse<PlatformRequestItem> findPlatform(final ExecutionContext executionContext, LogQuery query) {
        try {
            final String field = query.getField() == null ? "@timestamp" : query.getField();
            TabularResponse response = logRepository.query(
                executionContext.getQueryContext(),
                QueryBuilders
                    .tabular()
                    .page(query.getPage())
                    .size(query.getSize())
                    .query(query.getQuery())
                    .terms(query.getTerms())
                    .sort(SortBuilder.on(field, query.isOrder() ? Order.ASC : Order.DESC, null))
                    .timeRange(DateRangeBuilder.between(query.getFrom(), query.getTo()), IntervalBuilder.interval(query.getInterval()))
                    //                            .root("application", application)
                    .build()
            );

            if (response == null) {
                return new SearchLogResponse<>(0);
            }

            SearchLogResponse<PlatformRequestItem> logResponse = new SearchLogResponse<>(response.getSize());

            // Transform repository logs
            logResponse.setLogs(response.getLogs().stream().map(this::toPlatformRequestItem).collect(Collectors.toList()));

            // Add metadata (only if they are results)
            if (response.getSize() > 0) {
                Map<String, Map<String, String>> metadata = new HashMap<>();

                logResponse
                    .getLogs()
                    .forEach(logItem -> {
                        String api = logItem.getApi();
                        String application = logItem.getApplication();
                        String plan = logItem.getPlan();

                        if (api != null) {
                            metadata.computeIfAbsent(api, getAPIMetadata(executionContext, api));
                        }
                        if (application != null) {
                            metadata.computeIfAbsent(application, getApplicationMetadata(executionContext, application));
                        }
                        if (plan != null) {
                            metadata.computeIfAbsent(plan, getPlanMetadata(executionContext, plan));
                        }
                    });

                logResponse.setMetadata(metadata);
            }

            return logResponse;
        } catch (AnalyticsException ae) {
            throw new TechnicalManagementException("Unable to retrieve logs", ae);
        }
    }

    @Override
    public ApplicationRequest findApplicationLog(ExecutionContext executionContext, String applicationId, String id, Long timestamp) {
        try {
            ExtendedLog extendedLog = logRepository.findById(executionContext.getQueryContext(), id, timestamp);
            if (extendedLog == null) {
                return null;
            }

            if (!applicationId.equalsIgnoreCase(extendedLog.getApplication())) {
                log.warn("Requested log [{}] is not attached to application [{}]", id, applicationId);
                throw new LogNotFoundException(id);
            }

            return toApplicationRequest(executionContext, extendedLog);
        } catch (AnalyticsException ae) {
            if (ae.getMessage().equals("Request [" + id + "] does not exist")) {
                throw new LogNotFoundException(id);
            }
            throw new TechnicalManagementException(String.format("Unable to retrieve log: %s", id), ae);
        }
    }

    private Function<String, Map<String, String>> getAPIMetadata(ExecutionContext executionContext, String api) {
        return s -> {
            Map<String, String> metadata = new HashMap<>();

            try {
                if (api.equals(UNKNOWN_SERVICE) || api.equals(UNKNOWN_SERVICE_MAPPED)) {
                    metadata.put(METADATA_NAME, METADATA_UNKNOWN_API_NAME);
                    metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
                } else {
                    GenericApiEntity genericApiEntity = apiSearchService.findGenericById(executionContext, api);
                    metadata.put(METADATA_NAME, genericApiEntity.getName());
                    metadata.put(METADATA_VERSION, genericApiEntity.getApiVersion());
                    if (ApiLifecycleState.ARCHIVED.equals(genericApiEntity.getLifecycleState())) {
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

    private Function<String, Map<String, String>> getApplicationMetadata(final ExecutionContext executionContext, String application) {
        return s -> {
            Map<String, String> metadata = new HashMap<>();

            try {
                if (application.equals(UNKNOWN_SERVICE) || application.equals(UNKNOWN_SERVICE_MAPPED)) {
                    metadata.put(METADATA_NAME, METADATA_UNKNOWN_APPLICATION_NAME);
                    metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
                } else {
                    ApplicationEntity applicationEntity = applicationService.findById(executionContext, application);
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

    private Function<String, Map<String, String>> getPlanMetadata(ExecutionContext executionContext, String plan) {
        return s -> {
            Map<String, String> metadata = new HashMap<>();
            try {
                if (plan.equals(UNKNOWN_SERVICE) || plan.equals(UNKNOWN_SERVICE_MAPPED)) {
                    metadata.put(METADATA_NAME, METADATA_UNKNOWN_PLAN_NAME);
                    metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
                } else {
                    GenericPlanEntity planEntity = planSearchService.findById(executionContext, plan);
                    metadata.put(METADATA_NAME, planEntity.getName());
                }
            } catch (PlanNotFoundException anfe) {
                metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                metadata.put(METADATA_NAME, METADATA_DELETED_PLAN_NAME);
            }
            return metadata;
        };
    }

    private Function<String, Map<String, String>> getGatewayMetadata(ExecutionContext executionContext, String gateway) {
        return s -> {
            Map<String, String> metadata = new HashMap<>();

            try {
                InstanceEntity instance = instanceService.findById(executionContext, gateway);
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

    private String getSubscription(ExecutionContext executionContext, ExtendedLog extendedLog) {
        if (PlanSecurityType.API_KEY.name().equals(extendedLog.getSecurityType())) {
            try {
                return getApiKeySubscription(executionContext, extendedLog);
            } catch (ApiKeyNotFoundException e) {
                log.warn("Unable to find API Key for log [api={}, application={}]", extendedLog.getApi(), extendedLog.getApplication());
            }
        } else if (extendedLog.getPlan() != null && extendedLog.getApplication() != null) {
            try {
                return getJwtOrOauth2Subscription(executionContext, extendedLog);
            } catch (PlanNotFoundException | SubscriptionNotFoundException | IllegalStateException e) {
                log.warn(
                    "Unable to find subscription for log [plan={}, application={}]",
                    extendedLog.getPlan(),
                    extendedLog.getApplication(),
                    e
                );
            }
        }
        return null;
    }

    private String getApiKeySubscription(ExecutionContext executionContext, ExtendedLog extendedLog) {
        return apiKeyService
            .findByKeyAndApi(executionContext, extendedLog.getSecurityToken(), extendedLog.getApi())
            .getSubscriptions()
            .stream()
            .filter(s -> s.getApi().equals(extendedLog.getApi()))
            .findFirst()
            .map(SubscriptionEntity::getId)
            .orElseThrow(ApiKeyNotFoundException::new);
    }

    private String getJwtOrOauth2Subscription(ExecutionContext executionContext, ExtendedLog extendedLog) {
        GenericPlanEntity plan = planSearchService.findById(executionContext, extendedLog.getPlan());
        if (plan.getPlanSecurity() == null || plan.getPlanSecurity().getType() == null) {
            return null;
        }

        io.gravitee.rest.api.model.v4.plan.PlanSecurityType planSecurityType =
            io.gravitee.rest.api.model.v4.plan.PlanSecurityType.valueOfLabel(plan.getPlanSecurity().getType());
        if (
            io.gravitee.rest.api.model.v4.plan.PlanSecurityType.API_KEY == planSecurityType ||
            io.gravitee.rest.api.model.v4.plan.PlanSecurityType.KEY_LESS == planSecurityType
        ) {
            return null;
        }

        Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApplicationAndPlan(
            executionContext,
            extendedLog.getApplication(),
            extendedLog.getPlan()
        );

        if (subscriptions.size() > 1) {
            throw new IllegalStateException("Found more than one subscription for the same application and plan");
        }

        return subscriptions.stream().findFirst().map(SubscriptionEntity::getId).orElseThrow(() -> new SubscriptionNotFoundException(null));
    }

    @Override
    public String exportAsCsv(ExecutionContext executionContext, final SearchLogResponse<?> searchLogResponse) {
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
        final boolean userEnabled = parameterService.findAsBoolean(
            executionContext,
            Key.LOGGING_USER_DISPLAYED,
            ParameterReferenceType.ORGANIZATION
        );

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
                sb.append(csvUtils.sanitize(getName(application)));
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
                sb.append(csvUtils.sanitize(getName(api)));
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
                sb.append(csvUtils.sanitize(getName(api)));
                sb.append(separator);
                final Object application = searchLogResponse.getMetadata().get(platformLog.getApplication());
                sb.append(csvUtils.sanitize(getName(application)));
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
        sb.append(csvUtils.sanitize(id));
        sb.append(separator);
        sb.append(csvUtils.sanitize(transactionId));
        sb.append(separator);
        sb.append(method);
        sb.append(separator);
        sb.append(csvUtils.sanitize(path));
        sb.append(separator);
        sb.append(status);
        sb.append(separator);
        sb.append(responseTime);
        sb.append(separator);
        sb.append(csvUtils.sanitize(getName(searchLogResponse.getMetadata().get(plan))));
        sb.append(separator);
        if (userEnabled) {
            sb.append(csvUtils.sanitize(user));
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

    private ApiRequest toApiRequest(final ExecutionContext executionContext, io.gravitee.repository.log.model.ExtendedLog log) {
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
        req.setSubscription(getSubscription(executionContext, log));
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
            metadata.computeIfAbsent(application, getApplicationMetadata(executionContext, application));
        }
        if (plan != null) {
            metadata.computeIfAbsent(plan, getPlanMetadata(executionContext, plan));
        }
        if (gateway != null) {
            metadata.computeIfAbsent(gateway, getGatewayMetadata(executionContext, gateway));
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

    private ApplicationRequest toApplicationRequest(ExecutionContext executionContext, ExtendedLog extendedLog) {
        ApplicationRequest req = new ApplicationRequest();
        req.setId(extendedLog.getId());
        req.setTransactionId(extendedLog.getTransactionId());
        req.setApi(extendedLog.getApi());
        req.setMethod(extendedLog.getMethod());
        req.setUri(extendedLog.getUri());
        req.setPath(new QueryStringDecoder(extendedLog.getUri()).toString());
        req.setPlan(extendedLog.getPlan());
        req.setRequestContentLength(extendedLog.getRequestContentLength());
        req.setResponseContentLength(extendedLog.getResponseContentLength());
        req.setResponseTime(extendedLog.getResponseTime());
        req.setStatus(extendedLog.getStatus());
        req.setTimestamp(extendedLog.getTimestamp());
        req.setRequest(createRequest(extendedLog.getClientRequest()));
        req.setResponse(createResponse(extendedLog.getClientResponse()));
        req.setHost(extendedLog.getHost());
        req.setSecurityType(extendedLog.getSecurityType());
        req.setSecurityToken(extendedLog.getSecurityToken());

        Map<String, Map<String, String>> metadata = new HashMap<>();

        String api = extendedLog.getApi();
        String plan = extendedLog.getPlan();
        String gateway = extendedLog.getGateway();

        if (api != null) {
            metadata.computeIfAbsent(api, getAPIMetadata(executionContext, api));
        }
        if (plan != null) {
            metadata.computeIfAbsent(plan, getPlanMetadata(executionContext, plan));
        }
        if (gateway != null) {
            metadata.computeIfAbsent(gateway, getGatewayMetadata(executionContext, gateway));
        }

        req.setMetadata(metadata);
        req.setUser(extendedLog.getUser());

        return req;
    }
}
