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
package io.gravitee.rest.api.portal.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricFacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesMetricResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.portal.rest.model.AnalyticsComputationMetricRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetBucketGroup;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetBucketLeaf;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetMetricRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetName;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetsRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetsResponse;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFilter;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFilterName;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFilterOperator;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMeasureName;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMeasuresRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMeasuresResponse;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMetricName;
import io.gravitee.rest.api.portal.rest.model.AnalyticsStringFilter;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeRange;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeSeriesRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeSeriesResponse;
import io.gravitee.rest.api.portal.rest.model.AnalyticsUnitName;
import io.gravitee.rest.api.portal.rest.model.CustomInterval;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.container.ContainerRequestFilter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalAnalyticsComputationResourceTest extends AbstractResourceTest {

    @Autowired
    private ComputeMeasuresUseCase computeMeasuresUseCase;

    @Autowired
    private ComputeFacetsUseCase computeFacetsUseCase;

    @Autowired
    private ComputeTimeSeriesUseCase computeTimeSeriesUseCase;

    @Override
    protected String contextPath() {
        return "analytics";
    }

    @BeforeEach
    void setUp() {
        resetAllMocks();
        reset(computeMeasuresUseCase, computeFacetsUseCase, computeTimeSeriesUseCase);
        when(
            parameterService.findAsBoolean(any(), eq(Key.PORTAL_NEXT_ANALYTICS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);
    }

    /**
     * The base {@link io.gravitee.rest.api.portal.rest.JerseySpringTest.AuthenticationFilter} only populates
     * {@link SecurityContextHolder} as a side-effect when {@code getUserPrincipal()} is invoked. Since
     * {@link io.gravitee.rest.api.portal.rest.resource.AbstractResource#getAuditInfo()} reads the
     * holder directly, we eagerly populate it on the Jetty worker thread so the endpoint can resolve
     * the authenticated caller without having to call {@code getAuthenticatedUser()} first.
     */
    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        super.decorate(resourceConfig);
        resourceConfig.register(
            (ContainerRequestFilter) requestContext -> {
                final var principal = new UsernamePasswordAuthenticationToken(
                    new UserDetails(USER_NAME, "", Collections.emptyList()),
                    new Object()
                );
                SecurityContextHolder.getContext().setAuthentication(principal);
            },
            10
        );
    }

    @Test
    void post_measures_returns_response_and_delegates_to_compute_measures_use_case() {
        when(computeMeasuresUseCase.execute(any())).thenReturn(
            new ComputeMeasuresUseCase.Output(
                new MeasuresResponse(
                    List.of(
                        new MetricMeasuresResponse(
                            MetricSpec.Name.HTTP_REQUESTS,
                            MetricSpec.Unit.NUMBER,
                            List.of(new Measure(MetricSpec.Measure.COUNT, 42))
                        )
                    )
                )
            )
        );

        final var response = target().path("measures").request().post(Entity.json(aMeasuresRequest()));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final var body = response.readEntity(AnalyticsMeasuresResponse.class);
        assertThat(body.getMetrics()).hasSize(1);
        assertThat(body.getMetrics().getFirst().getName()).isEqualTo(AnalyticsMetricName.HTTP_REQUESTS);
        assertThat(body.getMetrics().getFirst().getUnit()).isEqualTo(AnalyticsUnitName.NUMBER);
        assertThat(body.getMetrics().getFirst().getMeasures()).hasSize(1);
        assertThat(body.getMetrics().getFirst().getMeasures().getFirst().getName()).isEqualTo(AnalyticsMeasureName.COUNT);
        verify(computeMeasuresUseCase).execute(any());
    }

    @Test
    void post_measures_forbidden_when_portal_next_analytics_disabled() {
        when(
            parameterService.findAsBoolean(any(), eq(Key.PORTAL_NEXT_ANALYTICS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(false);

        final var response = target().path("measures").request().post(Entity.json(aMeasuresRequest()));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(computeMeasuresUseCase, never()).execute(any());
    }

    @Test
    void post_facets_returns_response_and_delegates_to_compute_facets_use_case() {
        when(computeFacetsUseCase.execute(any())).thenReturn(new ComputeFacetsUseCase.Output(new FacetsResponse(List.of())));

        final var response = target().path("facets").request().post(Entity.json(aFacetsRequest()));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final var body = response.readEntity(AnalyticsFacetsResponse.class);
        assertThat(body.getMetrics()).isEmpty();
        verify(computeFacetsUseCase).execute(any());
    }

    @Test
    void post_facets_forbidden_when_portal_next_analytics_disabled() {
        when(
            parameterService.findAsBoolean(any(), eq(Key.PORTAL_NEXT_ANALYTICS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(false);

        final var response = target().path("facets").request().post(Entity.json(aFacetsRequest()));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(computeFacetsUseCase, never()).execute(any());
    }

    @Test
    void post_time_series_returns_response_and_delegates_to_compute_time_series_use_case() {
        when(computeTimeSeriesUseCase.execute(any())).thenReturn(new ComputeTimeSeriesUseCase.Output(new TimeSeriesResponse(List.of())));

        final var response = target().path("time-series").request().post(Entity.json(aTimeSeriesRequest()));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final var body = response.readEntity(AnalyticsTimeSeriesResponse.class);
        assertThat(body.getMetrics()).isEmpty();
        verify(computeTimeSeriesUseCase).execute(any());
    }

    @Test
    void post_time_series_forbidden_when_portal_next_analytics_disabled() {
        when(
            parameterService.findAsBoolean(any(), eq(Key.PORTAL_NEXT_ANALYTICS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(false);

        final var response = target().path("time-series").request().post(Entity.json(aTimeSeriesRequest()));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
        verify(computeTimeSeriesUseCase, never()).execute(any());
    }

    @Test
    void post_measures_maps_string_filter_to_core_filter() {
        when(computeMeasuresUseCase.execute(any())).thenReturn(new ComputeMeasuresUseCase.Output(new MeasuresResponse(List.of())));

        final var apiId = "7b6ebef3-6236-4ac5-815e-d3dcef83df5d";
        final var request = aMeasuresRequest().filters(
            List.of(
                new AnalyticsFilter(
                    new AnalyticsStringFilter().name(AnalyticsFilterName.API).operator(AnalyticsFilterOperator.EQ).value(apiId)
                )
            )
        );

        final var response = target().path("measures").request().post(Entity.json(request));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final var captor = ArgumentCaptor.forClass(ComputeMeasuresUseCase.Input.class);
        verify(computeMeasuresUseCase).execute(captor.capture());
        assertThat(captor.getValue().request().filters()).containsExactly(
            new io.gravitee.apim.core.analytics_engine.model.Filter(FilterSpec.Name.API, FilterOperator.EQ, apiId)
        );
    }

    @Test
    void post_measures_returns_400_for_unknown_filter_shape() {
        final var json = """
            {
              "timeRange": {"from": "2026-01-01T00:00:00Z", "to": "2026-01-02T00:00:00Z"},
              "filters": [{"name": "API", "operator": "UNKNOWN_OP", "value": "abc"}],
              "metrics": [{"name": "HTTP_REQUESTS", "measures": ["COUNT"]}]
            }
            """;

        final var response = target().path("measures").request().post(Entity.json(json));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        verify(computeMeasuresUseCase, never()).execute(any());
    }

    @Test
    void post_time_series_accepts_interval_as_milliseconds() {
        when(computeTimeSeriesUseCase.execute(any())).thenReturn(new ComputeTimeSeriesUseCase.Output(new TimeSeriesResponse(List.of())));

        final var request = aTimeSeriesRequest().interval(new CustomInterval(3_600_000L));

        final var response = target().path("time-series").request().post(Entity.json(request));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final var captor = ArgumentCaptor.forClass(ComputeTimeSeriesUseCase.Input.class);
        verify(computeTimeSeriesUseCase).execute(captor.capture());
        assertThat(captor.getValue().request().interval()).isEqualTo(3_600_000L);
    }

    @Test
    void post_time_series_accepts_interval_as_duration_shorthand() {
        when(computeTimeSeriesUseCase.execute(any())).thenReturn(new ComputeTimeSeriesUseCase.Output(new TimeSeriesResponse(List.of())));

        final var json = """
            {
              "timeRange": {"from": "2026-01-01T00:00:00Z", "to": "2026-01-02T00:00:00Z"},
              "interval": "1h",
              "metrics": [{"name": "HTTP_REQUESTS", "measures": ["COUNT"]}]
            }
            """;

        final var response = target().path("time-series").request().post(Entity.json(json));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final var captor = ArgumentCaptor.forClass(ComputeTimeSeriesUseCase.Input.class);
        verify(computeTimeSeriesUseCase).execute(captor.capture());
        assertThat(captor.getValue().request().interval()).isEqualTo(3_600_000L);
    }

    @Test
    void post_facets_maps_leaf_and_group_buckets_in_response() {
        final var leaf = new FacetBucketResponse("/api-x", null, null, List.of(new Measure(MetricSpec.Measure.COUNT, 5)));
        final var group = new FacetBucketResponse("APP-1", null, List.of(leaf), null);
        when(computeFacetsUseCase.execute(any())).thenReturn(
            new ComputeFacetsUseCase.Output(
                new FacetsResponse(List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, MetricSpec.Unit.NUMBER, List.of(group))))
            )
        );

        final var response = target().path("facets").request().post(Entity.json(aFacetsRequest()));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final var body = response.readEntity(AnalyticsFacetsResponse.class);
        assertThat(body.getMetrics()).hasSize(1);
        final var topBucket = body.getMetrics().getFirst().getBuckets().getFirst().getActualInstance();
        assertThat(topBucket).isInstanceOf(AnalyticsFacetBucketGroup.class);
        final var topGroup = (AnalyticsFacetBucketGroup) topBucket;
        assertThat(topGroup.getType()).isEqualTo(AnalyticsFacetBucketGroup.TypeEnum.GROUP);
        assertThat(topGroup.getKey()).isEqualTo("APP-1");
        assertThat(topGroup.getBuckets()).hasSize(1);
        final var nestedBucket = topGroup.getBuckets().getFirst().getActualInstance();
        assertThat(nestedBucket).isInstanceOf(AnalyticsFacetBucketLeaf.class);
        final var nestedLeaf = (AnalyticsFacetBucketLeaf) nestedBucket;
        assertThat(nestedLeaf.getType()).isEqualTo(AnalyticsFacetBucketLeaf.TypeEnum.LEAF);
        assertThat(nestedLeaf.getKey()).isEqualTo("/api-x");
        assertThat(nestedLeaf.getMeasures()).hasSize(1);
    }

    @Test
    void post_time_series_maps_leaf_bucket_in_response() {
        final var bucket = new TimeSeriesBucketResponse(
            "2026-01-01T00:00:00Z",
            null,
            1_735_689_600_000L,
            null,
            List.of(new Measure(MetricSpec.Measure.COUNT, 7))
        );
        when(computeTimeSeriesUseCase.execute(any())).thenReturn(
            new ComputeTimeSeriesUseCase.Output(
                new TimeSeriesResponse(
                    List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, MetricSpec.Unit.NUMBER, List.of(bucket)))
                )
            )
        );

        final var response = target().path("time-series").request().post(Entity.json(aTimeSeriesRequest()));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final var body = response.readEntity(AnalyticsTimeSeriesResponse.class);
        assertThat(body.getMetrics()).hasSize(1);
        assertThat(body.getMetrics().getFirst().getBuckets()).hasSize(1);
    }

    private static AnalyticsTimeRange aTimeRange() {
        final var now = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        return new AnalyticsTimeRange().from(now.minusDays(1)).to(now);
    }

    private static AnalyticsMeasuresRequest aMeasuresRequest() {
        return new AnalyticsMeasuresRequest()
            .timeRange(aTimeRange())
            .metrics(
                List.of(
                    new AnalyticsComputationMetricRequest()
                        .name(AnalyticsMetricName.HTTP_REQUESTS)
                        .measures(List.of(AnalyticsMeasureName.COUNT))
                )
            );
    }

    private static AnalyticsFacetsRequest aFacetsRequest() {
        return new AnalyticsFacetsRequest()
            .timeRange(aTimeRange())
            .by(List.of(AnalyticsFacetName.API))
            .metrics(
                List.of(
                    new AnalyticsFacetMetricRequest().name(AnalyticsMetricName.HTTP_REQUESTS).measures(List.of(AnalyticsMeasureName.COUNT))
                )
            );
    }

    private static AnalyticsTimeSeriesRequest aTimeSeriesRequest() {
        return new AnalyticsTimeSeriesRequest()
            .timeRange(aTimeRange())
            .interval(new CustomInterval(60_000L))
            .metrics(
                List.of(
                    new AnalyticsFacetMetricRequest().name(AnalyticsMetricName.HTTP_REQUESTS).measures(List.of(AnalyticsMeasureName.COUNT))
                )
            );
    }
}
