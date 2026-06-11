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

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
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
import io.gravitee.gamma.rest.core.observability.logs.model.LogEntry;
import io.gravitee.gamma.rest.core.observability.logs.model.LogEntryWarning;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsPage;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsSearchQuery;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.analytics.Range;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionDiagnosticModel;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Adapter that delegates to the APIM platform's connection-log search infrastructure
 * ({@link ConnectionLogsCrudService}, {@link UserContextLoader}) and enriches the raw results
 * with display names (plans, applications, gateways, API products).
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class ObservabilityLogsDataPortAdapter implements ObservabilityLogsDataPort {

    private final ConnectionLogsCrudService connectionLogsCrudService;
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
            .map(log -> mapToLogEntry(log, query.apiNamesById()))
            .toList();
        var enriched = enrichWithNames(executionContext, entries);

        return new LogsPage(enriched, result.total());
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
        Set<String> entrypointIds = new HashSet<>();
        Set<String> requestIds = new HashSet<>();
        Set<String> transactionIds = new HashSet<>();
        Set<String> errorKeys = new HashSet<>();
        Set<String> apiProductIds = new HashSet<>();
        String uri = null;
        List<Range> responseTimeRanges = new ArrayList<>();
        Long responseTimeFrom = null;
        Long responseTimeTo = null;

        for (FilterCondition condition : query.conditions()) {
            var values = condition.values() != null ? condition.values() : List.<String>of();
            switch (condition.name()) {
                case "APPLICATION" -> applicationIds.addAll(values);
                case "PLAN" -> planIds.addAll(values);
                case "HTTP_METHOD" -> methods.addAll(values.stream().map(this::toHttpMethod).toList());
                case "HTTP_STATUS" -> statuses.addAll(values.stream().map(Integer::valueOf).toList());
                case "URI" -> {
                    if (!values.isEmpty()) uri = values.getFirst();
                }
                case "HTTP_GATEWAY_RESPONSE_TIME" -> {
                    if (!values.isEmpty()) {
                        long val = Long.parseLong(values.getFirst());
                        if (condition.operator() == FilterOperator.GTE) responseTimeFrom = val;
                        else if (condition.operator() == FilterOperator.LTE) responseTimeTo = val;
                    }
                }
                case "ENTRYPOINT" -> entrypointIds.addAll(values);
                case "MCP_PROXY_METHOD" -> mcpMethods.addAll(values);
                case "REQUEST_ID" -> requestIds.addAll(values);
                case "TRANSACTION_ID" -> transactionIds.addAll(values);
                case "ERROR_KEY" -> errorKeys.addAll(values);
                case "API_PRODUCT" -> apiProductIds.addAll(values);
                default -> throw UnsupportedObservabilityFilterException.searchTranslationNotSupported(condition.name());
            }
        }

        if (responseTimeFrom != null || responseTimeTo != null) {
            responseTimeRanges.add(new Range(responseTimeFrom, responseTimeTo));
        }

        builder.applicationIds(applicationIds);
        builder.planIds(planIds);
        builder.methods(methods);
        builder.mcpMethods(mcpMethods);
        builder.statuses(statuses);
        builder.entrypointIds(entrypointIds);
        builder.requestIds(requestIds);
        builder.transactionIds(transactionIds);
        builder.errorKeys(errorKeys);
        builder.apiProductIds(apiProductIds);
        builder.uri(uri);
        builder.responseTimeRanges(responseTimeRanges);

        return builder.build();
    }

    private LogEntry mapToLogEntry(BaseConnectionLog log, Map<String, String> apiNamesById) {
        return LogEntry.builder()
            .apiId(log.getApiId())
            .apiName(apiNamesById.getOrDefault(log.getApiId(), null))
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
                    .planName(planNameById.get(entry.planId()))
                    .applicationName(appNameById.get(entry.applicationId()))
                    .gatewayHostname(gatewayHostnameById.get(entry.gateway()))
                    .apiProductName(entry.apiProductId() == null ? "Standalone API" : apiProductNameById.get(entry.apiProductId()))
                    .build()
            )
            .toList();
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
            case NATIVE -> io.gravitee.gamma.rest.core.observability.filter.model.ApiType.NATIVE;
            case EDGE -> io.gravitee.gamma.rest.core.observability.filter.model.ApiType.EDGE;
            case A2A_PROXY -> null; // No Gamma equivalent yet — APIs with this type are excluded from scope
        };
    }
}
