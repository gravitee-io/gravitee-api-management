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
package io.gravitee.apim.core.performance_target.crud_service;

import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import java.util.List;

public interface PerformanceTargetEvaluationCrudService {
    /**
     * Stores an evaluation; when it is flagged latest it replaces the target's previous latest evaluation.
     */
    PerformanceTargetEvaluation create(PerformanceTargetEvaluation evaluation);

    /**
     * Deletes every evaluation of the target but its {@code retention} most recent ones.
     *
     * @return the ids of the deleted evaluations
     */
    List<String> pruneHistory(String targetId, int retention);

    /**
     * @return the ids of the deleted evaluations
     */
    List<String> deleteByReference(String environmentId, String reference);

    void deleteByTargetId(String targetId);
}
