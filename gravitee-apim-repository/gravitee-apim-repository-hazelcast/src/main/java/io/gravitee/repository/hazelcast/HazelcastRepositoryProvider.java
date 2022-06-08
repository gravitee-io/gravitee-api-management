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
package io.gravitee.repository.hazelcast;

import io.gravitee.platform.repository.api.RepositoryProvider;
import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.hazelcast.spring.PluginConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HazelcastRepositoryProvider implements RepositoryProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(HazelcastRepositoryProvider.class);

    private static final String TYPE = "hazelcast";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Scope[] scopes() {
        return new Scope[] { Scope.RATE_LIMIT };
    }

    @Override
    public Class<?> configuration(Scope scope) {
        if (scope == Scope.RATE_LIMIT) {
            return PluginConfiguration.class;
        }
        LOGGER.debug("Skipping unhandled repository scope {}", scope);
        return null;
    }
}
