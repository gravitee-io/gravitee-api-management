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
package io.gravitee.apim.core.subscription.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.ApiExposedEntrypointDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.ExposedEntrypoint;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemVisibilityEvaluator;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemVisibilityService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.subscription.model.PortalApiProductSubscriptionDetails;
import io.gravitee.apim.core.subscription.model.PortalApiProductSubscriptionDetails.ApiAvailability;
import io.gravitee.apim.core.subscription.model.PortalApiProductSubscriptionDetails.ApiSummary;
import io.gravitee.apim.core.subscription.model.PortalApiProductSubscriptionDetails.Availability;
import io.gravitee.apim.core.subscription.model.PortalApiProductSubscriptionDetails.DocumentationTarget;
import io.gravitee.apim.core.subscription.model.PortalApiProductSubscriptionDetails.PlanSummary;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPortalApiProductSubscriptionDetailsUseCase {

    private static final Comparator<String> NULL_SAFE_STRING_COMPARATOR = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
    private static final ApiFieldFilter API_FIELD_FILTER = ApiFieldFilter.builder().pictureExcluded(true).build();

    private final ApiProductQueryService apiProductQueryService;
    private final PlanCrudService planCrudService;
    private final ApiQueryService apiQueryService;
    private final ApiExposedEntrypointDomainService apiExposedEntrypointDomainService;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final List<PortalNavigationItemVisibilityService> visibilityServices;

    public Output execute(Input input) {
        var plan = resolvePlan(input);
        var apiProduct = apiProductQueryService
            .findById(input.apiProductId())
            .filter(product -> input.environmentId().equals(product.getEnvironmentId()))
            .orElse(null);

        if (apiProduct == null) {
            return new Output(
                new PortalApiProductSubscriptionDetails(input.apiProductId(), null, null, Availability.UNAVAILABLE, plan, List.of())
            );
        }

        return new Output(
            new PortalApiProductSubscriptionDetails(
                apiProduct.getId(),
                apiProduct.getName(),
                apiProduct.getVersion(),
                Availability.AVAILABLE,
                plan,
                resolveApis(apiProduct, input)
            )
        );
    }

    private PlanSummary resolvePlan(Input input) {
        return planCrudService
            .findByPlanIdAndReferenceIdAndReferenceType(
                input.planId(),
                input.apiProductId(),
                GenericPlanEntity.ReferenceType.API_PRODUCT.name()
            )
            .map(this::toPlanSummary)
            .orElseGet(() -> new PlanSummary(input.planId(), null, null, null));
    }

    private PlanSummary toPlanSummary(Plan plan) {
        var security = plan.getPlanSecurity() == null ? null : plan.getPlanSecurity().getType();
        var mode = plan.getPlanMode() == null ? null : plan.getPlanMode().name();
        return new PlanSummary(plan.getId(), plan.getName(), security, mode);
    }

    private List<ApiSummary> resolveApis(ApiProduct apiProduct, Input input) {
        var apiIds = apiProduct.getApiIds() == null ? Set.<String>of() : Set.copyOf(apiProduct.getApiIds());
        if (apiIds.isEmpty()) {
            return List.of();
        }

        Map<String, Api> apisById = apiQueryService
            .search(
                ApiSearchCriteria.builder().environmentId(input.environmentId()).ids(List.copyOf(apiIds)).build(),
                null,
                API_FIELD_FILTER
            )
            .collect(Collectors.toMap(Api::getId, Function.identity(), (left, right) -> left));

        Map<String, List<PortalNavigationApi>> navigationItemsByApiId = portalNavigationItemsQueryService
            .search(
                PortalNavigationItemQueryCriteria.builder()
                    .environmentId(input.environmentId())
                    .published(true)
                    .type(PortalNavigationItemType.API)
                    .apiIds(apiIds)
                    .build()
            )
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .collect(Collectors.groupingBy(PortalNavigationApi::getApiId));
        Map<PortalNavigationItemId, PortalNavigationItem> navigationAncestorsById = new HashMap<>();
        var visibilityEvaluator = new PortalNavigationItemVisibilityEvaluator(
            input.environmentId(),
            input.viewerContext(),
            portalNavigationItemsQueryService,
            visibilityServices
        );

        return apiIds
            .stream()
            .map(apiId ->
                toApiSummary(
                    apiId,
                    apisById.get(apiId),
                    navigationItemsByApiId.getOrDefault(apiId, List.of()),
                    input,
                    navigationAncestorsById,
                    visibilityEvaluator
                )
            )
            .sorted(
                Comparator.comparing(ApiSummary::name, NULL_SAFE_STRING_COMPARATOR)
                    .thenComparing(ApiSummary::version, NULL_SAFE_STRING_COMPARATOR)
                    .thenComparing(ApiSummary::id, NULL_SAFE_STRING_COMPARATOR)
            )
            .toList();
    }

    private ApiSummary toApiSummary(
        String apiId,
        Api api,
        List<PortalNavigationApi> navigationItems,
        Input input,
        Map<PortalNavigationItemId, PortalNavigationItem> navigationAncestorsById,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator
    ) {
        if (api == null) {
            return new ApiSummary(apiId, null, null, null, ApiAvailability.UNAVAILABLE, List.of(), null);
        }

        var availability = apiAvailability(api);
        if (availability != ApiAvailability.AVAILABLE) {
            return new ApiSummary(
                api.getId(),
                api.getName(),
                api.getVersion(),
                api.getType() == null ? null : api.getType().name(),
                availability,
                List.of(),
                null
            );
        }

        var entrypoints = Objects.requireNonNullElse(
            apiExposedEntrypointDomainService.get(input.organizationId(), input.environmentId(), api),
            List.<ExposedEntrypoint>of()
        )
            .stream()
            .map(ExposedEntrypoint::value)
            .filter(Objects::nonNull)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();

        return new ApiSummary(
            api.getId(),
            api.getName(),
            api.getVersion(),
            api.getType() == null ? null : api.getType().name(),
            availability,
            entrypoints,
            resolveDocumentationTarget(navigationItems, input, navigationAncestorsById, visibilityEvaluator)
        );
    }

    private ApiAvailability apiAvailability(Api api) {
        if (api.getApiLifecycleState() == null) {
            return ApiAvailability.UNAVAILABLE;
        }
        return switch (api.getApiLifecycleState()) {
            case CREATED, UNPUBLISHED -> ApiAvailability.UNPUBLISHED;
            case ARCHIVED -> ApiAvailability.UNAVAILABLE;
            case PUBLISHED, DEPRECATED -> ApiAvailability.AVAILABLE;
        };
    }

    private DocumentationTarget resolveDocumentationTarget(
        List<PortalNavigationApi> navigationItems,
        Input input,
        Map<PortalNavigationItemId, PortalNavigationItem> navigationAncestorsById,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator
    ) {
        return navigationItems
            .stream()
            .filter(item -> isAccessibleFromSubscribedProduct(item, input, navigationAncestorsById, visibilityEvaluator))
            .sorted(Comparator.comparing(PortalNavigationItem::getOrder).thenComparing(item -> item.getId().json()))
            .findFirst()
            .map(item -> new DocumentationTarget(item.getRootId().json(), item.getId().json()))
            .orElse(null);
    }

    private boolean isAccessibleFromSubscribedProduct(
        PortalNavigationApi item,
        Input input,
        Map<PortalNavigationItemId, PortalNavigationItem> navigationAncestorsById,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator
    ) {
        Set<PortalNavigationItemId> visited = new HashSet<>();
        PortalNavigationItem current = item;
        boolean subscribedProductFound = false;

        while (current != null && visited.add(current.getId())) {
            if (input.viewerContext().shouldNotShow(current) || !visibilityEvaluator.isVisible(current)) {
                return false;
            }
            if (
                current instanceof PortalNavigationApiProduct apiProductItem &&
                input.apiProductId().equals(apiProductItem.getApiProductId())
            ) {
                subscribedProductFound = true;
            }
            if (current.getParentId() == null) {
                return subscribedProductFound;
            }
            current = findNavigationAncestor(current.getParentId(), input, navigationAncestorsById);
        }

        return false;
    }

    private PortalNavigationItem findNavigationAncestor(
        PortalNavigationItemId ancestorId,
        Input input,
        Map<PortalNavigationItemId, PortalNavigationItem> navigationAncestorsById
    ) {
        if (!navigationAncestorsById.containsKey(ancestorId)) {
            navigationAncestorsById.put(
                ancestorId,
                portalNavigationItemsQueryService.findByIdAndEnvironmentId(input.environmentId(), ancestorId)
            );
        }
        return navigationAncestorsById.get(ancestorId);
    }

    public record Input(
        String organizationId,
        String environmentId,
        String apiProductId,
        String planId,
        PortalNavigationItemViewerContext viewerContext
    ) {}

    public record Output(PortalApiProductSubscriptionDetails apiProduct) {}
}
