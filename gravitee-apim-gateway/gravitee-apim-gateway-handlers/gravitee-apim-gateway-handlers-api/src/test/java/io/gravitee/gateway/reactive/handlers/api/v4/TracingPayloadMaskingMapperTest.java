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
package io.gravitee.gateway.reactive.handlers.api.v4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.tracing.MaskingType;
import io.gravitee.definition.model.v4.analytics.tracing.Tracing;
import io.gravitee.definition.model.v4.analytics.tracing.TracingMaskingStrategy;
import io.gravitee.definition.model.v4.analytics.tracing.TracingPayloadFormat;
import io.gravitee.definition.model.v4.analytics.tracing.TracingPayloadMaskingConfig;
import io.gravitee.definition.model.v4.analytics.tracing.TracingPayloadMaskingRule;
import io.gravitee.definition.model.v4.analytics.tracing.TracingPayloadPhase;
import io.gravitee.node.api.opentelemetry.redaction.FullMaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.MaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.PartialMaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.PayloadMaskingConfig;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TracingPayloadMaskingMapperTest {

    @Nested
    class ToPayloadMaskingConfig {

        @Test
        void should_return_empty_when_definition_is_null() {
            Api api = mock(Api.class);
            when(api.getDefinition()).thenReturn(null);

            assertThat(TracingPayloadMaskingMapper.toPayloadMaskingConfig(api)).isSameAs(PayloadMaskingConfig.EMPTY);
        }

        @Test
        void should_return_empty_when_analytics_is_null() {
            assertThat(TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithPayloadMasking(null))).isSameAs(
                PayloadMaskingConfig.EMPTY
            );
        }

        @Test
        void should_return_empty_when_rules_list_is_null() {
            var config = new TracingPayloadMaskingConfig();
            config.setRules(null);
            assertThat(TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithPayloadMasking(config))).isSameAs(
                PayloadMaskingConfig.EMPTY
            );
        }

        @Test
        void should_return_empty_when_rules_list_is_empty() {
            var config = new TracingPayloadMaskingConfig();
            config.setRules(List.of());
            assertThat(TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithPayloadMasking(config))).isSameAs(
                PayloadMaskingConfig.EMPTY
            );
        }

        @Test
        void should_return_empty_when_all_rules_have_null_path() {
            var rule = new TracingPayloadMaskingRule();
            rule.setPath(null);
            var config = new TracingPayloadMaskingConfig();
            config.setRules(List.of(rule));
            assertThat(TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithPayloadMasking(config))).isSameAs(
                PayloadMaskingConfig.EMPTY
            );
        }

        @Test
        void should_return_empty_when_all_rules_have_blank_path() {
            var rule = new TracingPayloadMaskingRule();
            rule.setPath("  ");
            var config = new TracingPayloadMaskingConfig();
            config.setRules(List.of(rule));
            assertThat(TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithPayloadMasking(config))).isSameAs(
                PayloadMaskingConfig.EMPTY
            );
        }

        @Test
        void should_map_full_masking_with_explicit_replacement() {
            var strategy = new TracingMaskingStrategy();
            strategy.setType(MaskingType.FULL);
            strategy.setReplacement("[HIDDEN]");
            var config = TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithRules(rule("$.password", strategy)));

            assertThat(config.rules()).hasSize(1);
            assertThat(config.rules().get(0).maskingStrategy()).isInstanceOf(FullMaskingStrategy.class);
            assertThat(((FullMaskingStrategy) config.rules().get(0).maskingStrategy()).replacement()).isEqualTo("[HIDDEN]");
        }

        @Test
        void should_map_full_masking_with_no_replacement_to_default() {
            var strategy = new TracingMaskingStrategy();
            strategy.setType(MaskingType.FULL);
            strategy.setReplacement(null);
            var config = TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithRules(rule("$.password", strategy)));

            assertThat(config.rules().get(0).maskingStrategy()).isSameAs(MaskingStrategy.DEFAULT);
        }

        @Test
        void should_map_null_masking_strategy_to_default() {
            var config = TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithRules(rule("$.password", null)));

            assertThat(config.rules().get(0).maskingStrategy()).isSameAs(MaskingStrategy.DEFAULT);
        }

        @Test
        void should_map_partial_masking_strategy() {
            var strategy = new TracingMaskingStrategy();
            strategy.setType(MaskingType.PARTIAL);
            strategy.setPrefixLength(2);
            strategy.setSuffixLength(4);
            strategy.setReplacement("X");
            var config = TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithRules(rule("$.card.number", strategy)));

            assertThat(config.rules().get(0).maskingStrategy()).isInstanceOf(PartialMaskingStrategy.class);
            var partial = (PartialMaskingStrategy) config.rules().get(0).maskingStrategy();
            assertThat(partial.prefixLength()).isEqualTo(2);
            assertThat(partial.suffixLength()).isEqualTo(4);
            assertThat(partial.maskChar()).isEqualTo("X");
        }

        @Test
        void should_map_phase_request() {
            var r = rule("$.password", null);
            r.setPhase(TracingPayloadPhase.REQUEST);
            var config = TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithRules(r));

            assertThat(config.rules().get(0).phase()).isEqualTo(io.gravitee.node.api.opentelemetry.redaction.PayloadPhase.REQUEST);
        }

        @Test
        void should_map_phase_response() {
            var r = rule("$.password", null);
            r.setPhase(TracingPayloadPhase.RESPONSE);
            var config = TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithRules(r));

            assertThat(config.rules().get(0).phase()).isEqualTo(io.gravitee.node.api.opentelemetry.redaction.PayloadPhase.RESPONSE);
        }

        @Test
        void should_default_null_phase_to_both() {
            var r = rule("$.password", null);
            r.setPhase(null);
            var config = TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithRules(r));

            assertThat(config.rules().get(0).phase()).isEqualTo(io.gravitee.node.api.opentelemetry.redaction.PayloadPhase.BOTH);
        }

        @Test
        void should_map_format_json() {
            var r = rule("$.password", null);
            r.setFormat(TracingPayloadFormat.JSON);
            var config = TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithRules(r));

            assertThat(config.rules().get(0).format()).isEqualTo(io.gravitee.node.api.opentelemetry.redaction.PayloadFormat.JSON);
        }

        @Test
        void should_map_format_xml() {
            var r = rule("//password", null);
            r.setFormat(TracingPayloadFormat.XML);
            var config = TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithRules(r));

            assertThat(config.rules().get(0).format()).isEqualTo(io.gravitee.node.api.opentelemetry.redaction.PayloadFormat.XML);
        }

        @Test
        void should_default_null_format_to_auto() {
            var r = rule("$.password", null);
            r.setFormat(null);
            var config = TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithRules(r));

            assertThat(config.rules().get(0).format()).isEqualTo(io.gravitee.node.api.opentelemetry.redaction.PayloadFormat.AUTO);
        }

        @Test
        void should_preserve_config_default_replacement() {
            var maskingConfig = new TracingPayloadMaskingConfig();
            maskingConfig.setDefaultReplacement("[MASKED]");
            maskingConfig.setRules(List.of(rule("$.password", null)));
            var config = TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithPayloadMasking(maskingConfig));

            assertThat(config.defaultReplacement()).isEqualTo("[MASKED]");
        }

        @Test
        void should_preserve_rule_ordering() {
            var r1 = rule("$.password", null);
            var r2 = rule("$.creditCard", null);
            var config = TracingPayloadMaskingMapper.toPayloadMaskingConfig(apiWithRules(r1, r2));

            assertThat(config.rules()).hasSize(2);
            assertThat(config.rules().get(0).path()).isEqualTo("$.password");
            assertThat(config.rules().get(1).path()).isEqualTo("$.creditCard");
        }

        private Api apiWithPayloadMasking(TracingPayloadMaskingConfig payloadMasking) {
            var def = new io.gravitee.definition.model.v4.Api();
            if (payloadMasking != null) {
                var tracing = new Tracing();
                tracing.setPayloadMasking(payloadMasking);
                var analytics = new Analytics();
                analytics.setTracing(tracing);
                def.setAnalytics(analytics);
            }
            Api api = mock(Api.class);
            when(api.getDefinition()).thenReturn(def);
            return api;
        }

        private Api apiWithRules(TracingPayloadMaskingRule... rules) {
            var config = new TracingPayloadMaskingConfig();
            config.setRules(List.of(rules));
            return apiWithPayloadMasking(config);
        }

        private TracingPayloadMaskingRule rule(String path, TracingMaskingStrategy strategy) {
            var r = new TracingPayloadMaskingRule();
            r.setPath(path);
            r.setMaskingStrategy(strategy);
            return r;
        }
    }
}
