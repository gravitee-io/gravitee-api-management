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
package io.gravitee.gamma.rest.resources.observability.filters.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogFlags;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FilterSpecDtoTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void should_serialize_the_catalog_flags_of_a_declared_entry() {
        var spec = new FilterSpec(
            "additional-metrics.double_checkout_basket-value",
            "Basket value",
            FilterType.NUMBER,
            List.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE),
            null,
            null,
            Set.of(Signal.LOGS, Signal.ANALYTICS),
            Set.of(),
            new CatalogFlags(CatalogOrigin.DECLARED, "double_checkout_basket-value", AdditionalMetricValueType.DOUBLE, null, true)
        );

        JsonNode json = MAPPER.valueToTree(FilterSpecDto.from(spec));

        assertThat(json.get("origin").asText()).isEqualTo("DECLARED");
        assertThat(json.get("rawKey").asText()).isEqualTo("double_checkout_basket-value");
        assertThat(json.get("valueType").asText()).isEqualTo("DOUBLE");
        assertThat(json.get("exposed").asBoolean()).isTrue();
        assertThat(json.has("lastSeen")).isFalse();
        assertThat(json.get("apiTypes")).isEmpty();
    }

    @Test
    void should_serialize_the_last_seen_of_an_entry_carrying_one() {
        var spec = new FilterSpec(
            "additional-metrics.long_failover_count",
            "Failover count",
            FilterType.NUMBER,
            List.of(FilterOperator.EQ),
            null,
            null,
            Set.of(Signal.LOGS),
            Set.of(),
            new CatalogFlags(
                CatalogOrigin.DISCOVERED,
                "long_failover_count",
                AdditionalMetricValueType.LONG,
                Instant.parse("2026-10-29T08:00:00Z"),
                true
            )
        );

        JsonNode json = MAPPER.valueToTree(FilterSpecDto.from(spec));

        assertThat(json.get("lastSeen").asText()).isEqualTo("2026-10-29T08:00:00Z");
    }

    @Test
    void should_omit_the_catalog_flags_of_a_built_in_entry() {
        var spec = new FilterSpec(
            "HTTP_STATUS",
            "Status Code",
            FilterType.NUMBER,
            List.of(FilterOperator.EQ),
            null,
            new FilterSpec.Range(100, 599),
            Set.of(Signal.LOGS),
            Set.of(ApiType.HTTP_PROXY)
        );

        JsonNode json = MAPPER.valueToTree(FilterSpecDto.from(spec));

        assertThat(json.has("origin")).isFalse();
        assertThat(json.has("rawKey")).isFalse();
        assertThat(json.has("exposed")).isFalse();
    }
}
