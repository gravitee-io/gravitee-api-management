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
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianSearchQuery;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.util.Set;
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

    public Output execute(Input input) {
        var sortBy = input.sortBy() == null ? DEFAULT_SORT : input.sortBy();
        if (!ALLOWED_SORTABLE_FIELDS.contains(sortBy)) {
            throw new ValidationDomainException(
                "Invalid sort by field: " + input.sortBy() + ". Expected one of " + ALLOWED_SORTABLE_FIELDS + "."
            );
        }

        var page = input.pageNumber() > 0 ? input.pageNumber() : DEFAULT_PAGE_NUMBER;
        var size = input.pageSize() > 0 ? Math.min(input.pageSize(), MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;

        var guardians = guardianRepository.search(new GuardianSearchQuery(input.environmentId(), input.q(), page, size, sortBy));
        return new Output(guardians, size);
    }

    @Builder
    public record Input(String environmentId, String q, int pageNumber, int pageSize, String sortBy) {}

    /**
     * @param perPage the page size actually served, which is the default when the caller asked for none
     *                or for a non-positive one. The caller's own value cannot stand in for it, and the
     *                number of rows returned cannot either on a partial page.
     */
    public record Output(Page<Guardian> guardians, int perPage) {}
}
