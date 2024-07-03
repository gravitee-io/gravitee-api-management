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
package io.gravitee.repository.mongodb.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.mongodb.management.internal.application.ApplicationMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApplicationMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoApplicationRepository implements ApplicationRepository {

    @Autowired
    private ApplicationMongoRepository internalApplicationRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Set<Application> findAll(ApplicationStatus... statuses) throws TechnicalException {
        List<ApplicationMongo> applications;
        if (statuses != null && statuses.length > 0) {
            applications = internalApplicationRepo.findAll(Arrays.asList(statuses));
        } else {
            applications = internalApplicationRepo.findAll();
        }
        return mapApplications(applications);
    }

    @Override
    public Application create(Application application) throws TechnicalException {
        ApplicationMongo applicationMongo = mapApplication(application);
        ApplicationMongo applicationMongoCreated = internalApplicationRepo.insert(applicationMongo);
        return mapApplication(applicationMongoCreated);
    }

    @Override
    public Application update(Application application) throws TechnicalException {
        if (application == null || application.getId() == null) {
            throw new IllegalStateException("Application to update must have an id");
        }

        final ApplicationMongo applicationMongo = internalApplicationRepo.findById(application.getId()).orElse(null);
        if (applicationMongo == null) {
            throw new IllegalStateException(String.format("No application found with id [%s]", application.getId()));
        }

        applicationMongo.setName(application.getName());
        applicationMongo.setEnvironmentId(application.getEnvironmentId());
        applicationMongo.setDescription(application.getDescription());
        applicationMongo.setDomain(application.getDomain());
        applicationMongo.setCreatedAt(application.getCreatedAt());
        applicationMongo.setUpdatedAt(application.getUpdatedAt());
        applicationMongo.setType(application.getType().toString());
        applicationMongo.setGroups(application.getGroups());
        applicationMongo.setPicture(application.getPicture());
        applicationMongo.setBackground(application.getBackground());
        applicationMongo.setStatus(application.getStatus().toString());
        applicationMongo.setMetadata(application.getMetadata());
        applicationMongo.setApiKeyMode(application.getApiKeyMode().name());
        applicationMongo.setDisableMembershipNotifications(application.isDisableMembershipNotifications());

        ApplicationMongo applicationMongoUpdated = internalApplicationRepo.save(applicationMongo);
        return mapApplication(applicationMongoUpdated);
    }

    @Override
    public Optional<Application> findById(String applicationId) throws TechnicalException {
        ApplicationMongo application = internalApplicationRepo.findById(applicationId).orElse(null);
        return Optional.ofNullable(mapApplication(application));
    }

    @Override
    public Set<Application> findByIds(Collection<String> ids) {
        return mapApplications(internalApplicationRepo.findByIds(ids));
    }

    @Override
    public Set<Application> findByIds(Collection<String> ids, Sortable sortable) {
        return mapApplications(internalApplicationRepo.findByIds(ids, sortable));
    }

    @Override
    public Set<Application> findByGroups(List<String> groupIds, ApplicationStatus... statuses) {
        if (statuses != null && statuses.length > 0) {
            return mapApplications(internalApplicationRepo.findByGroups(groupIds, Arrays.asList(statuses)));
        } else {
            return mapApplications(internalApplicationRepo.findByGroups(groupIds));
        }
    }

    @Override
    public Set<Application> findByNameAndStatuses(String partialName, ApplicationStatus... statuses) {
        if (statuses != null && statuses.length > 0) {
            return mapApplications(internalApplicationRepo.findByNameAndStatuses(partialName, Arrays.asList(statuses)));
        } else {
            return mapApplications(internalApplicationRepo.findByName(partialName));
        }
    }

    @Override
    public Page<Application> search(ApplicationCriteria applicationCriteria, Pageable pageable, Sortable sortable) {
        return internalApplicationRepo.search(applicationCriteria, pageable, sortable).map(this::mapApplication);
    }

    @Override
    public Set<String> searchIds(ApplicationCriteria applicationCriteria, Sortable sortable) {
        if (sortable == null) {
            return internalApplicationRepo.searchIds(applicationCriteria).collect(Collectors.toSet());
        }
        final Page<ApplicationMongo> applicationsMongo = internalApplicationRepo.search(applicationCriteria, null, sortable);
        return applicationsMongo
            .getContent()
            .parallelStream()
            .map(ApplicationMongo::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public void delete(String applicationId) throws TechnicalException {
        internalApplicationRepo.deleteById(applicationId);
    }

    private Set<Application> mapApplications(Collection<ApplicationMongo> applications) {
        return applications.stream().map(this::mapApplication).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Application mapApplication(ApplicationMongo applicationMongo) {
        return (applicationMongo == null) ? null : mapper.map(applicationMongo);
    }

    private ApplicationMongo mapApplication(Application application) {
        return (application == null) ? null : mapper.map(application);
    }

    @Override
    public Set<Application> findAllByEnvironment(String environmentId, ApplicationStatus... statuses) {
        if (statuses != null && statuses.length > 0) {
            return mapApplications(internalApplicationRepo.findAllByEnvironmentId(environmentId, Arrays.asList(statuses)));
        } else {
            return mapApplications(internalApplicationRepo.findAllByEnvironmentId(environmentId));
        }
    }

    @Override
    public Set<Application> findAll() throws TechnicalException {
        return internalApplicationRepo.findAll().stream().map(this::mapApplication).collect(Collectors.toSet());
    }
}
