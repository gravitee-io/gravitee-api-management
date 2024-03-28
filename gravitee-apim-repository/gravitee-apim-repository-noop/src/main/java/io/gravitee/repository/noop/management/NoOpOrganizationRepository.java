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
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Organization;
import java.util.Optional;
import java.util.Set;

/**
 * This repository does no operation with any persistent storage but ensures that any call dealing with the 'DEFAULT' hardcoded organization behaves appropriately.
 * This is to ensure full compatibility with all the APIM components where the 'DEFAULT' organization is used.
 *
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpOrganizationRepository extends AbstractNoOpManagementRepository<Organization, String> implements OrganizationRepository {

    @Override
    public Long count() throws TechnicalException {
        // 1 single 'DEFAULT' organization.
        return 1L;
    }

    @Override
    public Optional<Organization> findById(String organizationId) throws TechnicalException {
        if (organizationId.equals(Organization.DEFAULT.getId())) {
            return Optional.of(Organization.DEFAULT);
        }

        return Optional.empty();
    }

    @Override
    public Set<Organization> findAll() throws TechnicalException {
        return Set.of(Organization.DEFAULT);
    }

    @Override
    public Set<Organization> findByHrids(Set<String> hrids) throws TechnicalException {
        if (hrids.stream().anyMatch(orgHrid -> Organization.DEFAULT.getHrids().contains(orgHrid))) {
            return Set.of(Organization.DEFAULT);
        }

        return Set.of();
    }

    @Override
    public Optional<Organization> findByCockpitId(String cockpitId) throws TechnicalException {
        return Optional.empty();
    }
}
