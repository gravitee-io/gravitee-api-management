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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiTypeFilterTransformerTest {

    private static final String ENVIRONMENT_ID = "DEFAULT";

    private final ApiTypeFilterTransformer transformer = new ApiTypeFilterTransformer();

    private static AnalyticsQueryContext buildContext(Set<String> authorizedApiIds, Map<ApiType, Set<String>> apiIdsByType) {
        var actor = AuditActor.builder().userId(UUID.randomUUID().toString()).build();
        var auditInfo = AuditInfo.builder().organizationId("DEFAULT").environmentId(ENVIRONMENT_ID).actor(actor).build();
        return new AnalyticsQueryContext(
            auditInfo,
            new ExecutionContext("DEFAULT", ENVIRONMENT_ID),
            authorizedApiIds,
            Map.of(),
            Map.of(),
            apiIdsByType
        );
    }

    private static AnalyticsQueryContext buildContext(Set<String> authorizedApiIds) {
        return buildContext(authorizedApiIds, Map.of());
    }

    private static final Map<ApiType, Set<String>> STANDARD_API_IDS_BY_TYPE = Map.of(
        ApiType.PROXY,
        Set.of("api-1"),
        ApiType.MCP_PROXY,
        Set.of("api-2"),
        ApiType.LLM_PROXY,
        Set.of("api-3")
    );

    @Test
    void should_add_api_filter_with_authorized_ids_when_no_api_type() {
        var authorizedApiIds = Set.of("api-1", "api-2", "api-3");
        var context = buildContext(authorizedApiIds);

        var filters = transformer.transform(context, List.of());

        assertThat(filters).hasSize(1);
        assertThat(filters.getFirst().name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filters.getFirst().operator()).isEqualTo(FilterSpec.Operator.IN);
        assertThat(filters.getFirst().value())
            .asInstanceOf(InstanceOfAssertFactories.collection(String.class))
            .containsExactlyInAnyOrderElementsOf(authorizedApiIds);
    }

    @Test
    void should_preserve_existing_filters() {
        var context = buildContext(Set.of("api-1"), Map.of(ApiType.PROXY, Set.of("api-1")));
        var existingFilter = new Filter(FilterSpec.Name.APPLICATION, FilterSpec.Operator.EQ, "app-1");

        var filters = transformer.transform(context, List.of(existingFilter));

        assertThat(filters).hasSize(2);
        assertThat(filters).contains(existingFilter);
    }

    @Test
    void should_add_empty_api_filter_when_no_authorized_apis() {
        var context = buildContext(Set.of());

        var filters = transformer.transform(context, List.of());

        assertThat(filters).hasSize(1);
        assertThat(filters.getFirst().name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filters.getFirst().value()).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).isEmpty();
    }

    @Test
    void should_narrow_to_mcp_apis_with_eq_operator() {
        var context = buildContext(Set.of("api-1", "api-2", "api-3"), STANDARD_API_IDS_BY_TYPE);

        var apiTypeFilter = new Filter(FilterSpec.Name.API_TYPE, FilterSpec.Operator.EQ, "MCP");
        var filters = transformer.transform(context, List.of(apiTypeFilter));

        assertThat(filters).hasSize(1);
        assertThat(filters.getFirst().name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filters.getFirst().operator()).isEqualTo(FilterSpec.Operator.IN);
        assertThat(filters.getFirst().value()).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).containsExactly("api-2");
    }

    @Test
    void should_narrow_to_multiple_api_types_with_in_operator() {
        var context = buildContext(Set.of("api-1", "api-2", "api-3"), STANDARD_API_IDS_BY_TYPE);

        var apiTypeFilter = new Filter(FilterSpec.Name.API_TYPE, FilterSpec.Operator.IN, List.of("LLM", "MCP"));
        var filters = transformer.transform(context, List.of(apiTypeFilter));

        assertThat(filters).hasSize(1);
        assertThat(filters.getFirst().name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filters.getFirst().value())
            .asInstanceOf(InstanceOfAssertFactories.collection(String.class))
            .containsExactlyInAnyOrder("api-2", "api-3");
    }

    @Test
    void should_remove_api_type_filter_from_output() {
        var context = buildContext(Set.of("api-1"), Map.of(ApiType.PROXY, Set.of("api-1")));

        var apiTypeFilter = new Filter(FilterSpec.Name.API_TYPE, FilterSpec.Operator.EQ, "HTTP_PROXY");
        var otherFilter = new Filter(FilterSpec.Name.APPLICATION, FilterSpec.Operator.EQ, "app-1");

        var filters = transformer.transform(context, List.of(apiTypeFilter, otherFilter));

        assertThat(filters).noneMatch(f -> f.name() == FilterSpec.Name.API_TYPE);
        assertThat(filters).anyMatch(f -> f.name() == FilterSpec.Name.APPLICATION);
        assertThat(filters).anyMatch(f -> f.name() == FilterSpec.Name.API);
    }

    @Test
    void should_return_empty_api_filter_when_api_type_present_but_no_authorized_apis() {
        var context = buildContext(Set.of());
        var apiTypeFilter = new Filter(FilterSpec.Name.API_TYPE, FilterSpec.Operator.EQ, "MCP");

        var filters = transformer.transform(context, List.of(apiTypeFilter));

        assertThat(filters).hasSize(1);
        assertThat(filters.getFirst().name()).isEqualTo(FilterSpec.Name.API);
        assertThat(filters.getFirst().value()).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).isEmpty();
    }

    @Test
    void should_map_http_proxy_to_proxy_api_type() {
        var context = buildContext(Set.of("api-1"), Map.of(ApiType.PROXY, Set.of("api-1")));

        var apiTypeFilter = new Filter(FilterSpec.Name.API_TYPE, FilterSpec.Operator.EQ, "HTTP_PROXY");
        var filters = transformer.transform(context, List.of(apiTypeFilter));

        assertThat(filters.getFirst().value()).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).containsExactly("api-1");
    }

    @Test
    void should_map_message_to_message_api_type() {
        var context = buildContext(Set.of("api-4"), Map.of(ApiType.MESSAGE, Set.of("api-4")));

        var apiTypeFilter = new Filter(FilterSpec.Name.API_TYPE, FilterSpec.Operator.EQ, "MESSAGE");
        var filters = transformer.transform(context, List.of(apiTypeFilter));

        assertThat(filters.getFirst().value()).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).containsExactly("api-4");
    }

    @Test
    void should_map_kafka_to_native_api_type() {
        var context = buildContext(Set.of("api-5"), Map.of(ApiType.NATIVE, Set.of("api-5")));

        var apiTypeFilter = new Filter(FilterSpec.Name.API_TYPE, FilterSpec.Operator.EQ, "KAFKA");
        var filters = transformer.transform(context, List.of(apiTypeFilter));

        assertThat(filters.getFirst().value()).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).containsExactly("api-5");
    }

    @Test
    void should_throw_on_unknown_api_type_value() {
        var context = buildContext(Set.of("api-1"));
        var apiTypeFilter = new Filter(FilterSpec.Name.API_TYPE, FilterSpec.Operator.EQ, "UNKNOWN");

        assertThatThrownBy(() -> transformer.transform(context, List.of(apiTypeFilter)))
            .isInstanceOf(InvalidQueryException.class)
            .hasMessageContaining("Unknown API type UNKNOWN");
    }

    @Test
    void should_only_return_ids_present_in_apiIdsByType() {
        var apiIdsByType = Map.of(ApiType.PROXY, Set.of("api-1"), ApiType.LLM_PROXY, Set.of("api-3"));
        var context = buildContext(Set.of("api-1", "api-3"), apiIdsByType);

        var apiTypeFilter = new Filter(FilterSpec.Name.API_TYPE, FilterSpec.Operator.EQ, "MCP");
        var filters = transformer.transform(context, List.of(apiTypeFilter));

        assertThat(filters.getFirst().value())
            .asInstanceOf(InstanceOfAssertFactories.collection(String.class))
            .as("MCP_PROXY type has no entries in apiIdsByType, so result should be empty")
            .isEmpty();
    }

    @Test
    void should_return_authorized_mcp_apis_only_when_multiple_types_exist() {
        var context = buildContext(Set.of("api-2", "api-3"), STANDARD_API_IDS_BY_TYPE);

        var apiTypeFilter = new Filter(FilterSpec.Name.API_TYPE, FilterSpec.Operator.EQ, "MCP");
        var filters = transformer.transform(context, List.of(apiTypeFilter));

        assertThat(filters.getFirst().value()).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).containsExactly("api-2");
    }
}
