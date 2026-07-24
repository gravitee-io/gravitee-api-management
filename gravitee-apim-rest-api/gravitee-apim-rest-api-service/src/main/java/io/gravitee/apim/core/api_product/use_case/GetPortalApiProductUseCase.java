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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.PortalApiProductDetails;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiProductVisibilityDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPortalApiProductUseCase {

    private static final Comparator<String> NULL_SAFE_STRING_COMPARATOR = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
    private static final ApiFieldFilter API_FIELD_FILTER = ApiFieldFilter.builder().definitionExcluded(true).pictureExcluded(true).build();

    private final ApiProductQueryService apiProductQueryService;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationApiProductVisibilityDomainService apiProductVisibilityDomainService;
    private final PortalNavigationApiVisibilityDomainService apiVisibilityDomainService;
    private final ApiQueryService apiQueryService;

    public Output execute(Input input) {
        var apiProduct = apiProductQueryService
            .findById(input.apiProductId())
            .filter(product -> input.environmentId().equals(product.getEnvironmentId()))
            .orElseThrow(() -> new ApiProductNotFoundException(input.apiProductId()));

        var navigationItem = findAccessibleNavigationItem(input);
        var apis = findAccessibleApis(apiProduct, input);
        var tags = apiProduct.getTags() == null
            ? List.<String>of()
            : apiProduct.getTags().stream().sorted(NULL_SAFE_STRING_COMPARATOR).toList();

        return new Output(
            new PortalApiProductDetails(
                apiProduct.getId(),
                apiProduct.getName(),
                apiProduct.getDescription(),
                apiProduct.getVersion(),
                apiProduct.getKind(),
                navigationItem.getId().json(),
                tags,
                apis
            )
        );
    }

    private PortalNavigationApiProduct findAccessibleNavigationItem(Input input) {
        Set<String> accessibleApiProductIds = apiProductVisibilityDomainService.resolveAccessibleApiProductIds(
            input.environmentId(),
            input.viewerContext()
        );

        return portalNavigationItemsQueryService
            .search(
                PortalNavigationItemQueryCriteria.builder()
                    .environmentId(input.environmentId())
                    .published(true)
                    .type(PortalNavigationItemType.API_PRODUCT)
                    .apiProductIds(Set.of(input.apiProductId()))
                    .build()
            )
            .stream()
            .filter(PortalNavigationApiProduct.class::isInstance)
            .map(PortalNavigationApiProduct.class::cast)
            .filter(item -> !input.viewerContext().shouldNotShow(item))
            .filter(item -> !apiProductVisibilityDomainService.isApiProductItemHidden(item, input.viewerContext(), accessibleApiProductIds))
            .filter(item ->
                !apiProductVisibilityDomainService.hasHiddenApiProductAncestor(
                    input.environmentId(),
                    item,
                    input.viewerContext(),
                    accessibleApiProductIds
                )
            )
            .filter(item -> !apiVisibilityDomainService.hasHiddenApiAncestor(input.environmentId(), item, input.viewerContext()))
            .findFirst()
            .orElseThrow(() -> new ApiProductNotFoundException(input.apiProductId()));
    }

    private List<PortalApiProductDetails.ApiSummary> findAccessibleApis(ApiProduct apiProduct, Input input) {
        if (apiProduct.getApiIds() == null || apiProduct.getApiIds().isEmpty()) {
            return List.of();
        }

        Set<String> visibleApiIds = input
            .viewerContext()
            .userId()
            .map(userId -> apiVisibilityDomainService.resolveVisibleItems(input.environmentId(), userId))
            .orElseGet(() -> apiVisibilityDomainService.resolveVisibleItems(input.environmentId()))
            .stream()
            .map(PortalNavigationApi::getApiId)
            .filter(apiProduct.getApiIds()::contains)
            .collect(Collectors.toSet());

        if (visibleApiIds.isEmpty()) {
            return List.of();
        }

        return apiQueryService
            .search(
                ApiSearchCriteria.builder().environmentId(input.environmentId()).ids(List.copyOf(visibleApiIds)).build(),
                null,
                API_FIELD_FILTER
            )
            .sorted(
                Comparator.comparing(Api::getName, NULL_SAFE_STRING_COMPARATOR)
                    .thenComparing(Api::getVersion, NULL_SAFE_STRING_COMPARATOR)
                    .thenComparing(Api::getId, NULL_SAFE_STRING_COMPARATOR)
            )
            .map(api -> new PortalApiProductDetails.ApiSummary(api.getId(), api.getName(), api.getVersion()))
            .toList();
    }

    public record Input(String environmentId, String apiProductId, PortalNavigationItemViewerContext viewerContext) {}

    public record Output(PortalApiProductDetails apiProduct) {}
}
