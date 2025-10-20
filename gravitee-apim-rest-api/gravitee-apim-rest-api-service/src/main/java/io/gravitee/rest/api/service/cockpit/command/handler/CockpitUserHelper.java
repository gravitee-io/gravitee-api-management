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
package io.gravitee.rest.api.service.cockpit.command.handler;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CockpitUserHelper {

    public static final String COCKPIT_SOURCE = "cockpit";

    /**
     * Resolves the apim user ID from a cockpit user ID by looking up the user in the repository.
     * Falls back to the provided userId if not found (cockpit system user is an expected example) or if an error occurs.
     *
     * @param userRepository the user repository to search in
     * @param executionContext the execution context containing organization information
     * @param cockpitUserId the cockpit user ID to resolve
     * @return the resolved user ID or the original cockpit user ID as fallback
     */
    public static String resolveApimUserId(UserRepository userRepository, ExecutionContext executionContext, String cockpitUserId) {
        try {
            return userRepository
                .findBySource(COCKPIT_SOURCE, cockpitUserId, executionContext.getOrganizationId())
                .map(User::getId)
                .orElse(cockpitUserId);
        } catch (TechnicalException ex) {
            return cockpitUserId;
        }
    }
}
