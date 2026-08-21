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
package io.gravitee.rest.api.management.v2.rest.resource.observability;

import static assertions.MAPIAssertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ApiName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterSpec;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Operator;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Signal;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityFiltersDefinitionResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";

    @BeforeEach
    void setup() {
        var environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);

        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/observability/filters/definition";
    }

    @Test
    void should_return_filter_definitions() {
        var response = rootTarget().request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterSpecsResponse.class)
            .extracting(FilterSpecsResponse::getData)
            .satisfies(filters -> {
                // The bare count keeps every catalog addition a deliberate decision. On its own it
                // says nothing about what broke, so the names of the last additions come with it.
                assertThat(filters).hasSize(58);
                assertThat(filters)
                    .extracting(filter -> filter.getName().getValue())
                    .contains(
                        "AUTHZ_DECISION",
                        "AUTHZ_OPERATION",
                        "AUTHZ_STATUS",
                        "AUTHZ_CALLER",
                        "AUTHZ_SUBJECT_ID",
                        "AUTHZ_ACTION",
                        "AUTHZ_RESOURCE_ID",
                        "AUTHZ_REASON"
                    );
            });
    }

    @Test
    void should_return_filter_definitions_with_correct_structure() {
        var response = rootTarget().request().get();

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

                var apiTypeFilter = filters
                    .stream()
                    .filter(f -> f.getName().getValue().equals("API_TYPE"))
                    .findFirst()
                    .orElseThrow();
                assertThat(apiTypeFilter.getLabel()).isEqualTo("API Type");
                assertThat(apiTypeFilter.getType()).isEqualTo(FilterSpec.TypeEnum.ENUM);
                assertThat(apiTypeFilter.getOperators()).containsExactlyInAnyOrder(Operator.EQ, Operator.IN);
                assertThat(apiTypeFilter.getEnumValues()).containsExactlyInAnyOrder(
                    "HTTP_PROXY",
                    "LLM",
                    "MESSAGE",
                    "MCP",
                    "A2A",
                    "NATIVE",
                    "EDGE"
                );

                var payloadFilter = filters
                    .stream()
                    .filter(f -> f.getName().getValue().equals("PAYLOAD"))
                    .findFirst()
                    .orElseThrow();
                assertThat(payloadFilter.getLabel()).isEqualTo("Payload content");
                assertThat(payloadFilter.getType()).isEqualTo(FilterSpec.TypeEnum.STRING);
                assertThat(payloadFilter.getOperators()).containsExactly(Operator.CONTAINS);
                assertThat(payloadFilter.getApiTypes()).containsExactlyInAnyOrder(ApiName.HTTP_PROXY, ApiName.LLM, ApiName.MCP);
            });
    }

    @Test
    void should_return_filter_definitions_with_api_types() {
        var response = rootTarget().request().get();

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
                assertThat(apiFilter.getApiTypes()).isNotNull();
                assertThat(apiFilter.getApiTypes()).isNotEmpty();
            });
    }

    @Test
    void should_expose_the_signals_a_filter_applies_to() {
        var response = rootTarget().request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterSpecsResponse.class)
            .extracting(FilterSpecsResponse::getData)
            .satisfies(filters -> {
                assertThat(filterNamed(filters, "API").getSignals()).containsExactlyInAnyOrder(Signal.LOGS, Signal.ANALYTICS);
                // Logs-only: the analytics engine has no counterpart for a payload search.
                assertThat(filterNamed(filters, "PAYLOAD").getSignals()).containsExactly(Signal.LOGS);
                // Unannotated entries fall back to ANALYTICS.
                assertThat(filterNamed(filters, "GATEWAY").getSignals()).containsExactly(Signal.ANALYTICS);
            });
    }

    @Test
    void should_narrow_the_catalog_to_the_requested_signal() {
        var response = rootTarget().queryParam("signal", "LOGS").request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(FilterSpecsResponse.class)
            .extracting(FilterSpecsResponse::getData)
            .satisfies(filters -> {
                assertThat(filters).allSatisfy(filter -> assertThat(filter.getSignals()).contains(Signal.LOGS));
                assertThat(names(filters)).containsExactlyInAnyOrder(
                    "API",
                    "APPLICATION",
                    "PLAN",
                    "API_PRODUCT",
                    "HTTP_METHOD",
                    "HTTP_STATUS",
                    "HTTP_STATUS_CODE_GROUP",
                    "HTTP_PATH",
                    "HTTP_GATEWAY_RESPONSE_TIME",
                    "MCP_PROXY_METHOD",
                    "API_TYPE",
                    "ERROR_KEY",
                    "REQUEST_ID",
                    "TRANSACTION_ID",
                    "PAYLOAD",
                    "TENANT"
                );
                // The filters APIM-14817 reported as offered-but-ignored on the logs screen.
                assertThat(names(filters)).doesNotContain("GATEWAY", "HTTP_ENDPOINT_RESPONSE_TIME", "GEO_IP_COUNTRY");
            });
    }

    @Test
    void should_reject_an_unknown_signal() {
        var response = rootTarget().queryParam("signal", "METRICS").request().get();

        assertThat(response).hasStatus(400);
    }

    private static FilterSpec filterNamed(List<FilterSpec> filters, String name) {
        return filters
            .stream()
            .filter(f -> f.getName().getValue().equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No filter named " + name + " in the catalog"));
    }

    private static List<String> names(List<FilterSpec> filters) {
        return filters
            .stream()
            .map(f -> f.getName().getValue())
            .toList();
    }
}
