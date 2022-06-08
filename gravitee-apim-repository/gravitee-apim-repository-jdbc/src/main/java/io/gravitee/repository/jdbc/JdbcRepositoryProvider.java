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
package io.gravitee.repository.jdbc;

import io.gravitee.platform.repository.api.RepositoryProvider;
import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.jdbc.management.JdbcManagementRepositoryConfiguration;
import io.gravitee.repository.jdbc.ratelimit.JdbcRateLimitRepositoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njt
 */
public class JdbcRepositoryProvider implements RepositoryProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcRepositoryProvider.class);

    @Override
    public String type() {
        LOGGER.debug("JdbcRepository.type()");
        return "jdbc";
    }

    @Override
    public Scope[] scopes() {
        LOGGER.debug("JdbcRepository.scopes()");
        return new Scope[] { Scope.MANAGEMENT, Scope.RATE_LIMIT };
    }

    @Override
    public Class<?> configuration(Scope scope) {
        LOGGER.debug("JdbcRepository.configuration({})", scope);
        switch (scope) {
            case MANAGEMENT:
                return JdbcManagementRepositoryConfiguration.class;
            case RATE_LIMIT:
                return JdbcRateLimitRepositoryConfiguration.class;
            default:
                LOGGER.debug("Skipping unhandled repository scope {}", scope);
                break;
        }
        return null;
    }
}
