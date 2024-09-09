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

import io.gravitee.apim.core.shared_policy_group.use_case.InitializeSharedPolicyGroupUseCase;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
public class EnvironmentServiceImpl extends TransactionalService implements EnvironmentService {

    private final Logger LOGGER = LoggerFactory.getLogger(EnvironmentServiceImpl.class);

    @Lazy
    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ApiHeaderService apiHeaderService;

    @Autowired
    private PageService pageService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private InitializeSharedPolicyGroupUseCase initializeSharedPolicyGroupUseCase;

    @Autowired
    private MetadataService metadataService;

    @Override
    public EnvironmentEntity findById(String environmentId) {
        try {
            LOGGER.debug("Find environment by ID: {}", environmentId);
            Optional<Environment> optEnvironment = environmentRepository.findById(environmentId);

            if (!optEnvironment.isPresent()) {
                throw new EnvironmentNotFoundException(environmentId);
            }

            return convert(optEnvironment.get());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find environment by ID", ex);
            throw new TechnicalManagementException("An error occurs while trying to find environment by ID", ex);
        }
    }

    @Override
    public List<EnvironmentEntity> findByUser(final String organizationId, String userId) {
        try {
            LOGGER.debug("Find all environments by user");

            Stream<Environment> envStream = environmentRepository.findByOrganization(organizationId).stream();

            if (userId != null) {
                final List<String> stringStream = membershipService
                    .getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.ENVIRONMENT)
                    .stream()
                    .map(MembershipEntity::getReferenceId)
                    .collect(Collectors.toList());

                envStream = envStream.filter(env -> stringStream.contains(env.getId()));
            }
            return envStream.map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all environments", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all environments", ex);
        }
    }

    @Override
    public EnvironmentEntity findByOrgAndIdOrHrid(final String organizationId, String idOrHrid) {
        try {
            LOGGER.debug("Find all environments by org and environment id or hrid");

            Set<Environment> byOrgAndIdOrHrid = environmentRepository
                .findByOrganization(organizationId)
                .stream()
                .filter(Objects::nonNull)
                .filter(env -> env.getId().equals(idOrHrid) || env.getHrids().contains(idOrHrid))
                .collect(Collectors.toSet());

            if (byOrgAndIdOrHrid.isEmpty()) {
                throw new EnvironmentNotFoundException(idOrHrid);
            }

            if (byOrgAndIdOrHrid.size() > 1) {
                throw new IllegalStateException("More than one environment found for hrid or id " + idOrHrid);
            }

            return convert(byOrgAndIdOrHrid.iterator().next());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all environments", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all environments", ex);
        }
    }

    @Override
    public Set<String> findOrganizationIdsByEnvironments(final Set<String> environmentIds) {
        try {
            LOGGER.debug("Find organization id from environment ids");

            return environmentRepository
                .findOrganizationIdsByEnvironments(environmentIds)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to find organization ids from environment ids", ex);
        }
    }

    @Override
    public EnvironmentEntity createOrUpdate(String organizationId, String environmentId, final UpdateEnvironmentEntity environmentEntity) {
        try {
            // First we check that organization exists
            this.organizationService.findById(organizationId);

            Optional<Environment> environmentOptional = environmentRepository.findById(environmentId);
            Environment environment = convert(environmentEntity);
            environment.setId(environmentId);
            environment.setOrganizationId(organizationId);

            if (environmentOptional.isPresent()) {
                return convert(environmentRepository.update(environment));
            } else {
                EnvironmentEntity createdEnvironment = convert(environmentRepository.create(environment));

                //create Default items for environment
                ExecutionContext executionContext = new ExecutionContext(organizationId, environmentId);
                apiHeaderService.initialize(executionContext);
                pageService.initialize(executionContext);
                dashboardService.initialize(executionContext);
                initializeSharedPolicyGroupUseCase.execute(
                    InitializeSharedPolicyGroupUseCase.Input.builder().organizationId(organizationId).environmentId(environmentId).build()
                );
                metadataService.initialize(executionContext);
                return createdEnvironment;
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update environment {}", environmentEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to update environment " + environmentEntity.getName(), ex);
        }
    }

    @Override
    public void delete(final String environmentId) {
        try {
            Optional<Environment> environmentOptional = environmentRepository.findById(environmentId);
            if (environmentOptional.isPresent()) {
                environmentRepository.delete(environmentId);
            } else {
                throw new EnvironmentNotFoundException(environmentId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete environment {}", environmentId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete environment " + environmentId, ex);
        }
    }

    @Override
    public EnvironmentEntity initialize() {
        Environment defaultEnvironment = new Environment();
        defaultEnvironment.setId(GraviteeContext.getDefaultEnvironment());
        defaultEnvironment.setName("Default environment");
        defaultEnvironment.setHrids(Collections.singletonList("default"));
        defaultEnvironment.setDescription("Default environment");
        defaultEnvironment.setOrganizationId(GraviteeContext.getDefaultOrganization());
        try {
            environmentRepository.create(defaultEnvironment);
            return convert(defaultEnvironment);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create default environment", ex);
            throw new TechnicalManagementException("An error occurs while trying to create default environment", ex);
        }
    }

    @Override
    public EnvironmentEntity findByCockpitId(String cockpitId) {
        try {
            LOGGER.debug("Find environment by cockpit id");
            return environmentRepository
                .findByCockpitId(cockpitId)
                .map(this::convert)
                .orElseThrow(() -> new EnvironmentNotFoundException(cockpitId));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find environment by cockpit id {}", cockpitId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find environment by cockpit id " + cockpitId, ex);
        }
    }

    @Override
    public List<EnvironmentEntity> findByOrganization(String organizationId) {
        try {
            LOGGER.debug("Find all environments by organization");
            return environmentRepository.findByOrganization(organizationId).stream().map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all environments by organization {}", organizationId, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to find all environments by organization " + organizationId,
                ex
            );
        }
    }

    @Override
    public EnvironmentEntity getDefaultOrInitialize() {
        try {
            return environmentRepository.findById(GraviteeContext.getDefaultEnvironment()).map(this::convert).orElseGet(this::initialize);
        } catch (final Exception ex) {
            LOGGER.error("Error while getting installation : {}", ex.getMessage());
            throw new TechnicalManagementException("Error while getting installation", ex);
        }
    }

    @Override
    public Set<EnvironmentEntity> findAllOrInitialize() {
        try {
            var environments = environmentRepository.findAll().stream().map(this::convert).collect(Collectors.toSet());
            if (environments.isEmpty()) {
                return Set.of(this.initialize());
            }
            return environments;
        } catch (final Exception ex) {
            LOGGER.error("Error while getting installation : {}", ex.getMessage());
            throw new TechnicalManagementException("Error while getting installation", ex);
        }
    }

    private Environment convert(final UpdateEnvironmentEntity environmentEntity) {
        final Environment environment = new Environment();
        environment.setCockpitId(environmentEntity.getCockpitId());
        environment.setHrids(environmentEntity.getHrids());
        environment.setName(environmentEntity.getName());
        environment.setDescription(environmentEntity.getDescription());
        return environment;
    }

    private EnvironmentEntity convert(final Environment environment) {
        final EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(environment.getId());
        environmentEntity.setCockpitId(environment.getCockpitId());
        environmentEntity.setHrids(environment.getHrids());
        environmentEntity.setName(environment.getName());
        environmentEntity.setDescription(environment.getDescription());
        environmentEntity.setOrganizationId(environment.getOrganizationId());
        return environmentEntity;
    }
}
