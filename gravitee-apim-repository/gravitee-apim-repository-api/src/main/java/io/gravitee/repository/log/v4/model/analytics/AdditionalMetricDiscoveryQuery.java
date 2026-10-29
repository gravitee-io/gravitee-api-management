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
package io.gravitee.repository.log.v4.model.analytics;

import io.gravitee.repository.common.query.QueryContext;
import java.time.Duration;
import java.time.Instant;

/**
 * Which additional metrics the requests of an environment carried over a window. The environment is the one of the
 * query context.
 *
 * @param timeout the budget of the whole pass: what is found when it runs out is answered, flagged as truncated
 */
public record AdditionalMetricDiscoveryQuery(Instant from, Instant to, Duration timeout) {
    /**
     * The environment a discovery runs in.
     *
     * @throws IllegalArgumentException when the query context names none: the metrics of every environment share the
     *     indexes, so a pass without one would list the keys of all of them
     */
    public static String environmentOf(QueryContext queryContext) {
        var environmentId = queryContext.getEnvId();
        if (environmentId == null || environmentId.isBlank()) {
            throw new IllegalArgumentException(
                "An additional metric discovery needs an environment: every environment of the cluster shares the indexes"
            );
        }
        return environmentId;
    }
}
