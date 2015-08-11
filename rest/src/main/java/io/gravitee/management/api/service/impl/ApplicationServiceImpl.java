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
package io.gravitee.management.api.service.impl;

import io.gravitee.management.api.model.ApplicationEntity;
import io.gravitee.management.api.model.NewApplicationEntity;
import io.gravitee.management.api.model.UpdateApplicationEntity;
import io.gravitee.management.api.service.ApplicationService;
import io.gravitee.repository.api.ApplicationRepository;
import io.gravitee.repository.model.Application;
import io.gravitee.repository.model.OwnerType;
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

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public Optional<ApplicationEntity> findByName(String applicationName) {
        return applicationRepository.findByName(applicationName).map(application -> convert(application));
    }

    @Override
    public Set<ApplicationEntity> findByTeam(String teamName) {
        Set<Application> applications = applicationRepository.findByTeam(teamName);
        Set<ApplicationEntity> applicationEntities = new HashSet<>(applications.size());

        for(Application application : applications) {
            applicationEntities.add(convert(application));
        }

        return applicationEntities;
    }

    @Override
    public Set<ApplicationEntity> findByUser(String username) {
        Set<Application> applications = applicationRepository.findByUser(username);
        Set<ApplicationEntity> applicationEntities = new HashSet<>(applications.size());

        for(Application application : applications) {
            applicationEntities.add(convert(application));
        }

        return applicationEntities;
    }

    @Override
    public ApplicationEntity createForUser(NewApplicationEntity newApplicationEntity, String username) {
        Application application = convert(newApplicationEntity);

        // Set owner and owner type
        application.setOwner(username);
        application.setOwnerType(OwnerType.USER);

        Application createdApplication = create(application);
        return convert(createdApplication);
    }

    @Override
    public ApplicationEntity createForTeam(NewApplicationEntity newApplicationEntity, String teamName) {
        Application application = convert(newApplicationEntity);

        // Set owner and owner type
        application.setOwner(teamName);
        application.setOwnerType(OwnerType.TEAM);

        Application createdApplication = create(application);
        return convert(createdApplication);
    }

    @Override
    public ApplicationEntity update(UpdateApplicationEntity updateApplicationEntity) {
        Application application = convert(updateApplicationEntity);

        application.setUpdatedAt(new Date());

        Application updatedApplication =  applicationRepository.update(application);
        return convert(updatedApplication);
    }

    @Override
    public void delete(String applicationName) {
        applicationRepository.delete(applicationName);
    }

    private Application create(Application application) {
        // Update date fields
        application.setCreatedAt(new Date());
        application.setUpdatedAt(application.getCreatedAt());

        return applicationRepository.create(application);
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
