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
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.definition.model.DefinitionVersion;
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
class DefinitionVersionFilterTransformerTest {

    private static final String ENVIRONMENT_ID = "DEFAULT";

    private final DefinitionVersionFilterTransformer transformer = new DefinitionVersionFilterTransformer();

    private static AnalyticsQueryContext buildContext(
        Set<String> authorizedApiIds,
        Map<DefinitionVersion, Set<String>> apiIdsByDefinitionVersion
    ) {
        var actor = AuditActor.builder().userId(UUID.randomUUID().toString()).build();
        var auditInfo = AuditInfo.builder().organizationId("DEFAULT").environmentId(ENVIRONMENT_ID).actor(actor).build();
        return new AnalyticsQueryContext(
            auditInfo,
            new ExecutionContext("DEFAULT", ENVIRONMENT_ID),
            authorizedApiIds,
            Map.of(),
            Map.of(),
            Map.of(),
            apiIdsByDefinitionVersion
        );
    }

    private static AnalyticsQueryContext buildContext(Set<String> authorizedApiIds) {
        return buildContext(authorizedApiIds, Map.of());
    }

    private static final Map<DefinitionVersion, Set<String>> STANDARD_API_IDS_BY_VERSION = Map.of(
        DefinitionVersion.V2,
        Set.of("api-v2"),
        DefinitionVersion.V4,
        Set.of("api-v4-1", "api-v4-2"),
        DefinitionVersion.FEDERATED,
        Set.of("api-fed")
    );

    @Test
    void should_leave_filters_untouched_when_no_definition_version_filter() {
        var context = buildContext(Set.of("api-1", "api-2"));
        var existingFilter = new Filter(FilterSpec.Name.APPLICATION, FilterOperator.EQ, "app-1");

        var filters = transformer.transform(context, List.of(existingFilter));

        assertThat(filters).hasSize(1);
        assertThat(filters).containsExactly(existingFilter);
    }

    @Test
    void should_narrow_to_v4_apis_with_eq_operator() {
        var context = buildContext(Set.of("api-v2", "api-v4-1", "api-v4-2", "api-fed"), STANDARD_API_IDS_BY_VERSION);

        var filter = new Filter(FilterSpec.Name.DEFINITION_VERSION, FilterOperator.EQ, "V4");
        var result = transformer.transform(context, List.of(filter));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo(FilterSpec.Name.API);
        assertThat(result.getFirst().operator()).isEqualTo(FilterOperator.IN);
        assertThat(result.getFirst().value())
            .asInstanceOf(InstanceOfAssertFactories.collection(String.class))
            .containsExactlyInAnyOrder("api-v4-1", "api-v4-2");
    }

    @Test
    void should_narrow_to_v2_apis_with_eq_operator() {
        var context = buildContext(Set.of("api-v2", "api-v4-1"), STANDARD_API_IDS_BY_VERSION);

        var filter = new Filter(FilterSpec.Name.DEFINITION_VERSION, FilterOperator.EQ, "V2");
        var result = transformer.transform(context, List.of(filter));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo(FilterSpec.Name.API);
        assertThat(result.getFirst().value()).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).containsExactly("api-v2");
    }

    @Test
    void should_narrow_to_multiple_definition_versions_with_in_operator() {
        var context = buildContext(Set.of("api-v2", "api-v4-1", "api-v4-2", "api-fed"), STANDARD_API_IDS_BY_VERSION);

        var filter = new Filter(FilterSpec.Name.DEFINITION_VERSION, FilterOperator.IN, List.of("V4", "FEDERATED"));
        var result = transformer.transform(context, List.of(filter));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo(FilterSpec.Name.API);
        assertThat(result.getFirst().value())
            .asInstanceOf(InstanceOfAssertFactories.collection(String.class))
            .containsExactlyInAnyOrder("api-v4-1", "api-v4-2", "api-fed");
    }

    @Test
    void should_preserve_existing_filters() {
        var context = buildContext(Set.of("api-v4-1"), Map.of(DefinitionVersion.V4, Set.of("api-v4-1")));
        var existingFilter = new Filter(FilterSpec.Name.APPLICATION, FilterOperator.EQ, "app-1");

        var filter = new Filter(FilterSpec.Name.DEFINITION_VERSION, FilterOperator.EQ, "V4");
        var result = transformer.transform(context, List.of(existingFilter, filter));

        assertThat(result).hasSize(2);
        assertThat(result).contains(existingFilter);
        assertThat(result).anyMatch(f -> f.name() == FilterSpec.Name.API);
    }

    @Test
    void should_remove_definition_version_filter_from_output() {
        var context = buildContext(Set.of("api-v4-1"), Map.of(DefinitionVersion.V4, Set.of("api-v4-1")));

        var defVersionFilter = new Filter(FilterSpec.Name.DEFINITION_VERSION, FilterOperator.EQ, "V4");
        var otherFilter = new Filter(FilterSpec.Name.APPLICATION, FilterOperator.EQ, "app-1");

        var result = transformer.transform(context, List.of(defVersionFilter, otherFilter));

        assertThat(result).noneMatch(f -> f.name() == FilterSpec.Name.DEFINITION_VERSION);
        assertThat(result).anyMatch(f -> f.name() == FilterSpec.Name.APPLICATION);
        assertThat(result).anyMatch(f -> f.name() == FilterSpec.Name.API);
    }

    @Test
    void should_return_empty_api_filter_when_version_has_no_matching_apis() {
        var context = buildContext(Set.of("api-v4-1"), Map.of(DefinitionVersion.V4, Set.of("api-v4-1")));

        var filter = new Filter(FilterSpec.Name.DEFINITION_VERSION, FilterOperator.EQ, "V2");
        var result = transformer.transform(context, List.of(filter));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo(FilterSpec.Name.API);
        assertThat(result.getFirst().value()).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).isEmpty();
    }

    @Test
    void should_throw_on_unknown_definition_version() {
        var context = buildContext(Set.of("api-1"));
        var filter = new Filter(FilterSpec.Name.DEFINITION_VERSION, FilterOperator.EQ, "UNKNOWN");

        assertThatThrownBy(() -> transformer.transform(context, List.of(filter)))
            .isInstanceOf(InvalidQueryException.class)
            .hasMessageContaining("Unknown definition version 'UNKNOWN'");
    }

    @Test
    void should_throw_on_null_definition_version() {
        var context = buildContext(Set.of("api-1"));
        var filter = new Filter(FilterSpec.Name.DEFINITION_VERSION, FilterOperator.EQ, null);

        assertThatThrownBy(() -> transformer.transform(context, List.of(filter)))
            .isInstanceOf(InvalidQueryException.class)
            .hasMessageContaining("requires a non-null value");
    }

    @Test
    void should_map_federated_agent_version() {
        var context = buildContext(Set.of("api-fa"), Map.of(DefinitionVersion.FEDERATED_AGENT, Set.of("api-fa")));

        var filter = new Filter(FilterSpec.Name.DEFINITION_VERSION, FilterOperator.EQ, "FEDERATED_AGENT");
        var result = transformer.transform(context, List.of(filter));

        assertThat(result.getFirst().value()).asInstanceOf(InstanceOfAssertFactories.collection(String.class)).containsExactly("api-fa");
    }
}
