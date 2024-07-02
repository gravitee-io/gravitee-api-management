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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.mongodb.management.internal.environment.EnvironmentMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.EnvironmentMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class MongoEnvironmentRepository implements EnvironmentRepository {

    @Autowired
    private EnvironmentMongoRepository internalEnvironmentRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Environment> findById(String environmentId) throws TechnicalException {
        log.debug("Find environment by ID [{}]", environmentId);

        final EnvironmentMongo environment = internalEnvironmentRepo.findById(environmentId).orElse(null);

        log.debug("Find environment by ID [{}] - Done", environment);
        return Optional.ofNullable(mapper.map(environment));
    }

    @Override
    public Environment create(Environment environment) throws TechnicalException {
        log.debug("Create environment [{}]", environment.getName());

        EnvironmentMongo environmentMongo = mapper.map(environment);
        EnvironmentMongo createdEnvironmentMongo = internalEnvironmentRepo.insert(environmentMongo);

        Environment res = mapper.map(createdEnvironmentMongo);

        log.debug("Create environment [{}] - Done", environment.getName());

        return res;
    }

    @Override
    public Environment update(Environment environment) throws TechnicalException {
        if (environment == null || environment.getName() == null) {
            throw new IllegalStateException("Environment to update must have a name");
        }

        final EnvironmentMongo environmentMongo = internalEnvironmentRepo.findById(environment.getId()).orElse(null);

        if (environmentMongo == null) {
            throw new IllegalStateException(String.format("No environment found with id [%s]", environment.getId()));
        }

        try {
            //Update
            environmentMongo.setName(environment.getName());
            environmentMongo.setDescription(environment.getDescription());
            environmentMongo.setHrids(environment.getHrids());
            environmentMongo.setCockpitId(environment.getCockpitId());

            EnvironmentMongo environmentMongoUpdated = internalEnvironmentRepo.save(environmentMongo);
            return mapper.map(environmentMongoUpdated);
        } catch (Exception e) {
            log.error("An error occurred when updating environment", e);
            throw new TechnicalException("An error occurred when updating environment");
        }
    }

    @Override
    public void delete(String environmentId) throws TechnicalException {
        try {
            internalEnvironmentRepo.deleteById(environmentId);
        } catch (Exception e) {
            log.error("An error occured when deleting environment [{}]", environmentId, e);
            throw new TechnicalException("An error occured when deleting environment");
        }
    }

    @Override
    public Set<Environment> findAll() throws TechnicalException {
        final List<EnvironmentMongo> environments = internalEnvironmentRepo.findAll();
        return environments.stream().map(environmentMongo -> mapper.map(environmentMongo)).collect(Collectors.toSet());
    }

    @Override
    public Set<Environment> findByOrganization(String organizationId) throws TechnicalException {
        final Set<EnvironmentMongo> environments = internalEnvironmentRepo.findByOrganizationId(organizationId);
        return environments.stream().map(environmentMongo -> mapper.map(environmentMongo)).collect(Collectors.toSet());
    }

    @Override
    public Set<Environment> findByOrganizationsAndHrids(Set<String> organizations, Set<String> hrids) throws TechnicalException {
        Set<EnvironmentMongo> environments = new HashSet<>();

        if (!CollectionUtils.isEmpty(organizations) && !CollectionUtils.isEmpty(hrids)) {
            environments = internalEnvironmentRepo.findByOrganizationsAndHrids(organizations, hrids);
        } else if (!CollectionUtils.isEmpty(organizations)) {
            environments = internalEnvironmentRepo.findByOrganizations(organizations);
        } else if (!CollectionUtils.isEmpty(hrids)) {
            environments = internalEnvironmentRepo.findByHrids(hrids);
        }

        return environments.stream().map(environmentMongo -> mapper.map(environmentMongo)).collect(Collectors.toSet());
    }

    @Override
    public Optional<Environment> findByCockpitId(String cockpitId) throws TechnicalException {
        log.debug("Find environment by cockpit ID [{}]", cockpitId);

        return internalEnvironmentRepo
            .findByCockpitId(cockpitId)
            .map(environment -> {
                log.debug("Find environment by cockpit ID [{}] - Done", environment);
                return mapper.map(environment);
            });
    }

    @Override
    public Set<String> findOrganizationIdsByEnvironments(final Set<String> ids) throws TechnicalException {
        log.debug("Find organization ids for environments [{}]", ids);

        return internalEnvironmentRepo
            .findOrganizationIdsByEnvironments(ids)
            .stream()
            .map(EnvironmentMongo::getOrganizationId)
            .collect(Collectors.toSet());
    }
}
