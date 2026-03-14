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
package io.gravitee.apim.core.api_product.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductSearchQueryService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchApiProductsUseCase {

    private final ApiProductSearchQueryService apiProductSearchQueryService;
    private final MembershipQueryService membershipQueryService;

    public Output execute(Input input) {
        if (input.isAdmin()) {
            return search(input, input.ids());
        }
        Optional<Set<String>> resolved = findAccessibleApiProductIds(input);
        if (resolved.isEmpty()) {
            return emptyPage(input);
        }
        return search(input, resolved.get());
    }

    private Optional<Set<String>> findAccessibleApiProductIds(Input input) {
        Set<String> allowedIds = membershipQueryService
            .findByMemberIdAndMemberTypeAndReferenceType(input.userId(), Membership.Type.USER, Membership.ReferenceType.API_PRODUCT)
            .stream()
            .map(Membership::getReferenceId)
            .collect(Collectors.toSet());
        if (allowedIds.isEmpty()) {
            return Optional.empty();
        }
        if (input.ids() != null && !input.ids().isEmpty()) {
            Set<String> filtered = input.ids().stream().filter(allowedIds::contains).collect(Collectors.toSet());
            return filtered.isEmpty() ? Optional.empty() : Optional.of(filtered);
        }
        return Optional.of(allowedIds);
    }

    private Output emptyPage(Input input) {
        return new Output(new Page<>(List.of(), input.pageable() != null ? input.pageable().getPageNumber() : 1, 0, 0));
    }

    private Output search(Input input, Set<String> ids) {
        return new Output(
            apiProductSearchQueryService.search(
                input.environmentId(),
                input.organizationId(),
                input.query(),
                ids,
                input.pageable(),
                input.sortable()
            )
        );
    }

    public record Input(
        String environmentId,
        String organizationId,
        String query,
        Set<String> ids,
        Pageable pageable,
        Sortable sortable,
        String userId,
        boolean isAdmin
    ) {
        public static Input of(
            String environmentId,
            String organizationId,
            String query,
            Set<String> ids,
            Pageable pageable,
            Sortable sortable,
            String userId,
            boolean isAdmin
        ) {
            return new Input(environmentId, organizationId, query != null ? query.trim() : null, ids, pageable, sortable, userId, isAdmin);
        }
    }

    public record Output(Page<ApiProduct> page) {}
}
