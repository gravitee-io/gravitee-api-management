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
import io.gravitee.repository.management.api.IntegrationJobRepository;
import io.gravitee.repository.management.model.IntegrationJob;
import io.gravitee.repository.mongodb.management.internal.integrationjob.IntegrationJobMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MongoIntegrationJobRepository implements IntegrationJobRepository {

    @Autowired
    private IntegrationJobMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<IntegrationJob> findById(String s) throws TechnicalException {
        log.debug("Find integrationJob by id [{}]", s);
        Optional<IntegrationJob> result = internalRepository.findById(s).map(source -> mapper.map(source));
        log.debug("Find integrationJob by id [{}] - DONE", s);
        return result;
    }

    @Override
    public IntegrationJob create(IntegrationJob job) throws TechnicalException {
        log.debug("Create integrationJob [{}]", job.getId());
        var created = internalRepository.insert(mapper.map(job));
        log.debug("Create integrationJob [{}] - Done", created.getId());
        return mapper.map(created);
    }

    @Override
    public IntegrationJob update(IntegrationJob job) throws TechnicalException {
        if (job == null) {
            throw new IllegalStateException("IntegrationJob must not be null");
        }

        return internalRepository
            .findById(job.getId())
            .map(found -> {
                log.debug("Update integrationJob [{}]", job.getId());
                var updated = internalRepository.save(mapper.map(job));
                log.debug("Update integrationJob [{}] - Done", updated.getId());
                return mapper.map(updated);
            })
            .orElseThrow(() -> new IllegalStateException(String.format("No integration job found with id [%s]", job.getId())));
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete integration [{}]", id);
        internalRepository.deleteById(id);
        log.debug("Delete integration [{}] - Done", id);
    }

    @Override
    public Set<IntegrationJob> findAll() throws TechnicalException {
        return internalRepository.findAll().stream().map(source -> mapper.map(source)).collect(Collectors.toSet());
    }

    @Override
    public Optional<IntegrationJob> findPendingJobFor(String sourceId) {
        return internalRepository.findPendingJobFor(sourceId).map(source -> mapper.map(source));
    }
}
