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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiPortalSearchQueryService;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_page.domain_service.CheckTypoToleranceDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalCatalogNavigationVisibilityDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiProductVisibilityDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalCatalogApiProductSummary;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalNavigationSearchInclude;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetVisiblePortalCatalogItemsUseCase {

    private static final int MIN_FUZZY_TOKEN_LENGTH = 4;
    private static final int MAX_FREE_TEXT_CHARS_FOR_FUZZY = 512;
    private static final String WORD_SEPARATOR_PATTERN = "[^\\p{L}\\p{N}]+";
    private static final Comparator<String> NULL_SAFE_STRING_COMPARATOR = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
    private static final Comparator<CatalogEntry> CATALOG_ENTRY_COMPARATOR = Comparator.comparing(
        CatalogEntry::name,
        NULL_SAFE_STRING_COMPARATOR
    )
        .thenComparing(entry -> entry.item().getType().name())
        .thenComparing(entry -> entry.item().getId().json());
    private static final Comparator<Api> API_COMPARATOR = Comparator.comparing(Api::getName, NULL_SAFE_STRING_COMPARATOR)
        .thenComparing(Api::getVersion, NULL_SAFE_STRING_COMPARATOR)
        .thenComparing(Api::getId, NULL_SAFE_STRING_COMPARATOR);
    private static final ApiFieldFilter API_FIELD_FILTER = ApiFieldFilter.builder().definitionExcluded(true).pictureExcluded(true).build();

    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationApiVisibilityDomainService apiVisibilityDomainService;
    private final PortalNavigationApiProductVisibilityDomainService apiProductVisibilityDomainService;
    private final PortalCatalogNavigationVisibilityDomainService catalogNavigationVisibilityDomainService;
    private final ApiPortalSearchQueryService apiPortalSearchQueryService;
    private final ApiQueryService apiQueryService;
    private final ApiProductQueryService apiProductQueryService;
    private final CheckTypoToleranceDomainService checkTypoToleranceDomainService;

    public Output execute(Input input) {
        String categoryId = input.categoryId().map(PortalCategoryId::toString).orElse(null);
        List<PortalNavigationItem> navigationItems = portalNavigationItemsQueryService.search(
            PortalNavigationItemQueryCriteria.builder().environmentId(input.environmentId()).build()
        );
        Map<PortalNavigationItemId, PortalNavigationItem> navigationItemsById = navigationItems
            .stream()
            .collect(Collectors.toMap(PortalNavigationItem::getId, Function.identity(), (first, ignored) -> first));
        List<PortalNavigationApi> accessibleApis = input
            .viewerContext()
            .userId()
            .map(userId -> apiVisibilityDomainService.resolveVisibleItems(input.environmentId(), userId, categoryId))
            .orElseGet(() -> apiVisibilityDomainService.resolveVisiblePublicItems(input.environmentId(), categoryId));
        Set<PortalNavigationItemId> accessibleApiNavigationItemIds = accessibleApis
            .stream()
            .map(PortalNavigationItem::getId)
            .collect(Collectors.toSet());
        Set<String> accessibleApiProductIds = apiProductVisibilityDomainService.resolveAccessibleApiProductIds(
            input.environmentId(),
            input.viewerContext()
        );

        List<PortalNavigationApi> visibleApis = catalogNavigationVisibilityDomainService.filterVisibleItems(
            accessibleApis,
            navigationItemsById,
            input.viewerContext(),
            accessibleApiNavigationItemIds,
            accessibleApiProductIds
        );
        List<PortalNavigationApi> catalogApiCandidates = catalogNavigationVisibilityDomainService.filterStandaloneApis(
            visibleApis,
            navigationItemsById
        );
        // API products carry no category association, so a category-filtered catalog search never matches any of them.
        List<PortalNavigationApiProduct> visibleApiProducts = input.categoryId().isPresent()
            ? List.of()
            : findVisibleApiProducts(navigationItems, input, navigationItemsById, accessibleApiNavigationItemIds, accessibleApiProductIds);

        Optional<String> query = input
            .query()
            .filter(value -> !value.isBlank())
            .map(String::trim);
        boolean typoToleranceEnabled =
            query.isPresent() &&
            (!catalogApiCandidates.isEmpty() || !visibleApiProducts.isEmpty()) &&
            checkTypoToleranceDomainService.isEnabled(input.environmentId(), input.organizationId());
        Map<String, Api> matchingApisById = findMatchingApis(input, catalogApiCandidates, query, typoToleranceEnabled)
            .stream()
            .collect(Collectors.toMap(Api::getId, Function.identity(), (first, ignored) -> first));
        Map<String, ApiProduct> visibleApiProductsById = loadApiProducts(input.environmentId(), visibleApiProducts);

        List<CatalogEntry> entries = createEntries(
            catalogApiCandidates,
            visibleApiProducts,
            matchingApisById,
            visibleApiProductsById,
            query,
            typoToleranceEnabled
        )
            .stream()
            .sorted(CATALOG_ENTRY_COMPARATOR)
            .toList();
        List<CatalogEntry> pageEntries = paginate(entries, input.pageable());
        List<PortalNavigationItem> pageItems = pageEntries.stream().map(CatalogEntry::item).toList();
        Page<PortalNavigationItem> page = new Page<>(pageItems, input.pageable().getPageNumber(), pageItems.size(), entries.size());

        List<Api> includedApis = resolveIncludedApis(input, pageEntries, matchingApisById);
        List<PortalCatalogApiProductSummary> includedApiProducts = resolveIncludedApiProducts(
            input,
            pageEntries,
            visibleApiProductsById,
            visibleApis
        );

        return new Output(page, includedApis, includedApiProducts);
    }

    private List<PortalNavigationApiProduct> findVisibleApiProducts(
        List<PortalNavigationItem> navigationItems,
        Input input,
        Map<PortalNavigationItemId, PortalNavigationItem> navigationItemsById,
        Set<PortalNavigationItemId> accessibleApiNavigationItemIds,
        Set<String> accessibleApiProductIds
    ) {
        List<PortalNavigationApiProduct> apiProductItems = navigationItems
            .stream()
            .filter(PortalNavigationApiProduct.class::isInstance)
            .map(PortalNavigationApiProduct.class::cast)
            .filter(item -> Boolean.TRUE.equals(item.getPublished()))
            .toList();
        return catalogNavigationVisibilityDomainService.filterVisibleItems(
            apiProductItems,
            navigationItemsById,
            input.viewerContext(),
            accessibleApiNavigationItemIds,
            accessibleApiProductIds
        );
    }

    private List<Api> findMatchingApis(
        Input input,
        List<PortalNavigationApi> visibleApis,
        Optional<String> query,
        boolean typoToleranceEnabled
    ) {
        Set<String> visibleApiIds = visibleApis.stream().map(PortalNavigationApi::getApiId).collect(Collectors.toSet());
        if (visibleApiIds.isEmpty()) {
            return List.of();
        }
        if (query.isPresent()) {
            return apiPortalSearchQueryService.search(
                input.environmentId(),
                input.organizationId(),
                query.get(),
                visibleApiIds,
                typoToleranceEnabled
            );
        }
        return loadApis(input.environmentId(), visibleApiIds);
    }

    private Map<String, ApiProduct> loadApiProducts(String environmentId, List<PortalNavigationApiProduct> items) {
        Set<String> ids = items.stream().map(PortalNavigationApiProduct::getApiProductId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return apiProductQueryService
            .findByEnvironmentIdAndIdIn(environmentId, ids)
            .stream()
            .collect(Collectors.toMap(ApiProduct::getId, Function.identity(), (first, ignored) -> first));
    }

    private List<CatalogEntry> createEntries(
        List<PortalNavigationApi> visibleApis,
        List<PortalNavigationApiProduct> visibleApiProducts,
        Map<String, Api> matchingApisById,
        Map<String, ApiProduct> visibleApiProductsById,
        Optional<String> query,
        boolean typoToleranceEnabled
    ) {
        List<CatalogEntry> apiEntries = visibleApis
            .stream()
            .filter(item -> matchingApisById.containsKey(item.getApiId()))
            .map(item -> new CatalogEntry(item, matchingApisById.get(item.getApiId()).getName()))
            .toList();
        List<CatalogEntry> productEntries = visibleApiProducts
            .stream()
            .filter(item -> visibleApiProductsById.containsKey(item.getApiProductId()))
            .filter(item -> matchesQuery(visibleApiProductsById.get(item.getApiProductId()).getName(), query, typoToleranceEnabled))
            .map(item -> new CatalogEntry(item, visibleApiProductsById.get(item.getApiProductId()).getName()))
            .toList();

        return java.util.stream.Stream.concat(apiEntries.stream(), productEntries.stream()).toList();
    }

    private boolean matchesQuery(String name, Optional<String> query, boolean typoToleranceEnabled) {
        if (query.isEmpty()) {
            return true;
        }
        if (name == null) {
            return false;
        }

        String normalizedName = name.toLowerCase(Locale.ROOT);
        String normalizedQuery = query.get().toLowerCase(Locale.ROOT);
        if (normalizedName.contains(normalizedQuery)) {
            return true;
        }
        if (!typoToleranceEnabled || normalizedQuery.length() > MAX_FREE_TEXT_CHARS_FOR_FUZZY) {
            return false;
        }

        List<String> nameTokens = tokenize(normalizedName);
        List<String> queryTokens = tokenize(normalizedQuery);
        return (
            !queryTokens.isEmpty() &&
            queryTokens
                .stream()
                .allMatch(queryToken ->
                    nameTokens
                        .stream()
                        .anyMatch(nameToken -> nameToken.equals(queryToken) || matchesWithTypoTolerance(queryToken, nameToken))
                )
        );
    }

    private List<String> tokenize(String value) {
        return Arrays.stream(value.split(WORD_SEPARATOR_PATTERN))
            .filter(token -> !token.isBlank())
            .toList();
    }

    private boolean matchesWithTypoTolerance(String queryToken, String nameToken) {
        if (queryToken.length() < MIN_FUZZY_TOKEN_LENGTH || nameToken.isEmpty() || queryToken.charAt(0) != nameToken.charAt(0)) {
            return false;
        }
        int maxEdits = queryToken.length() >= 8 ? 2 : 1;
        return isWithinEditDistance(queryToken, nameToken, maxEdits);
    }

    private boolean isWithinEditDistance(String left, String right, int maxEdits) {
        if (Math.abs(left.length() - right.length()) > maxEdits) {
            return false;
        }
        if (left.equals(right)) {
            return true;
        }

        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        Arrays.fill(previous, maxEdits + 1);
        for (int column = 0; column <= Math.min(right.length(), maxEdits); column++) {
            previous[column] = column;
        }

        for (int row = 1; row <= left.length(); row++) {
            Arrays.fill(current, maxEdits + 1);
            if (row <= maxEdits) {
                current[0] = row;
            }
            int firstColumn = Math.max(1, row - maxEdits);
            int lastColumn = Math.min(right.length(), row + maxEdits);
            for (int column = firstColumn; column <= lastColumn; column++) {
                int substitution = previous[column - 1] + (left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1);
                int insertion = current[column - 1] + 1;
                int deletion = previous[column] + 1;
                current[column] = Math.min(substitution, Math.min(insertion, deletion));
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[right.length()] <= maxEdits;
    }

    private List<CatalogEntry> paginate(List<CatalogEntry> entries, Pageable pageable) {
        if (pageable.getPageSize() == -1) {
            return entries;
        }
        long skip = (long) (pageable.getPageNumber() - 1) * pageable.getPageSize();
        return entries.stream().skip(skip).limit(pageable.getPageSize()).toList();
    }

    private List<Api> resolveIncludedApis(Input input, List<CatalogEntry> pageEntries, Map<String, Api> matchingApisById) {
        if (!input.includes().contains(PortalNavigationSearchInclude.API)) {
            return List.of();
        }
        Map<String, Api> includedApisById = new LinkedHashMap<>();
        pageEntries
            .stream()
            .map(CatalogEntry::item)
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .map(PortalNavigationApi::getApiId)
            .map(matchingApisById::get)
            .filter(java.util.Objects::nonNull)
            .forEach(api -> includedApisById.putIfAbsent(api.getId(), api));
        return List.copyOf(includedApisById.values());
    }

    private List<PortalCatalogApiProductSummary> resolveIncludedApiProducts(
        Input input,
        List<CatalogEntry> pageEntries,
        Map<String, ApiProduct> apiProductsById,
        List<PortalNavigationApi> visibleApis
    ) {
        if (!input.includes().contains(PortalNavigationSearchInclude.API_PRODUCT)) {
            return List.of();
        }
        List<PortalNavigationApiProduct> pageApiProducts = pageEntries
            .stream()
            .map(CatalogEntry::item)
            .filter(PortalNavigationApiProduct.class::isInstance)
            .map(PortalNavigationApiProduct.class::cast)
            .toList();
        Set<String> visibleApiIds = visibleApis.stream().map(PortalNavigationApi::getApiId).collect(Collectors.toSet());
        Set<String> includedApiIds = pageApiProducts
            .stream()
            .map(item -> apiProductsById.get(item.getApiProductId()))
            .filter(java.util.Objects::nonNull)
            .map(ApiProduct::getApiIds)
            .filter(java.util.Objects::nonNull)
            .flatMap(Set::stream)
            .filter(visibleApiIds::contains)
            .collect(Collectors.toSet());
        Map<String, Api> includedApisById = loadApis(input.environmentId(), includedApiIds)
            .stream()
            .collect(Collectors.toMap(Api::getId, Function.identity(), (first, ignored) -> first));

        return pageApiProducts
            .stream()
            .map(item -> toSummary(item, apiProductsById.get(item.getApiProductId()), includedApisById))
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    private PortalCatalogApiProductSummary toSummary(
        PortalNavigationApiProduct item,
        ApiProduct apiProduct,
        Map<String, Api> includedApisById
    ) {
        if (apiProduct == null) {
            return null;
        }
        List<PortalCatalogApiProductSummary.ApiSummary> apiSummaries = Optional.ofNullable(apiProduct.getApiIds())
            .orElse(Set.of())
            .stream()
            .map(includedApisById::get)
            .filter(java.util.Objects::nonNull)
            .sorted(API_COMPARATOR)
            .map(api -> new PortalCatalogApiProductSummary.ApiSummary(api.getId(), api.getName(), api.getVersion()))
            .toList();
        return new PortalCatalogApiProductSummary(
            apiProduct.getId(),
            apiProduct.getName(),
            apiProduct.getDescription(),
            apiProduct.getVersion(),
            item.getId().json(),
            apiSummaries
        );
    }

    private List<Api> loadApis(String environmentId, Set<String> apiIds) {
        if (apiIds.isEmpty()) {
            return List.of();
        }
        return apiQueryService
            .search(ApiSearchCriteria.builder().environmentId(environmentId).ids(List.copyOf(apiIds)).build(), null, API_FIELD_FILTER)
            .toList();
    }

    public record Input(
        String environmentId,
        String organizationId,
        PortalNavigationItemViewerContext viewerContext,
        Pageable pageable,
        Optional<String> query,
        Set<PortalNavigationSearchInclude> includes,
        Optional<PortalCategoryId> categoryId
    ) {}

    public record Output(
        Page<PortalNavigationItem> items,
        List<Api> includedApis,
        List<PortalCatalogApiProductSummary> includedApiProducts
    ) {}

    private record CatalogEntry(PortalNavigationItem item, String name) {}
}
