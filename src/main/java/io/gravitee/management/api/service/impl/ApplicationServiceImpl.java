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

import io.gravitee.management.api.service.ApplicationService;
import io.gravitee.repository.api.ApplicationRepository;
import io.gravitee.repository.model.Application;
import io.gravitee.repository.model.OwnerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
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
    public Optional<Application> findByName(String applicationName) {
        return applicationRepository.findByName(applicationName);
    }

    @Override
    public Set<Application> findByTeam(String teamName) {
        return applicationRepository.findByTeam(teamName);
    }

    @Override
    public Set<Application> findByUser(String username) {
        return applicationRepository.findByUser(username);
    }

    @Override
    public Application createForUser(Application application, String username) {
        // Set owner and owner type
        application.setOwner(username);
        application.setOwnerType(OwnerType.USER);

        Application createdApplication = create(application);
        return createdApplication;
    }

    @Override
    public Application createForTeam(Application application, String teamName) {
        // Set owner and owner type
        application.setOwner(teamName);
        application.setOwnerType(OwnerType.TEAM);

        Application createdApplication = create(application);
        return createdApplication;
    }

    @Override
    public Application update(Application application) {
        application.setUpdatedAt(new Date());

        return applicationRepository.update(application);
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
}
