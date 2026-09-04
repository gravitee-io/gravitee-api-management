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
package io.gravitee.apim.infra.query_service.performance_target;

import io.gravitee.apim.core.performance_target.model.PerformanceTargetEnvironmentSummary;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.query_service.PerformanceTargetEvaluationQueryService;
import io.gravitee.apim.infra.adapter.PerformanceTargetEvaluationAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PerformanceTargetEvaluationRepository;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Collection;
import java.util.List;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@CustomLog
public class PerformanceTargetEvaluationQueryServiceImpl extends AbstractService implements PerformanceTargetEvaluationQueryService {

    private final PerformanceTargetEvaluationRepository evaluationRepository;

    public PerformanceTargetEvaluationQueryServiceImpl(@Lazy PerformanceTargetEvaluationRepository evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    @Override
    public List<PerformanceTargetEvaluation> findLatestByReference(String environmentId, String reference) {
        try {
            return evaluationRepository
                .findLatestByReference(environmentId, reference)
                .stream()
                .map(PerformanceTargetEvaluationAdapter.INSTANCE::toEntity)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurred while finding latest Performance Target Evaluations of [%s/%s]".formatted(environmentId, reference),
                e
            );
        }
    }

    @Override
    public List<PerformanceTargetEvaluation> findLatestByReferences(String environmentId, Collection<String> references) {
        try {
            return evaluationRepository
                .findLatestByReferences(environmentId, references)
                .stream()
                .map(PerformanceTargetEvaluationAdapter.INSTANCE::toEntity)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurred while finding latest Performance Target Evaluations in environment " + environmentId,
                e
            );
        }
    }

    @Override
    public Page<PerformanceTargetEvaluation> findEnvironmentLatest(String environmentId, Pageable pageable) {
        try {
            return evaluationRepository
                .findEnvironmentLatest(environmentId, convert(pageable))
                .map(PerformanceTargetEvaluationAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurred while finding latest Performance Target Evaluations of environment " + environmentId,
                e
            );
        }
    }

    @Override
    public PerformanceTargetEnvironmentSummary getEnvironmentSummary(String environmentId) {
        try {
            return PerformanceTargetEvaluationAdapter.INSTANCE.toEntity(evaluationRepository.getEnvironmentSummary(environmentId));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurred while summarizing Performance Target Evaluations of environment " + environmentId,
                e
            );
        }
    }
}
