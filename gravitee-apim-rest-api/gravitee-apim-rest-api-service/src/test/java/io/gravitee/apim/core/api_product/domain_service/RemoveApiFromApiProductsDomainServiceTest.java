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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import fixtures.core.model.PlanFixtures;
import inmemory.AbstractUseCaseTest;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RemoveApiFromApiProductsDomainServiceTest extends AbstractUseCaseTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";
    private static final String USER_ID = "user-id";
    private static final String API_ID = "api-to-delete";
    private static final String PRODUCT_1 = "product-1";
    private static final String PRODUCT_2 = "product-2";

    private final ApiProductCrudServiceInMemory apiProductCrudService = new ApiProductCrudServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory(apiCrudService);
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final EventCrudService eventCrudService = mock(EventCrudService.class);
    private final EventLatestCrudService eventLatestCrudService = mock(EventLatestCrudService.class);

    private RemoveApiFromApiProductsDomainService domainService;

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var validateApiProductService = new ValidateApiProductService(
            apiQueryService,
            apiCrudService,
            planQueryService,
            apiProductQueryService
        );
        domainService = new RemoveApiFromApiProductsDomainService(
            apiProductQueryService,
            apiProductCrudService,
            validateApiProductService,
            eventCrudService,
            eventLatestCrudService,
            auditDomainService
        );
    }

    @Nested
    class WhenNoApiProductReferencesTheApi {

        @Test
        void should_do_nothing() {
            domainService.removeApiFromApiProducts(API_ID, ORG_ID, ENV_ID, USER_ID);

            verify(eventCrudService, never()).createEvent(any(), any(), any(), any(), any(), any());
            assertThat(auditCrudService.storage()).isEmpty();
        }
    }

    @Nested
    class WhenApiProductsReferenceTheApi {

        @BeforeEach
        void setUp() {
            apiProductCrudService.initWith(
                List.of(
                    ApiProduct.builder().id(PRODUCT_1).environmentId(ENV_ID).apiIds(new HashSet<>(Set.of(API_ID, "other-api"))).build(),
                    ApiProduct.builder().id(PRODUCT_2).environmentId(ENV_ID).apiIds(new HashSet<>(Set.of(API_ID))).build()
                )
            );
            apiProductQueryService.initWith(apiProductCrudService.storage());
        }

        @Test
        void should_remove_api_id_from_all_affected_products() {
            domainService.removeApiFromApiProducts(API_ID, ORG_ID, ENV_ID, USER_ID);

            apiProductCrudService.storage().forEach(product -> assertThat(product.getApiIds()).doesNotContain(API_ID));
        }

        @Test
        void should_retain_other_api_ids() {
            domainService.removeApiFromApiProducts(API_ID, ORG_ID, ENV_ID, USER_ID);

            var product1 = apiProductCrudService
                .storage()
                .stream()
                .filter(p -> p.getId().equals(PRODUCT_1))
                .findFirst();
            assertThat(product1).isPresent();
            assertThat(product1.get().getApiIds()).containsExactly("other-api");
        }

        @Test
        void should_create_audit_log_for_each_affected_product() {
            domainService.removeApiFromApiProducts(API_ID, ORG_ID, ENV_ID, USER_ID);

            assertThat(auditCrudService.storage()).hasSize(2);
            assertThat(auditCrudService.storage())
                .allMatch(audit -> audit.getOrganizationId().equals(ORG_ID))
                .allMatch(audit -> audit.getEnvironmentId().equals(ENV_ID));
        }

        @Nested
        class WhenProductHasValidPlan {

            @BeforeEach
            void setUp() {
                planQueryService.initWith(
                    List.of(
                        PlanFixtures.aPlanHttpV4()
                            .toBuilder()
                            .id("plan-product-1")
                            .referenceId(PRODUCT_1)
                            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                            .build(),
                        PlanFixtures.aPlanHttpV4()
                            .toBuilder()
                            .id("plan-product-2")
                            .referenceId(PRODUCT_2)
                            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                            .build()
                    )
                );
            }

            @Test
            void should_publish_deploy_event_for_each_affected_product() {
                domainService.removeApiFromApiProducts(API_ID, ORG_ID, ENV_ID, USER_ID);

                verify(eventCrudService, times(2)).createEvent(any(), any(), any(), any(), any(), any());
                verify(eventLatestCrudService, times(2)).createOrPatchLatestEvent(any(), any(), any());
            }
        }

        @Nested
        class WhenProductHasNoValidPlan {

            @BeforeEach
            void setUp() {
                apiProductCrudService.initWith(
                    List.of(
                        ApiProduct.builder().id(PRODUCT_1).environmentId(ENV_ID).apiIds(new HashSet<>(Set.of(API_ID, "other-api"))).build(),
                        ApiProduct.builder()
                            .id(PRODUCT_2)
                            .environmentId(ENV_ID)
                            .apiIds(new HashSet<>(Set.of(API_ID, "other-api-2")))
                            .build()
                    )
                );
                apiProductQueryService.initWith(apiProductCrudService.storage());
            }

            @Test
            void should_not_publish_deploy_event_when_remaining_apis_have_no_valid_plan() {
                domainService.removeApiFromApiProducts(API_ID, ORG_ID, ENV_ID, USER_ID);

                verify(eventCrudService, never()).createEvent(any(), any(), any(), any(), any(), any());
                verify(eventLatestCrudService, never()).createOrPatchLatestEvent(any(), any(), any());
            }

            @Test
            void should_still_create_audit_log_when_deploy_is_skipped() {
                domainService.removeApiFromApiProducts(API_ID, ORG_ID, ENV_ID, USER_ID);

                assertThat(auditCrudService.storage()).hasSize(2);
            }
        }
    }
}
