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

import static java.util.function.Predicate.not;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AsyncJobRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.AsyncJob;
import io.gravitee.repository.mongodb.management.internal.asyncjob.AsyncJobMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.AsyncJobMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MongoAsyncJobRepository implements AsyncJobRepository {

    @Autowired
    private AsyncJobMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<AsyncJob> findById(String s) throws TechnicalException {
        log.debug("Find asyncJob by id [{}]", s);
        Optional<AsyncJob> result = internalRepository
            .findById(s)
            .map(source -> mapper.map(source))
            .map(this::handleDeadLine);
        log.debug("Find asyncJob by id [{}] - DONE", s);
        return result;
    }

    @Override
    public AsyncJob create(AsyncJob job) throws TechnicalException {
        log.debug("Create asyncJob [{}]", job.getId());
        var created = internalRepository.insert(mapper.map(job));
        log.debug("Create asyncJob [{}] - Done", created.getId());
        return mapper.map(created);
    }

    @Override
    public AsyncJob update(AsyncJob job) throws TechnicalException {
        if (job == null) {
            throw new IllegalStateException("AsyncJob must not be null");
        }

        return internalRepository
            .findById(job.getId())
            .map(found -> {
                log.debug("Update asyncJob [{}]", job.getId());
                var updated = internalRepository.save(mapper.map(job));
                log.debug("Update asyncJob [{}] - Done", updated.getId());
                return mapper.map(updated);
            })
            .orElseThrow(() -> new IllegalStateException(String.format("No asyncJob found with id [%s]", job.getId())));
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete asyncJob [{}]", id);
        internalRepository.deleteById(id);
        log.debug("Delete asyncJob [{}] - Done", id);
    }

    @Override
    public Set<AsyncJob> findAll() throws TechnicalException {
        return internalRepository
            .findAll()
            .stream()
            .map(source -> mapper.map(source))
            .map(this::handleDeadLine)
            .collect(Collectors.toSet());
    }

    @Override
    public Optional<AsyncJob> findPendingJobFor(String sourceId) {
        return internalRepository
            .findPendingJobFor(sourceId)
            .map(source -> mapper.map(source))
            .map(this::handleDeadLine)
            .filter(not(AsyncJob::isTimedOut));
    }

    @Override
    public Page<AsyncJob> search(SearchCriteria criteria, Pageable pageable) throws TechnicalException {
        log.debug("Search by [{}]", criteria);
        var result = internalRepository.search(criteria, pageable);
        log.debug("Search by [{}] - Done", criteria);
        return result.map(source -> mapper.map(source)).map(this::handleDeadLine);
    }

    @Override
    public void delay(String id, Date newDeadLine) {
        log.debug("Delay asyncJob [{}]", id);
        internalRepository.delay(id, newDeadLine);
        log.debug("Delay asyncJob [{}] - Done", id);
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Delete integration jobs by environmentId: {}", environmentId);
        try {
            List<String> all = internalRepository.deleteByEnvironmentId(environmentId).stream().map(AsyncJobMongo::getId).toList();
            log.debug("Delete integration jobs by environment - Done {}", all);
            return all;
        } catch (Exception ex) {
            log.error("Failed to delete integration jobs by environmentId: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete integration jobs by environmentId");
        }
    }
}
