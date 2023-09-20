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
package io.gravitee.repository.elasticsearch.v4.log;

import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchConnectionLogQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchConnectionLogResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.message.SearchMessageLogQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.message.SearchMessageLogResponseAdapter;
import io.gravitee.repository.log.v4.api.LogRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.ConnectionLog;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogQuery;
import io.gravitee.repository.log.v4.model.message.MessageLog;
import io.gravitee.repository.log.v4.model.message.MessageLogQuery;

public class LogElasticsearchRepository extends AbstractElasticsearchRepository implements LogRepository {

    private final RepositoryConfiguration configuration;

    public LogElasticsearchRepository(RepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public LogResponse<ConnectionLog> searchConnectionLog(ConnectionLogQuery query) {
        var clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
        var index = this.indexNameGenerator.getWildcardIndexName(Type.V4_METRICS, clusters);

        return this.client.search(index, null, SearchConnectionLogQueryAdapter.adapt(query))
            .map(SearchConnectionLogResponseAdapter::adapt)
            .blockingGet();
    }

    @Override
    public LogResponse<MessageLog> searchMessageLog(MessageLogQuery query) {
        var clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
        var index = this.indexNameGenerator.getWildcardIndexName(Type.V4_MESSAGE_LOG, clusters);

        return this.client.search(index, null, SearchMessageLogQueryAdapter.adapt(query))
            .map(SearchMessageLogResponseAdapter::adapt)
            .blockingGet();
    }
}
