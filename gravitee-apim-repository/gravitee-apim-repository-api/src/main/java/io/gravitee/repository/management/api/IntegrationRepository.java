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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Integration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface IntegrationRepository {
    Page<Integration> findAllByEnvironment(String environmentId, Pageable pageable) throws TechnicalException;

    /**
     * Search integration that correspond to integrationIds or have one of group
     */
    Page<Integration> findAllByEnvironmentAndGroups(
        String environmentId,
        Collection<String> integrationIds,
        Collection<String> groups,
        Pageable pageable
    ) throws TechnicalException;

    /**
     * Delete integration by environmentId
     * @param environmentId
     * @return List of IDs for deleted integration
     * @throws TechnicalException
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;

    Integration create(final Integration integration) throws TechnicalException;
    Integration update(final Integration integration) throws TechnicalException;
    Optional<Integration> findByIntegrationId(String id) throws TechnicalException;
    void delete(String id) throws TechnicalException;
}
