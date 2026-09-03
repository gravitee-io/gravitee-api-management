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
import io.gravitee.repository.management.api.PerformanceTargetRepository;
import io.gravitee.repository.management.model.PerformanceTarget;
import io.gravitee.repository.mongodb.management.internal.model.PerformanceTargetMongo;
import io.gravitee.repository.mongodb.management.internal.performancetarget.PerformanceTargetMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@RequiredArgsConstructor
class MongoPerformanceTargetRepository implements PerformanceTargetRepository {

    private final PerformanceTargetMongoRepository internalRepository;
    private final GraviteeMapper mapper;

    @Override
    public PerformanceTarget create(PerformanceTarget target) throws TechnicalException {
        log.debug("Create performance target [{}]", target.getId());
        var created = mapper.map(internalRepository.insert(mapper.map(target)));
        log.debug("Create performance target [{}] - Done", created.getId());
        return created;
    }

    @Override
    public Optional<PerformanceTarget> findById(String id) throws TechnicalException {
        log.debug("Find performance target by id [{}]", id);
        var result = internalRepository.findById(id).map(mapper::map);
        log.debug("Find performance target by id [{}] - Done", id);
        return result;
    }

    @Override
    public PerformanceTarget update(PerformanceTarget target) throws TechnicalException {
        if (target == null) {
            throw new IllegalStateException("Performance target must not be null");
        }
        return internalRepository
            .findById(target.getId())
            .map(found -> {
                log.debug("Update performance target [{}]", target.getId());
                var updated = mapper.map(internalRepository.save(mapper.map(target)));
                log.debug("Update performance target [{}] - Done", updated.getId());
                return updated;
            })
            .orElseThrow(() -> new IllegalStateException(String.format("No performance target found with id [%s]", target.getId())));
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete performance target [{}]", id);
        internalRepository.deleteById(id);
        log.debug("Delete performance target [{}] - Done", id);
    }

    @Override
    public List<PerformanceTarget> findByReference(String environmentId, String reference) throws TechnicalException {
        log.debug("Find performance targets by reference [{}/{}]", environmentId, reference);
        var result = internalRepository.findByEnvironmentIdAndReference(environmentId, reference).stream().map(mapper::map).toList();
        log.debug("Find performance targets by reference [{}/{}] - Done", environmentId, reference);
        return result;
    }

    @Override
    public List<String> deleteByReference(String environmentId, String reference) throws TechnicalException {
        log.debug("Delete performance targets by reference [{}/{}]", environmentId, reference);
        try {
            var deleted = internalRepository
                .deleteByEnvironmentIdAndReference(environmentId, reference)
                .stream()
                .map(PerformanceTargetMongo::getId)
                .toList();
            log.debug("Delete performance targets by reference [{}/{}] - Done", environmentId, reference);
            return deleted;
        } catch (Exception ex) {
            log.error("Failed to delete performance targets by reference [{}/{}]", environmentId, reference, ex);
            throw new TechnicalException("Failed to delete performance targets by reference", ex);
        }
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Delete performance targets of environment [{}]", environmentId);
        try {
            var deleted = internalRepository.deleteByEnvironmentId(environmentId).stream().map(PerformanceTargetMongo::getId).toList();
            log.debug("Delete performance targets of environment [{}] - Done", environmentId);
            return deleted;
        } catch (Exception ex) {
            log.error("Failed to delete performance targets of environment [{}]", environmentId, ex);
            throw new TechnicalException("Failed to delete performance targets by environment", ex);
        }
    }

    @Override
    public List<String> removeApiId(String apiId) throws TechnicalException {
        log.debug("Remove api [{}] from performance targets", apiId);
        try {
            var touched = internalRepository.removeApiId(apiId);
            log.debug("Remove api [{}] from performance targets - Done", apiId);
            return touched;
        } catch (Exception ex) {
            log.error("Failed to remove api [{}] from performance targets", apiId, ex);
            throw new TechnicalException("Failed to remove api from performance targets", ex);
        }
    }
}
