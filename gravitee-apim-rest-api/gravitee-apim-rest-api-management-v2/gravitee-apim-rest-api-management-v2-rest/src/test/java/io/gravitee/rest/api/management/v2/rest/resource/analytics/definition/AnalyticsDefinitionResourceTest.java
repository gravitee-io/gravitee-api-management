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
package io.gravitee.rest.api.management.v2.rest.resource.analytics.definition;

import static assertions.MAPIAssertions.assertThat;

import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ApiName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ApiSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetSpec;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterSpec;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricType;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Operator;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsDefinitionResourceTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/analytics/definition";
    }

    @Test
    void should_return_api_specs() {
        var response = rootTarget().path("apis").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(ApiSpecsResponse.class)
            .extracting(ApiSpecsResponse::getData)
            .satisfies(apis -> assertThat(apis).isNotEmpty());
    }

    @Test
    void should_return_api_specs_with_correct_structure() {
        var response = rootTarget().path("apis").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(ApiSpecsResponse.class)
            .extracting(ApiSpecsResponse::getData)
            .satisfies(apis -> {
                var httpProxy = apis
                    .stream()
                    .filter(a -> a.getName().equals(ApiName.HTTP_PROXY))
                    .findFirst()
                    .orElseThrow();
                assertThat(httpProxy.getLabel()).isEqualTo("HTTP Proxy");

                var llm = apis
                    .stream()
                    .filter(a -> a.getName().equals(ApiName.LLM))
                    .findFirst()
                    .orElseThrow();
                assertThat(llm.getLabel()).isEqualTo("LLM");
            });
    }

    @Test
    void should_return_api_metrics() {
        var response = rootTarget().path("apis").path("HTTP_PROXY").path("metrics").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(MetricSpecsResponse.class)
            .extracting(MetricSpecsResponse::getData)
            .satisfies(metrics -> assertThat(metrics).isNotEmpty());
    }

    @Test
    void should_return_api_metrics_with_correct_structure() {
        var response = rootTarget().path("apis").path("HTTP_PROXY").path("metrics").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(MetricSpecsResponse.class)
            .extracting(MetricSpecsResponse::getData)
            .satisfies(metrics -> {
                var httpRequests = metrics
                    .stream()
                    .filter(m -> m.getName().equals(MetricName.HTTP_REQUESTS))
                    .findFirst()
                    .orElseThrow();
                assertThat(httpRequests.getLabel()).isEqualTo("HTTP Requests");
                assertThat(httpRequests.getType()).isEqualTo(MetricType.COUNTER);
                assertThat(httpRequests.getApis()).contains(ApiName.HTTP_PROXY);
                assertThat(httpRequests.getMeasures()).isNotEmpty();
                assertThat(httpRequests.getFacets()).isNotEmpty();
                assertThat(httpRequests.getFilters()).isNotEmpty();
            });
    }

    @Test
    void should_return_metric_filters() {
        var response = rootTarget().path("metrics").path("HTTP_REQUESTS").path("filters").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterSpecsResponse.class)
            .extracting(FilterSpecsResponse::getData)
            .satisfies(filters -> assertThat(filters).isNotEmpty());
    }

    @Test
    void should_return_metric_filters_with_correct_structure() {
        var response = rootTarget().path("metrics").path("HTTP_REQUESTS").path("filters").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterSpecsResponse.class)
            .extracting(FilterSpecsResponse::getData)
            .satisfies(filters -> {
                var apiFilter = filters
                    .stream()
                    .filter(f -> f.getName().getValue().equals("API"))
                    .findFirst()
                    .orElseThrow();
                assertThat(apiFilter.getLabel()).isEqualTo("API");
                assertThat(apiFilter.getType()).isEqualTo(FilterSpec.TypeEnum.KEYWORD);
                assertThat(apiFilter.getOperators()).containsExactlyInAnyOrder(Operator.EQ, Operator.IN);

                var httpStatusFilter = filters
                    .stream()
                    .filter(f -> f.getName().getValue().equals("HTTP_STATUS"))
                    .findFirst()
                    .orElseThrow();
                assertThat(httpStatusFilter.getLabel()).isEqualTo("Status Code");
                assertThat(httpStatusFilter.getType()).isEqualTo(FilterSpec.TypeEnum.NUMBER);
                assertThat(httpStatusFilter.getOperators()).containsExactlyInAnyOrder(Operator.EQ, Operator.LTE, Operator.GTE);
                assertThat(httpStatusFilter.getRange()).isNotNull();
                assertThat(httpStatusFilter.getRange().getMin()).isEqualTo(100);
                assertThat(httpStatusFilter.getRange().getMax()).isEqualTo(599);
            });
    }

    @Test
    void should_return_metric_facets() {
        var response = rootTarget().path("metrics").path("HTTP_REQUESTS").path("facets").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FacetSpecsResponse.class)
            .extracting(FacetSpecsResponse::getData)
            .satisfies(facets -> assertThat(facets).isNotEmpty());
    }

    @Test
    void should_return_metric_facets_with_correct_structure() {
        var response = rootTarget().path("metrics").path("HTTP_REQUESTS").path("facets").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FacetSpecsResponse.class)
            .extracting(FacetSpecsResponse::getData)
            .satisfies(facets -> {
                var apiFacet = facets
                    .stream()
                    .filter(f -> f.getName().getValue().equals("API"))
                    .findFirst()
                    .orElseThrow();
                assertThat(apiFacet.getLabel()).isEqualTo("API");
                assertThat(apiFacet.getType()).isEqualTo(FacetSpec.TypeEnum.KEYWORD);

                var httpStatusFacet = facets
                    .stream()
                    .filter(f -> f.getName().getValue().equals("HTTP_STATUS"))
                    .findFirst()
                    .orElseThrow();
                assertThat(httpStatusFacet.getLabel()).isEqualTo("Status Code");
                assertThat(httpStatusFacet.getType()).isEqualTo(FacetSpec.TypeEnum.NUMBER);
            });
    }

    @Test
    void should_return_error_for_unknown_metric_facets() {
        var response = rootTarget().path("metrics").path("FOO").path("facets").request().get();

        assertThat(response).hasStatus(400).asError().hasHttpStatus(400).hasMessage("Invalid metric name");
    }

    @Test
    void should_return_error_for_unknown_metric_filters() {
        var response = rootTarget().path("metrics").path("BAR").path("filters").request().get();

        assertThat(response).hasStatus(400).asError().hasHttpStatus(400).hasMessage("Invalid metric name");
    }

    @Test
    void should_return_error_for_unknown_api_metrics() {
        var response = rootTarget().path("apis").path("BAZ").path("metrics").request().get();

        assertThat(response).hasStatus(400).asError().hasHttpStatus(400).hasMessage("Invalid api name");
    }
}
