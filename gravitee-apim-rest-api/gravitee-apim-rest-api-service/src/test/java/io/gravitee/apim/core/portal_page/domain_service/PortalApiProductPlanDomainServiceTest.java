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
package io.gravitee.apim.core.portal_page.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.plan.domain_service.PlanExcludedGroupsDomainService;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class PortalApiProductPlanDomainServiceTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_PRODUCT_ID = "api-product-id";
    private static final String USER_ID = "user-id";
    private static final PortalNavigationItemViewerContext VIEWER_CONTEXT = PortalNavigationItemViewerContext.forPortal(USER_ID);

    @Mock
    private PortalApiProductAccessDomainService portalApiProductAccessDomainService;

    @Mock
    private PlanSearchQueryService planSearchQueryService;

    @Mock
    private PlanExcludedGroupsDomainService planExcludedGroupsDomainService;

    private PortalApiProductPlanDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new PortalApiProductPlanDomainService(
            portalApiProductAccessDomainService,
            planSearchQueryService,
            planExcludedGroupsDomainService
        );
    }

    @Test
    void should_return_authorized_published_plans_ordered_by_plan_order() {
        var second = PlanFixtures.HttpV4.anApiKey().toBuilder().id("plan-2").order(2).build();
        var first = PlanFixtures.HttpV4.anApiKey().toBuilder().id("plan-1").order(1).build();
        var restricted = PlanFixtures.HttpV4.anApiKey()
            .toBuilder()
            .id("restricted")
            .order(0)
            .excludedGroups(List.of("restricted-group"))
            .build();
        var apiProduct = accessibleApiProduct();
        when(portalApiProductAccessDomainService.findAccessible(ENVIRONMENT_ID, API_PRODUCT_ID, VIEWER_CONTEXT)).thenReturn(
            new PortalApiProductAccessDomainService.AccessibleApiProduct(apiProduct, null)
        );
        when(
            planSearchQueryService.searchPlans(
                eq(API_PRODUCT_ID),
                eq(GenericPlanEntity.ReferenceType.API_PRODUCT),
                any(PlanQuery.class),
                eq(USER_ID),
                eq(false)
            )
        ).thenReturn(List.of(second, restricted, first));
        when(planExcludedGroupsDomainService.isUserAuthorizedToAccessApiProductPlan(apiProduct, List.of(), USER_ID)).thenReturn(true);
        when(
            planExcludedGroupsDomainService.isUserAuthorizedToAccessApiProductPlan(apiProduct, restricted.getExcludedGroups(), USER_ID)
        ).thenReturn(false);

        var result = domainService.findAccessiblePlans(ENVIRONMENT_ID, API_PRODUCT_ID, VIEWER_CONTEXT);

        assertThat(result).containsExactly(first, second);
        var queryCaptor = ArgumentCaptor.forClass(PlanQuery.class);
        verify(planSearchQueryService).searchPlans(
            eq(API_PRODUCT_ID),
            eq(GenericPlanEntity.ReferenceType.API_PRODUCT),
            queryCaptor.capture(),
            eq(USER_ID),
            eq(false)
        );
        assertThat(queryCaptor.getValue().getStatus()).containsExactly(PlanStatus.PUBLISHED);
    }

    @Test
    void should_not_search_plans_when_api_product_is_inaccessible() {
        when(portalApiProductAccessDomainService.findAccessible(ENVIRONMENT_ID, API_PRODUCT_ID, VIEWER_CONTEXT)).thenThrow(
            new ApiProductNotFoundException(API_PRODUCT_ID)
        );

        assertThatThrownBy(() -> domainService.findAccessiblePlans(ENVIRONMENT_ID, API_PRODUCT_ID, VIEWER_CONTEXT)).isInstanceOf(
            ApiProductNotFoundException.class
        );
        verifyNoInteractions(planSearchQueryService, planExcludedGroupsDomainService);
    }

    private static ApiProduct accessibleApiProduct() {
        return ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENVIRONMENT_ID).build();
    }
}
