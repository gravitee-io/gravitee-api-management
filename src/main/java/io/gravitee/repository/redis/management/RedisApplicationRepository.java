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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.redis.management.internal.ApplicationRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisApplicationRepository implements ApplicationRepository {

    @Autowired
    private ApplicationRedisRepository applicationRedisRepository;

    @Override
    public Set<Application> findAll(ApplicationStatus... statuses) throws TechnicalException {
        Set<RedisApplication> applications = applicationRedisRepository.findAll();
        if(statuses != null && statuses.length > 0) {
            List<ApplicationStatus> applicationStatuses = Arrays.asList(statuses);
            applications = applications.stream().
                    filter(app ->
                            applicationStatuses.contains(ApplicationStatus.valueOf(app.getStatus()))).
                    collect(Collectors.toSet());
        }

        return applications.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Application> findByIds(List<String> ids) throws TechnicalException {
        return applicationRedisRepository.find(ids).stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Application> findByGroups(List<String> groupIds, ApplicationStatus ... statuses) throws TechnicalException {
        Set<RedisApplication> applications = applicationRedisRepository.findByGroups(groupIds);
        if(statuses != null && statuses.length > 0) {
            List<ApplicationStatus> applicationStatuses = Arrays.asList(statuses);
            applications = applications.stream().
                    filter(app ->
                            applicationStatuses.contains(ApplicationStatus.valueOf(app.getStatus()))).
                    collect(Collectors.toSet());
        }
        return applications
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Application> findByName(String partialName) throws TechnicalException {
        return applicationRedisRepository.findByName(partialName)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Application> findById(String applicationId) throws TechnicalException {
        RedisApplication redisApplication = this.applicationRedisRepository.find(applicationId);
        return Optional.ofNullable(convert(redisApplication));
    }

    @Override
    public Application create(Application application) throws TechnicalException {
        RedisApplication redisApplication = applicationRedisRepository.saveOrUpdate(convert(application));
        return convert(redisApplication);
    }

    @Override
    public Application update(Application application) throws TechnicalException {
        RedisApplication redisApplication = applicationRedisRepository.find(application.getId());

        redisApplication.setName(application.getName());
        redisApplication.setDescription(application.getDescription());
        redisApplication.setCreatedAt(application.getCreatedAt().getTime());
        redisApplication.setUpdatedAt(application.getUpdatedAt().getTime());
        redisApplication.setType(application.getType());
        redisApplication.setGroups(application.getGroups());
        redisApplication.setStatus(application.getStatus().toString());

        applicationRedisRepository.saveOrUpdate(redisApplication);
        return convert(redisApplication);
    }

    @Override
    public void delete(String applicationId) throws TechnicalException {
        applicationRedisRepository.delete(applicationId);
    }

    private Application convert(RedisApplication redisApplication) {
        if (redisApplication == null) {
            return null;
        }

        Application application = new Application();

        application.setId(redisApplication.getId());
        application.setName(redisApplication.getName());
        application.setCreatedAt(new Date(redisApplication.getCreatedAt()));
        application.setUpdatedAt(new Date(redisApplication.getUpdatedAt()));
        application.setDescription(redisApplication.getDescription());
        application.setType(redisApplication.getType());
        application.setGroups(redisApplication.getGroups());
        application.setStatus(ApplicationStatus.valueOf(redisApplication.getStatus()));

        return application;
    }

    private RedisApplication convert(Application application) {
        RedisApplication redisApplication = new RedisApplication();

        redisApplication.setId(application.getId());
        redisApplication.setName(application.getName());
        redisApplication.setCreatedAt(application.getCreatedAt().getTime());
        redisApplication.setUpdatedAt(application.getUpdatedAt().getTime());
        redisApplication.setDescription(application.getDescription());
        redisApplication.setType(application.getType());
        redisApplication.setGroups(application.getGroups());
        redisApplication.setStatus(application.getStatus().toString());

        return redisApplication;
    }
}
