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
package io.gravitee.gamma.rest.core.guardian.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.model.GuardianSearchQuery;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Lists the Guardians of an environment, optionally filtered by name and ordered.
 *
 * <p>The sort field is checked against a closed set rather than passed through: it reaches the
 * repository's {@code ORDER BY}, so anything the console has not been told about is refused here.
 * A rejected field answers 400 through {@link ValidationDomainException}, not 500 — the platform's
 * {@code IllegalArgumentException} had no mapper and surfaced as an internal error.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
public class SearchGuardianUseCase {

    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * Caps what a caller can ask for in one page, as {@code ListObservabilityDashboardUseCase} does
     * in this same module. A Guardian row carries its serialized definition, so an uncapped
     * {@code perPage} would read far more than the list ever renders.
     */
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT = "-updatedAt";

    /**
     * Mirrors {@code GuardianCriteria.SORTABLE_FIELDS}, with the two directions each field accepts.
     * Stated again rather than imported: the core may not depend on {@code io.gravitee.repository..}
     * (AGENTS.md §5). The API types are not sortable — the definition carries them as a set, which has
     * no single value to order rows by.
     */
    private static final Set<String> ALLOWED_SORTABLE_FIELDS = Set.of(
        "name",
        "-name",
        "phase",
        "-phase",
        "createdAt",
        "-createdAt",
        "updatedAt",
        "-updatedAt"
    );

    private final GuardianRepository guardianRepository;
    private final GuardianDeploymentDomainService deployments;

    public Output execute(Input input) {
        var sortBy = input.sortBy() == null ? DEFAULT_SORT : input.sortBy();
        if (!ALLOWED_SORTABLE_FIELDS.contains(sortBy)) {
            throw new ValidationDomainException(
                "Invalid sort by field: " + input.sortBy() + ". Expected one of " + ALLOWED_SORTABLE_FIELDS + "."
            );
        }

        var page = input.pageNumber() > 0 ? input.pageNumber() : DEFAULT_PAGE_NUMBER;
        var size = input.pageSize() > 0 ? Math.min(input.pageSize(), MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;

        if (input.deployedFor() != null) {
            return offered(input, apiTypeOf(input.deployedFor()), page, size, sortBy);
        }
        var guardians = guardianRepository.search(new GuardianSearchQuery(input.environmentId(), input.q(), page, size, sortBy));
        var content = deployments.states(input.environmentId(), guardians.getContent());
        return new Output(new Page<>(content, guardians.getPageNumber(), content.size(), guardians.getTotalElements()), size);
    }

    private static ApiType apiTypeOf(String label) {
        var apiType = ApiType.fromLabel(label);
        if (apiType == null) {
            throw new ValidationDomainException("Unknown API type: " + label + ".");
        }
        return apiType;
    }

    /**
     * The Guardians the gateways run whose deployed revision supports {@code apiType}. Filtered before the page is
     * cut, so every Guardian of the environment matching the name is read: the deployed state lives in the deployment
     * history and the API types inside the deployed revision, neither of which the store can filter on.
     */
    private Output offered(Input input, ApiType apiType, int page, int size, String sortBy) {
        var matching = allMatching(input.environmentId(), input.q(), sortBy);
        var current = deployments.current(input.environmentId(), matching.stream().map(Guardian::getId).collect(Collectors.toSet()));
        var offered = matching
            .stream()
            .filter(
                guardian ->
                    current.get(guardian.getId()) instanceof GuardianDeployment.Published(var deployed, _, _) &&
                    deployed.isCompatibleWith(apiType)
            )
            .toList();
        var from = Math.min((page - 1) * size, offered.size());
        var content = deployments.states(input.environmentId(), offered.subList(from, Math.min(from + size, offered.size())));
        return new Output(new Page<>(content, page, content.size(), offered.size()), size);
    }

    private List<Guardian> allMatching(String environmentId, String q, String sortBy) {
        var all = new ArrayList<Guardian>();
        for (int page = 1; ; page++) {
            var batch = guardianRepository.search(new GuardianSearchQuery(environmentId, q, page, MAX_PAGE_SIZE, sortBy));
            all.addAll(batch.getContent());
            if (batch.getContent().size() < MAX_PAGE_SIZE || all.size() >= batch.getTotalElements()) {
                return all;
            }
        }
    }

    @Builder
    /**
     * @param deployedFor the label of an API type; when set, only the Guardians the gateways run whose deployed revision
     *     supports that type are listed.
     */
    public record Input(String environmentId, String q, int pageNumber, int pageSize, String sortBy, String deployedFor) {}

    /**
     * @param perPage the page size actually served, which is the default when the caller asked for none
     *                or for a non-positive one. The caller's own value cannot stand in for it, and the
     *                number of rows returned cannot either on a partial page.
     */
    public record Output(Page<Guardian> guardians, int perPage) {}
}
