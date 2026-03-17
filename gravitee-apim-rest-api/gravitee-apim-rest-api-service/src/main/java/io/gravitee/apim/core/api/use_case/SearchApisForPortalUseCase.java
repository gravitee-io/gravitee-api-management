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
package io.gravitee.apim.core.api.use_case;

import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiPortalSearchQueryService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchApisForPortalUseCase {

    private final PortalNavigationApiVisibilityDomainService visibilityDomainService;
    private final ApiPortalSearchQueryService apiPortalSearchQueryService;

    public Output execute(Input input) {
        List<PortalNavigationApi> visible = input.userId() != null
            ? visibilityDomainService.resolveVisibleItems(input.environmentId(), input.userId())
            : visibilityDomainService.resolveVisibleItems(input.environmentId());
        Set<String> allowedIds = visible.stream().map(PortalNavigationApi::getApiId).collect(toSet());

        return new Output(
            apiPortalSearchQueryService.search(
                input.environmentId(),
                input.organizationId(),
                input.query(),
                allowedIds,
                input.pageable(),
                input.sortable()
            )
        );
    }

    public record Input(
        String environmentId,
        String organizationId,
        @Nullable String userId,
        @Nullable String query,
        Pageable pageable,
        Sortable sortable
    ) {}

    public record Output(Page<Api> apis) {}
}
