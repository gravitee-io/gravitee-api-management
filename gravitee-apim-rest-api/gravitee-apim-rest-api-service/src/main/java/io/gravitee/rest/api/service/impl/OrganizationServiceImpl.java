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
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.flow.FlowV2;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class OrganizationServiceImpl extends TransactionalService implements OrganizationService {

    private final Logger LOGGER = LoggerFactory.getLogger(OrganizationServiceImpl.class);

    @Lazy
    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleService roleService;

    @Autowired
    private FlowService flowService;

    @Autowired
    private EventService eventService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public OrganizationEntity findById(String organizationId) {
        try {
            LOGGER.debug("Find organization by ID: {}", organizationId);
            Optional<Organization> optOrganization = organizationRepository.findById(organizationId);

            if (!optOrganization.isPresent()) {
                throw new OrganizationNotFoundException(organizationId);
            }

            return convert(optOrganization.get());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find organization by ID", ex);
            throw new TechnicalManagementException("An error occurs while trying to find organization by ID", ex);
        }
    }

    @Override
    public OrganizationEntity updateOrganization(String organizationId, final UpdateOrganizationEntity organizationEntity) {
        try {
            Optional<Organization> organizationOptional = organizationRepository.findById(organizationId);
            if (organizationOptional.isPresent()) {
                Organization organization = convert(organizationEntity);
                organization.setId(organizationOptional.get().getId());
                OrganizationEntity updatedOrganization = convert(organizationRepository.update(organization));
                createPublishOrganizationEvent(updatedOrganization);
                return updatedOrganization;
            } else {
                throw new OrganizationNotFoundException(organizationId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update organization {}", organizationEntity.getName(), ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to update organization " + organizationEntity.getName(),
                ex
            );
        }
    }

    @Override
    public OrganizationEntity createOrUpdate(String organizationId, final UpdateOrganizationEntity organizationEntity) {
        try {
            try {
                return this.updateOrganizationAndFlows(organizationId, organizationEntity);
            } catch (OrganizationNotFoundException e) {
                Organization organization = convert(organizationEntity);
                organization.setId(organizationId);
                flowService.save(FlowReferenceType.ORGANIZATION, organizationId, organizationEntity.getFlows());
                OrganizationEntity createdOrganization = convert(organizationRepository.create(organization));

                //create Default role for organization
                ExecutionContext executionContext = new ExecutionContext(organizationId);
                roleService.initialize(executionContext, createdOrganization.getId());
                roleService.createOrUpdateSystemRoles(executionContext, createdOrganization.getId());
                createPublishOrganizationEvent(createdOrganization);

                return createdOrganization;
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update organization {}", organizationEntity.getName(), ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to update organization " + organizationEntity.getName(),
                ex
            );
        }
    }

    @Override
    public OrganizationEntity updateOrganizationAndFlows(String organizationId, final UpdateOrganizationEntity organizationEntity) {
        try {
            Optional<Organization> organizationOptional = organizationRepository.findById(organizationId);
            if (organizationOptional.isPresent()) {
                flowService.save(FlowReferenceType.ORGANIZATION, organizationId, organizationEntity.getFlows());
                Organization organization = convert(organizationEntity);
                organization.setId(organizationId);
                OrganizationEntity updatedOrganization = convert(organizationRepository.update(organization));
                createPublishOrganizationEvent(updatedOrganization);
                return updatedOrganization;
            } else {
                throw new OrganizationNotFoundException(organizationId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update organization {}", organizationEntity.getName(), ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to update organization " + organizationEntity.getName(),
                ex
            );
        }
    }

    private void createPublishOrganizationEvent(OrganizationEntity organizationEntity) {
        Set<String> environmentIds = environmentService
            .findByOrganization(organizationEntity.getId())
            .stream()
            .map(EnvironmentEntity::getId)
            .collect(Collectors.toSet());

        eventService.createOrganizationEvent(
            new ExecutionContext(organizationEntity.getId()),
            environmentIds,
            organizationEntity.getId(),
            EventType.PUBLISH_ORGANIZATION,
            organizationEntity
        );
    }

    @Override
    public Long count() {
        try {
            return organizationRepository.count();
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to count organizations", ex);
            throw new TechnicalManagementException("An error occurs while trying to count organizations ", ex);
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

    private Organization convert(final UpdateOrganizationEntity organizationEntity) {
        final Organization organization = new Organization();
        organization.setHrids(organizationEntity.getHrids());
        organization.setCockpitId(organizationEntity.getCockpitId());
        organization.setName(organizationEntity.getName());
        organization.setDescription(organizationEntity.getDescription());
        String flowMode = organizationEntity.getFlowMode() != null ? organizationEntity.getFlowMode().name() : FlowMode.DEFAULT.name();
        organization.setFlowMode(flowMode);
        return organization;
    }

    private OrganizationEntity convert(final Organization organization) {
        final OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(organization.getId());
        organizationEntity.setCockpitId(organization.getCockpitId());
        organizationEntity.setHrids(organization.getHrids());
        organizationEntity.setName(organization.getName());
        organizationEntity.setDescription(organization.getDescription());
        FlowMode flowMode = organization.getFlowMode() != null ? FlowMode.valueOf(organization.getFlowMode()) : FlowMode.DEFAULT;
        organizationEntity.setFlowMode(flowMode);
        List<FlowV2> flows = flowService.findByReference(FlowReferenceType.ORGANIZATION, organization.getId());
        organizationEntity.setFlows(flows);
        return organizationEntity;
    }

    @Override
    public OrganizationEntity initialize() {
        Organization defaultOrganization = new Organization();
        defaultOrganization.setId(GraviteeContext.getDefaultOrganization());
        defaultOrganization.setName("Default organization");
        defaultOrganization.setHrids(Collections.singletonList("default"));
        defaultOrganization.setDescription("Default organization");
        try {
            organizationRepository.create(defaultOrganization);
            return convert(defaultOrganization);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create default organization", ex);
            throw new TechnicalManagementException("An error occurs while trying to create default organization", ex);
        }
    }

    @Override
    public Collection<OrganizationEntity> findAll() {
        try {
            return organizationRepository.findAll().stream().map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to list all organizations", ex);
            throw new TechnicalManagementException("An error occurs while trying to list all organizations", ex);
        }
    }

    @Override
    public OrganizationEntity getDefaultOrInitialize() {
        try {
            return organizationRepository.findById(GraviteeContext.getDefaultOrganization()).map(this::convert).orElseGet(this::initialize);
        } catch (final Exception ex) {
            LOGGER.error("Error while getting installation : {}", ex.getMessage());
            throw new TechnicalManagementException("Error while getting installation", ex);
        }
    }

    @Override
    public OrganizationEntity findByCockpitId(String cockpitId) {
        try {
            LOGGER.debug("Find organization by cockpit id");
            return organizationRepository
                .findByCockpitId(cockpitId)
                .map(this::convert)
                .orElseThrow(() -> new OrganizationNotFoundException(cockpitId));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find organization by cockpit id {}", cockpitId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find organization by cockpit id " + cockpitId, ex);
        }
    }

    @Override
    public Set<OrganizationEntity> findByHrids(Set<String> hrids) {
        try {
            return organizationRepository.findByHrids(hrids).stream().map(this::convert).collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to list all organizations", ex);
            throw new TechnicalManagementException("An error occurs while trying to list all organizations", ex);
        }
    }
}
