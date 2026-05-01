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
import io.gravitee.definition.model.v4.analytics.tracing.TracingRedactionConfig;
import io.gravitee.definition.model.v4.analytics.tracing.TracingRedactionRule;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.node.api.opentelemetry.redaction.FullMaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.MaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.PartialMaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.RedactionConfig;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TracingRedactionMapperTest {

    @Nested
    class ToRedactionConfig {

        @Test
        void should_return_empty_when_definition_is_null() {
            Api api = mock(Api.class);
            when(api.getDefinition()).thenReturn(null);

            assertThat(TracingRedactionMapper.toRedactionConfig(api)).isSameAs(RedactionConfig.EMPTY);
        }

        @Test
        void should_return_empty_when_analytics_is_null() {
            assertThat(TracingRedactionMapper.toRedactionConfig(apiWithRedaction(null))).isSameAs(RedactionConfig.EMPTY);
        }

        @Test
        void should_return_empty_when_rules_list_is_null() {
            var redaction = new TracingRedactionConfig();
            redaction.setRules(null);
            assertThat(TracingRedactionMapper.toRedactionConfig(apiWithRedaction(redaction))).isSameAs(RedactionConfig.EMPTY);
        }

        @Test
        void should_return_empty_when_rules_list_is_empty() {
            var redaction = new TracingRedactionConfig();
            redaction.setRules(List.of());
            assertThat(TracingRedactionMapper.toRedactionConfig(apiWithRedaction(redaction))).isSameAs(RedactionConfig.EMPTY);
        }

        @Test
        void should_return_empty_when_all_rules_have_null_pattern() {
            var rule = new TracingRedactionRule();
            rule.setAttributeNamePattern(null);
            var redaction = new TracingRedactionConfig();
            redaction.setRules(List.of(rule));
            assertThat(TracingRedactionMapper.toRedactionConfig(apiWithRedaction(redaction))).isSameAs(RedactionConfig.EMPTY);
        }

        @Test
        void should_map_full_masking_with_explicit_replacement() {
            var strategy = new TracingMaskingStrategy();
            strategy.setType(MaskingType.FULL);
            strategy.setReplacement("[HIDDEN]");
            var rule = redactionRule("enduser.id", strategy);
            var config = TracingRedactionMapper.toRedactionConfig(apiWithRules(rule));

            assertThat(config.rules()).hasSize(1);
            assertThat(config.rules().get(0).maskingStrategy()).isInstanceOf(FullMaskingStrategy.class);
            assertThat(((FullMaskingStrategy) config.rules().get(0).maskingStrategy()).replacement()).isEqualTo("[HIDDEN]");
        }

        @Test
        void should_map_full_masking_with_no_replacement_to_default() {
            var strategy = new TracingMaskingStrategy();
            strategy.setType(MaskingType.FULL);
            strategy.setReplacement(null);
            var rule = redactionRule("enduser.id", strategy);
            var config = TracingRedactionMapper.toRedactionConfig(apiWithRules(rule));

            assertThat(config.rules().get(0).maskingStrategy()).isSameAs(MaskingStrategy.DEFAULT);
        }

        @Test
        void should_map_null_masking_strategy_to_default() {
            var rule = redactionRule("enduser.id", null);
            var config = TracingRedactionMapper.toRedactionConfig(apiWithRules(rule));

            assertThat(config.rules().get(0).maskingStrategy()).isSameAs(MaskingStrategy.DEFAULT);
        }

        @Test
        void should_map_partial_masking_strategy() {
            var strategy = new TracingMaskingStrategy();
            strategy.setType(MaskingType.PARTIAL);
            strategy.setPrefixLength(2);
            strategy.setSuffixLength(4);
            strategy.setReplacement("X");
            var rule = redactionRule("api.key", strategy);
            var config = TracingRedactionMapper.toRedactionConfig(apiWithRules(rule));

            assertThat(config.rules().get(0).maskingStrategy()).isInstanceOf(PartialMaskingStrategy.class);
            var partial = (PartialMaskingStrategy) config.rules().get(0).maskingStrategy();
            assertThat(partial.prefixLength()).isEqualTo(2);
            assertThat(partial.suffixLength()).isEqualTo(4);
            assertThat(partial.maskChar()).isEqualTo("X");
        }

        @Test
        void should_use_default_mask_char_when_partial_replacement_is_null() {
            var strategy = new TracingMaskingStrategy();
            strategy.setType(MaskingType.PARTIAL);
            strategy.setPrefixLength(0);
            strategy.setSuffixLength(0);
            strategy.setReplacement(null);
            var rule = redactionRule("api.key", strategy);
            var config = TracingRedactionMapper.toRedactionConfig(apiWithRules(rule));

            assertThat(((PartialMaskingStrategy) config.rules().get(0).maskingStrategy()).maskChar()).isEqualTo("*");
        }

        @Test
        void should_preserve_value_pattern_in_mapped_rule() {
            var rule = redactionRule("enduser.id", null);
            rule.setValuePattern("^Bearer ");
            var config = TracingRedactionMapper.toRedactionConfig(apiWithRules(rule));

            assertThat(config.rules().get(0).valuePattern()).isEqualTo("^Bearer ");
        }

        @Test
        void should_preserve_config_default_replacement() {
            var redaction = new TracingRedactionConfig();
            redaction.setDefaultReplacement("[MASKED]");
            var rule = redactionRule("api.key", null);
            redaction.setRules(List.of(rule));
            var config = TracingRedactionMapper.toRedactionConfig(apiWithRedaction(redaction));

            assertThat(config.defaultReplacement()).isEqualTo("[MASKED]");
        }

        private Api apiWithRedaction(TracingRedactionConfig redaction) {
            var def = new io.gravitee.definition.model.v4.Api();
            if (redaction != null) {
                var tracing = new Tracing();
                tracing.setRedaction(redaction);
                var analytics = new Analytics();
                analytics.setTracing(tracing);
                def.setAnalytics(analytics);
            }
            Api api = mock(Api.class);
            when(api.getDefinition()).thenReturn(def);
            return api;
        }

        private Api apiWithRules(TracingRedactionRule... rules) {
            var redaction = new TracingRedactionConfig();
            redaction.setRules(List.of(rules));
            return apiWithRedaction(redaction);
        }

        private TracingRedactionRule redactionRule(String pattern, TracingMaskingStrategy strategy) {
            var rule = new TracingRedactionRule();
            rule.setAttributeNamePattern(pattern);
            rule.setMaskingStrategy(strategy);
            return rule;
        }
    }
}
