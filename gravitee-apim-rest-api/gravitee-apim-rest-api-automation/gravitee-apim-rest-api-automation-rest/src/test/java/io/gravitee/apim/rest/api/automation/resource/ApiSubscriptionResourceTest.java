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

import static io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status.ACCEPTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.ApplicationModelFixtures;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.ApplicationFixture;
import fixtures.core.model.PlanFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDStatus;
import io.gravitee.apim.core.subscription.use_case.ImportSubscriptionSpecUseCase;
import io.gravitee.apim.rest.api.automation.model.SubscriptionState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApiSubscriptionResourceTest extends AbstractResourceTest {

    public static final String SUBSCRIPTION_ID = "sub-id";

    @Autowired
    private SubscriptionCrudServiceInMemory subscriptionCrudService;

    @Autowired
    private ApiCrudServiceInMemory apiCrudService;

    @Autowired
    private ApplicationCrudServiceInMemory applicationCrudService;

    @Autowired
    private PlanCrudServiceInMemory planCrudService;

    @Autowired
    private ImportSubscriptionSpecUseCase importSubscriptionSpecUseCase;

    static final String HRID = "subscription-hrid";
    static final String API_HRID = "api-hrid";
    static final String APPLICATION_HRID = "application-hrid";
    static final String PLAN_HRID = "plan-hrid";

    @AfterEach
    void tearDown() {
        subscriptionCrudService.reset();
        apiCrudService.reset();
        applicationCrudService.reset();
        planCrudService.reset();
        reset(importSubscriptionSpecUseCase);
    }

    @Nested
    class GET {

        @Test
        void should_get_subscription_from_known_hrid() {
            try (var ctx = mockStatic(GraviteeContext.class)) {
                ctx.when(GraviteeContext::getExecutionContext).thenReturn(new ExecutionContext(ORGANIZATION, ENVIRONMENT));

                subscriptionCrudService.initWith(
                    List.of(
                        SubscriptionFixtures.aSubscription()
                            .toBuilder()
                            .id(
                                HRIDToUUID.subscription()
                                    .context(new ExecutionContext(ORGANIZATION, ENVIRONMENT))
                                    .api(API_HRID)
                                    .subscription(HRID)
                                    .id()
                            )
                            .referenceId(HRIDToUUID.api().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).hrid(API_HRID).id())
                            .applicationId(
                                HRIDToUUID.application()
                                    .context(new ExecutionContext(ORGANIZATION, ENVIRONMENT))
                                    .hrid(APPLICATION_HRID)
                                    .id()
                            )
                            .planId(
                                HRIDToUUID.plan()
                                    .context(new ExecutionContext(ORGANIZATION, ENVIRONMENT))
                                    .api(API_HRID)
                                    .plan(PLAN_HRID)
                                    .id()
                            )
                            .build()
                    )
                );
                applicationCrudService.initWith(
                    List.of(
                        ApplicationModelFixtures.anApplicationEntity()
                            .toBuilder()
                            .id(
                                HRIDToUUID.application()
                                    .context(new ExecutionContext(ORGANIZATION, ENVIRONMENT))
                                    .hrid(APPLICATION_HRID)
                                    .id()
                            )
                            .hrid(APPLICATION_HRID)
                            .build()
                    )
                );

                planCrudService.initWith(
                    List.of(
                        PlanFixtures.aPlanHttpV4()
                            .toBuilder()
                            .id(
                                HRIDToUUID.plan()
                                    .context(new ExecutionContext(ORGANIZATION, ENVIRONMENT))
                                    .api(API_HRID)
                                    .plan(PLAN_HRID)
                                    .id()
                            )
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
        void should_get_subscription_from_known_legacy_id() {
            try (var ctx = mockStatic(GraviteeContext.class)) {
                ctx.when(GraviteeContext::getExecutionContext).thenReturn(new ExecutionContext(ORGANIZATION, ENVIRONMENT));

                subscriptionCrudService.initWith(
                    List.of(
                        SubscriptionFixtures.aSubscription()
                            .toBuilder()
                            .id(SUBSCRIPTION_ID)
                            .referenceId(API_HRID)
                            .applicationId(APPLICATION_HRID)
                            .planId(PLAN_HRID)
                            .build()
                    )
                );
                applicationCrudService.initWith(
                    List.of(ApplicationModelFixtures.anApplicationEntity().toBuilder().id(APPLICATION_HRID).hrid(APPLICATION_HRID).build())
                );

                planCrudService.initWith(List.of(PlanFixtures.aPlanHttpV4().toBuilder().id(PLAN_HRID).hrid(PLAN_HRID).build()));

                var state = expectEntity(SUBSCRIPTION_ID, true);
                SoftAssertions.assertSoftly(soft -> {
                    assertThat(state.getId()).isEqualTo(SUBSCRIPTION_ID);
                    assertThat(state.getHrid()).isNull();
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
                var response = rootTarget()
                    .queryParam("legacyID", legacy)
                    .path(hrid)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get()
            ) {
                return response.readEntity(SubscriptionState.class);
            }
        }
    }

    @Nested
    class DELETE {

        @Test
        void should_delete_subscription_and_return_no_content() {
            String applicationId = HRIDToUUID.application()
                .context(new ExecutionContext(ORGANIZATION, ENVIRONMENT))
                .hrid(APPLICATION_HRID)
                .id();
            String apiId = HRIDToUUID.api().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).hrid(API_HRID).id();
            subscriptionCrudService.initWith(
                List.of(
                    SubscriptionFixtures.aSubscription()
                        .toBuilder()
                        .id(
                            HRIDToUUID.subscription()
                                .context(new ExecutionContext(ORGANIZATION, ENVIRONMENT))
                                .api(API_HRID)
                                .subscription(HRID)
                                .id()
                        )
                        .referenceId(apiId)
                        .applicationId(applicationId)
                        .planId(
                            HRIDToUUID.plan().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).api(API_HRID).plan(PLAN_HRID).id()
                        )
                        .build()
                )
            );
            applicationCrudService.initWith(List.of(BaseApplicationEntity.builder().id(applicationId).hrid(APPLICATION_HRID).build()));

            apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id(apiId).hrid(API_HRID).build()));
            expectNoContent(HRID);
        }

        @Test
        void should_delete_subscription_and_return_no_content_known_legacy_id() {
            subscriptionCrudService.initWith(
                List.of(
                    SubscriptionFixtures.aSubscription()
                        .toBuilder()
                        .id(SUBSCRIPTION_ID)
                        .referenceId(API_HRID)
                        .applicationId(APPLICATION_HRID)
                        .planId(PLAN_HRID)
                        .build()
                )
            );
            applicationCrudService.initWith(List.of(BaseApplicationEntity.builder().id(APPLICATION_HRID).hrid(APPLICATION_HRID).build()));

            apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id(API_HRID).hrid(API_HRID).build()));
            expectNoContent(SUBSCRIPTION_ID, true);
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            expectNotFound("unknown");
        }

        private void expectNoContent(String hrid) {
            expectNoContent(hrid, false);
        }

        private void expectNoContent(String hrid, boolean legacy) {
            try (
                var response = rootTarget().queryParam("legacyID", legacy).queryParam("legacyApiID", legacy).path(hrid).request().delete()
            ) {
                assertThat(response.getStatus()).isEqualTo(204);
            }
        }

        private void expectNotFound(String hrid) {
            try (var response = rootTarget().path(hrid).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Nested
    class PutDeletePut {

        @Test
        void should_handle_put_delete_put_lifecycle() {
            when(importSubscriptionSpecUseCase.execute(any(ImportSubscriptionSpecUseCase.Input.class))).thenReturn(
                new ImportSubscriptionSpecUseCase.Output(
                    SubscriptionCRDStatus.builder()
                        .id(
                            HRIDToUUID.subscription()
                                .context(new ExecutionContext(ORGANIZATION, ENVIRONMENT))
                                .api(API_HRID)
                                .subscription(HRID)
                                .id()
                        )
                        .startingAt(Instant.now().atZone(ZoneOffset.UTC))
                        .status(ACCEPTED.name())
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .build()
                )
            );

            String applicationId = HRIDToUUID.application()
                .context(new ExecutionContext(ORGANIZATION, ENVIRONMENT))
                .hrid(APPLICATION_HRID)
                .id();
            String apiId = HRIDToUUID.api().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).hrid(API_HRID).id();
            subscriptionCrudService.initWith(
                List.of(
                    SubscriptionFixtures.aSubscription()
                        .toBuilder()
                        .id(
                            HRIDToUUID.subscription()
                                .context(new ExecutionContext(ORGANIZATION, ENVIRONMENT))
                                .api(API_HRID)
                                .subscription(HRID)
                                .id()
                        )
                        .referenceId(apiId)
                        .applicationId(applicationId)
                        .planId(
                            HRIDToUUID.plan().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).api(API_HRID).plan(PLAN_HRID).id()
                        )
                        .build()
                )
            );
            applicationCrudService.initWith(List.of(BaseApplicationEntity.builder().id(applicationId).hrid(APPLICATION_HRID).build()));

            apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id(apiId).hrid(API_HRID).build()));
            // PUT: create/update subscription
            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("subscription-with-hrid.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
            }

            // DELETE: close subscription
            try (var response = rootTarget().path(HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
            }

            // PUT again: should succeed (restore + update at domain service level)
            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("subscription-with-hrid.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
            }
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis/" + API_HRID + "/subscriptions";
    }
}
