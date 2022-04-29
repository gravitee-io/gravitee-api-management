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

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.index.IndexNameGenerator;
import io.gravitee.elasticsearch.model.CountResponse;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.repository.analytics.query.AbstractQuery;
import io.gravitee.repository.analytics.query.Query;
import io.gravitee.repository.analytics.query.response.Response;
import io.gravitee.repository.elasticsearch.analytics.ElasticsearchQueryCommand;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.reactivex.Single;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class used to execute an analytic Elasticsearch query.
 *
 * Based on Command Design Pattern.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Guillaume Waignier (Zenika)
 * @author Sebastien Devaux (Zenika)
 * @author GraviteeSource Team
 */
public abstract class AbstractElasticsearchQueryCommand<T extends Response> implements ElasticsearchQueryCommand<T> {

    /**
     * Logger.
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Elasticsearch client to perform search request.
     */
    @Autowired
    protected Client client;

    /**
     * Templating component
     */
    @Autowired
    private FreeMarkerComponent freeMarkerComponent;

    /**
     * Util component used to compute index name.
     */
    @Autowired
    protected IndexNameGenerator indexNameGenerator;

    @Autowired
    protected RepositoryConfiguration configuration;

    @Autowired
    protected ElasticsearchInfo info;

    /**
     * Create the elasticsearch query
     * @param templateName Freemarker template name
     * @param query query parameter
     * @return the elasticsearch json query
     */
    String createQuery(final String templateName, final Query<T> query) {
        return this.createQuery(templateName, query, null, null);
    }

    /**
     * Create the elasticsearch query
     * @param templateName Freemarker template name
     * @param query query parameter
     * @param roundedFrom from parameter
     * @param roundedTo to parameter
     * @return the elasticsearch json query
     */
    String createQuery(final String templateName, final Query<T> query, Long roundedFrom, Long roundedTo) {
        final Map<String, Object> data = new HashMap<>();
        data.put("query", query);
        if (roundedFrom != null) {
            data.put("roundedFrom", roundedFrom);
        }
        if (roundedTo != null) {
            data.put("roundedTo", roundedTo);
        }

        final String request = this.freeMarkerComponent.generateFromTemplate(templateName, data);

        logger.debug("ES request {}", request);

        return request;
    }

    Single<SearchResponse> execute(AbstractQuery<T> query, Type type, String sQuery) {
        final Single<SearchResponse> result;

        String[] clusters = ClusterUtils.extractClusterIndexPrefixes(query, configuration);

        sQuery = sQuery.replaceAll(":(((\\/)(\\w|-)*)|((\\w|-)*:(\\d)*))", ":\\\\\"$1\\\\\"");
        if (query.timeRange() != null) {
            final Long from = query.timeRange().range().from();
            final Long to = query.timeRange().range().to();

            result =
                this.client.search(
                        this.indexNameGenerator.getIndexName(type, from, to, clusters),
                        !info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : type.getType(),
                        sQuery
                    );
        } else {
            result =
                this.client.search(
                        this.indexNameGenerator.getTodayIndexName(type, clusters),
                        !info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : type.getType(),
                        sQuery
                    );
        }

        return result;
    }

    Single<CountResponse> executeCount(AbstractQuery<T> query, Type type, String sQuery) {
        final Single<CountResponse> result;

        String[] clusters = ClusterUtils.extractClusterIndexPrefixes(query, configuration);

        sQuery = sQuery.replaceAll(":(((\\/)(\\w|-)*)|((\\w|-)*:(\\d)*))", ":\\\\\"$1\\\\\"");
        if (query.timeRange() != null) {
            final Long from = query.timeRange().range().from();
            final Long to = query.timeRange().range().to();

            result =
                this.client.count(
                        this.indexNameGenerator.getIndexName(type, from, to, clusters),
                        !info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : type.getType(),
                        sQuery
                    );
        } else {
            result =
                this.client.count(
                        this.indexNameGenerator.getTodayIndexName(type, clusters),
                        !info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : type.getType(),
                        sQuery
                    );
        }

        return result;
    }
}
