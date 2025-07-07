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
package io.gravitee.apim.rest.api.automation.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import fixtures.ApplicationModelFixtures;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.automation.model.SubscriptionState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionResourceTest extends AbstractResourceTest {

    @Autowired
    private SubscriptionCrudServiceInMemory subscriptionCrudService;

    @Autowired
    private ApiCrudServiceInMemory apiCrudService;

    @Autowired
    private ApplicationCrudServiceInMemory applicationCrudService;

    @Autowired
    private PlanCrudServiceInMemory planCrudService;

    static final String HRID = "subscription-hrid";
    static final String API_HRID = "api-hrid";
    static final String APPLICATION_HRID = "application-hrid";
    static final String PLAN_HRID = "plan-hrid";
    static final AuditInfo auditInfo = AuditInfo.builder().organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build();

    @AfterEach
    void tearDown() {
        subscriptionCrudService.reset();
        apiCrudService.reset();
        applicationCrudService.reset();
        planCrudService.reset();
    }

    @Nested
    class GET {

        @Test
        void should_get_application_from_known_hrid() {
            try (var ctx = mockStatic(GraviteeContext.class)) {
                ctx.when(GraviteeContext::getExecutionContext).thenReturn(new ExecutionContext(ORGANIZATION, ENVIRONMENT));

                subscriptionCrudService.initWith(
                    List.of(
                        SubscriptionFixtures
                            .aSubscription()
                            .toBuilder()
                            .id(IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), HRID).buildId())
                            .apiId(IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), API_HRID).buildId())
                            .applicationId(IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), APPLICATION_HRID).buildId())
                            .planId(IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), PLAN_HRID).buildId())
                            .build()
                    )
                );
                applicationCrudService.initWith(
                    List.of(
                        ApplicationModelFixtures
                            .anApplicationEntity()
                            .toBuilder()
                            .id(IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), APPLICATION_HRID).buildId())
                            .hrid(APPLICATION_HRID)
                            .build()
                    )
                );

                apiCrudService.initWith(
                    List.of(
                        ApiFixtures
                            .aProxyApiV4()
                            .toBuilder()
                            .id(IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), API_HRID).buildId())
                            .hrid(API_HRID)
                            .build()
                    )
                );

                planCrudService.initWith(
                    List.of(
                        PlanFixtures
                            .aPlanHttpV4()
                            .toBuilder()
                            .id(IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), PLAN_HRID).buildId())
                            .hrid(PLAN_HRID)
                            .build()
                    )
                );

                var state = expectEntity(HRID);
                SoftAssertions.assertSoftly(soft -> {
                    assertThat(state.getHrid()).isEqualTo(HRID);
                    assertThat(state.getApiHrid()).isEqualTo(API_HRID);
                    assertThat(state.getApplicationHrid()).isEqualTo(APPLICATION_HRID);
                    assertThat(state.getPlanHrid()).isEqualTo(PLAN_HRID);
                    assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                });
            }
        }

        @Test
        void should_get_application_from_known_legacy_id() {
            try (var ctx = mockStatic(GraviteeContext.class)) {
                ctx.when(GraviteeContext::getExecutionContext).thenReturn(new ExecutionContext(ORGANIZATION, ENVIRONMENT));

                subscriptionCrudService.initWith(
                    List.of(
                        SubscriptionFixtures
                            .aSubscription()
                            .toBuilder()
                            .id(HRID)
                            .apiId(API_HRID)
                            .applicationId(APPLICATION_HRID)
                            .planId(PLAN_HRID)
                            .build()
                    )
                );
                applicationCrudService.initWith(
                    List.of(ApplicationModelFixtures.anApplicationEntity().toBuilder().id(APPLICATION_HRID).hrid(APPLICATION_HRID).build())
                );

                apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id(API_HRID).hrid(API_HRID).build()));

                planCrudService.initWith(List.of(PlanFixtures.aPlanHttpV4().toBuilder().id(PLAN_HRID).hrid(PLAN_HRID).build()));

                var state = expectEntity(HRID, true);
                SoftAssertions.assertSoftly(soft -> {
                    assertThat(state.getHrid()).isEqualTo(HRID);
                    assertThat(state.getApiHrid()).isEqualTo(API_HRID);
                    assertThat(state.getApplicationHrid()).isEqualTo(APPLICATION_HRID);
                    assertThat(state.getPlanHrid()).isEqualTo(PLAN_HRID);
                    assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                });
            }
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            expectNotFound("unknown");
        }

        private void expectNotFound(String hrid) {
            try (var response = rootTarget().path(hrid).request().get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }

        private SubscriptionState expectEntity(String hrid) {
            return expectEntity(hrid, false);
        }

        private SubscriptionState expectEntity(String hrid, boolean legacy) {
            try (
                var response = rootTarget().queryParam("legacy", legacy).path(hrid).request().accept(MediaType.APPLICATION_JSON_TYPE).get()
            ) {
                return response.readEntity(SubscriptionState.class);
            }
        }
    }

    @Nested
    class DELETE {

        @Test
        void should_delete_application_and_return_no_content() {
            subscriptionCrudService.initWith(
                List.of(
                    SubscriptionFixtures
                        .aSubscription()
                        .toBuilder()
                        .id(IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), HRID).buildId())
                        .apiId(IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), API_HRID).buildId())
                        .applicationId(IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), APPLICATION_HRID).buildId())
                        .planId(IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), PLAN_HRID).buildId())
                        .build()
                )
            );

            expectNoContent(HRID);
        }

        @Test
        void should_delete_application_and_return_no_content_known_legacy_id() {
            subscriptionCrudService.initWith(
                List.of(
                    SubscriptionFixtures
                        .aSubscription()
                        .toBuilder()
                        .id(HRID)
                        .apiId(API_HRID)
                        .applicationId(APPLICATION_HRID)
                        .planId(PLAN_HRID)
                        .build()
                )
            );

            expectNoContent(HRID, true);
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            expectNotFound("unknown");
        }

        private void expectNoContent(String hrid) {
            expectNoContent(hrid, false);
        }

        private void expectNoContent(String hrid, boolean legacy) {
            try (var response = rootTarget().queryParam("legacy", legacy).path(hrid).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
            }
        }

        private void expectNotFound(String hrid) {
            try (var response = rootTarget().path(hrid).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/subscriptions";
    }
}
