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
import io.gravitee.definition.model.v4.analytics.tracing.TracingPayloadFormat;
import io.gravitee.definition.model.v4.analytics.tracing.TracingPayloadMaskingRule;
import io.gravitee.definition.model.v4.analytics.tracing.TracingPayloadPhase;
import io.gravitee.node.api.opentelemetry.redaction.MaskingStrategy;
import io.gravitee.node.api.opentelemetry.redaction.PayloadFormat;
import io.gravitee.node.api.opentelemetry.redaction.PayloadMaskingConfig;
import io.gravitee.node.api.opentelemetry.redaction.PayloadMaskingRule;
import io.gravitee.node.api.opentelemetry.redaction.PayloadPhase;
import java.util.Optional;
import lombok.CustomLog;

@CustomLog
final class TracingPayloadMaskingMapper {

    private TracingPayloadMaskingMapper() {}

    static PayloadMaskingConfig toPayloadMaskingConfig(final Api api) {
        return Optional.ofNullable(api.getDefinition())
            .map(definition -> definition.getAnalytics())
            .map(analytics -> analytics.getTracing())
            .map(tracing -> tracing.getPayloadMasking())
            .filter(config -> config.getRules() != null && !config.getRules().isEmpty())
            .map(config -> {
                var mappedRules = config
                    .getRules()
                    .stream()
                    .filter(rule -> rule.getPath() != null && !rule.getPath().isBlank())
                    .map(rule ->
                        new PayloadMaskingRule(
                            rule.getPath(),
                            toMaskingStrategy(rule),
                            toPayloadPhase(rule.getPhase()),
                            toPayloadFormat(rule.getFormat())
                        )
                    )
                    .toList();
                return mappedRules.isEmpty()
                    ? PayloadMaskingConfig.EMPTY
                    : new PayloadMaskingConfig(mappedRules, config.getDefaultReplacement());
            })
            .orElse(PayloadMaskingConfig.EMPTY);
    }

    @VisibleForTesting
    static MaskingStrategy toMaskingStrategy(final TracingPayloadMaskingRule rule) {
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

    @VisibleForTesting
    static PayloadPhase toPayloadPhase(final TracingPayloadPhase phase) {
        if (phase == null) return PayloadPhase.BOTH;
        return switch (phase) {
            case REQUEST -> PayloadPhase.REQUEST;
            case RESPONSE -> PayloadPhase.RESPONSE;
            case BOTH -> PayloadPhase.BOTH;
        };
    }

    @VisibleForTesting
    static PayloadFormat toPayloadFormat(final TracingPayloadFormat format) {
        if (format == null) return PayloadFormat.AUTO;
        return switch (format) {
            case JSON -> PayloadFormat.JSON;
            case XML -> PayloadFormat.XML;
            case AUTO -> PayloadFormat.AUTO;
        };
    }
}
