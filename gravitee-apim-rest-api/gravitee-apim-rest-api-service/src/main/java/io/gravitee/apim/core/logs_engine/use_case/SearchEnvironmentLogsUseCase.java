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
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.gateway.model.BaseInstance;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.apim.core.logs_engine.domain_service.FilterContext;
import io.gravitee.apim.core.logs_engine.domain_service.LogNamesPostProcessor;
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
import io.gravitee.apim.core.logs_engine.model.StatusCodeGroups;
import io.gravitee.apim.core.logs_engine.model.StringFilter;
import io.gravitee.apim.core.logs_engine.model.TimeRange;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.BaseApplicationEntity;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UseCase
public class SearchEnvironmentLogsUseCase {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PER_PAGE = 10;
    private final ConnectionLogsCrudService connectionLogsCrudService;
    private final UserContextLoader userContextLoader;
    private final LogNamesPostProcessor logNamesPostProcessor;
    private final PlanCrudService planCrudService;
    private final ApplicationCrudService applicationCrudService;
    private final InstanceQueryService instanceQueryService;
    private final ApiProductQueryService apiProductQueryService;

    public SearchEnvironmentLogsUseCase(
        ConnectionLogsCrudService connectionLogsCrudService,
        UserContextLoader userContextLoader,
        LogNamesPostProcessor logNamesPostProcessor,
        PlanCrudService planCrudService,
        ApplicationCrudService applicationCrudService,
        InstanceQueryService instanceQueryService,
        ApiProductQueryService apiProductQueryService
    ) {
        this.connectionLogsCrudService = connectionLogsCrudService;
        this.userContextLoader = userContextLoader;
        this.logNamesPostProcessor = logNamesPostProcessor;
        this.planCrudService = planCrudService;
        this.applicationCrudService = applicationCrudService;
        this.instanceQueryService = instanceQueryService;
        this.apiProductQueryService = apiProductQueryService;
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

        var response = mapResponse(result, pageable);

        var enrichedContext = loadNames(executionContext, userContext, response);

        var namedResponse = logNamesPostProcessor.mapLogNames(enrichedContext, response);

        return new Output(enrichWithApiProductNames(executionContext, namedResponse));
    }

    private Pageable buildPageable(SearchLogsRequest request) {
        var page = Optional.ofNullable(request.page()).orElse(DEFAULT_PAGE);
        var perPage = Optional.ofNullable(request.perPage()).orElse(DEFAULT_PER_PAGE);
        return new PageableImpl(page, perPage);
    }

    private UserContext loadNames(ExecutionContext executionContext, UserContext userContext, SearchLogsResponse response) {
        var planIds = response
            .data()
            .stream()
            .map(ApiLog::plan)
            .filter(Objects::nonNull)
            .map(BasePlan::id)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        var planNameById = planIds.isEmpty()
            ? Map.<String, String>of()
            : planCrudService
                .findByIds(planIds)
                .stream()
                .filter(p -> p.getName() != null)
                .collect(Collectors.toMap(Plan::getId, Plan::getName, (a, b) -> a));

        var appIds = response
            .data()
            .stream()
            .map(ApiLog::application)
            .filter(Objects::nonNull)
            .map(BaseApplication::id)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        var applicationNameById = appIds.isEmpty()
            ? Map.<String, String>of()
            : applicationCrudService
                .findByIds(appIds, executionContext.getEnvironmentId())
                .stream()
                .filter(a -> a.getName() != null)
                .collect(Collectors.toMap(BaseApplicationEntity::getId, BaseApplicationEntity::getName, (a, b) -> a));

        var gatewayIds = response.data().stream().map(ApiLog::gateway).filter(Objects::nonNull).distinct().toList();

        var gatewayHostnameById = gatewayIds.isEmpty()
            ? Map.<String, String>of()
            : instanceQueryService
                .findByIds(executionContext, gatewayIds)
                .stream()
                .filter(i -> i.getHostname() != null)
                .collect(Collectors.toMap(BaseInstance::getId, BaseInstance::getHostname, (a, b) -> a));

        return userContext
            .withPlanNameById(planNameById)
            .withApplicationNameById(applicationNameById)
            .withGatewayHostnameById(gatewayHostnameById);
    }

    private SearchLogsFilters buildFilters(UserContext userContext, SearchLogsRequest request) {
        var filterContext = new FilterContext();
        // API_TYPE narrows which APIs are in scope rather than adding a clause, so it is read before the
        // loop and excluded from it — the search is scoped by api id, and the type only exists upstream.
        var requestedApiTypes = requestedApiTypes(request);

        if (request.filters() != null) {
            for (Filter filter : request.filters()) {
                var instance = filter.actualInstance();
                if (filterName(instance) == FilterName.API_TYPE) {
                    continue;
                }
                if (instance instanceof StringFilter stringFilter) {
                    applyStringFilter(stringFilter, filterContext);
                } else if (instance instanceof ArrayFilter arrayFilter) {
                    applyArrayFilter(arrayFilter, filterContext);
                } else if (instance instanceof NumericFilter numericFilter) {
                    applyNumericFilter(numericFilter, filterContext);
                }
            }
        }

        Set<String> apiIds = userContext
            .apis()
            .orElseGet(Collections::emptyList)
            .stream()
            .filter(api -> isWantedHttpApi(api.getType()))
            .filter(api -> requestedApiTypes.map(types -> types.contains(api.getType())).orElse(true))
            .map(Api::getId)
            .collect(Collectors.toSet());
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
        builder.statusCodeGroups(filterContext.statusCodeGroups().orElseGet(Collections::emptySet));
        builder.statusRanges(buildStatusRanges(filterContext));
        builder.entrypointIds(filterContext.entrypointIds().orElseGet(Collections::emptySet));
        builder.mcpMethods(filterContext.mcpMethods().orElseGet(Collections::emptySet));
        builder.requestIds(filterContext.requestIds().orElseGet(Collections::emptySet));
        builder.transactionIds(filterContext.transactionIds().orElseGet(Collections::emptySet));
        builder.uri(filterContext.uri().orElse(null));
        builder.responseTimeRanges(buildResponseTimeRanges(filterContext));
        builder.errorKeys(filterContext.errorKeys().orElseGet(Collections::emptySet));
        builder.nativeClientIds(filterContext.nativeClientIds().orElseGet(Collections::emptySet));
        builder.nativeClientSoftwareNames(filterContext.nativeClientSoftwareNames().orElseGet(Collections::emptySet));
        builder.apiProductIds(filterContext.apiProductIds().orElseGet(Collections::emptySet));
        builder.tenants(filterContext.tenants().orElseGet(Collections::emptySet));
        builder.bodyText(filterContext.bodyText().orElse(null));

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
        if (filter.name() == FilterName.PAYLOAD) {
            if (filter.operator() != Operator.CONTAINS) {
                throw new ValidationDomainException("Filter PAYLOAD only supports operator CONTAINS.");
            }
            if (filter.value() == null || filter.value().isBlank()) {
                throw new ValidationDomainException("Filter PAYLOAD requires a non-blank value.");
            }
            filterContext.limitByBodyText(filter.value());
            return;
        }

        if (filter.operator() == Operator.EQ) {
            updateFilterIds(filter.name(), filterContext, Set.of(filter.value()));
            return;
        }

        clearFilterIds(filter, filterContext);
    }

    private void clearFilterIds(StringFilter filter, FilterContext filterContext) {
        updateFilterIds(filter.name(), filterContext, Collections.emptySet());
    }

    /**
     * Filters the engine holds one value for, so a list cannot be expressed: {@code URI} is a single path
     * pattern, {@code RESPONSE_TIME} a single bound. Taking the first element of an unordered set would keep
     * an arbitrary one and drop the rest — silently, and differently between runs.
     */
    private static final Set<FilterName> SINGLE_VALUED_FILTERS = Set.of(FilterName.URI, FilterName.RESPONSE_TIME);

    private void applyArrayFilter(ArrayFilter filter, FilterContext filterContext) {
        // Say so instead of returning early: silently ignoring it is what made the catalog and the search
        // disagree (APIM-14817).
        if (SINGLE_VALUED_FILTERS.contains(filter.name())) {
            throw new ValidationDomainException("Filter " + filter.name() + " does not support operator " + filter.operator() + ".");
        }

        if (filter.operator() == Operator.IN) {
            updateFilterIds(filter.name(), filterContext, filter.value().stream().map(String::valueOf).collect(Collectors.toSet()));
            return;
        }

        updateFilterIds(filter.name(), filterContext, Collections.emptySet());
    }

    private void applyNumericFilter(NumericFilter filter, FilterContext filterContext) {
        switch (filter.name()) {
            case RESPONSE_TIME -> {
                var value = requirePositiveValue(filter);
                // EQ collapses to a single-point range: the engine only knows how to bound, not to match exactly.
                switch (filter.operator()) {
                    case GTE -> filterContext.limitByResponseTimeFrom(value.longValue());
                    case LTE -> filterContext.limitByResponseTimeTo(value.longValue());
                    case EQ -> {
                        filterContext.limitByResponseTimeFrom(value.longValue());
                        filterContext.limitByResponseTimeTo(value.longValue());
                    }
                    default -> throw new ValidationDomainException(
                        "Filter RESPONSE_TIME does not support operator " + filter.operator() + "."
                    );
                }
            }
            case HTTP_STATUS -> {
                var value = requirePositiveValue(filter).intValue();
                switch (filter.operator()) {
                    case GTE -> filterContext.limitByStatusFrom(value);
                    case LTE -> filterContext.limitByStatusTo(value);
                    // EQ on a status arrives as a StringFilter/ArrayFilter and lands in `statuses`; a numeric EQ
                    // means the same thing, so route it there rather than building a degenerate range.
                    case EQ -> filterContext.limitByHttpStatuses(Set.of(value));
                    default -> throw new ValidationDomainException(
                        "Filter HTTP_STATUS does not support operator " + filter.operator() + "."
                    );
                }
            }
            default -> throw new ValidationDomainException(
                "Filter " + filter.name() + " does not support numeric operator " + filter.operator() + "."
            );
        }
    }

    private static Integer requirePositiveValue(NumericFilter filter) {
        if (filter.value() == null) {
            throw new ValidationDomainException("Filter " + filter.name() + " requires a non-null value");
        }
        if (filter.value() < 0) {
            throw new ValidationDomainException("Filter " + filter.name() + " does not accept negative values.");
        }
        return filter.value();
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

    private List<SearchLogsFilters.StatusRange> buildStatusRanges(FilterContext filterContext) {
        var from = filterContext.statusFrom().orElse(null);
        var to = filterContext.statusTo().orElse(null);
        if (from == null && to == null) {
            return List.of();
        }
        if (from != null && to != null && from > to) {
            throw new ValidationDomainException("Invalid HTTP_STATUS range: 'from' (gte) must not be greater than 'to' (lte).");
        }
        return List.of(SearchLogsFilters.StatusRange.builder().gte(from).lte(to).build());
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
            case ERROR_KEY -> filterContext.limitByErrorKeys(ids);
            // Kafka connection dimensions: they live in additional-metrics rather than at the document root,
            // which the Elasticsearch adapter handles; here they are plain keyword sets like the rest.
            case NATIVE_CLIENT_ID -> filterContext.limitByNativeClientIds(ids);
            case NATIVE_CLIENT_SOFTWARE_NAME -> filterContext.limitByNativeClientSoftwareNames(ids);
            case API_PRODUCT -> filterContext.limitByApiProductIds(ids);
            case TENANT -> filterContext.limitByTenants(ids);
            case HTTP_STATUS_CODE_GROUP -> filterContext.limitByStatusCodeGroups(validateStatusCodeGroups(ids));
            case URI -> {
                if (!ids.isEmpty()) {
                    filterContext.limitByUri(ids.iterator().next());
                }
            }
            // EQ reaches this method as a string — the request schema discriminates on the operator, so a
            // numeric filter only ever carries GTE/LTE. An exact response time is a single-point range.
            case RESPONSE_TIME -> {
                if (!ids.isEmpty()) {
                    var value = parseResponseTime(ids.iterator().next());
                    filterContext.limitByResponseTimeFrom(value);
                    filterContext.limitByResponseTimeTo(value);
                }
            }
            // PAYLOAD is consumed by applyStringFilter, which enforces CONTAINS. Reaching here means some
            // other operator was used, and dropping it silently is the bug this ticket is about.
            case PAYLOAD -> throw new ValidationDomainException("Filter PAYLOAD only supports operator CONTAINS.");
        }
    }

    private static long parseResponseTime(String raw) {
        try {
            var value = Long.parseLong(raw.trim());
            if (value < 0) {
                throw new ValidationDomainException("Filter RESPONSE_TIME does not accept negative values.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new ValidationDomainException("Filter RESPONSE_TIME expects a number, got '" + raw + "'.", e);
        }
    }

    /**
     * Rejects unknown groups instead of letting them through: the Elasticsearch adapter silently drops a
     * clause it cannot resolve, which would turn a typo into a full, unfiltered result set.
     */
    private static Set<String> validateStatusCodeGroups(Set<String> groups) {
        return groups
            .stream()
            .map(group ->
                StatusCodeGroups.canonicalise(group).orElseThrow(() ->
                    new ValidationDomainException(
                        "Unknown HTTP status code group '" + group + "'. Expected one of " + StatusCodeGroups.NAMES
                    )
                )
            )
            .collect(Collectors.toSet());
    }

    /**
     * The catalog names API kinds after the product ({@code HTTP_PROXY}, {@code LLM}, {@code MCP}); the API
     * definition names them after the reactor ({@code PROXY}, {@code LLM_PROXY}, {@code MCP_PROXY}). Kinds the
     * logs signal does not serve are absent, so asking for one yields an empty scope rather than a 400 — the
     * request is valid, it just cannot match.
     */
    private static final Map<String, ApiType> LOGS_API_TYPES = Map.of(
        "HTTP_PROXY",
        ApiType.PROXY,
        "LLM",
        ApiType.LLM_PROXY,
        "MCP",
        ApiType.MCP_PROXY
    );

    /**
     * Catalog kinds the logs signal knows of but cannot serve. Asking for one is a valid request that matches
     * nothing; asking for a name in neither set is a typo, and gets the same 400 an unknown status code group
     * gets — a request that cannot mean anything should say so rather than quietly return an empty page.
     *
     * <p>Together with {@link #LOGS_API_TYPES} this is the catalog's {@code API_TYPE} vocabulary, pinned
     * against it by {@code SearchEnvironmentLogsUseCaseTest}.
     */
    private static final Set<String> UNSERVED_API_TYPES = Set.of("MESSAGE", "A2A", "NATIVE", "EDGE");

    /** The full {@code API_TYPE} vocabulary, sorted so the error message is stable. */
    static Set<String> knownApiTypes() {
        return Stream.concat(LOGS_API_TYPES.keySet().stream(), UNSERVED_API_TYPES.stream()).collect(Collectors.toCollection(TreeSet::new));
    }

    private static FilterName filterName(Object instance) {
        return switch (instance) {
            case StringFilter s -> s.name();
            case ArrayFilter a -> a.name();
            case NumericFilter n -> n.name();
            case null, default -> null;
        };
    }

    /**
     * Requested API kinds, mapped to the definition vocabulary.
     *
     * <p>{@link Optional#empty()} means the request carries no {@code API_TYPE} filter. A present but empty set
     * means one was supplied naming only kinds the logs signal does not serve — that must match nothing, not
     * everything, or the filter would read as ignored. The two cases are distinct values rather than a sentinel
     * kind, so this does not depend on which kinds {@link #isWantedHttpApi} happens to exclude.
     */
    private static Optional<Set<ApiType>> requestedApiTypes(SearchLogsRequest request) {
        if (request.filters() == null) {
            return Optional.empty();
        }
        Set<ApiType> requested = null;
        for (Filter filter : request.filters()) {
            var instance = filter.actualInstance();
            if (filterName(instance) != FilterName.API_TYPE) {
                continue;
            }
            if (requested == null) {
                requested = new HashSet<>();
            }
            for (String value : apiTypeValues(instance)) {
                var normalised = value.trim().toUpperCase(Locale.ROOT);
                var mapped = LOGS_API_TYPES.get(normalised);
                if (mapped != null) {
                    requested.add(mapped);
                } else if (!UNSERVED_API_TYPES.contains(normalised)) {
                    throw new ValidationDomainException("Unknown API type '" + value + "'. Expected one of " + knownApiTypes());
                }
            }
        }
        return Optional.ofNullable(requested);
    }

    private static List<String> apiTypeValues(Object instance) {
        return switch (instance) {
            case StringFilter s -> s.value() == null ? List.of() : List.of(s.value());
            case ArrayFilter a -> a.value() == null ? List.of() : a.value().stream().map(String::valueOf).toList();
            case null, default -> List.of();
        };
    }

    private static boolean isWantedHttpApi(ApiType type) {
        return type == ApiType.PROXY || type == ApiType.LLM_PROXY || type == ApiType.MCP_PROXY;
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
        return ApiLog.builder()
            .apiId(item.getApiId())
            .timestamp(toOffsetDateTime(item.getTimestamp()))
            .id(item.getRequestId())
            .requestId(item.getRequestId())
            .method(mapHttpMethod(item.getMethod()))
            .clientIdentifier(item.getClientIdentifier())
            .plan(mapPlan(item.getPlanId()))
            .application(mapApplication(item.getApplicationId()))
            .transactionId(item.getTransactionId())
            .status(item.getStatus())
            .requestEnded(item.isRequestEnded())
            .gatewayResponseTime(safeToInteger(item.getGatewayResponseTime()))
            .gateway(item.getGateway())
            .uri(item.getUri())
            .endpoint(item.getEndpoint())
            .message(item.getMessage())
            .errorKey(item.getErrorKey())
            .errorComponentName(item.getErrorComponentName())
            .errorComponentType(item.getErrorComponentType())
            .warnings(mapWarnings(item.getWarnings()))
            .additionalMetrics(item.getAdditionalMetrics() != null ? item.getAdditionalMetrics() : Map.of())
            .mcpMethod(item.getMcpMethod())
            .apiProductId(item.getApiProductId())
            .build();
    }

    private SearchLogsResponse enrichWithApiProductNames(ExecutionContext executionContext, SearchLogsResponse response) {
        var ids = response.data().stream().map(ApiLog::apiProductId).filter(Objects::nonNull).collect(Collectors.toSet());

        var nameById = ids.isEmpty()
            ? Map.<String, String>of()
            : apiProductQueryService
                .findByEnvironmentIdAndIdIn(executionContext.getEnvironmentId(), ids)
                .stream()
                .filter(p -> p.getName() != null)
                .collect(Collectors.toMap(ApiProduct::getId, ApiProduct::getName));

        var enriched = response
            .data()
            .stream()
            .map(log -> {
                if (log.apiProductId() == null) {
                    return log.toBuilder().apiProductName("Standalone API").build();
                }
                return log.toBuilder().apiProductName(nameById.get(log.apiProductId())).build();
            })
            .toList();
        return new SearchLogsResponse(enriched, response.pagination());
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
