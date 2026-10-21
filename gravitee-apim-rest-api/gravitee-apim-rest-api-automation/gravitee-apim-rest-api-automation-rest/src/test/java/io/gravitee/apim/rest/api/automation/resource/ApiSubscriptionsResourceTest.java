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
import static io.gravitee.rest.api.model.permissions.RolePermission.API_SUBSCRIPTION;
import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_SUBSCRIPTION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.SubscriptionFixtures;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDStatus;
import io.gravitee.apim.core.subscription.use_case.ImportSubscriptionSpecUseCase;
import io.gravitee.apim.rest.api.automation.model.SubscriptionState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApiSubscriptionsResourceTest extends AbstractResourceTest {

    @Autowired
    private ImportSubscriptionSpecUseCase importSubscriptionSpecUseCase;

    @Autowired
    private SubscriptionCrudServiceInMemory subscriptionCrudService;

    @AfterEach
    void tearDown() {
        reset(importSubscriptionSpecUseCase);
    }

    static final String API_HRID = "api-hrid";

    @Nested
    class Run {

        @Test
        void should_return_state_from_hrid() {
            when(importSubscriptionSpecUseCase.execute(any(ImportSubscriptionSpecUseCase.Input.class))).thenReturn(
                new ImportSubscriptionSpecUseCase.Output(
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
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }

        @Test
        void should_import_on_the_path_api() {
            when(importSubscriptionSpecUseCase.execute(any(ImportSubscriptionSpecUseCase.Input.class))).thenReturn(
                new ImportSubscriptionSpecUseCase.Output(
                    SubscriptionCRDStatus.builder()
                        .startingAt(Instant.now().atZone(ZoneOffset.UTC))
                        .status(ACCEPTED.name())
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .build()
                )
            );

            expectEntity("subscription-with-hrid.json");

            String apiId = IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), API_HRID).buildId();
            verify(importSubscriptionSpecUseCase).execute(argThat(input -> apiId.equals(input.apiId())));
        }

        @Test
        void should_return_state_from_legacy_id() {
            when(importSubscriptionSpecUseCase.execute(any(ImportSubscriptionSpecUseCase.Input.class))).thenReturn(
                new ImportSubscriptionSpecUseCase.Output(
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
    }

    @Nested
    class Authorization {

        private final ExecutionContext executionContext = new ExecutionContext(ORGANIZATION, ENVIRONMENT);
        private final String apiId = IdBuilder.builder(executionContext, API_HRID).buildId();
        private final String subscriptionId = IdBuilder.builder(executionContext, API_HRID).withExtraId("subscription_hrid").buildId();
        private final String applicationId = IdBuilder.builder(executionContext, "application_hrid").buildId();

        @BeforeEach
        void setUp() {
            when(importSubscriptionSpecUseCase.execute(any(ImportSubscriptionSpecUseCase.Input.class))).thenReturn(
                new ImportSubscriptionSpecUseCase.Output(
                    SubscriptionCRDStatus.builder()
                        .startingAt(Instant.now().atZone(ZoneOffset.UTC))
                        .status(ACCEPTED.name())
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .build()
                )
            );
        }

        @AfterEach
        void tearDown() {
            subscriptionCrudService.reset();
            reset(permissionService);
        }

        @Test
        void should_return_403_when_the_caller_cannot_update_the_existing_subscription() {
            givenExistingSubscription();
            deny(UPDATE);

            assertThat(put()).isEqualTo(403);
            verify(importSubscriptionSpecUseCase, never()).execute(any());
        }

        @Test
        void should_update_the_existing_subscription_with_update_and_no_create() {
            givenExistingSubscription();
            deny(CREATE);

            assertThat(put()).isEqualTo(200);
            verify(importSubscriptionSpecUseCase).execute(any());
        }

        @Test
        void should_return_403_when_the_caller_cannot_create_a_new_subscription() {
            deny(CREATE);

            assertThat(put()).isEqualTo(403);
            verify(importSubscriptionSpecUseCase, never()).execute(any());
        }

        @Test
        void should_create_a_new_subscription_with_create_and_no_update() {
            deny(UPDATE);

            assertThat(put()).isEqualTo(200);
            verify(importSubscriptionSpecUseCase).execute(any());
        }

        @Test
        void should_return_403_when_the_caller_cannot_subscribe_the_application() {
            denyApplicationSubscription();

            assertThat(put()).isEqualTo(403);
            verify(importSubscriptionSpecUseCase, never()).execute(any());
        }

        @Test
        void should_update_a_closed_subscription_without_the_application_permission() {
            givenExistingSubscription(SubscriptionEntity.Status.CLOSED);
            denyApplicationSubscription();

            assertThat(put()).isEqualTo(200);
            verify(importSubscriptionSpecUseCase).execute(any());
        }

        @Test
        void should_update_an_open_subscription_without_the_application_permission() {
            givenExistingSubscription();
            denyApplicationSubscription();

            assertThat(put()).isEqualTo(200);
            verify(importSubscriptionSpecUseCase).execute(any());
        }

        private void givenExistingSubscription() {
            givenExistingSubscription(SubscriptionEntity.Status.ACCEPTED);
        }

        private void givenExistingSubscription(SubscriptionEntity.Status status) {
            subscriptionCrudService.initWith(
                List.of(
                    SubscriptionFixtures.aSubscription()
                        .toBuilder()
                        .id(subscriptionId)
                        .apiId(apiId)
                        .applicationId(applicationId)
                        .status(status)
                        .build()
                )
            );
        }

        private void denyApplicationSubscription() {
            when(permissionService.hasPermission(any(), eq(APPLICATION_SUBSCRIPTION), eq(applicationId), eq(CREATE))).thenReturn(false);
        }

        private void deny(RolePermissionAction action) {
            when(permissionService.hasPermission(any(), eq(API_SUBSCRIPTION), eq(apiId), eq(action))).thenReturn(false);
        }

        private int put() {
            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("subscription-with-hrid.json")))
            ) {
                return response.getStatus();
            }
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis/" + API_HRID + "/subscriptions";
    }

    private SubscriptionState expectEntity(String spec) {
        return expectEntity(spec, false);
    }

    private SubscriptionState expectEntity(String spec, boolean legacy) {
        try (
            var response = rootTarget()
                .queryParam("legacy", legacy)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(readJSON(spec)))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            return response.readEntity(SubscriptionState.class);
        }
    }
}
