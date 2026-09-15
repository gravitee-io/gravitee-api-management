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
package io.gravitee.apim.core.log.model;

import java.util.Set;
import lombok.Builder;

/**
 * What a decision search can narrow on. Grouped rather than passed as loose parameters so a new predicate
 * lands as a field here instead of widening the port signature again.
 *
 * <p>{@link #decisionPointType} is required and is the caller's, not the service's: every kind of point
 * writes to the same data stream, so a search that does not name one reads guardian verdicts and human
 * approvals as a single list. One service serves every family because the family is an argument.
 *
 * <p>{@link #apiIds} says something different depending on how it is set. {@code null} means "no api
 * restriction" — the org and the environment already come from the execution context. An <em>empty</em>
 * set means "the caller may see no api at all" and yields an empty page without reaching the index, so a
 * permission filter that came back empty cannot widen into a read of the whole environment.
 */
@Builder
public record DecisionLogFilters(
    String decisionPointType,
    Set<String> apiIds,
    Set<String> applicationIds,
    Set<String> planIds,
    Long from,
    Long to,
    Set<String> decisionPointIds,
    Set<String> checkpoints,
    Set<String> callers,
    Set<String> outcomes,
    Set<String> enforcements,
    Set<String> verdicts,
    Set<String> statuses,
    Set<String> subjectIds,
    Set<String> actorIds,
    Set<String> actions,
    Set<String> resourceIds,
    Set<String> caseIds,
    Set<String> requestIds,
    Set<String> traceIds,
    String reasonContains
) {}
