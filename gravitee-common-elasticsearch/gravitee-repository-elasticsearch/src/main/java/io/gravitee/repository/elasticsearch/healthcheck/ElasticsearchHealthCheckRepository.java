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
package io.gravitee.repository.elasticsearch.healthcheck;

import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.healthcheck.query.LogBuilder;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.healthcheck.api.HealthCheckRepository;
import io.gravitee.repository.healthcheck.query.Query;
import io.gravitee.repository.healthcheck.query.Response;
import io.gravitee.repository.healthcheck.query.log.ExtendedLog;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Guillaume Waignier (Zenika)
 * @author Sebastien Devaux (Zenika)
 * @author GraviteeSource Team
 */
public class ElasticsearchHealthCheckRepository extends AbstractElasticsearchRepository implements HealthCheckRepository {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ElasticsearchHealthCheckRepository.class);

    /**
     * List of all supported command
     */
    @Autowired
    private List<ElasticsearchQueryCommand<?>> listQueryCommands;

    @Autowired
    protected RepositoryConfiguration configuration;

    /**
     * Freemarker template name for health-check log by id.
     */
    private static final String HEALTHCHECK_BY_ID_TEMPLATE = "healthcheck/log.ftl";

    private final Map<Class<? extends Query<?>>, ElasticsearchQueryCommand<?>> queryCommands = new HashMap<>();

    /**
     * Initialize the map of command.
     */
    @PostConstruct
    private void init() {
        this.listQueryCommands.forEach(command -> this.queryCommands.put(command.getSupportedQuery(), command));
    }

    @Override
    public <T extends Response> T query(final Query<T> query) throws AnalyticsException {

        @SuppressWarnings("unchecked")
        final ElasticsearchQueryCommand<T> handler = (ElasticsearchQueryCommand<T>) this.queryCommands.get(query.getClass());

        if (handler == null) {
            logger.error("No command found to handle query of type {}", query.getClass());
            throw new AnalyticsException("No command found to handle query of type " + query.getClass());
        }

        return handler.executeQuery(query);
    }

    @Override
    public ExtendedLog findById(String id) throws AnalyticsException {
        final Map<String, Object> data = new HashMap<>();
        data.put("id", id);

        String sQuery = this.freeMarkerComponent.generateFromTemplate(HEALTHCHECK_BY_ID_TEMPLATE, data);
        String[] clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);

        try {
            final Single<SearchResponse> result = this.client.search(
                    this.indexNameGenerator.getWildcardIndexName(Type.HEALTH_CHECK, clusters),
                    !info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : Type.HEALTH_CHECK.getType(),
                    sQuery);

            SearchResponse searchResponse = result.blockingGet();
            if (searchResponse.getSearchHits().getTotal().getValue() == 0) {
                throw new AnalyticsException("Health [" + id + "] does not exist");
            }

            SearchHit searchHit = searchResponse.getSearchHits().getHits().get(0);

            return LogBuilder.createExtendedLog(searchHit);
        } catch (TechnicalException e) {
            logger.error("Health [{}] does not exist", id, e);
            throw new AnalyticsException("Health [" + id + "] does not exist");
        }
    }
}
