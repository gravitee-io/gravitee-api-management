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
import io.gravitee.repository.management.api.PerformanceTargetEvaluationRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.PerformanceTargetEnvironmentSummary;
import io.gravitee.repository.management.model.PerformanceTargetEvaluation;
import io.gravitee.repository.mongodb.management.internal.model.PerformanceTargetEvaluationMongo;
import io.gravitee.repository.mongodb.management.internal.performancetarget.PerformanceTargetEvaluationMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@RequiredArgsConstructor
class MongoPerformanceTargetEvaluationRepository implements PerformanceTargetEvaluationRepository {

    private final PerformanceTargetEvaluationMongoRepository internalRepository;
    private final GraviteeMapper mapper;

    @Override
    public PerformanceTargetEvaluation create(PerformanceTargetEvaluation evaluation) throws TechnicalException {
        log.debug("Create performance target evaluation [{}]", evaluation.getId());
        try {
            if (evaluation.isLatest()) {
                internalRepository.unsetLatest(evaluation.getTargetId());
            }
            var created = mapper.map(internalRepository.insert(mapper.map(evaluation)));
            log.debug("Create performance target evaluation [{}] - Done", created.getId());
            return created;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create performance target evaluation", ex);
        }
    }

    @Override
    public List<PerformanceTargetEvaluation> findLatestByReference(String environmentId, String reference) throws TechnicalException {
        log.debug("Find latest performance target evaluations by reference [{}/{}]", environmentId, reference);
        return internalRepository.findLatestByEnvironmentIdAndReference(environmentId, reference).stream().map(mapper::map).toList();
    }

    @Override
    public List<PerformanceTargetEvaluation> findLatestByReferences(String environmentId, Collection<String> references)
        throws TechnicalException {
        log.debug("Find latest performance target evaluations by references [{}/{}]", environmentId, references);
        if (references.isEmpty()) {
            return List.of();
        }
        return internalRepository.findLatestByEnvironmentIdAndReferenceIn(environmentId, references).stream().map(mapper::map).toList();
    }

    @Override
    public Page<PerformanceTargetEvaluation> findEnvironmentLatest(String environmentId, Pageable pageable) throws TechnicalException {
        log.debug("Find latest performance target evaluations of environment [{}]", environmentId);
        var page = internalRepository.findEnvironmentLatest(environmentId, pageable);
        return new Page<>(
            page.getContent().stream().map(mapper::map).toList(),
            page.getPageNumber(),
            (int) page.getPageElements(),
            page.getTotalElements()
        );
    }

    @Override
    public PerformanceTargetEnvironmentSummary getEnvironmentSummary(String environmentId) throws TechnicalException {
        log.debug("Summarize latest performance target evaluations of environment [{}]", environmentId);
        return internalRepository.getEnvironmentSummary(environmentId);
    }

    @Override
    public List<String> deleteByReference(String environmentId, String reference) throws TechnicalException {
        log.debug("Delete performance target evaluations by reference [{}/{}]", environmentId, reference);
        try {
            return internalRepository
                .deleteByEnvironmentIdAndReference(environmentId, reference)
                .stream()
                .map(PerformanceTargetEvaluationMongo::getId)
                .toList();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete performance target evaluations by reference", ex);
        }
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Delete performance target evaluations of environment [{}]", environmentId);
        try {
            return internalRepository.deleteByEnvironmentId(environmentId).stream().map(PerformanceTargetEvaluationMongo::getId).toList();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete performance target evaluations by environment", ex);
        }
    }

    @Override
    public void deleteByTargetId(String targetId) throws TechnicalException {
        log.debug("Delete performance target evaluations of target [{}]", targetId);
        try {
            internalRepository.deleteByTargetId(targetId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete performance target evaluations by target", ex);
        }
    }
}
