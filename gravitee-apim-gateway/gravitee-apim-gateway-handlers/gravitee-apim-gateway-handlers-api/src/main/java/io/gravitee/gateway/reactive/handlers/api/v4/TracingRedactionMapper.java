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

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.definition.model.v4.analytics.tracing.MaskingType;
import io.gravitee.definition.model.v4.analytics.tracing.TracingRedactionRule;
import io.gravitee.node.api.opentelemetry.redaction.MaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.RedactionConfig;
import io.gravitee.node.api.opentelemetry.redaction.RedactionRule;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;

@CustomLog
final class TracingRedactionMapper {

    private TracingRedactionMapper() {}

    static RedactionConfig toRedactionConfig(final Api api) {
        return Optional.ofNullable(api.getDefinition())
            .map(definition -> definition.getAnalytics())
            .map(analytics -> analytics.getTracing())
            .map(tracing -> tracing.getRedaction())
            .filter(redactionConfig -> redactionConfig.getRules() != null && !redactionConfig.getRules().isEmpty())
            .map(redactionConfig -> {
                var mappedRules = redactionConfig
                    .getRules()
                    .stream()
                    .filter(rule -> rule.getAttributeNamePattern() != null)
                    .map(rule -> new RedactionRule(rule.getAttributeNamePattern(), toMaskingStrategy(rule), rule.getValuePattern()))
                    .toList();
                return mappedRules.isEmpty()
                    ? RedactionConfig.EMPTY
                    : new RedactionConfig(mappedRules, redactionConfig.getDefaultReplacement());
            })
            .orElse(RedactionConfig.EMPTY);
    }

    @VisibleForTesting
    static MaskingStrategy toMaskingStrategy(final TracingRedactionRule rule) {
        var maskingStrategy = rule.getMaskingStrategy();
        if (maskingStrategy == null) return MaskingStrategy.DEFAULT;
        MaskingType maskingType = maskingStrategy.getType();
        if (maskingType == null) return MaskingStrategy.DEFAULT;
        return switch (maskingType) {
            case FULL -> maskingStrategy.getReplacement() != null
                ? MaskingStrategy.fullMask(maskingStrategy.getReplacement())
                : MaskingStrategy.DEFAULT;
            case PARTIAL -> {
                var maskChar = maskingStrategy.getReplacement() != null ? maskingStrategy.getReplacement() : "*";
                if (maskChar.length() != 1) {
                    log.warn("PARTIAL masking strategy replacement must be a single character, got '{}' — defaulting to '*'", maskChar);
                    maskChar = "*";
                }
                int prefix = Math.max(0, maskingStrategy.getPrefixLength() != null ? maskingStrategy.getPrefixLength() : 0);
                int suffix = Math.max(0, maskingStrategy.getSuffixLength() != null ? maskingStrategy.getSuffixLength() : 0);
                yield MaskingStrategy.partialMask(prefix, suffix, maskChar);
            }
        };
    }
}
