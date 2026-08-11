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
package io.gravitee.apim.core.plan.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.portal_page.domain_service.PortalApiProductPlanDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class GetPortalApiProductPlansUseCaseTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_PRODUCT_ID = "api-product-id";
    private static final PortalNavigationItemViewerContext VIEWER_CONTEXT = PortalNavigationItemViewerContext.forPortal("user-id");

    @Mock
    private PortalApiProductPlanDomainService portalApiProductPlanDomainService;

    private GetPortalApiProductPlansUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPortalApiProductPlansUseCase(portalApiProductPlanDomainService);
    }

    @Test
    void should_return_published_api_product_plans_for_current_user() {
        var first = PlanFixtures.HttpV4.anApiKey().toBuilder().id("plan-1").order(1).build();
        var second = PlanFixtures.HttpV4.anApiKey().toBuilder().id("plan-2").order(2).build();
        var input = input();
        when(portalApiProductPlanDomainService.findAccessiblePlans(ENVIRONMENT_ID, API_PRODUCT_ID, VIEWER_CONTEXT)).thenReturn(
            List.of(first, second)
        );

        var output = useCase.execute(input);

        assertThat(output.plans()).containsExactly(first, second);
    }

    @Test
    void should_not_load_plans_when_api_product_is_inaccessible() {
        var input = input();
        when(portalApiProductPlanDomainService.findAccessiblePlans(ENVIRONMENT_ID, API_PRODUCT_ID, VIEWER_CONTEXT)).thenThrow(
            new ApiProductNotFoundException(API_PRODUCT_ID)
        );

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(ApiProductNotFoundException.class);
    }

    private static GetPortalApiProductPlansUseCase.Input input() {
        return new GetPortalApiProductPlansUseCase.Input(ENVIRONMENT_ID, API_PRODUCT_ID, VIEWER_CONTEXT);
    }
}
