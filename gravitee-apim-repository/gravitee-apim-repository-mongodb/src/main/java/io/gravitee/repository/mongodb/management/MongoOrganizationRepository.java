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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.repository.mongodb.management.internal.model.OrganizationMongo;
import io.gravitee.repository.mongodb.management.internal.organization.OrganizationMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoOrganizationRepository implements OrganizationRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoOrganizationRepository.class);

    @Autowired
    private OrganizationMongoRepository internalOrganizationRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Organization> findById(String organizationId) throws TechnicalException {
        LOGGER.debug("Find organization by ID [{}]", organizationId);

        final OrganizationMongo organization = internalOrganizationRepo.findById(organizationId).orElse(null);

        LOGGER.debug("Find organization by ID [{}] - Done", organization);
        return Optional.ofNullable(mapper.map(organization, Organization.class));
    }

    @Override
    public Organization create(Organization organization) throws TechnicalException {
        LOGGER.debug("Create organization [{}]", organization.getName());

        OrganizationMongo organizationMongo = mapper.map(organization, OrganizationMongo.class);
        OrganizationMongo createdOrganizationMongo = internalOrganizationRepo.insert(organizationMongo);

        Organization res = mapper.map(createdOrganizationMongo, Organization.class);

        LOGGER.debug("Create organization [{}] - Done", organization.getName());

        return res;
    }

    @Override
    public Organization update(Organization organization) throws TechnicalException {
        if (organization == null || organization.getName() == null) {
            throw new IllegalStateException("Organization to update must have a name");
        }

        final OrganizationMongo organizationMongo = internalOrganizationRepo.findById(organization.getId()).orElse(null);

        if (organizationMongo == null) {
            throw new IllegalStateException(String.format("No organization found with id [%s]", organization.getId()));
        }

        try {
            //Update
            organizationMongo.setName(organization.getName());
            organizationMongo.setCockpitId(organization.getCockpitId());
            organizationMongo.setDescription(organization.getDescription());
            organizationMongo.setHrids(organization.getHrids());
            organizationMongo.setDomainRestrictions(organization.getDomainRestrictions());
            organizationMongo.setFlowMode(organization.getFlowMode());

            OrganizationMongo organizationMongoUpdated = internalOrganizationRepo.save(organizationMongo);
            return mapper.map(organizationMongoUpdated, Organization.class);
        } catch (Exception e) {
            LOGGER.error("An error occurred when updating organization", e);
            throw new TechnicalException("An error occurred when updating organization");
        }
    }

    @Override
    public void delete(String organizationId) throws TechnicalException {
        try {
            internalOrganizationRepo.deleteById(organizationId);
        } catch (Exception e) {
            LOGGER.error("An error occurred when deleting organization [{}]", organizationId, e);
            throw new TechnicalException("An error occurred when deleting organization");
        }
    }

    @Override
    public Long count() throws TechnicalException {
        try {
            return internalOrganizationRepo.count();
        } catch (Exception e) {
            LOGGER.error("An error occurred when counting organizations", e);
            throw new TechnicalException("An error occurred when counting organization");
        }
    }

    @Override
    public Set<Organization> findAll() throws TechnicalException {
        try {
            return internalOrganizationRepo
                .findAll()
                .stream()
                .map(organization -> mapper.map(organization, Organization.class))
                .collect(Collectors.toSet());
        } catch (Exception e) {
            LOGGER.error("An error occurred when counting organizations", e);
            throw new TechnicalException("An error occurred when counting organization");
        }
    }

    @Override
    public Set<Organization> findByHrids(Set<String> hrids) {
        final Set<OrganizationMongo> organizations = internalOrganizationRepo.findByHrids(hrids);
        return organizations
            .stream()
            .map(organizationMongo -> mapper.map(organizationMongo, Organization.class))
            .collect(Collectors.toSet());
    }
}
