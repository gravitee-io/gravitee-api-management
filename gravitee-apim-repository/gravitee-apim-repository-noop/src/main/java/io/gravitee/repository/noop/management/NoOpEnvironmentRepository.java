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
package io.gravitee.repository.noop.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Organization;
import java.util.Optional;
import java.util.Set;

/**
 * This repository does no operation with any persistent storage but ensures that any call dealing with the 'DEFAULT' hardcoded environment behaves appropriately.
 * This is to ensure full compatibility with all the APIM components where the 'DEFAULT' environment is used.
 *
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpEnvironmentRepository extends AbstractNoOpManagementRepository<Environment, String> implements EnvironmentRepository {

    @Override
    public Optional<Environment> findById(String environmentId) throws TechnicalException {
        if (environmentId.equals(Environment.DEFAULT.getId())) {
            return Optional.of(Environment.DEFAULT);
        }

        return Optional.empty();
    }

    @Override
    public Set<Environment> findByOrganization(String organizationId) throws TechnicalException {
        if (organizationId.equals(Environment.DEFAULT.getOrganizationId())) {
            return Set.of(Environment.DEFAULT);
        }
        return Set.of();
    }

    @Override
    public Set<Environment> findByOrganizationsAndHrids(Set<String> organizationsHrids, Set<String> hrids) throws TechnicalException {
        if (
            organizationsHrids.stream().anyMatch(orgHrid -> Organization.DEFAULT.getHrids().contains(orgHrid)) &&
            hrids.stream().anyMatch(envHrid -> Environment.DEFAULT.getHrids().contains(envHrid))
        ) {
            return Set.of(Environment.DEFAULT);
        }
        return Set.of();
    }

    @Override
    public Optional<Environment> findByCockpitId(String cockpitId) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public Set<Environment> findAll() throws TechnicalException {
        return Set.of(Environment.DEFAULT);
    }

    @Override
    public Set<String> findOrganizationIdsByEnvironments(final Set<String> ids) throws TechnicalException {
        if (ids.stream().anyMatch(envHrid -> Environment.DEFAULT.getHrids().contains(envHrid))) {
            return Set.of(Organization.DEFAULT.getId());
        }
        return Set.of();
    }
}
