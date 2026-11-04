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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricUnit;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ServedMetricUnitTest {

    @ParameterizedTest
    @EnumSource(MetricSpec.Unit.class)
    void should_serve_every_engine_unit_as_a_named_unit_of_the_gamma_vocabulary(MetricSpec.Unit unit) {
        assertThat(AdditionalMetricUnit.NAMED_UNITS).contains(ServedMetricUnit.of(unit));
    }

    @ParameterizedTest
    @EnumSource(value = MetricSpec.Unit.class, names = "NUMBER", mode = EnumSource.Mode.EXCLUDE)
    void should_serve_every_other_engine_unit_by_its_name(MetricSpec.Unit unit) {
        assertThat(ServedMetricUnit.of(unit)).isEqualTo(unit.name());
    }

    @Test
    void should_serve_a_number_as_a_count() {
        assertThat(ServedMetricUnit.of(MetricSpec.Unit.NUMBER)).isEqualTo("COUNT");
    }

    @Test
    void should_rewrite_an_engine_unit_at_any_depth_and_leave_any_other_value_as_is() {
        var response = JsonNodeFactory.instance.objectNode();
        var nested = response.putObject("data").putArray("groups").addObject();
        nested.putObject("metric").put("unit", "NUMBER");
        nested.put("unit", "MILLISECONDS");
        response.putObject("declared").put("unit", "€");
        response.putObject("served").put("unit", "COUNT_PER_SECOND");
        response.putObject("none").putNull("unit");
        response.putObject("odd").put("unit", 3);

        ServedMetricUnit.serveIn(response);

        assertThat(response.at("/data/groups/0/metric/unit").asText()).isEqualTo("COUNT");
        assertThat(response.at("/data/groups/0/unit").asText()).isEqualTo("MILLISECONDS");
        assertThat(response.at("/declared/unit").asText()).isEqualTo("€");
        assertThat(response.at("/served/unit").asText()).isEqualTo("COUNT_PER_SECOND");
        assertThat(response.at("/none/unit").isNull()).isTrue();
        assertThat(response.at("/odd/unit").asInt()).isEqualTo(3);
    }

    @Test
    void should_serve_the_platform_currency_next_to_a_currency_unit_only() {
        var response = JsonNodeFactory.instance.objectNode();
        var metrics = response.putArray("metrics");
        metrics.addObject().put("name", "LLM_PROMPT_TOKEN_TOTAL_COST").put("unit", "CURRENCY");
        metrics.addObject().put("name", "HTTP_REQUESTS").put("unit", "NUMBER");
        metrics.addObject().put("name", "LLM_PROMPT_TOTAL_TOKEN").put("unit", "TOKENS");
        metrics.addObject().put("name", "additional-metrics.double_basket").put("unit", "€");

        ServedMetricUnit.serveIn(response);
        ServedMetricUnit.serveIn(response);

        assertThat(metrics.get(0).get("unit").asText()).isEqualTo("CURRENCY");
        assertThat(metrics.get(0).get("currency").asText()).isEqualTo("USD");
        assertThat(metrics.get(0).size()).as("served once, whatever the passes").isEqualTo(3);
        assertThat(metrics.get(1).has("currency")).isFalse();
        assertThat(metrics.get(2).has("currency")).isFalse();
        assertThat(metrics.get(3).has("currency")).isFalse();
    }

    @ParameterizedTest
    @EnumSource(MetricSpec.Unit.class)
    void should_give_a_currency_code_to_the_currency_unit_alone(MetricSpec.Unit unit) {
        assertThat(ServedMetricUnit.currencyOf(unit)).isEqualTo(unit == MetricSpec.Unit.CURRENCY ? Optional.of("USD") : Optional.empty());
    }

    @Test
    void should_rewrite_each_metric_unit_and_leave_a_metric_without_unit_as_is() {
        var response = JsonNodeFactory.instance.objectNode();
        var metrics = response.putArray("metrics");
        metrics.addObject().put("name", "HTTP_REQUESTS").put("unit", "NUMBER");
        metrics.addObject().put("name", "LLM_PROMPT_TOTAL_TOKEN").put("unit", "TOKENS");
        metrics.addObject().put("name", "additional-metrics.long_failover_count");

        ServedMetricUnit.serveIn(response);

        assertThat(metrics.get(0).get("unit").asText()).isEqualTo("COUNT");
        assertThat(metrics.get(1).get("unit").asText()).isEqualTo("TOKENS");
        assertThat(metrics.get(2).has("unit")).isFalse();
    }
}
