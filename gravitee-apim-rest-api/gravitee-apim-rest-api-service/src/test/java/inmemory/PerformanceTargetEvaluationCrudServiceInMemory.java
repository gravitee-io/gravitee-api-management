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

import io.gravitee.apim.core.performance_target.crud_service.PerformanceTargetEvaluationCrudService;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PerformanceTargetEvaluationCrudServiceInMemory
    implements PerformanceTargetEvaluationCrudService, InMemoryAlternative<PerformanceTargetEvaluation> {

    final ArrayList<PerformanceTargetEvaluation> storage = new ArrayList<>();

    @Override
    public PerformanceTargetEvaluation create(PerformanceTargetEvaluation evaluation) {
        if (evaluation.latest()) {
            storage.replaceAll(stored ->
                stored.targetId().equals(evaluation.targetId()) && stored.latest() ? stored.toBuilder().latest(false).build() : stored
            );
        }
        storage.add(evaluation);
        return evaluation;
    }

    @Override
    public List<String> pruneHistory(String targetId, int retention) {
        var pruned = storage
            .stream()
            .filter(evaluation -> evaluation.targetId().equals(targetId))
            .sorted(Comparator.comparing(PerformanceTargetEvaluation::evaluatedAt).reversed())
            .skip(retention)
            .map(PerformanceTargetEvaluation::id)
            .toList();
        storage.removeIf(evaluation -> pruned.contains(evaluation.id()));
        return pruned;
    }

    @Override
    public List<String> deleteByReference(String environmentId, String reference) {
        var deleted = storage
            .stream()
            .filter(evaluation -> evaluation.environmentId().equals(environmentId) && evaluation.reference().equals(reference))
            .map(PerformanceTargetEvaluation::id)
            .toList();
        storage.removeIf(evaluation -> deleted.contains(evaluation.id()));
        return deleted;
    }

    @Override
    public void deleteByTargetId(String targetId) {
        storage.removeIf(evaluation -> evaluation.targetId().equals(targetId));
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
