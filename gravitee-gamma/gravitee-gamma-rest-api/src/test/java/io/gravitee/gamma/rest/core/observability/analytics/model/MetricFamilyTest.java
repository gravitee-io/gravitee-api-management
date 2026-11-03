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
package io.gravitee.gamma.rest.core.observability.analytics.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricFamilyTest {

    @ParameterizedTest
    @CsvSource({ "LLM, LLM", "MESSAGE, MESSAGE", "EDGE, EDGE", "NATIVE, NATIVE", "AUTHZ, AUTHZ" })
    void should_put_a_metric_of_one_kind_in_that_kinds_family(ApiType kind, MetricFamily family) {
        assertThat(MetricFamily.of(List.of(kind))).isEqualTo(family);
    }

    @Test
    void should_put_a_metric_shared_by_several_kinds_in_the_gateway_family() {
        assertThat(MetricFamily.of(List.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MESSAGE, ApiType.MCP, ApiType.A2A))).isEqualTo(
            MetricFamily.GATEWAY
        );
    }

    @ParameterizedTest
    @CsvSource({ "HTTP_PROXY", "MCP", "A2A" })
    void should_put_a_metric_of_a_kind_without_a_family_of_its_own_in_the_gateway_family(ApiType kind) {
        assertThat(MetricFamily.of(List.of(kind))).isEqualTo(MetricFamily.GATEWAY);
    }

    @Test
    void should_count_a_kind_listed_twice_once() {
        assertThat(MetricFamily.of(List.of(ApiType.LLM, ApiType.LLM))).isEqualTo(MetricFamily.LLM);
    }

    @Test
    void should_label_native_kafka_metrics_like_the_editor() {
        assertThat(MetricFamily.NATIVE.label()).isEqualTo("Kafka Service");
    }
}
