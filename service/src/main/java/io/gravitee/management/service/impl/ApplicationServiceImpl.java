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

import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.NewApplicationEntity;
import io.gravitee.management.model.UpdateApplicationEntity;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.exceptions.ApplicationAlreadyExistsException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.api.ApplicationRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Application;
import io.gravitee.repository.model.OwnerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class ApplicationServiceImpl implements ApplicationService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public Optional<ApplicationEntity> findByName(String applicationName) {
        try {
            LOGGER.debug("Find application by name: {}", applicationName);
            return applicationRepository.findByName(applicationName).map(ApplicationServiceImpl::convert);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an application using its name {}", applicationName, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an application using its name " + applicationName, ex);
        }
    }

    @Override
    public Set<ApplicationEntity> findByTeam(String teamName) {
        try {
            LOGGER.debug("Find applications for team {}", teamName);

            Set<Application> applications = applicationRepository.findByTeam(teamName);
            Set<ApplicationEntity> applicationEntities = new HashSet<>(applications.size());

            for(Application application : applications) {
                applicationEntities.add(convert(application));
            }

            return applicationEntities;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find applications for team {}", teamName, ex);
            throw new TechnicalManagementException("An error occurs while trying to find applications for team " + teamName, ex);
        }
    }

    @Override
    public Set<ApplicationEntity> findByUser(String username) {
        try {
            LOGGER.debug("Find applications for user {}", username);

            Set<Application> applications = applicationRepository.findByUser(username);
            Set<ApplicationEntity> applicationEntities = new HashSet<>(applications.size());

            for(Application application : applications) {
                applicationEntities.add(convert(application));
            }

            return applicationEntities;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find applications for user {}", username, ex);
            throw new TechnicalManagementException("An error occurs while trying to find applications for user " + username, ex);
        }
    }

    @Override
    public ApplicationEntity createForUser(NewApplicationEntity newApplicationEntity, String owner) {
        try {
            LOGGER.debug("Create {} for user {}", newApplicationEntity, owner);
            return create(newApplicationEntity, OwnerType.USER, owner);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for user {}", newApplicationEntity, owner, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + newApplicationEntity + " for user " + owner, ex);
        }
    }

    @Override
    public ApplicationEntity createForTeam(NewApplicationEntity newApplicationEntity, String owner) {
        try {
            LOGGER.debug("Create {} for team {}", newApplicationEntity, owner);
            return create(newApplicationEntity, OwnerType.TEAM, owner);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for team {}", newApplicationEntity, owner, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + newApplicationEntity + " for team " + owner, ex);
        }
    }

    @Override
    public ApplicationEntity update(String applicationName, UpdateApplicationEntity updateApplicationEntity) {
        try {
            LOGGER.debug("Update application {}", applicationName);
            Application application = convert(updateApplicationEntity);
            application.setName(applicationName);
            application.setUpdatedAt(new Date());

            Application updatedApplication =  applicationRepository.update(application);
            return convert(updatedApplication);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update application {}", applicationName, ex);
            throw new TechnicalManagementException("An error occurs while trying to update application " + applicationName, ex);
        }
    }

    @Override
    public void delete(String applicationName) {
        try {
            LOGGER.debug("Delete application {}", applicationName);
            applicationRepository.delete(applicationName);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete application {}", applicationName, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete application " + applicationName, ex);
        }
    }

    @Override
    public Set<ApplicationEntity> findByApi(String apiName) {
        return null;
    }

    private ApplicationEntity create(NewApplicationEntity newApplicationEntity, OwnerType ownerType, String owner) throws ApplicationAlreadyExistsException, TechnicalException {
        Optional<ApplicationEntity> checkApplication = findByName(newApplicationEntity.getName());
        if (checkApplication.isPresent()) {
            throw new ApplicationAlreadyExistsException(newApplicationEntity.getName());
        }

        Application application = convert(newApplicationEntity);

        // Set owner and owner type
        application.setOwner(owner);
        application.setOwnerType(ownerType);
        application.setCreator(owner);

        // Set date fields
        application.setCreatedAt(new Date());
        application.setUpdatedAt(application.getCreatedAt());

        Application createdApplication = applicationRepository.create(application);
        return convert(createdApplication);
    }

    private static ApplicationEntity convert(Application application) {
        ApplicationEntity applicationEntity = new ApplicationEntity();

        applicationEntity.setName(application.getName());
        applicationEntity.setDescription(application.getDescription());
        applicationEntity.setType(application.getType());

        applicationEntity.setCreatedAt(application.getCreatedAt());
        applicationEntity.setUpdatedAt(application.getUpdatedAt());

        return applicationEntity;
    }

    private static Application convert(NewApplicationEntity newApplicationEntity) {
        Application application = new Application();

        application.setName(newApplicationEntity.getName());
        application.setDescription(newApplicationEntity.getDescription());
        application.setType(newApplicationEntity.getType());

        return application;
    }

    private static Application convert(UpdateApplicationEntity updateApplicationEntity) {
        Application application = new Application();

        application.setDescription(updateApplicationEntity.getDescription());
        application.setType(updateApplicationEntity.getType());

        return application;
    }
}
