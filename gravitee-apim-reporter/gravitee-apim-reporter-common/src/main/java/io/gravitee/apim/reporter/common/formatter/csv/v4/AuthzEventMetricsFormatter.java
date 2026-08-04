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

import io.gravitee.reporter.api.v4.metric.event.AuthzEventMetrics;
import io.vertx.core.buffer.Buffer;
import lombok.CustomLog;

/**
 * Flat projection of an authorization outcome. Matched policies and reasons are lists, so they are
 * joined rather than expanded — CSV has no place for them and the JSON output keeps the full shape.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class AuthzEventMetricsFormatter extends BaseEventMetricsFormatter<AuthzEventMetrics> {

    private static final String LIST_SEPARATOR = "|";

    @Override
    protected Buffer format0(AuthzEventMetrics data) {
        final Buffer buffer = super.format0(data);
        appendString(buffer, data.getOperation());
        appendString(buffer, data.getEventId());
        appendString(buffer, data.getStatus());
        appendString(buffer, data.getRequestId());
        appendString(buffer, data.getCaller());
        appendString(buffer, data.getTargetPdpId());
        appendLong(buffer, data.getPolicyGeneration());
        appendString(buffer, data.getBatchId());
        appendLong(buffer, data.getBatchIndex());
        appendLong(buffer, data.getBatchSize());
        appendString(buffer, data.getSubjectType());
        appendString(buffer, data.getSubjectId());
        appendString(buffer, data.getAction());
        appendString(buffer, data.getResourceType());
        appendString(buffer, data.getResourceId());
        appendString(buffer, data.getDecision());
        appendString(buffer, joinPolicyNames(data));
        appendString(buffer, join(data.getReasons()));
        appendString(buffer, data.getSearchType());
        appendLong(buffer, data.getResultCount());
        appendLong(buffer, data.getPageSize());
        appendString(buffer, data.getHasMore() == null ? null : String.valueOf(data.getHasMore()));
        appendString(buffer, data.getErrorType());
        appendLong(buffer, data.getDurationNanos());
        return buffer;
    }

    private static String joinPolicyNames(AuthzEventMetrics data) {
        if (data.getMatchedPolicies() == null || data.getMatchedPolicies().isEmpty()) {
            return null;
        }
        return data
            .getMatchedPolicies()
            .stream()
            .map(AuthzEventMetrics.MatchedPolicy::name)
            .filter(java.util.Objects::nonNull)
            .reduce((a, b) -> a + LIST_SEPARATOR + b)
            .orElse(null);
    }

    private static String join(java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values
            .stream()
            .filter(java.util.Objects::nonNull)
            .reduce((a, b) -> a + LIST_SEPARATOR + b)
            .orElse(null);
    }
}
