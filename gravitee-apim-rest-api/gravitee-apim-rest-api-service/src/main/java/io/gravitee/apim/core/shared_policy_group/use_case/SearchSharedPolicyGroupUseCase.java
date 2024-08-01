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
package io.gravitee.apim.core.shared_policy_group.use_case;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.query_service.SharedPolicyGroupQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import java.util.Set;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@DomainService
public class SearchSharedPolicyGroupUseCase {

    private final SharedPolicyGroupQueryService sharedPolicyGroupQueryService;

    private static final Set<String> allowedSortableFields = Set.of(
        "name",
        "-name",
        "apiType",
        "-apiType",
        "phase",
        "-phase",
        "updatedAt",
        "-updatedAt",
        "deployedAt",
        "-deployedAt"
    );

    public Output execute(Input input) {
        var pageable = input.pageable != null ? input.pageable : new PageableImpl(1, 10);

        Sortable sortable = toSortable(input.sortBy());

        return new Output(this.sharedPolicyGroupQueryService.searchByEnvironmentId(input.environmentId, input.q, pageable, sortable));
    }

    private static Sortable toSortable(String sortBy) {
        validateSortBy(sortBy);

        if (sortBy == null) {
            return new SortableImpl("updatedAt", false);
        }

        var isAscending = !sortBy.startsWith("-");
        var field = isAscending ? sortBy : sortBy.substring(1);
        return new SortableImpl(field, isAscending);
    }

    private static void validateSortBy(String sortBy) {
        if (sortBy != null && !allowedSortableFields.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort by field: " + sortBy);
        }
    }

    @Builder
    public record Input(String environmentId, String q, Pageable pageable, String sortBy) {}

    public record Output(Page<SharedPolicyGroup> result) {}
}
