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
package io.gravitee.apim.core.logs_engine.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.apim.core.logs_engine.domain_service.FilterContext;
import io.gravitee.apim.core.logs_engine.model.ApiLog;
import io.gravitee.apim.core.logs_engine.model.ApiLogDiagnostic;
import io.gravitee.apim.core.logs_engine.model.ArrayFilter;
import io.gravitee.apim.core.logs_engine.model.BaseApplication;
import io.gravitee.apim.core.logs_engine.model.BasePlan;
import io.gravitee.apim.core.logs_engine.model.Filter;
import io.gravitee.apim.core.logs_engine.model.FilterName;
import io.gravitee.apim.core.logs_engine.model.HttpMethod;
import io.gravitee.apim.core.logs_engine.model.NumericFilter;
import io.gravitee.apim.core.logs_engine.model.Operator;
import io.gravitee.apim.core.logs_engine.model.Pagination;
import io.gravitee.apim.core.logs_engine.model.SearchLogsRequest;
import io.gravitee.apim.core.logs_engine.model.SearchLogsResponse;
import io.gravitee.apim.core.logs_engine.model.StringFilter;
import io.gravitee.apim.core.logs_engine.model.TimeRange;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.analytics.Range;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionDiagnosticModel;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@UseCase
public class SearchEnvironmentLogsUseCase {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PER_PAGE = 10;
    private final ConnectionLogsCrudService connectionLogsCrudService;
    private final UserContextLoader userContextLoader;

    public SearchEnvironmentLogsUseCase(ConnectionLogsCrudService connectionLogsCrudService, UserContextLoader userContextLoader) {
        this.connectionLogsCrudService = connectionLogsCrudService;
        this.userContextLoader = userContextLoader;
    }

    public record Input(AuditInfo auditInfo, SearchLogsRequest request) {}

    public record Output(SearchLogsResponse response) {}

    public Output execute(Input input) {
        var executionContext = new ExecutionContext(input.auditInfo.organizationId(), input.auditInfo.environmentId());
        var pageable = buildPageable(input.request);

        var userContext = userContextLoader.loadApis(new UserContext(input.auditInfo));

        var searchFilters = buildFilters(userContext, input.request);

        if (searchFilters.apiIds().isEmpty()) {
            return new Output(new SearchLogsResponse(List.of(), new Pagination(0, 0, 0, 0, 0L)));
        }

        var result = connectionLogsCrudService.searchApiConnectionLogs(
            executionContext,
            searchFilters,
            pageable,
            List.of(DefinitionVersion.V4)
        );

        return new Output(mapResponse(result, pageable));
    }

    private Pageable buildPageable(SearchLogsRequest request) {
        var page = Optional.ofNullable(request.page()).orElse(DEFAULT_PAGE);
        var perPage = Optional.ofNullable(request.perPage()).orElse(DEFAULT_PER_PAGE);
        return new PageableImpl(page, perPage);
    }

    private SearchLogsFilters buildFilters(UserContext userContext, SearchLogsRequest request) {
        var filterContext = new FilterContext();

        if (request.filters() != null) {
            for (Filter filter : request.filters()) {
                var instance = filter.actualInstance();
                if (instance instanceof StringFilter stringFilter) {
                    applyStringFilter(stringFilter, filterContext);
                } else if (instance instanceof ArrayFilter arrayFilter) {
                    applyArrayFilter(arrayFilter, filterContext);
                } else if (instance instanceof NumericFilter numericFilter) {
                    applyNumericFilter(numericFilter, filterContext);
                }
            }
        }

        Set<String> apiIds = userContext.apis().orElseGet(Collections::emptyList).stream().map(Api::getId).collect(Collectors.toSet());
        filterContext.limitByApiIds(apiIds);

        var builder = SearchLogsFilters.builder();
        builder.apiIds(filterContext.apiIds().orElseGet(Collections::emptySet));
        builder.applicationIds(filterContext.applicationIds().orElseGet(Collections::emptySet));
        builder.planIds(filterContext.planIds().orElseGet(Collections::emptySet));
        builder.methods(
            filterContext
                .methods()
                .orElseGet(Collections::emptySet)
                .stream()
                .map(m -> io.gravitee.common.http.HttpMethod.valueOf(m.name()))
                .collect(Collectors.toSet())
        );
        builder.statuses(filterContext.statuses().orElseGet(Collections::emptySet));
        builder.entrypointIds(filterContext.entrypointIds().orElseGet(Collections::emptySet));
        builder.mcpMethods(filterContext.mcpMethods().orElseGet(Collections::emptySet));
        builder.requestIds(filterContext.requestIds().orElseGet(Collections::emptySet));
        builder.transactionIds(filterContext.transactionIds().orElseGet(Collections::emptySet));
        builder.uri(filterContext.uri().orElse(null));
        builder.responseTimeRanges(buildResponseTimeRanges(filterContext));

        if (request.timeRange() != null) {
            if (isTimeRangeInvalid(request.timeRange())) {
                throw new ValidationDomainException("Invalid time range: 'from' must be before 'to'.");
            }

            builder.from(toEpochMilli(request.timeRange().from()));
            builder.to(toEpochMilli(request.timeRange().to()));
        }
        return builder.build();
    }

    private static boolean isTimeRangeInvalid(TimeRange timeRange) {
        return (timeRange.from() != null && timeRange.to() != null && timeRange.from().isAfter(timeRange.to()));
    }

    private void applyStringFilter(StringFilter filter, FilterContext filterContext) {
        if (filter.operator() == Operator.EQ) {
            updateFilterIds(filter.name(), filterContext, Set.of(filter.value()));
            return;
        }

        clearFilterIds(filter, filterContext);
    }

    private void clearFilterIds(StringFilter filter, FilterContext filterContext) {
        updateFilterIds(filter.name(), filterContext, Collections.emptySet());
    }

    private void applyArrayFilter(ArrayFilter filter, FilterContext filterContext) {
        // URI only supports EQ operator from StringFilter, ignore all ArrayFilters for
        // URI
        if (filter.name() == FilterName.URI) {
            return;
        }

        if (filter.operator() == Operator.IN) {
            updateFilterIds(filter.name(), filterContext, filter.value().stream().map(String::valueOf).collect(Collectors.toSet()));
            return;
        }

        updateFilterIds(filter.name(), filterContext, Collections.emptySet());
    }

    private void applyNumericFilter(NumericFilter filter, FilterContext filterContext) {
        if (filter.name() == FilterName.RESPONSE_TIME) {
            if (filter.value() == null) {
                throw new ValidationDomainException("Filter RESPONSE_TIME requires a non-null value");
            }
            if (filter.value() < 0) {
                throw new ValidationDomainException("Filter RESPONSE_TIME does not accept negative values.");
            }
            if (filter.operator() == Operator.GTE) {
                filterContext.limitByResponseTimeFrom(filter.value().longValue());
            } else if (filter.operator() == Operator.LTE) {
                filterContext.limitByResponseTimeTo(filter.value().longValue());
            }
        }
    }

    private List<Range> buildResponseTimeRanges(FilterContext filterContext) {
        var from = filterContext.responseTimeFrom().orElse(null);
        var to = filterContext.responseTimeTo().orElse(null);
        if (from == null && to == null) {
            return List.of();
        }
        if (from != null && to != null && from > to) {
            throw new ValidationDomainException("Invalid RESPONSE_TIME range: 'from' (gte) must not be greater than 'to' (lte).");
        }
        return List.of(new Range(from, to));
    }

    private void updateFilterIds(FilterName name, FilterContext filterContext, Set<String> ids) {
        switch (name) {
            case API -> filterContext.limitByApiIds(ids);
            case APPLICATION -> filterContext.limitByApplicationIds(ids);
            case PLAN -> filterContext.limitByPlanIds(ids);
            case HTTP_METHOD -> filterContext.limitByHttpMethods(ids.stream().map(this::httpMethod).collect(Collectors.toSet()));
            case HTTP_STATUS -> filterContext.limitByHttpStatuses(ids.stream().map(Integer::valueOf).collect(Collectors.toSet()));
            case ENTRYPOINT -> filterContext.limitByEntrypointIds(ids);
            case MCP_METHOD -> filterContext.limitByMcpMethods(ids);
            case TRANSACTION_ID -> filterContext.limitByTransactionIds(ids);
            case REQUEST_ID -> filterContext.limitByRequestIds(ids);
            case URI -> {
                // For URI, only EQ filters are supported, so we take the first (and presumably
                // only) value
                if (!ids.isEmpty()) {
                    filterContext.limitByUri(ids.iterator().next());
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + name);
        }
    }

    private io.gravitee.apim.core.logs_engine.model.HttpMethod httpMethod(String method) {
        try {
            return io.gravitee.apim.core.logs_engine.model.HttpMethod.valueOf(method);
        } catch (IllegalArgumentException iae) {
            // Unknown HTTP method — mapped to OTHER
            return io.gravitee.apim.core.logs_engine.model.HttpMethod.OTHER;
        }
    }

    private long toEpochMilli(OffsetDateTime odt) {
        return odt.toInstant().toEpochMilli();
    }

    private SearchLogsResponse mapResponse(
        io.gravitee.rest.api.model.v4.log.SearchLogsResponse<BaseConnectionLog> source,
        Pageable pageable
    ) {
        final List<ApiLog> apiLogs = source.logs() == null ? List.of() : source.logs().stream().map(this::mapApiLog).toList();
        return new SearchLogsResponse(
            apiLogs,
            new Pagination(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) source.total() / pageable.getPageSize()),
                apiLogs.size(),
                source.total()
            )
        );
    }

    private ApiLog mapApiLog(BaseConnectionLog item) {
        return new ApiLog(
            item.getApiId(),
            toOffsetDateTime(item.getTimestamp()),
            item.getRequestId(),
            item.getRequestId(),
            mapHttpMethod(item.getMethod()),
            item.getClientIdentifier(),
            mapPlan(item.getPlanId()),
            mapApplication(item.getApplicationId()),
            item.getTransactionId(),
            item.getStatus(),
            item.isRequestEnded(),
            safeToInteger(item.getGatewayResponseTime()),
            item.getGateway(),
            item.getUri(),
            item.getEndpoint(),
            item.getMessage(),
            item.getErrorKey(),
            item.getErrorComponentName(),
            item.getErrorComponentType(),
            mapWarnings(item.getWarnings()),
            item.getAdditionalMetrics() != null ? item.getAdditionalMetrics() : Map.of()
        );
    }

    private OffsetDateTime toOffsetDateTime(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(timestamp);
        } catch (DateTimeParseException e) {
            // Malformed timestamp — treat as null
            return null;
        }
    }

    private HttpMethod mapHttpMethod(io.gravitee.common.http.HttpMethod method) {
        return method == null ? null : HttpMethod.valueOf(method.name());
    }

    private BasePlan mapPlan(String planId) {
        if (planId == null) {
            return null;
        }
        return new BasePlan(planId, null, null, null, null, null);
    }

    private BaseApplication mapApplication(String applicationId) {
        if (applicationId == null) {
            return null;
        }
        return new BaseApplication(applicationId, null, null, null, null, null, null);
    }

    private Integer safeToInteger(long value) {
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private List<ApiLogDiagnostic> mapWarnings(List<ConnectionDiagnosticModel> warnings) {
        if (warnings == null) {
            return List.of();
        }
        return warnings
            .stream()
            .map(warning ->
                new ApiLogDiagnostic(warning.getComponentType(), warning.getComponentName(), warning.getKey(), warning.getMessage())
            )
            .collect(Collectors.toList());
    }
}
