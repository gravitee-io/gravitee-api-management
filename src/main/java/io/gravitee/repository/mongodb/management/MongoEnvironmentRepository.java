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
package io.gravitee.repository.mongodb.management;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Tenant;
import io.gravitee.repository.mongodb.management.internal.environment.EnvironmentMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.EnvironmentMongo;
import io.gravitee.repository.mongodb.management.internal.model.TenantMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoEnvironmentRepository implements EnvironmentRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoEnvironmentRepository.class);

    @Autowired
    private EnvironmentMongoRepository internalEnvironmentRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Environment> findById(String environmentId) throws TechnicalException {
        LOGGER.debug("Find environment by ID [{}]", environmentId);

        final EnvironmentMongo environment = internalEnvironmentRepo.findById(environmentId).orElse(null);

        LOGGER.debug("Find environment by ID [{}] - Done", environment);
        return Optional.ofNullable(mapper.map(environment, Environment.class));
    }

    @Override
    public Environment create(Environment environment) throws TechnicalException {
        LOGGER.debug("Create environment [{}]", environment.getName());

        EnvironmentMongo environmentMongo = mapper.map(environment, EnvironmentMongo.class);
        EnvironmentMongo createdEnvironmentMongo = internalEnvironmentRepo.insert(environmentMongo);

        Environment res = mapper.map(createdEnvironmentMongo, Environment.class);

        LOGGER.debug("Create environment [{}] - Done", environment.getName());

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

            EnvironmentMongo environmentMongoUpdated = internalEnvironmentRepo.save(environmentMongo);
            return mapper.map(environmentMongoUpdated, Environment.class);

        } catch (Exception e) {

            LOGGER.error("An error occured when updating environment", e);
            throw new TechnicalException("An error occured when updating environment");
        }
    }

    @Override
    public void delete(String environmentId) throws TechnicalException {
        try {
            internalEnvironmentRepo.deleteById(environmentId);
        } catch (Exception e) {
            LOGGER.error("An error occured when deleting environment [{}]", environmentId, e);
            throw new TechnicalException("An error occured when deleting environment");
        }
    }

    @Override
    public Set<Environment> findAll() throws TechnicalException {
        final List<EnvironmentMongo> environments = internalEnvironmentRepo.findAll();
        return environments.stream()
                .map(environmentMongo -> {
                    final Environment environment = new Environment();
                    environment.setId(environmentMongo.getId());
                    environment.setName(environmentMongo.getName());
                    return environment;
                })
                .collect(Collectors.toSet());
    }

}
