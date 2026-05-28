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
package io.gravitee.gamma.rest.resource.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.tracing.exception.TraceFilterNotFoundException;
import io.gravitee.gamma.rest.core.tracing.exception.UnsupportedFilterException;
import io.gravitee.gamma.rest.core.tracing.model.FilterOperator;
import io.gravitee.gamma.rest.core.tracing.model.FilterType;
import io.gravitee.gamma.rest.core.tracing.model.PayloadLog;
import io.gravitee.gamma.rest.core.tracing.model.Span;
import io.gravitee.gamma.rest.core.tracing.model.SpanEvent;
import io.gravitee.gamma.rest.core.tracing.model.SpanKind;
import io.gravitee.gamma.rest.core.tracing.model.SpanStatus;
import io.gravitee.gamma.rest.core.tracing.model.Trace;
import io.gravitee.gamma.rest.core.tracing.model.TraceDetail;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterSpec;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterValue;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterValuesPage;
import io.gravitee.gamma.rest.core.tracing.use_case.GetTraceDetailUseCase;
import io.gravitee.gamma.rest.core.tracing.use_case.GetTraceFilterDefinitionsUseCase;
import io.gravitee.gamma.rest.core.tracing.use_case.GetTraceFilterValuesUseCase;
import io.gravitee.gamma.rest.core.tracing.use_case.SearchTracesUseCase;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resource.tracing.TracingResourceTest.TracingTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { ResourceContextConfiguration.class, TracingTestConfiguration.class })
class TracingResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final String API_ID = "api-1";
    private static final String MODULE = "aim";
    private static final String TRACE_ID = "trace-abc";
    private static final Instant SAMPLE_INSTANT = Instant.parse("2026-05-26T15:14:46.277Z");

    @Inject
    private SearchTracesUseCase searchTracesUseCase;

    @Inject
    private GetTraceDetailUseCase getTraceDetailUseCase;

    @Inject
    private GetTraceFilterDefinitionsUseCase getTraceFilterDefinitionsUseCase;

    @Inject
    private GetTraceFilterValuesUseCase getTraceFilterValuesUseCase;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/observability/traces";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
    }

    @AfterEach
    void resetUseCases() {
        reset(searchTracesUseCase, getTraceDetailUseCase, getTraceFilterDefinitionsUseCase, getTraceFilterValuesUseCase);
    }

    @Nested
    class SearchTraces {

        @Test
        void should_return_400_when_api_id_is_missing() {
            Response response = postSearch(Map.of());

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_body_is_empty() {
            // A completely empty body must still trigger the apiId-scope 400 — defensive against
            // misconfigured clients that POST with no body.
            Response response = postSearch(Map.of());

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_traces_with_logs_aligned_envelope_and_iso_8601_start_time() {
            Trace trace = new Trace(TRACE_ID, SAMPLE_INSTANT, 1_234_000L, "gateway", "GET /pets", false);
            when(searchTracesUseCase.execute(any())).thenReturn(new SearchTracesUseCase.Output(new Page<>(List.of(trace), 0, 1, 1)));

            Response response = postSearch(Map.of("apiId", API_ID));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            // Logs-aligned envelope: `data` array + nested `pagination` { totalCount, page, pageCount }.
            assertThat(body.get("data")).hasSize(1);
            assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(1L);
            assertThat(body.get("pagination").get("page").asInt()).isEqualTo(1);
            assertThat(body.get("pagination").get("pageCount").asInt()).isEqualTo(1);
            JsonNode row = body.get("data").get(0);
            assertThat(row.get("traceId").asText()).isEqualTo(TRACE_ID);
            assertThat(row.get("rootServiceName").asText()).isEqualTo("gateway");
            assertThat(row.get("rootOperationName").asText()).isEqualTo("GET /pets");
            assertThat(row.get("durationNanos").asLong()).isEqualTo(1_234_000L);
            assertThat(row.get("hasError").asBoolean()).isFalse();
            // ms-since-epoch emission is load-bearing: see TraceSummaryDto's javadoc — the long
            // field type bypasses Jackson's Instant handling and the parent rest-api ObjectMapper's
            // WRITE_DATES_AS_TIMESTAMPS default that would otherwise emit <epoch_seconds>.<nanos>.
            assertThat(row.get("startTimeEpochMs").isNumber()).isTrue();
            assertThat(row.get("startTimeEpochMs").asLong()).isEqualTo(SAMPLE_INSTANT.toEpochMilli());
        }

        @Test
        void should_pass_filters_in_body_through_to_the_use_case() {
            when(searchTracesUseCase.execute(any())).thenReturn(new SearchTracesUseCase.Output(new Page<>(List.of(), 0, 0, 0)));

            Response response = postSearch(
                Map.of("apiId", API_ID, "filters", List.of(Map.of("name", "HTTP_METHOD", "operator", "EQ", "value", "POST")))
            );

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            ArgumentCaptor<SearchTracesUseCase.Input> captor = ArgumentCaptor.forClass(SearchTracesUseCase.Input.class);
            Mockito.verify(searchTracesUseCase).execute(captor.capture());
            assertThat(captor.getValue().filters())
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.name()).isEqualTo("HTTP_METHOD");
                    assertThat(c.operator()).isEqualTo(FilterOperator.EQ);
                    // DTO normalises the polymorphic wire value to the core's List<String>.
                    assertThat(c.values()).containsExactly("POST");
                });
        }

        @Test
        void should_pass_time_range_iso_strings_through_to_the_use_case() {
            when(searchTracesUseCase.execute(any())).thenReturn(new SearchTracesUseCase.Output(new Page<>(List.of(), 0, 0, 0)));

            postSearch(Map.of("apiId", API_ID, "timeRange", Map.of("from", "2026-05-26T10:00:00Z", "to", "2026-05-26T11:00:00Z")));

            ArgumentCaptor<SearchTracesUseCase.Input> captor = ArgumentCaptor.forClass(SearchTracesUseCase.Input.class);
            Mockito.verify(searchTracesUseCase).execute(captor.capture());
            assertThat(captor.getValue().start()).isEqualTo(Instant.parse("2026-05-26T10:00:00Z"));
            assertThat(captor.getValue().end()).isEqualTo(Instant.parse("2026-05-26T11:00:00Z"));
        }

        @Test
        void should_pass_pagination_query_params_to_the_use_case() {
            when(searchTracesUseCase.execute(any())).thenReturn(new SearchTracesUseCase.Output(new Page<>(List.of(), 0, 0, 0)));

            rootTarget("search")
                .queryParam("page", 3)
                .queryParam("perPage", 50)
                .request()
                .post(Entity.entity(Map.of("apiId", API_ID), MediaType.APPLICATION_JSON_TYPE));

            ArgumentCaptor<SearchTracesUseCase.Input> captor = ArgumentCaptor.forClass(SearchTracesUseCase.Input.class);
            Mockito.verify(searchTracesUseCase).execute(captor.capture());
            assertThat(captor.getValue().page()).isEqualTo(3);
            assertThat(captor.getValue().perPage()).isEqualTo(50);
        }

        private Response postSearch(Map<String, Object> body) {
            return rootTarget("search").request().post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
        }
    }

    @Nested
    class GetTrace {

        @Test
        void should_return_400_when_api_id_is_missing() {
            Response response = rootTarget(TRACE_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_404_when_trace_does_not_exist() {
            when(getTraceDetailUseCase.execute(any())).thenReturn(new GetTraceDetailUseCase.Output(Optional.empty()));

            Response response = detailRequest().request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        void should_return_trace_detail_with_spans_status_kind_and_epoch_ms_timestamps() {
            Span span = new Span(
                "span-1",
                null,
                "GET /pets",
                "gateway",
                SAMPLE_INSTANT,
                1_234_000L,
                SpanStatus.OK,
                SpanKind.SERVER,
                Map.of("http.method", "GET"),
                List.<SpanEvent>of(),
                List.<PayloadLog>of()
            );
            TraceDetail detail = new TraceDetail(TRACE_ID, SAMPLE_INSTANT, 1_234_000L, "gateway", "GET /pets", false, List.of(span));
            when(getTraceDetailUseCase.execute(any())).thenReturn(new GetTraceDetailUseCase.Output(Optional.of(detail)));

            Response response = detailRequest().request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("traceId").asText()).isEqualTo(TRACE_ID);
            assertThat(body.get("startTimeEpochMs").asLong()).isEqualTo(SAMPLE_INSTANT.toEpochMilli());
            assertThat(body.get("spans")).hasSize(1);
            JsonNode spanNode = body.get("spans").get(0);
            // traceId echoed on every span — matches OTel transports and lib's TraceSpan required field.
            assertThat(spanNode.get("traceId").asText()).isEqualTo(TRACE_ID);
            assertThat(spanNode.get("spanId").asText()).isEqualTo("span-1");
            // parentSpanId is omitted from the JSON when null — matches the lib's optional field shape.
            assertThat(spanNode.has("parentSpanId")).isFalse();
            assertThat(spanNode.get("startTimeEpochMs").asLong()).isEqualTo(SAMPLE_INSTANT.toEpochMilli());
            // status and kind are emitted lowercase to match the lib's union types.
            assertThat(spanNode.get("status").asText()).isEqualTo("ok");
            assertThat(spanNode.get("kind").asText()).isEqualTo("server");
        }

        private WebTarget detailRequest() {
            return rootTarget(TRACE_ID).queryParam("apiId", API_ID);
        }
    }

    @Nested
    class GetFilterDefinitions {

        @Test
        void should_return_data_envelope_with_lowercase_type_and_uppercase_operators() {
            TraceFilterSpec spec = new TraceFilterSpec(
                "HTTP_METHOD",
                "HTTP method",
                FilterType.ENUM,
                List.of(FilterOperator.EQ),
                List.of("GET", "POST"),
                null
            );
            when(getTraceFilterDefinitionsUseCase.execute(any())).thenReturn(new GetTraceFilterDefinitionsUseCase.Output(List.of(spec)));

            Response response = rootTarget("filters/definition").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("data")).hasSize(1);
            JsonNode first = body.get("data").get(0);
            assertThat(first.get("name").asText()).isEqualTo("HTTP_METHOD");
            // Lowercase type matches the lib's FilterType union spelling.
            assertThat(first.get("type").asText()).isEqualTo("enum");
            // UPPERCASE operators match apim's analytics/logs wire convention. Lib's TS FilterOperator
            // union is lowercase; the lib's toWireFilter uppercases before sending.
            assertThat(first.get("operators")).extracting(JsonNode::asText).containsExactly("EQ");
            assertThat(first.get("enumValues")).extracting(JsonNode::asText).containsExactly("GET", "POST");
            // Range absent → omitted from the JSON (NON_NULL).
            assertThat(first.has("range")).isFalse();
        }

        @Test
        void should_pass_module_query_param_to_use_case_when_provided() {
            when(getTraceFilterDefinitionsUseCase.execute(any())).thenReturn(new GetTraceFilterDefinitionsUseCase.Output(List.of()));

            Response response = rootTarget("filters/definition").queryParam("module", MODULE).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            ArgumentCaptor<GetTraceFilterDefinitionsUseCase.Input> captor = ArgumentCaptor.forClass(
                GetTraceFilterDefinitionsUseCase.Input.class
            );
            Mockito.verify(getTraceFilterDefinitionsUseCase).execute(captor.capture());
            assertThat(captor.getValue().moduleId()).isEqualTo(MODULE);
        }

        @Test
        void should_be_callable_without_module_query_param() {
            when(getTraceFilterDefinitionsUseCase.execute(any())).thenReturn(new GetTraceFilterDefinitionsUseCase.Output(List.of()));

            Response response = rootTarget("filters/definition").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            ArgumentCaptor<GetTraceFilterDefinitionsUseCase.Input> captor = ArgumentCaptor.forClass(
                GetTraceFilterDefinitionsUseCase.Input.class
            );
            Mockito.verify(getTraceFilterDefinitionsUseCase).execute(captor.capture());
            assertThat(captor.getValue().moduleId()).isNull();
        }
    }

    @Nested
    class GetFilterValues {

        @Test
        void should_return_200_with_logs_aligned_envelope_for_enum_filter() {
            TraceFilterValuesPage page = new TraceFilterValuesPage(
                List.of(new TraceFilterValue("GET", null), new TraceFilterValue("POST", null)),
                2L
            );
            when(getTraceFilterValuesUseCase.execute(any())).thenReturn(new GetTraceFilterValuesUseCase.Output(page));

            Response response = rootTarget("filters/HTTP_METHOD/values").request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            JsonNode body = response.readEntity(JsonNode.class);
            assertThat(body.get("data")).hasSize(2);
            assertThat(body.get("data").get(0).get("value").asText()).isEqualTo("GET");
            // label is absent when null (NON_NULL on the DTO).
            assertThat(body.get("data").get(0).has("label")).isFalse();
            // Logs-aligned envelope: nested pagination { totalCount, page, pageCount }.
            assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(2L);
            assertThat(body.get("pagination").get("page").asInt()).isEqualTo(1);
            assertThat(body.get("pagination").get("pageCount").asInt()).isEqualTo(1);
        }

        @Test
        void should_forward_query_module_page_perPage_to_use_case() {
            when(getTraceFilterValuesUseCase.execute(any())).thenReturn(
                new GetTraceFilterValuesUseCase.Output(new TraceFilterValuesPage(List.of(), 0L))
            );

            Response response = rootTarget("filters/HTTP_METHOD/values")
                .queryParam("module", MODULE)
                .queryParam("query", "ge")
                .queryParam("page", 2)
                .queryParam("perPage", 5)
                .request()
                .get();

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            ArgumentCaptor<GetTraceFilterValuesUseCase.Input> captor = ArgumentCaptor.forClass(GetTraceFilterValuesUseCase.Input.class);
            Mockito.verify(getTraceFilterValuesUseCase).execute(captor.capture());
            assertThat(captor.getValue().filterName()).isEqualTo("HTTP_METHOD");
            assertThat(captor.getValue().moduleId()).isEqualTo(MODULE);
            assertThat(captor.getValue().query()).isEqualTo("ge");
            assertThat(captor.getValue().page()).isEqualTo(2);
            assertThat(captor.getValue().perPage()).isEqualTo(5);
        }

        @Test
        void should_return_400_when_use_case_throws_UnsupportedFilterException_for_non_enum_type() {
            when(getTraceFilterValuesUseCase.execute(any())).thenThrow(
                UnsupportedFilterException.valueListingNotSupported("HTTP_STATUS_CODE", "NUMBER")
            );

            Response response = rootTarget("filters/HTTP_STATUS_CODE/values").request().get();

            // ValidationDomainException → 400 via apim's ValidationDomainExceptionMapper. The technical
            // code is captured at the exception layer (asserted in the use-case test) — the apim mapper
            // currently does not propagate it to the wire envelope, so the REST test just pins status.
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        }

        @Test
        void should_return_404_when_use_case_throws_TraceFilterNotFoundException() {
            when(getTraceFilterValuesUseCase.execute(any())).thenThrow(new TraceFilterNotFoundException("UNKNOWN"));

            Response response = rootTarget("filters/UNKNOWN/values").request().get();

            // NotFoundDomainException → 404 via apim's NotFoundDomainExceptionMapper.
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        }
    }

    @Configuration
    static class TracingTestConfiguration {

        @Bean
        SearchTracesUseCase searchTracesUseCase() {
            return mock(SearchTracesUseCase.class);
        }

        @Bean
        GetTraceDetailUseCase getTraceDetailUseCase() {
            return mock(GetTraceDetailUseCase.class);
        }

        @Bean
        GetTraceFilterDefinitionsUseCase getTraceFilterDefinitionsUseCase() {
            return mock(GetTraceFilterDefinitionsUseCase.class);
        }

        @Bean
        GetTraceFilterValuesUseCase getTraceFilterValuesUseCase() {
            return mock(GetTraceFilterValuesUseCase.class);
        }
    }
}
