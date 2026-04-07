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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.LicenseFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.AbstractUseCaseTest;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.LicenseCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api_product.domain_service.DeployApiProductDomainService;
import io.gravitee.apim.core.api_product.domain_service.ValidateApiProductService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeployApiProductUseCaseTest extends AbstractUseCaseTest {

    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final EventCrudService eventCrudService = mock(EventCrudService.class);
    private final EventLatestCrudService eventLatestCrudService = mock(EventLatestCrudService.class);
    private final LicenseManager licenseManager = mock(LicenseManager.class);
    private DeployApiProductUseCase deployApiProductUseCase;

    @BeforeEach
    void setUp() {
        when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(LicenseFixtures.anEnterpriseLicense());
        var validateApiProductService = new ValidateApiProductService(
            new ApiQueryServiceInMemory(),
            new ApiCrudServiceInMemory(),
            planQueryService,
            mock(ApiProductQueryService.class)
        );
        deployApiProductUseCase = new DeployApiProductUseCase(
            apiProductQueryService,
            new LicenseDomainService(new LicenseCrudServiceInMemory(), licenseManager),
            validateApiProductService,
            new DeployApiProductDomainService(planQueryService, eventCrudService, eventLatestCrudService)
        );
    }

    @Test
    void should_deploy_api_product() {
        var productId = "api-product-id";
        var apiProduct = ApiProduct.builder().id(productId).name("Product").environmentId(ENV_ID).version("1.0.0").build();
        apiProductQueryService.initWith(List.of(apiProduct));

        var input = new DeployApiProductUseCase.Input(productId, AUDIT_INFO);
        var output = deployApiProductUseCase.execute(input);

        assertThat(output.apiProduct())
            .hasFieldOrPropertyWithValue("id", productId)
            .hasFieldOrPropertyWithValue("name", "Product")
            .hasFieldOrPropertyWithValue("environmentId", ENV_ID);

        verify(eventCrudService).createEvent(eq(ORG_ID), eq(ENV_ID), any(), any(), any(), any());
        verify(eventLatestCrudService).createOrPatchLatestEvent(eq(ORG_ID), eq(productId), any());
    }

    @Test
    void should_throw_exception_if_product_does_not_exist() {
        var input = new DeployApiProductUseCase.Input("missing-id", AUDIT_INFO);

        assertThatThrownBy(() -> deployApiProductUseCase.execute(input))
            .isInstanceOf(ApiProductNotFoundException.class)
            .hasMessage("API Product not found.");
    }

    @Test
    void should_deploy_when_all_apis_have_their_own_published_plan() {
        var productId = "api-product-id";
        var apiId = "api-1";
        var apiProduct = ApiProduct.builder()
            .id(productId)
            .name("Product")
            .environmentId(ENV_ID)
            .version("1.0.0")
            .apiIds(Set.of(apiId))
            .build();
        apiProductQueryService.initWith(List.of(apiProduct));
        planQueryService.initWith(
            List.of(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .referenceId(apiId)
                    .referenceType(GenericPlanEntity.ReferenceType.API)
                    .environmentId(ENV_ID)
                    .planDefinitionHttpV4(
                        PlanFixtures.aPlanHttpV4().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.PUBLISHED).build()
                    )
                    .build()
            )
        );

        var output = deployApiProductUseCase.execute(new DeployApiProductUseCase.Input(productId, AUDIT_INFO));

        assertThat(output.apiProduct().getId()).isEqualTo(productId);
        verify(eventCrudService).createEvent(eq(ORG_ID), eq(ENV_ID), any(), any(), any(), any());
    }

    @Test
    void should_deploy_when_apis_without_own_plan_are_covered_by_a_published_product_plan() {
        var productId = "api-product-id";
        var apiId = "api-without-plan";
        var apiProduct = ApiProduct.builder()
            .id(productId)
            .name("Product")
            .environmentId(ENV_ID)
            .version("1.0.0")
            .apiIds(Set.of(apiId))
            .build();
        apiProductQueryService.initWith(List.of(apiProduct));
        planQueryService.initWith(
            List.of(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .referenceId(productId)
                    .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                    .environmentId(ENV_ID)
                    .planDefinitionHttpV4(
                        PlanFixtures.aPlanHttpV4().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.PUBLISHED).build()
                    )
                    .build()
            )
        );

        var output = deployApiProductUseCase.execute(new DeployApiProductUseCase.Input(productId, AUDIT_INFO));

        assertThat(output.apiProduct().getId()).isEqualTo(productId);
        verify(eventCrudService).createEvent(eq(ORG_ID), eq(ENV_ID), any(), any(), any(), any());
    }

    @Test
    void should_deploy_when_apis_without_own_plan_are_covered_by_a_deprecated_product_plan() {
        var productId = "api-product-id";
        var apiId = "api-without-plan";
        var apiProduct = ApiProduct.builder()
            .id(productId)
            .name("Product")
            .environmentId(ENV_ID)
            .version("1.0.0")
            .apiIds(Set.of(apiId))
            .build();
        apiProductQueryService.initWith(List.of(apiProduct));
        planQueryService.initWith(
            List.of(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .referenceId(productId)
                    .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                    .environmentId(ENV_ID)
                    .planDefinitionHttpV4(
                        PlanFixtures.aPlanHttpV4().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.DEPRECATED).build()
                    )
                    .build()
            )
        );

        var output = deployApiProductUseCase.execute(new DeployApiProductUseCase.Input(productId, AUDIT_INFO));

        assertThat(output.apiProduct().getId()).isEqualTo(productId);
        verify(eventCrudService).createEvent(eq(ORG_ID), eq(ENV_ID), any(), any(), any(), any());
    }

    @Test
    void should_deploy_when_all_apis_have_deprecated_plan_and_product_has_no_valid_plan() {
        var productId = "api-product-id";
        var apiId = "api-1";
        var apiProduct = ApiProduct.builder()
            .id(productId)
            .name("Product")
            .environmentId(ENV_ID)
            .version("1.0.0")
            .apiIds(Set.of(apiId))
            .build();
        apiProductQueryService.initWith(List.of(apiProduct));
        planQueryService.initWith(
            List.of(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .referenceId(apiId)
                    .referenceType(GenericPlanEntity.ReferenceType.API)
                    .environmentId(ENV_ID)
                    .planDefinitionHttpV4(
                        PlanFixtures.aPlanHttpV4().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.DEPRECATED).build()
                    )
                    .build()
            )
        );

        var output = deployApiProductUseCase.execute(new DeployApiProductUseCase.Input(productId, AUDIT_INFO));

        assertThat(output.apiProduct().getId()).isEqualTo(productId);
        verify(eventCrudService).createEvent(eq(ORG_ID), eq(ENV_ID), any(), any(), any(), any());
    }

    @Test
    void should_throw_when_product_has_only_staging_plan_and_apis_have_no_plan() {
        var productId = "api-product-id";
        var apiId = "api-without-plan";
        var apiProduct = ApiProduct.builder()
            .id(productId)
            .name("Product")
            .environmentId(ENV_ID)
            .version("1.0.0")
            .apiIds(Set.of(apiId))
            .build();
        apiProductQueryService.initWith(List.of(apiProduct));
        planQueryService.initWith(
            List.of(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .referenceId(productId)
                    .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                    .environmentId(ENV_ID)
                    .planDefinitionHttpV4(
                        PlanFixtures.aPlanHttpV4().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.STAGING).build()
                    )
                    .build()
            )
        );

        assertThatThrownBy(() -> deployApiProductUseCase.execute(new DeployApiProductUseCase.Input(productId, AUDIT_INFO)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("some APIs have no published plan");
    }

    @Test
    void should_throw_when_api_has_no_own_plan_and_product_has_no_published_plan() {
        var productId = "api-product-id";
        var apiId = "api-without-plan";
        var apiProduct = ApiProduct.builder()
            .id(productId)
            .name("Product")
            .environmentId(ENV_ID)
            .version("1.0.0")
            .apiIds(Set.of(apiId))
            .build();
        apiProductQueryService.initWith(List.of(apiProduct));
        // No plans at all

        assertThatThrownBy(() -> deployApiProductUseCase.execute(new DeployApiProductUseCase.Input(productId, AUDIT_INFO)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("some APIs have no published plan");
    }

    @Test
    void should_throw_when_api_has_only_staging_plan_and_product_has_no_published_plan() {
        var productId = "api-product-id";
        var apiId = "api-staging-plan";
        var apiProduct = ApiProduct.builder()
            .id(productId)
            .name("Product")
            .environmentId(ENV_ID)
            .version("1.0.0")
            .apiIds(Set.of(apiId))
            .build();
        apiProductQueryService.initWith(List.of(apiProduct));
        planQueryService.initWith(
            List.of(
                PlanFixtures.aPlanHttpV4()
                    .toBuilder()
                    .referenceId(apiId)
                    .referenceType(GenericPlanEntity.ReferenceType.API)
                    .environmentId(ENV_ID)
                    .planDefinitionHttpV4(
                        PlanFixtures.aPlanHttpV4().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.STAGING).build()
                    )
                    .build()
            )
        );

        assertThatThrownBy(() -> deployApiProductUseCase.execute(new DeployApiProductUseCase.Input(productId, AUDIT_INFO)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("some APIs have no published plan");
    }

    @Test
    void should_throw_exception_when_license_does_not_allow_api_product() {
        when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(LicenseFixtures.anOssLicense());

        var productId = "api-product-id";
        var apiProduct = ApiProduct.builder().id(productId).name("Product").environmentId(ENV_ID).version("1.0.0").build();
        apiProductQueryService.initWith(List.of(apiProduct));

        var input = new DeployApiProductUseCase.Input(productId, AUDIT_INFO);

        assertThatThrownBy(() -> deployApiProductUseCase.execute(input)).isInstanceOf(ForbiddenFeatureException.class);
    }
}
