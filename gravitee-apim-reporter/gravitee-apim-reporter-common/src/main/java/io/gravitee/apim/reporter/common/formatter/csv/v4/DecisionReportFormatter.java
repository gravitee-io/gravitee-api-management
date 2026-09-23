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

import io.gravitee.apim.reporter.common.formatter.csv.SingleValueFormatter;
import io.gravitee.reporter.api.v4.report.DecisionReport;
import io.vertx.core.buffer.Buffer;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DecisionReportFormatter extends SingleValueFormatter<DecisionReport> {

    @Override
    protected Buffer format0(DecisionReport data) {
        final Buffer buffer = Buffer.buffer();
        appendLong(buffer, data.getTimestamp());

        appendString(buffer, data.getGatewayId());
        appendString(buffer, data.getOrganizationId());
        appendString(buffer, data.getEnvironmentId());
        appendString(buffer, data.getApiId());
        appendString(buffer, data.getPlanId());
        appendString(buffer, data.getApplicationId());

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
        appendAdditional(data, buffer);

        return buffer;
    }

    private static String joinRuleNames(DecisionReport data) {
        if (data.getMatchedRules() == null || data.getMatchedRules().isEmpty()) {
            return null;
        }
        String collect = data
            .getMatchedRules()
            .stream()
            .map(DecisionReport.MatchedRule::name)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(LIST_SEPARATOR));
        return collect.isBlank() ? null : collect;
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        String collect = values.stream().filter(Objects::nonNull).collect(Collectors.joining(LIST_SEPARATOR));
        return collect.isBlank() ? null : collect;
    }
}
