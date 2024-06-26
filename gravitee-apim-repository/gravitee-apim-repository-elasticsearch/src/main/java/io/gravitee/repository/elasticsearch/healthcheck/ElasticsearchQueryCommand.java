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
package io.gravitee.repository.elasticsearch.healthcheck;

import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.healthcheck.query.Query;
import io.gravitee.repository.healthcheck.query.Response;

/**
 * Common interface used to execute an analytic Elasticsearch query.
 *
 * Based on Command Design Pattern.
 *
 * @author Guillaume Waignier (Zenika)
 * @author Sebastien Devaux (Zenika)
 *
 */
public interface ElasticsearchQueryCommand<T extends Response> {
    /**
     * Execute an analytic Elasticsearch query.
     *
     * @param query
     *            query to execute
     * @return response
     * @throws AnalyticsException
     *             in case of analytic exception
     */
    T executeQuery(final QueryContext queryContext, final Query<T> query) throws AnalyticsException;

    /**
     * Get the supported query
     *
     * @return the query supported by this handler
     */
    Class<? extends Query<T>> getSupportedQuery();
}
