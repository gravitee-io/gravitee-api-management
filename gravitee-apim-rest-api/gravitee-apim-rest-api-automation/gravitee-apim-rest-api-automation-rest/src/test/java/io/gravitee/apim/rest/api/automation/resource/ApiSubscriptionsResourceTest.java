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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import inmemory.PlanCrudServiceInMemory;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDStatus;
import io.gravitee.apim.core.subscription.use_case.ImportSubscriptionCRDUseCase;
import io.gravitee.apim.rest.api.automation.model.SubscriptionState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApiSubscriptionsResourceTest extends AbstractResourceTest {

    @Autowired
    private ImportSubscriptionCRDUseCase importSubscriptionCRDUseCase;

    @Autowired
    private PlanCrudServiceInMemory planCrudService;

    @AfterEach
    void tearDown() {
        reset(importSubscriptionCRDUseCase, apiSearchServiceV4);
        planCrudService.reset();
    }

    static final String API_HRID = "api-hrid";
    static final String API_ID = HRIDToUUID.api().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).hrid(API_HRID).id();

    @Nested
    class Run {

        @Test
        void should_return_state_from_hrid() {
            givenPlanOnApi(hashedPlanId("plan_hrid"), API_ID);
            when(importSubscriptionCRDUseCase.execute(any(ImportSubscriptionCRDUseCase.Input.class))).thenReturn(
                new ImportSubscriptionCRDUseCase.Output(
                    SubscriptionCRDStatus.builder()
                        .startingAt(Instant.now().atZone(ZoneOffset.UTC))
                        .status(ACCEPTED.name())
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .build()
                )
            );

            var state = expectEntity("subscription-with-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getHrid()).isEqualTo("subscription_hrid");
                soft.assertThat(state.getApiHrid()).isEqualTo(API_HRID);
                soft.assertThat(state.getApplicationHrid()).isEqualTo("application_hrid");
                soft.assertThat(state.getPlanHrid()).isEqualTo("plan_hrid");
                soft.assertThat(state.getApiKeys()).hasSize(1);
                soft.assertThat(state.getApiKeys().get(0).getKey()).isEqualTo("custom-api-key");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });

            verify(importSubscriptionCRDUseCase, atMostOnce()).execute(
                argThat(input -> {
                    var consumerConfiguration = input.spec().getConsumerConfiguration();
                    return (
                        consumerConfiguration != null &&
                        "webhook".equals(consumerConfiguration.getEntrypointId()) &&
                        "/notifications".equals(consumerConfiguration.getChannel()) &&
                        Map.of(
                            "callbackUrl",
                            "https://exmaple.com/callback",
                            "headers",
                            List.of(
                                Map.of("name", "demoHeader", "value", "my-value"),
                                Map.of("name", "anotherHeader", "value", "my-value2")
                            )
                        ).equals(consumerConfiguration.getEntrypointConfiguration())
                    );
                })
            );
        }

        @Test
        void should_return_state_from_guid() {
            givenPlanOnApi(hashedPlanId("plan_id"), API_ID);
            when(importSubscriptionCRDUseCase.execute(any(ImportSubscriptionCRDUseCase.Input.class))).thenReturn(
                new ImportSubscriptionCRDUseCase.Output(
                    SubscriptionCRDStatus.builder()
                        .id("subscription_id")
                        .startingAt(Instant.now().atZone(ZoneOffset.UTC))
                        .status(ACCEPTED.name())
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .build()
                )
            );

            var state = expectEntity("subscription-with-id.json", true);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getId()).isEqualTo("subscription_id");
                soft.assertThat(state.getApiHrid()).isEqualTo(API_HRID);
                soft.assertThat(state.getApplicationHrid()).isEqualTo("application_id");
                soft.assertThat(state.getPlanHrid()).isEqualTo("plan_id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }

        @Test
        void should_return_a_404_status_code_and_not_import_a_plan_of_another_api() {
            givenPlanOnApi("plan_id", "another-api");

            try (var response = putWithApiUUID("subscription-with-id.json")) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
            verify(importSubscriptionCRDUseCase, never()).execute(any());
        }

        @Test
        void should_return_a_404_status_code_and_not_import_on_an_api_outside_the_environment() {
            givenPlanOnApi("plan_id", API_HRID);
            when(
                apiSearchServiceV4.findRepositoryApiById(
                    argThat(executionContext -> ENVIRONMENT.equals(executionContext.getEnvironmentId())),
                    eq(API_HRID)
                )
            ).thenThrow(new ApiNotFoundException(API_HRID));

            try (var response = putWithApiUUID("subscription-with-id.json")) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
            verify(importSubscriptionCRDUseCase, never()).execute(any());
        }
    }

    private void givenPlanOnApi(String planId, String apiId) {
        planCrudService.initWith(List.of(PlanFixtures.aPlanHttpV4().toBuilder().id(planId).referenceId(apiId).build()));
    }

    private static String hashedPlanId(String planHrid) {
        return HRIDToUUID.plan().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).api(API_HRID).plan(planHrid).id();
    }

    private Response putWithApiUUID(String spec) {
        return rootTarget()
            .queryParam("hridContainsUUID", true)
            .queryParam("hridContainsApiUUID", true)
            .queryParam("hridContainsAppUUID", true)
            .request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.json(readJSON(spec)));
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis/" + API_HRID + "/subscriptions";
    }

    private SubscriptionState expectEntity(String spec) {
        return expectEntity(spec, false);
    }

    private SubscriptionState expectEntity(String spec, boolean hridContainsUUID) {
        try (
            var response = rootTarget()
                .queryParam("hridContainsUUID", hridContainsUUID)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(readJSON(spec)))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            return response.readEntity(SubscriptionState.class);
        }
    }
}
