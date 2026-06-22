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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.gateway.model.BaseInstance;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.StaticFilters;
import io.gravitee.gamma.rest.core.observability.logs.model.ApiReference;
import io.gravitee.gamma.rest.core.observability.logs.model.HttpPayload;
import io.gravitee.gamma.rest.core.observability.logs.model.LogDetail;
import io.gravitee.gamma.rest.core.observability.logs.model.LogEntry;
import io.gravitee.gamma.rest.core.observability.logs.model.LogEntryWarning;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsPage;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsSearchQuery;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort;
import io.gravitee.repository.analytics.engine.api.query.HttpStatusCodeGroups;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.analytics.Range;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionDiagnosticModel;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.InstanceNotFoundException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Adapter that delegates to the APIM platform's connection-log search infrastructure
 * ({@link ConnectionLogsCrudService}, {@link UserContextLoader}) and enriches the raw results
 * with display names (plans, applications, gateways, API products).
 *
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class ObservabilityLogsDataPortAdapter implements ObservabilityLogsDataPort {

    /**
     * Inclusive HTTP status bounds, sourced from the unified catalog ({@link StaticFilters#HTTP_STATUS})
     * so the adapter and the advertised filter range never drift apart. Used both to validate incoming
     * status values and to bound {@code GTE}/{@code LTE} range expansion.
     */
    private static final FilterSpec.Range HTTP_STATUS_RANGE = StaticFilters.HTTP_STATUS.toSpec().range();
    private static final int HTTP_STATUS_MIN = HTTP_STATUS_RANGE.min().intValue();
    private static final int HTTP_STATUS_MAX = HTTP_STATUS_RANGE.max().intValue();

    private final ConnectionLogsCrudService connectionLogsCrudService;
    private final AnalyticsQueryService analyticsQueryService;
    private final UserContextLoader userContextLoader;
    private final PlanCrudService planCrudService;
    private final ApplicationCrudService applicationCrudService;
    private final InstanceQueryService instanceQueryService;
    private final ApiProductQueryService apiProductQueryService;

    @Override
    public List<AccessibleApi> loadAccessibleApis(String organizationId, String environmentId) {
        var auditInfo = currentAuditInfo(organizationId, environmentId);
        var userContext = userContextLoader.loadApis(new UserContext(auditInfo));

        return userContext
            .apis()
            .orElseGet(Collections::emptyList)
            .stream()
            .map(api -> new AccessibleApi(api.getId(), api.getName(), toGammaApiType(api.getType())))
            .toList();
    }

    @Override
    public LogsPage searchLogs(String organizationId, String environmentId, LogsSearchQuery query) {
        var executionContext = new ExecutionContext(organizationId, environmentId);
        var pageable = new PageableImpl(query.page(), query.perPage());

        var searchFilters = buildSearchFilters(query);
        var result = connectionLogsCrudService.searchApiConnectionLogs(
            executionContext,
            searchFilters,
            pageable,
            List.of(DefinitionVersion.V4)
        );

        var rawEntries = result.logs() == null ? List.<BaseConnectionLog>of() : result.logs();

        var entries = rawEntries
            .stream()
            .map(log -> mapToLogEntry(log, query.apisById()))
            .toList();
        var enriched = enrichWithNames(executionContext, entries);

        return new LogsPage(enriched, result.total());
    }

    @Override
    public Optional<LogDetail> getLogDetail(String organizationId, String environmentId, String apiId, String requestId) {
        var executionContext = new ExecutionContext(organizationId, environmentId);

        var metricsOpt = analyticsQueryService.findApiMetricsDetail(executionContext, apiId, requestId);
        var logDetailOpt = connectionLogsCrudService.searchApiConnectionLog(executionContext, apiId, requestId);

        if (metricsOpt.isEmpty() && logDetailOpt.isEmpty()) {
            return Optional.empty();
        }

        var builder = LogDetail.builder().requestId(requestId).apiId(apiId);

        metricsOpt.ifPresent(metrics -> {
            builder
                .timestamp(parseTimestamp(metrics.getTimestamp()))
                .transactionId(metrics.getTransactionId())
                .method(metrics.getMethod() != null ? metrics.getMethod().name() : null)
                .uri(metrics.getUri())
                .status(metrics.getStatus())
                .endpoint(metrics.getEndpoint())
                .host(metrics.getHost())
                .planId(metrics.getPlanId())
                .applicationId(metrics.getApplicationId())
                .gateway(metrics.getGateway())
                .remoteAddress(metrics.getRemoteAddress())
                .requestContentLength(metrics.getRequestContentLength())
                .responseContentLength(metrics.getResponseContentLength())
                .gatewayLatency(metrics.getGatewayLatency())
                .gatewayResponseTime(metrics.getGatewayResponseTime())
                .endpointResponseTime(metrics.getEndpointResponseTime())
                .message(metrics.getMessage())
                .errorKey(metrics.getErrorKey())
                .errorComponentName(metrics.getErrorComponentName())
                .errorComponentType(metrics.getErrorComponentType())
                .warnings(mapWarnings(metrics.getWarnings()))
                .additionalMetrics(metrics.getAdditionalMetrics() != null ? metrics.getAdditionalMetrics() : Map.of());

            var names = resolveDetailNames(executionContext, metrics);
            builder
                .planName(names.planName)
                .applicationName(names.applicationName)
                .gatewayHostname(names.gatewayHostname)
                .gatewayIp(names.gatewayIp);
        });

        logDetailOpt.ifPresent(log -> {
            builder
                .clientIdentifier(log.getClientIdentifier())
                .requestEnded(log.isRequestEnded())
                .entrypointRequest(mapRequest(log.getEntrypointRequest()))
                .entrypointResponse(mapResponse(log.getEntrypointResponse()))
                .endpointRequest(mapRequest(log.getEndpointRequest()))
                .endpointResponse(mapResponse(log.getEndpointResponse()));

            if (metricsOpt.isEmpty()) {
                builder.timestamp(parseTimestamp(log.getTimestamp()));
            }
        });

        return Optional.of(builder.build());
    }

    private record ResolvedNames(String planName, String applicationName, String gatewayHostname, String gatewayIp) {}

    private ResolvedNames resolveDetailNames(
        ExecutionContext executionContext,
        io.gravitee.rest.api.model.v4.analytics.ApiMetricsDetail metrics
    ) {
        String planName = null;
        String applicationName = null;
        String gatewayHostname = null;
        String gatewayIp = null;

        if (metrics.getPlanId() != null) {
            try {
                planName = planCrudService.getById(metrics.getPlanId()).getName();
            } catch (PlanNotFoundException | TechnicalManagementException e) {
                log.debug("Could not resolve plan name for planId={}", metrics.getPlanId(), e);
            }
        }

        if (metrics.getApplicationId() != null) {
            try {
                applicationName = applicationCrudService
                    .findById(metrics.getApplicationId(), executionContext.getEnvironmentId())
                    .getName();
            } catch (ApplicationNotFoundException | TechnicalManagementException e) {
                log.debug("Could not resolve application name for applicationId={}", metrics.getApplicationId(), e);
            }
        }

        if (metrics.getGateway() != null) {
            try {
                var instance = instanceQueryService.findById(executionContext, metrics.getGateway());
                gatewayHostname = instance.getHostname();
                gatewayIp = instance.getIp();
            } catch (InstanceNotFoundException e) {
                log.debug("Could not resolve gateway hostname for gatewayId={}", metrics.getGateway(), e);
            }
        }

        return new ResolvedNames(planName, applicationName, gatewayHostname, gatewayIp);
    }

    private static HttpPayload mapRequest(ConnectionLogDetail.Request request) {
        if (request == null) {
            return null;
        }
        return HttpPayload.builder()
            .method(request.getMethod())
            .uri(request.getUri())
            .headers(request.getHeaders())
            .body(request.getBody())
            .build();
    }

    private static HttpPayload mapResponse(ConnectionLogDetail.Response response) {
        if (response == null) {
            return null;
        }
        return HttpPayload.builder().status(response.getStatus()).headers(response.getHeaders()).body(response.getBody()).build();
    }

    private SearchLogsFilters buildSearchFilters(LogsSearchQuery query) {
        var builder = SearchLogsFilters.builder();
        builder.apiIds(query.apiIds());
        builder.from(query.from());
        builder.to(query.to());

        Set<String> applicationIds = new HashSet<>();
        Set<String> planIds = new HashSet<>();
        Set<HttpMethod> methods = new HashSet<>();
        Set<String> mcpMethods = new HashSet<>();
        Set<Integer> statuses = new HashSet<>();
        List<SearchLogsFilters.StatusRange> statusRanges = new ArrayList<>();
        Set<String> statusCodeGroups = new HashSet<>();
        var statusAccumulator = new NumericRangeAccumulator<Integer>("HTTP_STATUS");
        Set<String> entrypointIds = new HashSet<>();
        Set<String> requestIds = new HashSet<>();
        Set<String> transactionIds = new HashSet<>();
        Set<String> errorKeys = new HashSet<>();
        String bodyText = null;
        Set<String> apiProductIds = new HashSet<>();
        Set<String> llmProxyModels = new HashSet<>();
        Set<String> llmProxyProviders = new HashSet<>();
        Set<String> mcpProxyTools = new HashSet<>();
        Set<String> mcpProxyResources = new HashSet<>();
        Set<String> mcpProxyPrompts = new HashSet<>();
        String uri = null;
        List<Range> responseTimeRanges = new ArrayList<>();
        var responseTimeAccumulator = new NumericRangeAccumulator<Long>("HTTP_GATEWAY_RESPONSE_TIME");

        for (FilterCondition condition : query.conditions()) {
            var values = condition.values() != null ? condition.values() : List.<String>of();
            switch (condition.name()) {
                case "APPLICATION" -> applicationIds.addAll(values);
                case "PLAN" -> planIds.addAll(values);
                case "HTTP_METHOD" -> methods.addAll(values.stream().map(this::toHttpMethod).toList());
                case "HTTP_STATUS" -> {
                    if (!values.isEmpty()) {
                        if (condition.operator() == FilterOperator.EQ) {
                            values.forEach(value -> statuses.add(parseHttpStatus(value)));
                        } else {
                            applyNumericBound(statusAccumulator, condition.operator(), parseHttpStatus(values.getFirst()));
                        }
                    }
                }
                case "HTTP_STATUS_CODE_GROUP" -> addStatusCodeGroupFilter(values, statusCodeGroups);
                case "URI" -> {
                    if (!values.isEmpty()) uri = values.getFirst();
                }
                case "HTTP_GATEWAY_RESPONSE_TIME" -> {
                    if (!values.isEmpty()) {
                        applyNumericBound(responseTimeAccumulator, condition.operator(), parseResponseTime(values.getFirst()));
                    }
                }
                case "ENTRYPOINT" -> entrypointIds.addAll(values);
                case "MCP_PROXY_METHOD" -> mcpMethods.addAll(values);
                case "LLM_PROXY_MODEL" -> llmProxyModels.addAll(values);
                case "LLM_PROXY_PROVIDER" -> llmProxyProviders.addAll(values);
                case "MCP_PROXY_TOOL" -> mcpProxyTools.addAll(values);
                case "MCP_PROXY_RESOURCE" -> mcpProxyResources.addAll(values);
                case "MCP_PROXY_PROMPT" -> mcpProxyPrompts.addAll(values);
                case "REQUEST_ID" -> requestIds.addAll(values);
                case "TRANSACTION_ID" -> transactionIds.addAll(values);
                case "ERROR_KEY" -> errorKeys.addAll(values);
                case "API_PRODUCT" -> apiProductIds.addAll(values);
                case "PAYLOAD" -> {
                    if (values.isEmpty() || values.stream().allMatch(value -> value == null || value.isBlank())) {
                        throw UnsupportedObservabilityFilterException.blankValue("PAYLOAD");
                    }
                    bodyText = values
                        .stream()
                        .filter(v -> v != null && !v.isBlank())
                        .findFirst()
                        .orElse(null);
                }
                default -> throw UnsupportedObservabilityFilterException.searchTranslationNotSupported(condition.name());
            }
        }

        if (statusAccumulator.hasValue()) {
            var b = statusAccumulator.build();
            statusRanges.add(SearchLogsFilters.StatusRange.builder().gte(b.gte()).lte(b.lte()).build());
        }

        if (responseTimeAccumulator.hasValue()) {
            var b = responseTimeAccumulator.build();
            responseTimeRanges.add(new Range(b.gte(), b.lte()));
        }

        builder.applicationIds(applicationIds);
        builder.planIds(planIds);
        builder.methods(methods);
        builder.mcpMethods(mcpMethods);
        builder.statuses(statuses);
        builder.statusRanges(statusRanges);
        builder.statusCodeGroups(statusCodeGroups);
        builder.entrypointIds(entrypointIds);
        builder.requestIds(requestIds);
        builder.transactionIds(transactionIds);
        builder.errorKeys(errorKeys);
        builder.apiProductIds(apiProductIds);
        builder.llmProxyModels(llmProxyModels);
        builder.llmProxyProviders(llmProxyProviders);
        builder.mcpProxyTools(mcpProxyTools);
        builder.mcpProxyResources(mcpProxyResources);
        builder.mcpProxyPrompts(mcpProxyPrompts);
        builder.uri(uri);
        builder.bodyText(bodyText);
        builder.responseTimeRanges(responseTimeRanges);

        return builder.build();
    }

    private LogEntry mapToLogEntry(BaseConnectionLog log, Map<String, ApiReference> apisById) {
        var apiRef = apisById != null ? apisById.get(log.getApiId()) : null;
        return LogEntry.builder()
            .apiId(log.getApiId())
            .apiName(apiRef != null ? apiRef.name() : null)
            .apiType(apiRef != null ? apiRef.apiType() : null)
            .timestamp(parseTimestamp(log.getTimestamp()))
            .requestId(log.getRequestId())
            .method(log.getMethod() != null ? log.getMethod().name() : null)
            .clientIdentifier(log.getClientIdentifier())
            .planId(log.getPlanId())
            .applicationId(log.getApplicationId())
            .transactionId(log.getTransactionId())
            .status(log.getStatus())
            .requestEnded(log.isRequestEnded())
            .gatewayResponseTime(safeToInteger(log.getGatewayResponseTime()))
            .gateway(log.getGateway())
            .uri(log.getUri())
            .endpoint(log.getEndpoint())
            .message(log.getMessage())
            .errorKey(log.getErrorKey())
            .errorComponentName(log.getErrorComponentName())
            .errorComponentType(log.getErrorComponentType())
            .warnings(mapWarnings(log.getWarnings()))
            .additionalMetrics(log.getAdditionalMetrics() != null ? log.getAdditionalMetrics() : Map.of())
            .mcpMethod(log.getMcpMethod())
            .apiProductId(log.getApiProductId())
            .build();
    }

    private List<LogEntry> enrichWithNames(ExecutionContext executionContext, List<LogEntry> entries) {
        var planIds = entries.stream().map(LogEntry::planId).filter(Objects::nonNull).distinct().toList();
        var planNameById = planIds.isEmpty()
            ? Map.<String, String>of()
            : planCrudService
                .findByIds(planIds)
                .stream()
                .filter(p -> p.getName() != null)
                .collect(Collectors.toMap(Plan::getId, Plan::getName, (a, b) -> a));

        var appIds = entries.stream().map(LogEntry::applicationId).filter(Objects::nonNull).distinct().toList();
        var appNameById = appIds.isEmpty()
            ? Map.<String, String>of()
            : applicationCrudService
                .findByIds(appIds, executionContext.getEnvironmentId())
                .stream()
                .filter(a -> a.getName() != null)
                .collect(Collectors.toMap(BaseApplicationEntity::getId, BaseApplicationEntity::getName, (a, b) -> a));

        var gatewayIds = entries.stream().map(LogEntry::gateway).filter(Objects::nonNull).distinct().toList();
        var gatewayHostnameById = gatewayIds.isEmpty()
            ? Map.<String, String>of()
            : instanceQueryService
                .findByIds(executionContext, gatewayIds)
                .stream()
                .filter(i -> i.getHostname() != null)
                .collect(Collectors.toMap(BaseInstance::getId, BaseInstance::getHostname, (a, b) -> a));

        var apiProductIds = entries.stream().map(LogEntry::apiProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        var apiProductNameById = apiProductIds.isEmpty()
            ? Map.<String, String>of()
            : apiProductQueryService
                .findByEnvironmentIdAndIdIn(executionContext.getEnvironmentId(), apiProductIds)
                .stream()
                .filter(p -> p.getName() != null)
                .collect(Collectors.toMap(ApiProduct::getId, ApiProduct::getName, (a, b) -> a));

        return entries
            .stream()
            .map(entry ->
                entry
                    .toBuilder()
                    .planName(lookup(planNameById, entry.planId()))
                    .applicationName(lookup(appNameById, entry.applicationId()))
                    .gatewayHostname(lookup(gatewayHostnameById, entry.gateway()))
                    .apiProductName(entry.apiProductId() == null ? "Standalone API" : lookup(apiProductNameById, entry.apiProductId()))
                    .build()
            )
            .toList();
    }

    /**
     * Null-safe map lookup: the enrichment maps fall back to {@link Map#of()} (immutable, which throws
     * on {@code get(null)}) when there is nothing to resolve, and a log row may carry a null
     * plan/application/gateway id (e.g. anonymous calls). Guarding here keeps a single missing id from
     * failing the whole search.
     */
    private static String lookup(Map<String, String> namesById, String id) {
        return id == null ? null : namesById.get(id);
    }

    private static Instant parseTimestamp(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(timestamp).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Integer safeToInteger(long value) {
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private HttpMethod toHttpMethod(String method) {
        try {
            return HttpMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            return HttpMethod.OTHER;
        }
    }

    private static List<LogEntryWarning> mapWarnings(List<ConnectionDiagnosticModel> warnings) {
        if (warnings == null) {
            return List.of();
        }
        return warnings
            .stream()
            .map(w -> new LogEntryWarning(w.getComponentType(), w.getComponentName(), w.getKey(), w.getMessage()))
            .toList();
    }

    private static void addStatusCodeGroupFilter(List<String> values, Set<String> statusCodeGroups) {
        for (String group : values) {
            HttpStatusCodeGroups.resolve(group).orElseThrow(() ->
                new ValidationDomainException("Unknown HTTP status code group: " + group)
            );
            statusCodeGroups.add(group.toUpperCase(java.util.Locale.ROOT));
        }
    }

    private static long parseResponseTime(String value) {
        if (value == null) {
            throw new ValidationDomainException("Invalid response time value: null");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationDomainException("Invalid response time value: " + value);
        }
    }

    private static int parseHttpStatus(String value) {
        if (value == null) {
            throw new ValidationDomainException("Invalid HTTP status code value: null");
        }
        int status;
        try {
            status = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationDomainException("Invalid HTTP status code value: " + value);
        }
        if (status < HTTP_STATUS_MIN || status > HTTP_STATUS_MAX) {
            throw new ValidationDomainException(
                "Invalid HTTP status code: " + value + ". Must be between " + HTTP_STATUS_MIN + " and " + HTTP_STATUS_MAX + "."
            );
        }
        return status;
    }

    private static <T extends Comparable<T>> void applyNumericBound(
        NumericRangeAccumulator<T> accumulator,
        FilterOperator operator,
        T value
    ) {
        switch (operator) {
            case GTE -> accumulator.setGte(value);
            case LTE -> accumulator.setLte(value);
            case EQ -> accumulator.setBoth(value);
            default -> throw new IllegalStateException("Operator " + operator + " should have been rejected by filter validation");
        }
    }

    private static io.gravitee.apim.core.audit.model.AuditInfo currentAuditInfo(String organizationId, String environmentId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        io.gravitee.apim.core.audit.model.AuditActor actor;
        if (authentication != null && authentication.getName() != null) {
            actor = io.gravitee.apim.core.audit.model.AuditActor.builder().userId(authentication.getName()).build();
        } else {
            actor = io.gravitee.apim.core.audit.model.AuditActor.builder().userId("unknown").build();
        }
        return io.gravitee.apim.core.audit.model.AuditInfo.builder()
            .organizationId(organizationId)
            .environmentId(environmentId)
            .actor(actor)
            .build();
    }

    private static io.gravitee.gamma.rest.core.observability.filter.model.ApiType toGammaApiType(
        io.gravitee.definition.model.v4.ApiType definitionType
    ) {
        if (definitionType == null) {
            return null;
        }
        return switch (definitionType) {
            case PROXY -> io.gravitee.gamma.rest.core.observability.filter.model.ApiType.HTTP_PROXY;
            case MESSAGE -> io.gravitee.gamma.rest.core.observability.filter.model.ApiType.MESSAGE;
            case LLM_PROXY -> io.gravitee.gamma.rest.core.observability.filter.model.ApiType.LLM;
            case MCP_PROXY -> io.gravitee.gamma.rest.core.observability.filter.model.ApiType.MCP;
            case A2A_PROXY -> io.gravitee.gamma.rest.core.observability.filter.model.ApiType.A2A;
            case NATIVE -> io.gravitee.gamma.rest.core.observability.filter.model.ApiType.NATIVE;
            case EDGE -> io.gravitee.gamma.rest.core.observability.filter.model.ApiType.EDGE;
            case AUTHZ -> null; // No Gamma observability equivalent — AUTHZ APIs are excluded from logs scope
        };
    }
}
