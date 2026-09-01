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
package io.gravitee.apim.reporter.common.formatter.csv.v4;

import io.gravitee.reporter.api.v4.metric.event.DecisionEventMetrics;
import io.vertx.core.buffer.Buffer;
import java.util.List;
import java.util.Objects;
import lombok.CustomLog;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public class DecisionEventMetricsFormatter extends BaseEventMetricsFormatter<DecisionEventMetrics> {

    private static final String LIST_SEPARATOR = "|";

    @Override
    protected Buffer format0(DecisionEventMetrics data) {
        final Buffer buffer = super.format0(data);

        appendString(buffer, data.getEventId());
        appendString(buffer, data.getCaseId());
        appendString(buffer, data.getBatchId());
        appendString(buffer, data.getPhase() == null ? null : data.getPhase().getLabel());

        appendString(buffer, data.getDecisionPointType());
        appendString(buffer, data.getDecisionPointId());
        appendString(buffer, data.getDecisionPointVersion());
        appendString(buffer, data.getCheckpoint());
        appendString(buffer, data.getCaller());

        appendString(buffer, data.getSubjectType());
        appendString(buffer, data.getSubjectId());
        appendString(buffer, data.getActorType());
        appendString(buffer, data.getActorId());
        appendString(buffer, data.getAction());
        appendString(buffer, data.getResourceType());
        appendString(buffer, data.getResourceId());
        appendString(buffer, data.getArgsHash());

        appendString(buffer, data.getOutcome() == null ? null : data.getOutcome().getLabel());
        appendString(buffer, data.getEnforced() == null ? null : data.getEnforced().getLabel());
        appendString(buffer, data.getVerdict());
        appendString(buffer, data.getIndeterminateCause() == null ? null : data.getIndeterminateCause().getLabel());
        appendString(buffer, data.getConfidence() == null ? null : String.valueOf(data.getConfidence()));
        appendString(buffer, join(data.getReasons()));
        appendString(buffer, joinRuleNames(data));

        appendString(buffer, data.getTransformed() == null ? null : String.valueOf(data.getTransformed()));
        appendString(buffer, data.getTransformationType() == null ? null : data.getTransformationType().getLabel());

        appendString(buffer, data.getRequiredApprover());
        appendString(buffer, data.getDeciderType());
        appendString(buffer, data.getDeciderId());
        appendString(buffer, data.getChannel());

        appendString(buffer, data.getRequestId());
        appendString(buffer, data.getTraceId());
        appendString(buffer, data.getConversationId());
        appendString(buffer, data.getMissionId());

        appendString(buffer, data.getStatus() == null ? null : data.getStatus().getLabel());
        appendString(buffer, data.getErrorType());
        appendLong(buffer, data.getDurationNanos());
        appendLong(buffer, data.getWaitedNanos());

        return buffer;
    }

    private static String joinRuleNames(DecisionEventMetrics data) {
        if (data.getMatchedRules() == null || data.getMatchedRules().isEmpty()) {
            return null;
        }
        return data
            .getMatchedRules()
            .stream()
            .map(DecisionEventMetrics.MatchedRule::name)
            .filter(Objects::nonNull)
            .reduce((a, b) -> a + LIST_SEPARATOR + b)
            .orElse(null);
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values
            .stream()
            .filter(Objects::nonNull)
            .reduce((a, b) -> a + LIST_SEPARATOR + b)
            .orElse(null);
    }
}
