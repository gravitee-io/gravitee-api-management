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
import io.gravitee.management.service.exceptions.ApplicationNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.MembershipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class ApplicationServiceImpl extends TransactionalService implements ApplicationService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public ApplicationEntity findByName(String applicationName) {
        try {
            LOGGER.debug("Find application by name: {}", applicationName);

            Optional<Application> application = applicationRepository.findById(applicationName);

            if (application.isPresent()) {
                return convert(application.get());
            }

            throw new ApplicationNotFoundException(applicationName);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an application using its name {}", applicationName, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an application using its name " + applicationName, ex);
        }
    }

    @Override
    public Set<ApplicationEntity> findByUser(String username) {
        try {
            LOGGER.debug("Find applications for user {}", username);

            final Set<Application> applications = applicationRepository.findByUser(username, null);

            if (applications == null || applications.isEmpty()) {
                return emptySet();
            }

            final Set<ApplicationEntity> applicationEntities = new HashSet<>(applications.size());

            applicationEntities.addAll(applications.stream()
                    .map(ApplicationServiceImpl::convert)
                    .collect(Collectors.toSet())
            );

            return applicationEntities;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find applications for user {}", username, ex);
            throw new TechnicalManagementException("An error occurs while trying to find applications for user " + username, ex);
        }
    }

    @Override
    public ApplicationEntity create(NewApplicationEntity newApplicationEntity, String username) {
        try {
            LOGGER.debug("Create {} for user {}", newApplicationEntity, username);

            Optional<Application> checkApplication = applicationRepository.findById(newApplicationEntity.getName());
            if (checkApplication.isPresent()) {
                throw new ApplicationAlreadyExistsException(newApplicationEntity.getName());
            }

            Application application = convert(newApplicationEntity);

            // Set date fields
            application.setCreatedAt(new Date());
            application.setUpdatedAt(application.getCreatedAt());

            Application createdApplication = applicationRepository.create(application);

            // Add the primary owner of the newly created API
            applicationRepository.addMember(createdApplication.getName(), username, MembershipType.PRIMARY_OWNER);

            return convert(createdApplication);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for user {}", newApplicationEntity, username, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + newApplicationEntity + " for user " + username, ex);
        }
    }

    @Override
    public ApplicationEntity update(String applicationName, UpdateApplicationEntity updateApplicationEntity) {
        try {
            LOGGER.debug("Update application {}", applicationName);

            Optional<Application> optApplicationToUpdate = applicationRepository.findById(applicationName);
            if (!optApplicationToUpdate.isPresent()) {
                throw new ApplicationNotFoundException(applicationName);
            }

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
        // TODO Implements and test me
        return null;
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
