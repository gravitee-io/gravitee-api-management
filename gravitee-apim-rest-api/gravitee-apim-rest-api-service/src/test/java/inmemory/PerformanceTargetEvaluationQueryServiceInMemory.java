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
package inmemory;

import io.gravitee.apim.core.performance_target.model.PerformanceTargetEnvironmentSummary;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.query_service.PerformanceTargetEvaluationQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PerformanceTargetEvaluationQueryServiceInMemory
    implements PerformanceTargetEvaluationQueryService, InMemoryAlternative<PerformanceTargetEvaluation> {

    private final List<PerformanceTargetEvaluation> storage;

    public PerformanceTargetEvaluationQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public PerformanceTargetEvaluationQueryServiceInMemory(PerformanceTargetEvaluationCrudServiceInMemory crudService) {
        storage = crudService.storage;
    }

    @Override
    public List<PerformanceTargetEvaluation> findLatestByReference(String environmentId, String reference) {
        return latestOf(environmentId)
            .filter(evaluation -> evaluation.reference().equals(reference))
            .toList();
    }

    @Override
    public List<PerformanceTargetEvaluation> findLatestByReferences(String environmentId, Collection<String> references) {
        return latestOf(environmentId)
            .filter(evaluation -> references.contains(evaluation.reference()))
            .toList();
    }

    @Override
    public Page<PerformanceTargetEvaluation> findEnvironmentLatest(String environmentId, Pageable pageable) {
        return page(
            latestOf(environmentId).sorted(Comparator.comparing(PerformanceTargetEvaluation::evaluatedAt).reversed()).toList(),
            pageable
        );
    }

    @Override
    public Page<PerformanceTargetEvaluation> findByTargetId(String targetId, Pageable pageable) {
        return page(
            storage
                .stream()
                .filter(evaluation -> evaluation.targetId().equals(targetId))
                .sorted(Comparator.comparing(PerformanceTargetEvaluation::evaluatedAt).reversed())
                .toList(),
            pageable
        );
    }

    @Override
    public PerformanceTargetEnvironmentSummary getEnvironmentSummary(String environmentId) {
        var latest = latestOf(environmentId).toList();
        return new PerformanceTargetEnvironmentSummary(
            environmentId,
            latest
                .stream()
                .filter(e -> e.status() == PerformanceTargetEvaluation.Status.PASS)
                .count(),
            latest
                .stream()
                .filter(e -> e.status() == PerformanceTargetEvaluation.Status.BREACH)
                .count(),
            latest
                .stream()
                .filter(e -> e.status() == PerformanceTargetEvaluation.Status.NOT_EVALUABLE)
                .count()
        );
    }

    private java.util.stream.Stream<PerformanceTargetEvaluation> latestOf(String environmentId) {
        return storage.stream().filter(evaluation -> evaluation.environmentId().equals(environmentId) && evaluation.latest());
    }

    private static Page<PerformanceTargetEvaluation> page(List<PerformanceTargetEvaluation> matches, Pageable pageable) {
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();
        var from = Math.min((pageNumber - 1) * pageSize, matches.size());
        var to = Math.min(pageNumber * pageSize, matches.size());
        return new Page<>(matches.subList(from, to), pageNumber, pageSize, matches.size());
    }

    @Override
    public void initWith(List<PerformanceTargetEvaluation> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PerformanceTargetEvaluation> storage() {
        return Collections.unmodifiableList(storage);
    }
}
