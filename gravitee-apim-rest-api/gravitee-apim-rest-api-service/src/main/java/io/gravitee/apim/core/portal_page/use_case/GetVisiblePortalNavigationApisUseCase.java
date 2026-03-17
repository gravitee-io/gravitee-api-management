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
package io.gravitee.apim.core.portal_page.use_case;

import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiPortalSearchQueryService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationSearchInclude;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetVisiblePortalNavigationApisUseCase {

    private final PortalNavigationApiVisibilityDomainService visibilityDomainService;
    private final ApiPortalSearchQueryService apiPortalSearchQueryService;

    public Output execute(Input input) {
        List<PortalNavigationApi> visible = input.userId().isPresent()
            ? visibilityDomainService.resolveVisibleItems(input.environmentId(), input.userId().get())
            : visibilityDomainService.resolveVisibleItems(input.environmentId());

        List<Api> searchedApis = List.of();
        Optional<String> queryText = input.query().filter(q -> !q.isBlank());

        List<PortalNavigationApi> filtered;
        if (queryText.isEmpty()) {
            filtered = visible;
        } else {
            Set<String> allowedApiIds = visible.stream().map(PortalNavigationApi::getApiId).collect(toSet());
            searchedApis = apiPortalSearchQueryService.search(
                input.environmentId(),
                input.organizationId(),
                queryText.get(),
                allowedApiIds
            );
            Set<String> matchingIds = searchedApis.stream().map(Api::getId).collect(toSet());
            filtered = visible
                .stream()
                .filter(i -> matchingIds.contains(i.getApiId()))
                .toList();
        }

        int skip = (input.pageable().getPageNumber() - 1) * input.pageable().getPageSize();
        List<PortalNavigationApi> pageItems = filtered.stream().skip(skip).limit(input.pageable().getPageSize()).toList();
        Page<PortalNavigationApi> page = new Page<>(pageItems, input.pageable().getPageNumber(), pageItems.size(), filtered.size());

        List<Api> includedApis = resolveIncludedApis(input, searchedApis, pageItems);

        return new Output(page, includedApis);
    }

    private List<Api> resolveIncludedApis(Input input, List<Api> searchedApis, List<PortalNavigationApi> pageItems) {
        if (!input.includes().contains(PortalNavigationSearchInclude.API)) {
            return List.of();
        }
        Set<String> pageApiIds = pageItems.stream().map(PortalNavigationApi::getApiId).collect(toSet());
        if (pageApiIds.isEmpty()) {
            return List.of();
        }
        if (!searchedApis.isEmpty()) {
            return searchedApis
                .stream()
                .filter(a -> pageApiIds.contains(a.getId()))
                .toList();
        }
        return apiPortalSearchQueryService.search(input.environmentId(), input.organizationId(), pageApiIds);
    }

    public record Input(
        String environmentId,
        String organizationId,
        Optional<String> userId,
        Pageable pageable,
        Optional<String> query,
        Set<PortalNavigationSearchInclude> includes
    ) {}

    public record Output(Page<PortalNavigationApi> apis, List<Api> includedApis) {}
}
