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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.EQ;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.IN;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.v4.ApiType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiTypePreProcessorTest extends AbstractFilterProcessor {

    static final String PROXY_API_ID = "proxy-api-id";
    static final String MESSAGE_API_ID = "message-api-id";
    static final String NATIVE_API_ID = "native-api-id";
    static final String LLM_API_ID = "llm-api-id";
    static final String MCP_API_ID = "mcp-api-id";

    List<Api> apis = List.of(
        Api.builder().id(PROXY_API_ID).type(ApiType.PROXY).build(),
        Api.builder().id(MESSAGE_API_ID).type(ApiType.MESSAGE).build(),
        Api.builder().id(NATIVE_API_ID).type(ApiType.NATIVE).build()
    );

    MetricsContext metricsContext;

    private final ApiTypePreProcessor apiTypePreProcessor = new ApiTypePreProcessor();

    @BeforeEach
    void setUp() {
        metricsContext = new MetricsContext(auditInfo).withApis(apis);
    }

    @Test
    void should_return_api_filter_empty_when_context_has_no_apis() {
        metricsContext = new MetricsContext(auditInfo).withApis(Collections.emptyList());

        var results = apiTypePreProcessor.buildFilters(metricsContext, List.of(new Filter(FilterSpec.Name.API_NAME, EQ, "HTTP_PROXY")));

        assertThat(results).hasSize(1);
        var filter = results.getFirst();
        assertThat(filter.name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filter.operator()).isEqualTo(IN);
        assertThat(filter.value()).asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
    }

    @Test
    void should_return_empty_list_when_no_filters() {
        var results = apiTypePreProcessor.buildFilters(metricsContext, List.of());

        assertThat(results).isEmpty();
    }

    @Test
    void should_return_empty_list_when_no_api_name_filter_present() {
        var filters = List.of(new Filter(FilterSpec.Name.APPLICATION, EQ, "some-app"));

        var results = apiTypePreProcessor.buildFilters(metricsContext, filters);

        assertThat(results).isEmpty();
    }

    @Test
    void should_filter_apis_matching_single_api_name() {
        var filters = List.of(new Filter(FilterSpec.Name.API_NAME, EQ, "HTTP_PROXY"));

        var results = apiTypePreProcessor.buildFilters(metricsContext, filters);

        assertThat(results).hasSize(1);
        var filter = results.getFirst();
        assertThat(filter.name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filter.operator()).isEqualTo(FilterSpec.Operator.IN);
        assertThat(filter.value()).asInstanceOf(InstanceOfAssertFactories.LIST).containsExactly(PROXY_API_ID);
    }

    @Test
    void should_filter_apis_matching_multiple_api_names() {
        var filters = List.of(new Filter(FilterSpec.Name.API_NAME, IN, List.of("HTTP_PROXY", "MESSAGE")));

        var results = apiTypePreProcessor.buildFilters(metricsContext, filters);

        assertThat(results).hasSize(1);
        var filter = results.getFirst();
        assertThat(filter.name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filter.operator()).isEqualTo(FilterSpec.Operator.IN);
        assertThat(filter.value()).asInstanceOf(InstanceOfAssertFactories.LIST).containsExactlyInAnyOrder(PROXY_API_ID, MESSAGE_API_ID);
    }

    @Test
    void should_return_empty_filter_when_api_name_matches_no_apis() {
        // Context has PROXY, MESSAGE, NATIVE. Let's filter for LLM which is not in
        // context
        var filters = List.of(new Filter(FilterSpec.Name.API_NAME, EQ, "LLM"));

        var results = apiTypePreProcessor.buildFilters(metricsContext, filters);

        assertThat(results).hasSize(1);
        var filter = results.getFirst();
        assertThat(filter.name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filter.operator()).isEqualTo(FilterSpec.Operator.IN);
        assertThat(filter.value()).asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
    }

    @Test
    void should_return_empty_filter_when_multiple_api_name_filters_conflict() {
        var filters = List.of(new Filter(FilterSpec.Name.API_NAME, EQ, "HTTP_PROXY"), new Filter(FilterSpec.Name.API_NAME, EQ, "MESSAGE"));

        var results = apiTypePreProcessor.buildFilters(metricsContext, filters);

        assertThat(results).hasSize(1);
        var filter = results.getFirst();
        assertThat(filter.name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filter.operator()).isEqualTo(FilterSpec.Operator.IN);
        assertThat(filter.value()).asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generateEquivalentFilters")
    void should_return_apis_when_filters_are_equivalent(String name, List<Filter> filters, List<String> expectedApiTypes) {
        var results = apiTypePreProcessor.buildFilters(metricsContext, filters);

        assertThat(results).hasSize(1);
        var filter = results.getFirst();
        assertThat(filter.name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filter.operator()).isEqualTo(FilterSpec.Operator.IN);
        assertThat(filter.value()).asInstanceOf(InstanceOfAssertFactories.LIST).containsExactlyInAnyOrderElementsOf(expectedApiTypes);
    }

    @Test
    void should_return_empty_filter_when_api_names_are_unknown() {
        var filters = List.of(new Filter(FilterSpec.Name.API_NAME, IN, List.of("HTTP_PROXY", "UNKNOWN_TYPE")));

        var results = apiTypePreProcessor.buildFilters(metricsContext, filters);

        assertThat(results).hasSize(1);
        var filter = results.getFirst();
        assertThat(filter.name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filter.operator()).isEqualTo(FilterSpec.Operator.IN);
        assertThat(filter.value()).asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
    }

    @Test
    void should_return_empty_filter_when_all_api_names_are_unknown() {
        var filters = List.of(new Filter(FilterSpec.Name.API_NAME, EQ, "UNKNOWN_TYPE"));

        var results = apiTypePreProcessor.buildFilters(metricsContext, filters);

        assertThat(results).hasSize(1);
        var filter = results.getFirst();
        assertThat(filter.name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filter.operator()).isEqualTo(FilterSpec.Operator.IN);
        assertThat(filter.value()).asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("generateApiTypeFilters")
    void should_map_all_api_names_correctly(Filter filter, String expectedApiType) {
        // This test verifies that we can filter by all supported types
        // We add missing types to a specific context for this test
        var allTypesApis = List.of(
            Api.builder().id(PROXY_API_ID).type(ApiType.PROXY).build(),
            Api.builder().id(MESSAGE_API_ID).type(ApiType.MESSAGE).build(),
            Api.builder().id(NATIVE_API_ID).type(ApiType.NATIVE).build(),
            Api.builder().id(LLM_API_ID).type(ApiType.LLM_PROXY).build(),
            Api.builder().id(MCP_API_ID).type(ApiType.MCP_PROXY).build()
        );

        var context = new MetricsContext(auditInfo).withApis(allTypesApis);

        assertThat(apiTypePreProcessor.buildFilters(context, List.of(filter)).getFirst().value())
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .containsExactly(expectedApiType);
    }

    public static Stream<Arguments> generateEquivalentFilters() {
        return Stream.of(
            Arguments.of(
                "Multiple EQ filters",
                List.of(new Filter(FilterSpec.Name.API_NAME, EQ, "MESSAGE"), new Filter(FilterSpec.Name.API_NAME, EQ, "MESSAGE")),
                List.of(MESSAGE_API_ID)
            ),
            Arguments.of(
                "Multiple IN filters with elements out of order",
                List.of(
                    new Filter(FilterSpec.Name.API_NAME, IN, List.of("MESSAGE", "HTTP_PROXY")),
                    new Filter(FilterSpec.Name.API_NAME, IN, List.of("HTTP_PROXY", "MESSAGE"))
                ),
                List.of(MESSAGE_API_ID, PROXY_API_ID)
            ),
            Arguments.of(
                "Filters contain EQ and IN operators",
                List.of(
                    new Filter(FilterSpec.Name.API_NAME, EQ, "HTTP_PROXY"),
                    new Filter(FilterSpec.Name.API_NAME, IN, List.of("HTTP_PROXY"))
                ),
                List.of(PROXY_API_ID)
            )
        );
    }

    public static Stream<Arguments> generateApiTypeFilters() {
        return Stream.of(
            Arguments.of(new Filter(FilterSpec.Name.API_NAME, EQ, "HTTP_PROXY"), PROXY_API_ID),
            Arguments.of(new Filter(FilterSpec.Name.API_NAME, EQ, "MESSAGE"), MESSAGE_API_ID),
            Arguments.of(new Filter(FilterSpec.Name.API_NAME, EQ, "KAFKA"), NATIVE_API_ID),
            Arguments.of(new Filter(FilterSpec.Name.API_NAME, EQ, "MCP"), MCP_API_ID),
            Arguments.of(new Filter(FilterSpec.Name.API_NAME, EQ, "LLM"), LLM_API_ID)
        );
    }
}
