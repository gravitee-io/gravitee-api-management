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
package io.gravitee.repository.management.api;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.PerformanceTargetEnvironmentSummary;
import io.gravitee.repository.management.model.PerformanceTargetEvaluation;
import java.util.Collection;
import java.util.List;

public interface PerformanceTargetEvaluationRepository {
    /**
     * Stores an evaluation. When it is flagged latest, the previous latest evaluation of the same target loses the flag,
     * so a target has at most one latest evaluation.
     *
     * @throws DuplicateKeyException when an evaluation with the same id exists; the stored one and its flag are untouched
     */
    PerformanceTargetEvaluation create(PerformanceTargetEvaluation evaluation) throws TechnicalException;

    /**
     * @return the latest evaluation of every target of the reference
     */
    List<PerformanceTargetEvaluation> findLatestByReference(String environmentId, String reference) throws TechnicalException;

    List<PerformanceTargetEvaluation> findLatestByReferences(String environmentId, Collection<String> references) throws TechnicalException;

    /**
     * @return the latest evaluations of the environment, most recently evaluated first
     */
    Page<PerformanceTargetEvaluation> findEnvironmentLatest(String environmentId, Pageable pageable) throws TechnicalException;

    /**
     * @return the stored evaluations of the target, most recently evaluated first
     */
    Page<PerformanceTargetEvaluation> findByTargetId(String targetId, Pageable pageable) throws TechnicalException;

    PerformanceTargetEnvironmentSummary getEnvironmentSummary(String environmentId) throws TechnicalException;

    /**
     * Deletes every evaluation of the target but its {@code retention} most recent ones.
     *
     * @return the ids of the deleted evaluations
     */
    List<String> pruneHistory(String targetId, int retention) throws TechnicalException;

    /**
     * @return the ids of the deleted evaluations
     */
    List<String> deleteByReference(String environmentId, String reference) throws TechnicalException;

    void deleteByTargetId(String targetId) throws TechnicalException;

    /**
     * @return the ids of the deleted evaluations
     */
    List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
