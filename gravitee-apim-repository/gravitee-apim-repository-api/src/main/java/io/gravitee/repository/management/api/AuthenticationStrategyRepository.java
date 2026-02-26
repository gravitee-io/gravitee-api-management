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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.AuthenticationStrategy;
import java.util.List;
import java.util.Set;

/**
 * Repository for AuthenticationStrategy entities.
 * An authentication strategy defines how applications authenticate with APIs,
 * optionally referencing a ClientRegistrationProvider for DCR-based strategies.
 */
public interface AuthenticationStrategyRepository extends CrudRepository<AuthenticationStrategy, String> {
    /**
     * List all authentication strategies.
     */
    Set<AuthenticationStrategy> findAll() throws TechnicalException;

    /**
     * List all authentication strategies for a given environment.
     */
    Set<AuthenticationStrategy> findAllByEnvironment(String environmentId) throws TechnicalException;

    /**
     * Find all authentication strategies that reference a given client registration provider.
     */
    Set<AuthenticationStrategy> findByClientRegistrationProviderId(String clientRegistrationProviderId) throws TechnicalException;

    /**
     * Delete all authentication strategies for a given environment.
     * @return list of deleted IDs
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
