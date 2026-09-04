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
package io.gravitee.apim.infra.crud_service.performance_target;

import io.gravitee.apim.core.performance_target.crud_service.PerformanceTargetEvaluationCrudService;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.infra.adapter.PerformanceTargetEvaluationAdapter;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PerformanceTargetEvaluationRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@CustomLog
@Service
public class PerformanceTargetEvaluationCrudServiceImpl extends AbstractService implements PerformanceTargetEvaluationCrudService {

    private final PerformanceTargetEvaluationRepository evaluationRepository;

    public PerformanceTargetEvaluationCrudServiceImpl(@Lazy PerformanceTargetEvaluationRepository evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    @Override
    public PerformanceTargetEvaluation create(PerformanceTargetEvaluation evaluation) {
        try {
            var created = evaluationRepository.create(PerformanceTargetEvaluationAdapter.INSTANCE.toRepository(evaluation));
            return PerformanceTargetEvaluationAdapter.INSTANCE.toEntity(created);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when creating Performance Target Evaluation: " + evaluation.id(), e);
        }
    }

    @Override
    public Optional<PerformanceTargetEvaluation> createIfAbsent(PerformanceTargetEvaluation evaluation) {
        try {
            var created = evaluationRepository.create(PerformanceTargetEvaluationAdapter.INSTANCE.toRepository(evaluation));
            return Optional.of(PerformanceTargetEvaluationAdapter.INSTANCE.toEntity(created));
        } catch (DuplicateKeyException e) {
            return Optional.empty();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when creating Performance Target Evaluation: " + evaluation.id(), e);
        }
    }

    @Override
    public List<String> pruneHistory(String targetId, int retention) {
        try {
            return evaluationRepository.pruneHistory(targetId, retention);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when pruning Performance Target Evaluations of target: " + targetId, e);
        }
    }

    @Override
    public List<String> deleteByReference(String environmentId, String reference) {
        try {
            return evaluationRepository.deleteByReference(environmentId, reference);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "Error when deleting Performance Target Evaluations for reference [%s/%s]".formatted(environmentId, reference),
                e
            );
        }
    }

    @Override
    public void deleteByTargetId(String targetId) {
        try {
            evaluationRepository.deleteByTargetId(targetId);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when deleting Performance Target Evaluations of target: " + targetId, e);
        }
    }
}
