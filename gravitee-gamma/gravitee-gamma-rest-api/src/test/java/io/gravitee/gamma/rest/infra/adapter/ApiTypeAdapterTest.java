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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.infra.domain_service.analytics_engine.processors.ApiTypeFilterTransformer;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiTypeAdapterTest {

    @ParameterizedTest
    @EnumSource(io.gravitee.definition.model.v4.ApiType.class)
    void should_translate_every_definition_type_back_to_itself(io.gravitee.definition.model.v4.ApiType definitionType) {
        assertThat(ApiTypeAdapter.toDefinition(ApiTypeAdapter.toObservability(definitionType))).contains(definitionType);
    }

    @Test
    void should_give_every_api_kind_a_definition_type_and_none_to_the_decision_scope() {
        var mapped = Arrays.stream(io.gravitee.definition.model.v4.ApiType.values())
            .map(ApiTypeAdapter::toObservability)
            .collect(Collectors.toSet());

        assertThat(mapped).isEqualTo(ApiType.API_KINDS);
        assertThat(ApiTypeAdapter.toDefinition(ApiType.AUTHZ_DECISION)).isEmpty();
    }

    // The analytics engine resolves API_TYPE with its own table: every kind the catalog lets a caller pick
    // must resolve there, or analytics answers 400 where logs answers rows (AUTHZ did, NATIVE before it).
    @ParameterizedTest
    @MethodSource("apiKinds")
    void should_resolve_every_api_kind_on_the_analytics_engine(ApiType apiType) {
        var definitionType = ApiTypeAdapter.toDefinition(apiType).orElseThrow();
        var context = new AnalyticsQueryContext(
            AuditInfo.builder().organizationId("org").environmentId("env").actor(AuditActor.builder().userId("user").build()).build(),
            new ExecutionContext("org", "env"),
            Set.of("api-1"),
            Map.of(),
            Map.of(),
            Map.of(definitionType, Set.of("api-1"))
        );

        var filters = new ApiTypeFilterTransformer().transform(
            context,
            List.of(new Filter(FilterSpec.Name.API_TYPE, FilterOperator.EQ, apiType.name()))
        );

        assertThat(filters).singleElement().extracting(Filter::value).isEqualTo(Set.of("api-1"));
    }

    static Stream<ApiType> apiKinds() {
        return ApiType.API_KINDS.stream();
    }
}
