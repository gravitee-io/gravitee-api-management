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

import io.gravitee.apim.core.log.model.GuardianDecision;
import io.gravitee.apim.core.log.model.GuardianDecisionFilters;
import io.gravitee.apim.core.log.query_service.GuardianDecisionQueryService;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Honors the contract of the Elasticsearch implementation: scoped to the organization and environment of the
 * context, a {@code null} API set reads every API and an empty one none, newest first with the id breaking ties.
 */
public class GuardianDecisionQueryServiceInMemory
    extends AbstractQueryServiceInMemory<GuardianDecision>
    implements GuardianDecisionQueryService {

    private static final Comparator<GuardianDecision> NEWEST_FIRST = Comparator.comparing(
        GuardianDecision::timestamp,
        Comparator.nullsLast(Comparator.reverseOrder())
    ).thenComparing(GuardianDecision::id, Comparator.nullsLast(Comparator.naturalOrder()));

    @Override
    public SearchLogsResponse<GuardianDecision> search(
        ExecutionContext executionContext,
        GuardianDecisionFilters filters,
        Pageable pageable
    ) {
        var matches = storage
            .stream()
            .filter(decision -> inScope(executionContext, decision))
            .filter(decision -> filters.apiIds() == null || filters.apiIds().contains(decision.apiId()))
            .filter(decision -> matchesAny(filters.guardianIds(), decision, GuardianDecision::guardianId))
            .filter(decision -> matchesAny(filters.outcomes(), decision, GuardianDecision::outcome))
            .filter(decision -> matchesAny(filters.requestIds(), decision, GuardianDecision::requestId))
            .filter(decision -> requestIdContains(filters.requestIdContains(), decision))
            .filter(decision -> filters.from() == null || (decision.timestamp() != null && decision.timestamp() >= filters.from()))
            .filter(decision -> filters.to() == null || (decision.timestamp() != null && decision.timestamp() <= filters.to()))
            .sorted(NEWEST_FIRST)
            .toList();

        var page = matches
            .stream()
            .skip((long) (pageable.getPageNumber() - 1) * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .toList();
        return new SearchLogsResponse<>(matches.size(), page);
    }

    @Override
    public Optional<GuardianDecision> findById(ExecutionContext executionContext, String decisionId) {
        return storage
            .stream()
            .filter(decision -> inScope(executionContext, decision))
            .filter(decision -> Objects.equals(decision.id(), decisionId))
            .findFirst();
    }

    private static boolean inScope(ExecutionContext executionContext, GuardianDecision decision) {
        return (
            Objects.equals(decision.organizationId(), executionContext.getOrganizationId()) &&
            Objects.equals(decision.environmentId(), executionContext.getEnvironmentId())
        );
    }

    private static boolean matchesAny(Collection<String> values, GuardianDecision decision, Function<GuardianDecision, String> field) {
        return values == null || values.isEmpty() || values.contains(field.apply(decision));
    }

    private static boolean requestIdContains(String needle, GuardianDecision decision) {
        if (needle == null || needle.isBlank()) {
            return true;
        }
        return decision.requestId() != null && decision.requestId().toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }
}
