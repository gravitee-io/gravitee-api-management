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
package io.gravitee.repository.elasticsearch;

import io.gravitee.platform.repository.api.RepositoryProvider;
import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.elasticsearch.spring.ElasticsearchRepositoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ElasticsearchRepositoryProvider implements RepositoryProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchRepositoryProvider.class);

    @Override
    public String type() {
        return "elasticsearch";
    }

    @Override
    public Scope[] scopes() {
        return new Scope[] { Scope.ANALYTICS };
    }

    @Override
    public Class<?> configuration(Scope scope) {
        if (scope == Scope.ANALYTICS) {
            return ElasticsearchRepositoryConfiguration.class;
        }
        LOGGER.debug("Skipping unhandled repository scope {}", scope);
        return null;
    }
}
