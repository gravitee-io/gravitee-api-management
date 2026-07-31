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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.repository.ConnectionLogFixtures;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.apim.core.logs_engine.domain_service.LogNamesPostProcessor;
import io.gravitee.apim.core.logs_engine.model.ArrayFilter;
import io.gravitee.apim.core.logs_engine.model.Filter;
import io.gravitee.apim.core.logs_engine.model.FilterName;
import io.gravitee.apim.core.logs_engine.model.NumericFilter;
import io.gravitee.apim.core.logs_engine.model.Operator;
import io.gravitee.apim.core.logs_engine.model.SearchLogsRequest;
import io.gravitee.apim.core.logs_engine.model.StringFilter;
import io.gravitee.apim.core.logs_engine.use_case.SearchEnvironmentLogsUseCase.Input;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.Signal;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Contract between the filter catalog and the logs search engine.
 *
 * <p>APIM-14817: the console offered every catalog filter on the logs screen, but the engine translated only a
 * hard-coded subset and dropped the rest without a word — an active chip that changed nothing. Nothing in the
 * test suite tied the two together, so the gap could widen unnoticed with each new catalog entry.
 *
 * <p>This test closes that: it loads the <b>real</b> definition file — not a fixture — and asserts, for every
 * operator the catalog advertises, that a filter declared for {@link Signal#LOGS} actually reaches
 * {@link SearchLogsFilters}. Adding a {@code signals: [LOGS]} entry without wiring its translation now fails
 * here rather than in production.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityLogsFilterCoverageTest {

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo("org-id", "env-id", "user-id");
    private static final Api API = ApiFixtures.aProxyApiV4();

    /**
     * A second accessible API, of a different kind. Both {@code API} and {@code API_TYPE} narrow the caller's
     * accessible set rather than adding a clause, so with a single API — or a single kind — the filtered and
     * unfiltered queries would be indistinguishable and the coverage assertion would be vacuous.
     */
    private static final Api OTHER_API = ApiFixtures.aProxyApiV4()
        .toBuilder()
        .id("other-api")
        .type(io.gravitee.definition.model.v4.ApiType.LLM_PROXY)
        .build();

    private static final BaseConnectionLog LOG = new ConnectionLogFixtures(API.getId(), "1", UUID.randomUUID().toString()).aConnectionLog();

    /**
     * Catalog names that the logs engine knows under a different spelling. The v2 wire layer aliases them
     * ({@code LogsEngineMapper#mapFilterName}); this test drives the engine directly, so it aliases them too.
     */
    private static final Map<String, FilterName> ENGINE_ALIASES = Map.of(
        "HTTP_PATH",
        FilterName.URI,
        "HTTP_GATEWAY_RESPONSE_TIME",
        FilterName.RESPONSE_TIME,
        "MCP_PROXY_METHOD",
        FilterName.MCP_METHOD
    );

    private final AnalyticsDefinitionYAMLQueryService catalog = new AnalyticsDefinitionYAMLQueryService();

    private ConnectionLogsCrudService connectionLogsCrudService;
    private UserContextLoader userContextLoader;
    private SearchEnvironmentLogsUseCase useCase;

    @BeforeEach
    void setUp() {
        connectionLogsCrudService = mock(ConnectionLogsCrudService.class);
        userContextLoader = mock(UserContextLoader.class);
        var logNamesPostProcessor = mock(LogNamesPostProcessor.class);
        var planCrudService = mock(PlanCrudService.class);
        var applicationCrudService = mock(ApplicationCrudService.class);
        var instanceQueryService = mock(InstanceQueryService.class);
        var apiProductQueryService = mock(ApiProductQueryService.class);
        useCase = new SearchEnvironmentLogsUseCase(
            connectionLogsCrudService,
            userContextLoader,
            logNamesPostProcessor,
            planCrudService,
            applicationCrudService,
            instanceQueryService,
            apiProductQueryService
        );
        when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(), any(), any())).thenReturn(
            new io.gravitee.rest.api.model.v4.log.SearchLogsResponse<>(1, List.of(LOG))
        );
        when(userContextLoader.loadApis(any())).thenReturn(
            new UserContext(
                AUDIT_INFO,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(List.of(API, OTHER_API))
            )
        );
        when(planCrudService.findByIds(any())).thenReturn(List.of());
        when(applicationCrudService.findByIds(any(), any())).thenReturn(List.of());
        when(apiProductQueryService.findByEnvironmentIdAndIdIn(any(), any())).thenReturn(Set.of());
        when(logNamesPostProcessor.mapLogNames(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void every_logs_filter_in_the_catalog_should_reach_the_search_query() {
        var logsFilters = catalog.getFilters(Set.of(Signal.LOGS));
        assertThat(logsFilters).as("the catalog must advertise filters for the LOGS signal").isNotEmpty();

        var unfiltered = searchWith(List.of());
        var softly = new SoftAssertions();

        for (FilterSpec spec : logsFilters) {
            for (FilterOperator operator : spec.operators()) {
                var filter = buildFilter(spec, operator);
                softly
                    .assertThat(searchWith(List.of(filter)))
                    .as(
                        "Filter %s is advertised for LOGS with operator %s but does not reach the search query. " +
                            "Either translate it in SearchEnvironmentLogsUseCase, or drop LOGS from its signals in " +
                            "analytics-definition.yaml.",
                        spec.name(),
                        operator
                    )
                    .isNotEqualTo(unfiltered);
            }
        }

        softly.assertAll();
    }

    @Test
    void the_logs_signal_should_only_carry_filters_the_engine_knows() {
        var engineNames = java.util.Arrays.stream(FilterName.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet());

        assertThat(catalog.getFilters(Set.of(Signal.LOGS)))
            .as("every LOGS filter must resolve to a logs engine filter name, directly or through an alias")
            .allSatisfy(spec -> {
                var name = spec.name().name();
                assertThat(engineNames.contains(name) || ENGINE_ALIASES.containsKey(name))
                    .as("%s has no counterpart in the logs engine FilterName", name)
                    .isTrue();
            });
    }

    @Test
    void analytics_only_filters_should_not_be_advertised_on_the_logs_signal() {
        // The exact set APIM-14817 reported as offered-but-ignored on the logs screen.
        assertThat(
            catalog
                .getFilters(Set.of(Signal.LOGS))
                .stream()
                .map(spec -> spec.name().name())
        ).doesNotContain(
            "GATEWAY",
            "TENANT",
            "ZONE",
            "HOST",
            "HTTP_ENDPOINT_RESPONSE_TIME",
            "HTTP_GATEWAY_LATENCY",
            "GEO_IP_COUNTRY",
            "MESSAGE_SIZE",
            "EDGE_PROVIDER"
        );
    }

    /**
     * Runs one search and returns the filters handed to the repository. Invocations are cleared first so each
     * call is captured in isolation — without it the captor accumulates across the whole loop and
     * {@code getValue()} stops meaning "the filters for this call".
     */
    private SearchLogsFilters searchWith(List<Filter> filters) {
        clearInvocations(connectionLogsCrudService);
        useCase.execute(new Input(AUDIT_INFO, new SearchLogsRequest(null, filters, 1, 10)));
        var captor = ArgumentCaptor.forClass(SearchLogsFilters.class);
        verify(connectionLogsCrudService).searchApiConnectionLogs(any(), captor.capture(), any(), any());
        return captor.getValue();
    }

    /**
     * Synthesises the request filter a client would actually send for this spec and operator.
     *
     * <p>The shape must mirror the {@code Filter} oneOf of {@code openapi-logs.yaml}, which discriminates on
     * the <b>operator</b>, not on the filter's declared type: {@code IN} is an array, {@code GTE}/{@code LTE}
     * are numeric, and everything else — including {@code EQ} on a NUMBER filter — is a string. Building a
     * numeric filter for {@code EQ} would exercise a path no client can reach, and let a silently-dropped
     * filter pass this test.
     */
    private static Filter buildFilter(FilterSpec spec, FilterOperator operator) {
        var catalogName = spec.name().name();
        var alias = ENGINE_ALIASES.get(catalogName);
        var name = alias != null ? alias : FilterName.valueOf(catalogName);
        var value = sampleValue(spec);
        return switch (operator) {
            case IN -> new Filter(new ArrayFilter(name, Operator.IN, List.of(value)));
            case GTE, LTE -> new Filter(new NumericFilter(name, Operator.valueOf(operator.name()), Integer.valueOf(value)));
            case EQ, CONTAINS -> new Filter(new StringFilter(name, Operator.valueOf(operator.name()), value));
        };
    }

    private static String sampleValue(FilterSpec spec) {
        // The API filter narrows the caller's accessible set rather than adding a clause, so an arbitrary id
        // would empty the scope and short-circuit the search before it reaches the repository.
        if (spec.name() == FilterSpec.Name.API) {
            return API.getId();
        }
        // The kind of the first API only; the second one is of another kind, so this narrows the scope.
        if (spec.name() == FilterSpec.Name.API_TYPE) {
            return "HTTP_PROXY";
        }
        if (spec.enumValues() != null && !spec.enumValues().isEmpty()) {
            return spec.enumValues().getFirst();
        }
        return switch (spec.type()) {
            // Stay inside the advertised bounds so a range check cannot reject the sample.
            case NUMBER -> spec.range() != null ? String.valueOf(spec.range().from()) : "1";
            default -> "sample-value";
        };
    }
}
