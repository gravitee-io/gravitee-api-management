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
 * What a search of Guardian decisions narrows on, within the environment searched. A {@code null} filter, or an empty
 * one, does not narrow the search — except {@link #apiIds()}.
 *
 * @param apiIds {@code null} reads every API of the environment; an empty set reads none, so that a caller passing the
 *     APIs it may read and finding none gets nothing rather than everything
 * @param guardianIds the Guardians that ran
 * @param outcomes {@code ALLOW}, {@code DENY}, {@code TRANSFORM}, {@code INDETERMINATE}
 * @param from the oldest decision, in epoch milliseconds, inclusive
 * @param to the newest decision, in epoch milliseconds, inclusive
 * @param requestIds the calls, by exact request id
 * @param requestIdContains a fragment of the request id, ignoring case
 * @author GraviteeSource Team
 */
@Builder(toBuilder = true)
public record GuardianDecisionFilters(
    Set<String> apiIds,
    Set<String> guardianIds,
    Set<String> outcomes,
    Long from,
    Long to,
    Set<String> requestIds,
    String requestIdContains
) {}
