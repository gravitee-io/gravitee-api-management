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

import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ApiSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricSpecsResponse;
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
    void should_return_api_metrics() {
        var response = rootTarget().path("apis").path("HTTP_PROXY").path("metrics").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(MetricSpecsResponse.class)
            .extracting(MetricSpecsResponse::getData)
            .satisfies(metrics -> assertThat(metrics).isNotEmpty());
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
    void should_return_metric_facets() {
        var response = rootTarget().path("metrics").path("HTTP_REQUESTS").path("facets").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FacetSpecsResponse.class)
            .extracting(FacetSpecsResponse::getData)
            .satisfies(facets -> assertThat(facets).isNotEmpty());
    }

    @Test
    void should_return_error_for_unknown_metric_facets() {
        var response = rootTarget().path("metrics").path("FOO").path("facets").request().get();

        assertThat(response).hasStatus(400);
    }

    @Test
    void should_return_error_for_unknown_metric_filters() {
        var response = rootTarget().path("metrics").path("BAR").path("facets").request().get();

        assertThat(response).hasStatus(400);
    }

    @Test
    void should_return_error_for_unknown_api_metrics() {
        var response = rootTarget().path("apis").path("BAZ").path("metrics").request().get();

        assertThat(response).hasStatus(400);
    }
}
