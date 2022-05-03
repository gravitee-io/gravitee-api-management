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
package io.gravitee.repository.elasticsearch.analytics;

import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.Query;
import io.gravitee.repository.analytics.query.response.Response;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 * @author Guillaume WAIGNIER (Zenika)
 * @author Sebastien DEVAUX (Zenika)
 */
public class ElasticsearchAnalyticsRepository extends AbstractElasticsearchRepository implements AnalyticsRepository {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ElasticsearchAnalyticsRepository.class);

    /**
     * List of all supported command
     */
    @Autowired
    private List<ElasticsearchQueryCommand<?>> listQueryCommands;

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
        return handler.executeQuery(handler.prepareQuery(query));
    }
}
