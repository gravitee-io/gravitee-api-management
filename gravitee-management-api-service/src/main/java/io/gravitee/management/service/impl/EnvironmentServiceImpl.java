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
package io.gravitee.management.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.management.model.EnvironmentEntity;
import io.gravitee.management.model.NewEnvironmentEntity;
import io.gravitee.management.model.UpdateEnvironmentEntity;
import io.gravitee.management.service.EnvironmentService;
import io.gravitee.management.service.common.GraviteeContext;
import io.gravitee.management.service.exceptions.DuplicateEnvironmentNameException;
import io.gravitee.management.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class EnvironmentServiceImpl extends TransactionalService implements EnvironmentService {

    private final Logger LOGGER = LoggerFactory.getLogger(EnvironmentServiceImpl.class);

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Override
    public EnvironmentEntity findById(String environmentId) {
        try {
            LOGGER.debug("Find environment by ID: {}", environmentId);
            Optional<Environment> optEnvironment = environmentRepository.findById(environmentId);

            if (! optEnvironment.isPresent()) {
                throw new EnvironmentNotFoundException(environmentId);
            }

            return convert(optEnvironment.get());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find environment by ID", ex);
            throw new TechnicalManagementException("An error occurs while trying to find environment by ID", ex);
        }
    }

    @Override
    public List<EnvironmentEntity> findAll() {
        try {
            LOGGER.debug("Find all environments");
            return environmentRepository.findAll()
                    .stream()
                    .map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all environments", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all environments", ex);
        }
    }

    @Override
    public EnvironmentEntity create(final NewEnvironmentEntity environmentEntity) {
        // First we prevent the duplicate environment name
        final List<String> environmentNames = this.findAll().stream()
                .map(EnvironmentEntity::getName)
                .collect(Collectors.toList());

        if (environmentNames.contains(environmentEntity.getName())) {
            throw new DuplicateEnvironmentNameException(environmentEntity.getName());
        }

        try {
            Environment environment = convert(environmentEntity);
            EnvironmentEntity savedEnvironmentEntity = convert(environmentRepository.create(environment));
            return savedEnvironmentEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create environment {}", environmentEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to create environment " + environmentEntity.getName(), ex);
        }
    }

    @Override
    public EnvironmentEntity update(final UpdateEnvironmentEntity environmentEntity) {
        try {
            Environment environment = convert(environmentEntity);
            Optional<Environment> environmentOptional = environmentRepository.findById(environment.getId());
            if (environmentOptional.isPresent()) {
                EnvironmentEntity updatedEnvironmentEntity = convert(environmentRepository.update(environment));
                return updatedEnvironmentEntity;
            } else {
                LOGGER.error("An error occurs while trying to update environment {}", environmentEntity.getName());
                throw new EnvironmentNotFoundException(environmentEntity.getId());
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
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete environment {}", environmentId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete environment " + environmentId, ex);
        }
    }

    private Environment convert(final NewEnvironmentEntity environmentEntity) {
        final Environment environment = new Environment();
        environment.setId(IdGenerator.generate(environmentEntity.getName()));
        environment.setName(environmentEntity.getName());
        return environment;
    }

    private Environment convert(final UpdateEnvironmentEntity environmentEntity) {
        final Environment environment = new Environment();
        environment.setId(environmentEntity.getId());
        environment.setName(environmentEntity.getName());
        return environment;
    }

    private EnvironmentEntity convert(final Environment environment) {
        final EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(environment.getId());
        environmentEntity.setName(environment.getName());
        return environmentEntity;
    }

    @Override
    public void createDefaultEnvironment() {
        Environment defaultEnvironment = new Environment();
        defaultEnvironment.setId(GraviteeContext.getDefaultEnvironment());
        defaultEnvironment.setName("Default environment");
        try {
            environmentRepository.create(defaultEnvironment);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create default environment", ex);
            throw new TechnicalManagementException("An error occurs while trying to create default environment", ex);
        }
    }
}
