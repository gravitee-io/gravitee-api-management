/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.elasticsearch.analytics.query;

import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.Query;
import io.gravitee.repository.analytics.query.count.CountQuery;
import io.gravitee.repository.analytics.query.count.CountResponse;

/**
 * Commmand used to handle CountQuery
 *
 * @author Guillaume Waignier (Zenika)
 * @author Sebastien Devaux (Zenika)
 *
 */
public class CountQueryCommand extends AbstractElasticsearchQueryCommand<CountResponse> {

    private static final String TEMPLATE = "count.ftl";

    @Override
    public Class<? extends Query<CountResponse>> getSupportedQuery() {
        return CountQuery.class;
    }

    @Override
    public CountResponse executeQuery(Query<CountResponse> query) throws AnalyticsException {
        final CountQuery countQuery = (CountQuery) query;
        final String sQuery = this.createQuery(TEMPLATE, query);

        try {
            io.gravitee.elasticsearch.model.CountResponse response = executeCount(countQuery, Type.REQUEST, sQuery).blockingGet();
            return this.toCountResponse(response);
        } catch (final Exception eex) {
            logger.error("Impossible to perform CountQuery", eex);
            throw new AnalyticsException("Impossible to perform CountQuery", eex);
        }
    }

    private CountResponse toCountResponse(final io.gravitee.elasticsearch.model.CountResponse response) {
        final CountResponse countResponse = new CountResponse();
        countResponse.setCount(response.getCount());
        return countResponse;
    }
}
