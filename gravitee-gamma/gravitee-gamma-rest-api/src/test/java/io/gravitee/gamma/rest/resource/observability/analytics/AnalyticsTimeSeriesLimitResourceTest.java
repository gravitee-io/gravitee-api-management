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
package io.gravitee.gamma.rest.resource.observability.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.analytics.domain_service.AiWorkspaceAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MetricFacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesMetricResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.apim.infra.domain_service.analytics_engine.processors.UnitEnrichmentPostProcessorImpl;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.AnalyticsRequestPipeline;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityFacetsUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityMeasuresUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityTimeSeriesUseCase;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import io.gravitee.gamma.rest.infra.adapter.EntrypointScopeProviderAdapter;
import io.gravitee.gamma.rest.infra.adapter.ObservabilityAnalyticsDataPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resource.observability.analytics.AnalyticsTimeSeriesLimitResourceTest.TimeSeriesLimitTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

/**
 * A "Top N" time-series end to end: real payloads through Jersey, the real use case and the real data
 * port adapter; only the analytics engine's compute use cases are mocked.
 */
@ContextConfiguration(classes = { ResourceContextConfiguration.class, TimeSeriesLimitTestConfiguration.class })
class AnalyticsTimeSeriesLimitResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final Map<String, String> TIME_RANGE = Map.of("from", "2026-06-10T00:00:00Z", "to", "2026-06-10T03:00:00Z");
    private static final Filter DASHBOARD_FILTER = new Filter(
        FilterSpec.Name.HTTP_METHOD,
        io.gravitee.apim.core.observability.model.FilterOperator.IN,
        List.of("POST")
    );
    private static final Filter API_RESTRICTION = new Filter(
        FilterSpec.Name.API,
        io.gravitee.apim.core.observability.model.FilterOperator.IN,
        List.of("api-1")
    );

    @Inject
    private ComputeFacetsUseCase computeFacetsUseCase;

    @Inject
    private ComputeTimeSeriesUseCase computeTimeSeriesUseCase;

    @Inject
    private UserContextLoader userContextLoader;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/observability/analytics";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
        when(userContextLoader.loadApis(any(), eq(ApiDataKind.AGGREGATES))).thenAnswer(invocation ->
            ((UserContext) invocation.getArgument(0)).withApis(
                List.of(
                    Api.builder().id("api-1").name("API 1").type(ApiType.LLM_PROXY).build(),
                    Api.builder().id("api-2").name("API 2").type(ApiType.LLM_PROXY).build()
                )
            )
        );
    }

    @AfterEach
    void resetEngine() {
        reset(computeFacetsUseCase, computeTimeSeriesUseCase, userContextLoader);
    }

    @ParameterizedTest
    @ValueSource(strings = { "limit", "facetSize" })
    void should_draw_the_same_top_series_in_every_bucket(String limitField) {
        // Over the range: m7 > m6 > m5 = m4 > m3 > m2 > m1, so the top 5 keeps m4 and m5 and drops m1 and m2.
        givenTheRangeTotals(Map.of("m1", 1, "m2", 2, "m3", 30, "m4", 40, "m5", 40, "m6", 60, "m7", 70));
        // Each hour has its own leaders, m1 and m2 included.
        givenTheHourlyBuckets(List.of(List.of("m1", "m2", "m3"), List.of("m7", "m2", "m6"), List.of("m5", "m4", "m1")));

        var response = postTimeSeries(limitField);

        assertThat(response.at("/metrics/0/buckets")).hasSize(3);
        var drawn = new LinkedHashSet<String>();
        response.at("/metrics/0/buckets").forEach(bucket -> drawn.addAll(bucket.path("buckets").findValuesAsText("key")));
        assertThat(drawn).containsExactlyInAnyOrder("m3", "m4", "m5", "m6", "m7");
        assertThat(response.at("/metrics/0/buckets/0/buckets").findValuesAsText("key")).containsExactly("m3");
    }

    @Test
    void should_rank_and_plot_the_same_population() {
        givenTheRangeTotals(Map.of("m1", 1, "m2", 2));
        givenTheHourlyBuckets(List.of(List.of("m1")));

        postTimeSeries("limit");

        var facetsRequest = facetsRequest();
        var timeSeriesRequest = timeSeriesRequest();
        assertThat(facetsRequest.filters()).contains(DASHBOARD_FILTER, API_RESTRICTION);
        assertThat(facetsRequest.timeRange()).isEqualTo(timeSeriesRequest.timeRange());
        assertThat(timeSeriesRequest.filters()).containsExactlyElementsOf(
            concat(
                facetsRequest.filters(),
                new Filter(
                    FilterSpec.Name.LLM_PROXY_MODEL,
                    io.gravitee.apim.core.observability.model.FilterOperator.IN,
                    List.of("m1", "m2")
                )
            )
        );
    }

    @Test
    void should_leave_the_engine_its_per_bucket_default_without_a_limit() {
        givenTheHourlyBuckets(List.of(List.of("m1", "m2")));

        var response = rootTarget("time-series").request().post(Entity.entity(body(Map.of()), MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        verify(computeFacetsUseCase, never()).executeForApis(any(), any(), any());
        assertThat(timeSeriesRequest().limit()).isNull();
    }

    private JsonNode postTimeSeries(String limitField) {
        var response = rootTarget("time-series")
            .request()
            .post(Entity.entity(body(Map.of(limitField, 5)), MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        return response.readEntity(JsonNode.class);
    }

    private static Map<String, Object> body(Map<String, Object> limit) {
        var body = new HashMap<String, Object>(limit);
        body.put("timeRange", TIME_RANGE);
        body.put("interval", 3_600_000L);
        body.put("metrics", List.of(Map.of("name", "HTTP_REQUESTS", "measures", List.of("COUNT"))));
        body.put("by", List.of("LLM_PROXY_MODEL"));
        body.put(
            "filters",
            List.of(
                Map.of("name", "HTTP_METHOD", "operator", "IN", "value", List.of("POST")),
                Map.of("name", "API", "operator", "IN", "value", List.of("api-1"))
            )
        );
        return body;
    }

    private void givenTheRangeTotals(Map<String, Integer> countByModel) {
        var buckets = countByModel
            .entrySet()
            .stream()
            .map(entry ->
                new FacetBucketResponse(entry.getKey(), null, null, List.of(new Measure(MetricSpec.Measure.COUNT, entry.getValue())))
            )
            .toList();
        when(computeFacetsUseCase.executeForApis(any(), any(), any())).thenReturn(
            new ComputeFacetsUseCase.Output(
                new FacetsResponse(
                    List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, MetricSpec.Unit.NUMBER, buckets, List.of()))
                )
            )
        );
    }

    private void givenTheHourlyBuckets(List<List<String>> modelsByHour) {
        var buckets = IntStream.range(0, modelsByHour.size())
            .mapToObj(hour ->
                new TimeSeriesBucketResponse(
                    "hour-" + hour,
                    null,
                    hour * 3_600_000L,
                    modelsByHour
                        .get(hour)
                        .stream()
                        .map(model -> new FacetBucketResponse(model, null, null, List.of(new Measure(MetricSpec.Measure.COUNT, 1))))
                        .toList(),
                    null
                )
            )
            .toList();
        when(computeTimeSeriesUseCase.executeForApis(any(), any(), any())).thenReturn(
            new ComputeTimeSeriesUseCase.Output(
                new TimeSeriesResponse(
                    List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, MetricSpec.Unit.NUMBER, buckets, List.of()))
                )
            )
        );
    }

    private io.gravitee.apim.core.analytics_engine.model.FacetsRequest facetsRequest() {
        var captor = ArgumentCaptor.forClass(io.gravitee.apim.core.analytics_engine.model.FacetsRequest.class);
        verify(computeFacetsUseCase).executeForApis(any(), captor.capture(), any());
        return captor.getValue();
    }

    private io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest timeSeriesRequest() {
        var captor = ArgumentCaptor.forClass(io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest.class);
        verify(computeTimeSeriesUseCase).executeForApis(any(), captor.capture(), any());
        return captor.getValue();
    }

    private static List<Filter> concat(List<Filter> filters, Filter filter) {
        var all = new ArrayList<>(filters);
        all.add(filter);
        return all;
    }

    @Configuration
    static class TimeSeriesLimitTestConfiguration {

        @Bean
        ComputeMeasuresUseCase computeMeasuresUseCase() {
            return mock(ComputeMeasuresUseCase.class);
        }

        @Bean
        ComputeFacetsUseCase computeFacetsUseCase() {
            return mock(ComputeFacetsUseCase.class);
        }

        @Bean
        ComputeTimeSeriesUseCase computeTimeSeriesUseCase() {
            return mock(ComputeTimeSeriesUseCase.class);
        }

        @Bean
        UserContextLoader userContextLoader() {
            return mock(UserContextLoader.class);
        }

        @Bean
        ObservabilityAnalyticsDataPort observabilityAnalyticsDataPort(
            ComputeMeasuresUseCase computeMeasuresUseCase,
            ComputeFacetsUseCase computeFacetsUseCase,
            ComputeTimeSeriesUseCase computeTimeSeriesUseCase,
            UserContextLoader userContextLoader
        ) {
            return new ObservabilityAnalyticsDataPortAdapter(
                computeMeasuresUseCase,
                computeFacetsUseCase,
                computeTimeSeriesUseCase,
                new AnalyticsQueryValidator(new AnalyticsDefinitionYAMLQueryService()),
                new UnitEnrichmentPostProcessorImpl(new AnalyticsDefinitionYAMLQueryService()),
                userContextLoader,
                mock(AiWorkspaceAnalyticsApisDomainService.class),
                mock(AnalyticsQueryContextProvider.class),
                new ObjectMapper()
            );
        }

        @Bean
        AnalyticsRequestPipeline analyticsRequestPipeline() {
            return new AnalyticsRequestPipeline(
                new ObservabilityFilterValidator(new SpiFilterRegistry()),
                new AccessibleApiScopeDomainService(),
                new EntrypointScopeProviderAdapter()
            );
        }

        @Bean
        ComputeObservabilityMeasuresUseCase computeObservabilityMeasuresUseCase(
            ObservabilityAnalyticsDataPort port,
            AnalyticsRequestPipeline pipeline
        ) {
            return new ComputeObservabilityMeasuresUseCase(port, pipeline);
        }

        @Bean
        ComputeObservabilityFacetsUseCase computeObservabilityFacetsUseCase(
            ObservabilityAnalyticsDataPort port,
            AnalyticsRequestPipeline pipeline
        ) {
            return new ComputeObservabilityFacetsUseCase(port, pipeline);
        }

        @Bean
        ComputeObservabilityTimeSeriesUseCase computeObservabilityTimeSeriesUseCase(
            ObservabilityAnalyticsDataPort port,
            AnalyticsRequestPipeline pipeline
        ) {
            return new ComputeObservabilityTimeSeriesUseCase(port, pipeline);
        }
    }
}
