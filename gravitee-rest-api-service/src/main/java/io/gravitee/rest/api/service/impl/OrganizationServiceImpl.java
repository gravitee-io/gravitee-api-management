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
package io.gravitee.rest.api.service.impl;

import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.NewOrganizationEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.OrganizationAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class OrganizationServiceImpl extends TransactionalService implements OrganizationService {

    private final Logger LOGGER = LoggerFactory.getLogger(OrganizationServiceImpl.class);

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleService roleService;

    @Override
    public OrganizationEntity findById(String organizationId) {
        try {
            LOGGER.debug("Find organization by ID: {}", organizationId);
            Optional<Organization> optOrganization = organizationRepository.findById(organizationId);

            if (! optOrganization.isPresent()) {
                throw new OrganizationNotFoundException(organizationId);
            }

            return convert(optOrganization.get());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find organization by ID", ex);
            throw new TechnicalManagementException("An error occurs while trying to find organization by ID", ex);
        }
    }

    @Override
    public List<OrganizationEntity> findAll() {
        try {
            LOGGER.debug("Find all organizations");
            return organizationRepository.findAll()
                    .stream()
                    .map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all organizations", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all organizations", ex);
        }
    }

    @Override
    public OrganizationEntity create(final NewOrganizationEntity organizationEntity) {
        try {
            String id = UUID.toString(UUID.random());
            Optional<Organization> checkOrganization = organizationRepository.findById(id);
            if (checkOrganization.isPresent()) {
                throw new OrganizationAlreadyExistsException(id);
            }

            Organization organization = convert(id, organizationEntity);
            OrganizationEntity createdOrganization = convert(organizationRepository.create(organization));
            
            //create Default role for organization
            roleService.initialize(createdOrganization.getId());
            roleService.createOrUpdateSystemRoles(createdOrganization.getId());
            
            return createdOrganization;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create organization {}", organizationEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to create organization " + organizationEntity.getName(), ex);
        }
    }

    @Override
    public OrganizationEntity update(final UpdateOrganizationEntity organizationEntity) {
        try {
            Organization organization = convert(organizationEntity);
            Optional<Organization> organizationOptional = organizationRepository.findById(organization.getId());
            if (organizationOptional.isPresent()) {
                return convert(organizationRepository.update(organization));
            } else {
                LOGGER.error("An error occurs while trying to update organization {}", organizationEntity.getName());
                throw new OrganizationNotFoundException(organizationEntity.getId());
            }
            
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update organization {}", organizationEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to update organization " + organizationEntity.getName(), ex);
        }
        
    }

    @Override
    public void delete(final String organizationId) {
        try {
            Optional<Organization> organizationOptional = organizationRepository.findById(organizationId);
            if (organizationOptional.isPresent()) {
                organizationRepository.delete(organizationId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete organization {}", organizationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete organization " + organizationId, ex);
        }
    }

    private Organization convert(final String id, final NewOrganizationEntity organizationEntity) {
        final Organization organization = new Organization();
        organization.setId(id);
        organization.setName(organizationEntity.getName());
        organization.setDescription(organizationEntity.getDescription());
        organization.setDomainRestrictions(organizationEntity.getDomainRestrictions());
        return organization;
    }

    private Organization convert(final UpdateOrganizationEntity organizationEntity) {
        final Organization organization = new Organization();
        organization.setId(organizationEntity.getId());
        organization.setName(organizationEntity.getName());
        organization.setDescription(organizationEntity.getDescription());
        organization.setDomainRestrictions(organizationEntity.getDomainRestrictions());
        return organization;
    }

    private OrganizationEntity convert(final Organization organization) {
        final OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(organization.getId());
        organizationEntity.setName(organization.getName());
        organizationEntity.setDescription(organization.getDescription());
        organizationEntity.setDomainRestrictions(organization.getDomainRestrictions());
        return organizationEntity;
    }

    @Override
    public void initialize() {
        Organization defaultOrganization = new Organization();
        defaultOrganization.setId(GraviteeContext.getDefaultOrganization());
        defaultOrganization.setName("Default organization");
        defaultOrganization.setDescription("Default organization");
        try {
            organizationRepository.create(defaultOrganization);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create default organization", ex);
            throw new TechnicalManagementException("An error occurs while trying to create default organization", ex);
        }
    }
}
