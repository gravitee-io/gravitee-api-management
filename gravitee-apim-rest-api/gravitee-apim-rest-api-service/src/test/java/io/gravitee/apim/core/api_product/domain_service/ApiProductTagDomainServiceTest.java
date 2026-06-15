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
package io.gravitee.apim.core.api_product.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import fixtures.core.model.PlanFixtures;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiProductTagDomainServiceTest {

    private static final String ENV_ID = "DEFAULT";
    private static final String PRODUCT_ID = "product-1";

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();

    @Mock
    private ApiProductTagsValidationDomainService apiProductTagsValidationDomainService;

    private ApiProductTagDomainService apiProductTagDomainService;

    @BeforeEach
    void setUp() {
        apiProductCrudService.reset();
        apiProductQueryService.reset();
        planQueryService.reset();
        planCrudService.reset();
        apiProductTagDomainService = new ApiProductTagDomainService(
            apiProductQueryService,
            apiProductCrudService,
            planQueryService,
            planCrudService,
            apiProductTagsValidationDomainService
        );
    }

    @Test
    void should_validate_tags_on_create_when_tags_are_present() {
        var auditInfo = io.gravitee.apim.core.audit.model.AuditInfo.builder().organizationId("org-id").environmentId(ENV_ID).build();

        apiProductTagDomainService.validateTagsOnCreate(auditInfo, Set.of("internal"));

        verify(apiProductTagsValidationDomainService).validateAndSanitize(eq("org-id"), eq(ENV_ID), any(), any());
    }

    @Test
    void should_skip_validation_on_create_when_tags_are_empty() {
        var auditInfo = io.gravitee.apim.core.audit.model.AuditInfo.builder().organizationId("org-id").environmentId(ENV_ID).build();

        apiProductTagDomainService.validateTagsOnCreate(auditInfo, Set.of());

        org.mockito.Mockito.verifyNoInteractions(apiProductTagsValidationDomainService);
    }

    @Test
    void should_remove_deleted_tag_from_product_and_its_plans() {
        ApiProduct product = ApiProduct.builder()
            .id(PRODUCT_ID)
            .name("Product")
            .environmentId(ENV_ID)
            .tags(new HashSet<>(Set.of("internal", "external")))
            .build();
        apiProductCrudService.initWith(List.of(product));
        apiProductQueryService.initWith(List.of(product));

        Plan productPlan = productPlanWithTags("plan-1", Set.of("internal", "external"));
        planQueryService.initWith(List.of(productPlan));
        planCrudService.initWith(List.of(productPlan));

        apiProductTagDomainService.deleteTagFromApiProducts(ENV_ID, "external");

        assertThat(apiProductCrudService.get(PRODUCT_ID).getTags()).containsExactly("internal");
        assertThat(planCrudService.getById("plan-1").getPlanDefinitionHttpV4().getTags()).containsExactly("internal");
    }

    @Test
    void should_not_update_product_when_tag_is_not_present() {
        ApiProduct product = ApiProduct.builder().id(PRODUCT_ID).name("Product").environmentId(ENV_ID).tags(Set.of("internal")).build();
        apiProductCrudService.initWith(List.of(product));
        apiProductQueryService.initWith(List.of(product));

        apiProductTagDomainService.deleteTagFromApiProducts(ENV_ID, "external");

        assertThat(apiProductCrudService.get(PRODUCT_ID).getTags()).containsExactly("internal");
    }

    @Test
    void should_set_product_tags_to_null_when_last_tag_is_removed() {
        ApiProduct product = ApiProduct.builder().id(PRODUCT_ID).name("Product").environmentId(ENV_ID).tags(Set.of("internal")).build();
        apiProductCrudService.initWith(List.of(product));
        apiProductQueryService.initWith(List.of(product));

        apiProductTagDomainService.deleteTagFromApiProducts(ENV_ID, "internal");

        assertThat(apiProductCrudService.get(PRODUCT_ID).getTags()).isNull();
    }

    @Test
    void should_not_remove_tag_when_identifier_does_not_match_stored_tag_key() {
        ApiProduct product = ApiProduct.builder().id(PRODUCT_ID).name("Product").environmentId(ENV_ID).tags(Set.of("internal")).build();
        apiProductCrudService.initWith(List.of(product));
        apiProductQueryService.initWith(List.of(product));

        apiProductTagDomainService.deleteTagFromApiProducts(ENV_ID, "tag-uuid-not-key");

        assertThat(apiProductCrudService.get(PRODUCT_ID).getTags()).containsExactly("internal");
    }

    @Test
    void should_clean_plan_tags_even_when_product_tag_was_already_removed() {
        ApiProduct product = ApiProduct.builder().id(PRODUCT_ID).name("Product").environmentId(ENV_ID).tags(Set.of("internal")).build();
        apiProductCrudService.initWith(List.of(product));
        apiProductQueryService.initWith(List.of(product));

        Plan productPlan = productPlanWithTags("plan-1", Set.of("external"));
        planQueryService.initWith(List.of(productPlan));
        planCrudService.initWith(List.of(productPlan));

        apiProductTagDomainService.deleteTagFromApiProducts(ENV_ID, "external");

        assertThat(apiProductCrudService.get(PRODUCT_ID).getTags()).containsExactly("internal");
        assertThat(planCrudService.getById("plan-1").getPlanDefinitionHttpV4().getTags()).isNull();
    }

    @Test
    void should_be_idempotent_when_delete_tag_from_api_products_is_called_twice() {
        ApiProduct product = ApiProduct.builder()
            .id(PRODUCT_ID)
            .name("Product")
            .environmentId(ENV_ID)
            .tags(new HashSet<>(Set.of("internal", "external")))
            .build();
        apiProductCrudService.initWith(List.of(product));
        apiProductQueryService.initWith(List.of(product));

        Plan productPlan = productPlanWithTags("plan-1", Set.of("external"));
        planQueryService.initWith(List.of(productPlan));
        planCrudService.initWith(List.of(productPlan));

        apiProductTagDomainService.deleteTagFromApiProducts(ENV_ID, "external");
        apiProductTagDomainService.deleteTagFromApiProducts(ENV_ID, "external");

        assertThat(apiProductCrudService.get(PRODUCT_ID).getTags()).containsExactly("internal");
        assertThat(planCrudService.getById("plan-1").getPlanDefinitionHttpV4().getTags()).isNull();
    }

    private Plan productPlanWithTags(String planId, Set<String> tags) {
        var planDefinition = PlanFixtures.aPlanHttpV4()
            .getPlanDefinitionHttpV4()
            .toBuilder()
            .status(PlanStatus.PUBLISHED)
            .tags(tags)
            .build();
        return PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(planId)
            .referenceId(PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .environmentId(ENV_ID)
            .planDefinitionHttpV4(planDefinition)
            .build();
    }
}
