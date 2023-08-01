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
package io.gravitee.repository.noop;

import io.gravitee.platform.repository.api.RepositoryProvider;
import io.gravitee.platform.repository.api.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpRepositoryProvider implements RepositoryProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpRepositoryProvider.class);

    @Override
    public String type() {
        return "none";
    }

    @Override
    public Scope[] scopes() {
        return new Scope[]{Scope.ANALYTICS, Scope.MANAGEMENT, Scope.RATE_LIMIT};
    }

    @Override
    public Class<?> configuration(Scope scope) {
        if (scope == Scope.ANALYTICS) {
            return NoOpAnalyticsRepositoryConfiguration.class;
        } else if (scope == Scope.MANAGEMENT) {
            return NoOpManagementRepositoryConfiguration.class;
        } else if (scope == Scope.RATE_LIMIT) {
            return NoOpRateLimitRepositoryConfiguration.class;
        }

        LOGGER.debug("Skipping unhandled repository scope {}", scope);
        return null;
    }
}
