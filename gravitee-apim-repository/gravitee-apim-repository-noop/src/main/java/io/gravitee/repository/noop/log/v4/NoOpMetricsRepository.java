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
package io.gravitee.repository.noop.log.v4;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery;
import io.gravitee.repository.log.v4.model.message.MessageMetrics;
import io.gravitee.repository.log.v4.model.message.MessageMetricsQuery;
import java.util.ArrayList;
import java.util.List;

public class NoOpMetricsRepository implements MetricsRepository {

    @Override
    public LogResponse<MessageMetrics> searchMessageMetrics(QueryContext queryContext, MessageMetricsQuery metricsQuery) {
        return new LogResponse<>(0L, new ArrayList<>());
    }

    @Override
    public LogResponse<Metrics> searchMetrics(QueryContext queryContext, MetricsQuery query, List<DefinitionVersion> definitionVersions) {
        return new LogResponse<>(0L, new ArrayList<>());
    }
}
