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
package io.gravitee.apim.core.analytics.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics.model.AnalyticsType;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified analytics use case — validates the request, then dispatches to the
 * appropriate Elasticsearch aggregation strategy based on {@link AnalyticsType}.
 *
 * <p>US-01 implements COUNT; US-03/04 add STATS, GROUP_BY, DATE_HISTO.</p>
 * <p>US-02 adds centralised validation for all 12 error conditions.</p>
 */
@Slf4j
@RequiredArgsConstructor
@UseCase
public class GetApiAnalyticsUseCase {

    // ── Allowed field values per analytics type ───────────────────────────────
    // Populated with the fields required by the M1 dashboard (US-07/08).
    // Extended in US-03 via FieldMapper once all aggregation types ship.
    static final Set<String> STATS_FIELDS = Set.of("gateway-response-time-ms", "endpoint-response-time-ms", "request-content-length");
    static final Set<String> GROUP_BY_FIELDS = Set.of("status");
    static final Set<String> DATE_HISTO_FIELDS = Set.of("status");

    static final long MAX_WINDOW_MS = Duration.ofDays(366).toMillis();

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;

    public Output execute(ExecutionContext executionContext, Input input) {
        AnalyticsType type = validate(input);
        validateApiRequirements(input);

        return switch (type) {
            case COUNT -> executeCount(executionContext, input);
            default -> throw new IllegalArgumentException("Unsupported analytics type: " + type);
        };
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validates all request parameters according to US-02 ACs 1–12.
     *
     * @return the parsed {@link AnalyticsType} so callers can dispatch on it.
     * @throws ValidationDomainException for any invalid input combination.
     */
    private AnalyticsType validate(Input input) {
        // AC 1 — type is required
        if (input.type() == null || input.type().isBlank()) {
            throw new ValidationDomainException("type is required", Map.of("type", "type"));
        }

        // AC 2 — type must be a known value
        AnalyticsType analyticsType;
        try {
            analyticsType = AnalyticsType.valueOf(input.type().toUpperCase());
        } catch (IllegalArgumentException e) {
            String allowed = Arrays.stream(AnalyticsType.values()).map(Enum::name).collect(Collectors.joining(", "));
            throw new ValidationDomainException(
                "Unknown analytics type '%s'. Allowed values: %s".formatted(input.type(), allowed),
                Map.of("type", input.type())
            );
        }

        // AC 3 — from and to are required
        if (input.from() == null) {
            throw new ValidationDomainException("from is required", Map.of("from", "from"));
        }
        if (input.to() == null) {
            throw new ValidationDomainException("to is required", Map.of("to", "to"));
        }

        // AC 4 — from must be strictly before to  (AC 6: future to is fine)
        if (!input.from().isBefore(input.to())) {
            throw new ValidationDomainException(
                "from (%d) must be strictly before to (%d)".formatted(input.from().toEpochMilli(), input.to().toEpochMilli()),
                Map.of("from", String.valueOf(input.from().toEpochMilli()), "to", String.valueOf(input.to().toEpochMilli()))
            );
        }

        // AC 5 — time window must not exceed 366 days
        long windowMs = input.to().toEpochMilli() - input.from().toEpochMilli();
        if (windowMs > MAX_WINDOW_MS) {
            throw new ValidationDomainException(
                "Requested time window (%d ms) exceeds the maximum supported range of 366 days (%d ms)".formatted(windowMs, MAX_WINDOW_MS)
            );
        }

        // AC 11 — order must be ASC or DESC when provided
        if (input.order() != null && !input.order().equalsIgnoreCase("ASC") && !input.order().equalsIgnoreCase("DESC")) {
            throw new ValidationDomainException(
                "order must be ASC or DESC, got '%s'".formatted(input.order()),
                Map.of("order", input.order())
            );
        }

        // Type-specific field / interval validations
        switch (analyticsType) {
            case STATS -> validateStatsParams(input);
            case GROUP_BY -> validateGroupByParams(input);
            case DATE_HISTO -> validateDateHistoParams(input);
            case COUNT -> {} // COUNT needs no extra parameters
        }

        return analyticsType;
    }

    // AC 7 & 8: STATS requires a known field
    private static void validateStatsParams(Input input) {
        requireField(input, "STATS");
        if (!STATS_FIELDS.contains(input.field())) {
            throw new ValidationDomainException(
                "Unknown field '%s' for STATS. Allowed values: %s".formatted(input.field(), STATS_FIELDS),
                Map.of("field", input.field())
            );
        }
    }

    // AC 8: GROUP_BY requires a known field
    private static void validateGroupByParams(Input input) {
        requireField(input, "GROUP_BY");
        if (!GROUP_BY_FIELDS.contains(input.field())) {
            throw new ValidationDomainException(
                "Unknown field '%s' for GROUP_BY. Allowed values: %s".formatted(input.field(), GROUP_BY_FIELDS),
                Map.of("field", input.field())
            );
        }
    }

    // AC 9 & 10: DATE_HISTO requires a known field and a positive interval
    private static void validateDateHistoParams(Input input) {
        requireField(input, "DATE_HISTO");
        if (!DATE_HISTO_FIELDS.contains(input.field())) {
            throw new ValidationDomainException(
                "Unknown field '%s' for DATE_HISTO. Allowed values: %s".formatted(input.field(), DATE_HISTO_FIELDS),
                Map.of("field", input.field())
            );
        }
        if (input.interval() == null) {
            throw new ValidationDomainException("interval is required for DATE_HISTO", Map.of("interval", "interval"));
        }
        if (input.interval() <= 0) {
            throw new ValidationDomainException(
                "interval must be > 0, got %d".formatted(input.interval()),
                Map.of("interval", String.valueOf(input.interval()))
            );
        }
    }

    private static void requireField(Input input, String typeName) {
        if (input.field() == null || input.field().isBlank()) {
            throw new ValidationDomainException("field is required for %s".formatted(typeName), Map.of("field", "field"));
        }
    }

    // ── COUNT execution ───────────────────────────────────────────────────────

    private CountOutput executeCount(ExecutionContext ctx, Input input) {
        return analyticsQueryService
            .searchRequestsCount(ctx, input.apiId(), input.from(), input.to())
            .map(rc -> new CountOutput(rc.getTotal() != null ? rc.getTotal() : 0L))
            .orElse(new CountOutput(0L));
    }

    // ── API validation ────────────────────────────────────────────────────────

    private void validateApiRequirements(Input input) {
        final Api api = apiCrudService.get(input.apiId());
        validateApiDefinitionVersion(api.getDefinitionVersion(), input.apiId());
        validateApiIsNotTcp(api.getApiDefinitionHttpV4());
        validateApiMultiTenancyAccess(api, input.environmentId());
    }

    private static void validateApiDefinitionVersion(DefinitionVersion version, String apiId) {
        if (!DefinitionVersion.V4.equals(version)) {
            throw new ApiInvalidDefinitionVersionException(apiId);
        }
    }

    private static void validateApiIsNotTcp(io.gravitee.definition.model.v4.Api apiDefinitionV4) {
        if (apiDefinitionV4 != null && apiDefinitionV4.isTcpProxy()) {
            throw new IllegalArgumentException("Analytics are not supported for TCP Proxy APIs");
        }
    }

    private static void validateApiMultiTenancyAccess(Api api, String environmentId) {
        if (!api.belongsToEnvironment(environmentId)) {
            throw new ApiNotFoundException(api.getId());
        }
    }

    // ── I/O types ─────────────────────────────────────────────────────────────

    /**
     * @param type     raw query-param string — validated and parsed inside the use case
     * @param from     epoch-millisecond lower bound; null triggers a 400
     * @param to       epoch-millisecond upper bound; null triggers a 400; future values are accepted
     * @param field    required for STATS, GROUP_BY and DATE_HISTO; null triggers a 400 for those types
     * @param interval bucket size in ms; required for DATE_HISTO; must be > 0
     * @param order    bucket sort order; ASC or DESC; null means use the type's default
     */
    public record Input(
        String apiId,
        String environmentId,
        String type,
        Instant from,
        Instant to,
        String field,
        Long interval,
        String order
    ) {}

    /**
     * Sealed output hierarchy — one subtype per {@link AnalyticsType}.
     * US-03/04 will add StatsOutput, GroupByOutput, DateHistoOutput.
     */
    public sealed interface Output permits GetApiAnalyticsUseCase.CountOutput {}

    public record CountOutput(long count) implements Output {}
}
