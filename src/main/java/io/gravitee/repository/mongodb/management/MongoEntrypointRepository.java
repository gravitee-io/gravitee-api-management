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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EntrypointRepository;
import io.gravitee.repository.management.model.Entrypoint;
import io.gravitee.repository.mongodb.management.internal.api.EntrypointMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.EntrypointMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoEntrypointRepository implements EntrypointRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoEntrypointRepository.class);

    @Autowired
    private EntrypointMongoRepository internalEntryPointRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Entrypoint> findById(String entrypointId) throws TechnicalException {
        LOGGER.debug("Find entry point by ID [{}]", entrypointId);

        final EntrypointMongo entrypoint = internalEntryPointRepo.findById(entrypointId).orElse(null);

        LOGGER.debug("Find entry point by ID [{}] - Done", entrypointId);
        return Optional.ofNullable(mapper.map(entrypoint, Entrypoint.class));
    }

    @Override
    public Entrypoint create(Entrypoint entrypoint) throws TechnicalException {
        LOGGER.debug("Create entry point [{}]", entrypoint.getValue());

        EntrypointMongo entrypointMongo = mapper.map(entrypoint, EntrypointMongo.class);
        EntrypointMongo createdEntryPointMongo = internalEntryPointRepo.insert(entrypointMongo);

        Entrypoint res = mapper.map(createdEntryPointMongo, Entrypoint.class);

        LOGGER.debug("Create entry point [{}] - Done", entrypoint.getValue());

        return res;
    }

    @Override
    public Entrypoint update(Entrypoint entrypoint) throws TechnicalException {
        if (entrypoint == null || entrypoint.getValue() == null) {
            throw new IllegalStateException("Entry point to update must have a value");
        }

        final EntrypointMongo entrypointMongo = internalEntryPointRepo.findById(entrypoint.getId()).orElse(null);

        if (entrypointMongo == null) {
            throw new IllegalStateException(String.format("No entry point found with name [%s]", entrypoint.getId()));
        }

        try {
            //Update
            entrypointMongo.setValue(entrypoint.getValue());
            entrypointMongo.setEnvironment(entrypoint.getEnvironment());
            entrypointMongo.setTags(entrypoint.getTags());

            EntrypointMongo entrypointMongoUpdated = internalEntryPointRepo.save(entrypointMongo);
            return mapper.map(entrypointMongoUpdated, Entrypoint.class);

        } catch (Exception e) {

            LOGGER.error("An error occurred when updating entry point", e);
            throw new TechnicalException("An error occurred when updating entry point");
        }
    }

    @Override
    public void delete(String entrypointId) throws TechnicalException {
        try {
            internalEntryPointRepo.deleteById(entrypointId);
        } catch (Exception e) {
            LOGGER.error("An error occurred when deleting entry point [{}]", entrypointId, e);
            throw new TechnicalException("An error occurred when deleting entry point");
        }
    }

    @Override
    public Set<Entrypoint> findAll() {
        final List<EntrypointMongo> entrypoints = internalEntryPointRepo.findAll();
        return entrypoints.stream()
                .map(entrypointMongo -> {
                    final Entrypoint entrypoint = new Entrypoint();
                    entrypoint.setId(entrypointMongo.getId());
                    entrypoint.setEnvironment(entrypointMongo.getEnvironment());
                    entrypoint.setValue(entrypointMongo.getValue());
                    entrypoint.setTags(entrypointMongo.getTags());
                    return entrypoint;
                })
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Entrypoint> findAllByEnvironment(String environment) throws TechnicalException {
        final List<EntrypointMongo> entrypoints = internalEntryPointRepo.findByEnvironment(environment);
        return entrypoints.stream()
                .map(entrypointMongo -> {
                    final Entrypoint entrypoint = new Entrypoint();
                    entrypoint.setId(entrypointMongo.getId());
                    entrypoint.setEnvironment(entrypointMongo.getEnvironment());
                    entrypoint.setValue(entrypointMongo.getValue());
                    entrypoint.setTags(entrypointMongo.getTags());
                    return entrypoint;
                })
                .collect(Collectors.toSet());
    }
}
