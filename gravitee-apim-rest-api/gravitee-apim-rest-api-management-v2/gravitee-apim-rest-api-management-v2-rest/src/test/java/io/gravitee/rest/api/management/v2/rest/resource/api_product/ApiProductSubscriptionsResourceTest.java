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
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.use_case.CreateSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.GetSubscriptionsUseCase;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApiProductSubscriptionsResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";

    @Inject
    private GetSubscriptionsUseCase getSubscriptionsUseCase;

    @Inject
    private CreateSubscriptionUseCase createSubscriptionUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/api-products/" + API_PRODUCT_ID + "/subscriptions";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(getSubscriptionsUseCase, createSubscriptionUseCase);
    }

    @Nested
    class ListSubscriptionsTest {

        @Test
        void should_return_empty_list_when_no_subscriptions() {
            when(getSubscriptionsUseCase.execute(any())).thenReturn(GetSubscriptionsUseCase.Output.multiple(List.of()));

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.SubscriptionsResponse.class);
            assertThat(body.getData()).isEmpty();

            var captor = ArgumentCaptor.forClass(GetSubscriptionsUseCase.Input.class);
            verify(getSubscriptionsUseCase).execute(captor.capture());
            assertThat(captor.getValue().referenceId()).isEqualTo(API_PRODUCT_ID);
            assertThat(captor.getValue().referenceType()).isEqualTo(SubscriptionReferenceType.API_PRODUCT);
        }

        @Test
        void should_return_subscriptions_list() {
            SubscriptionEntity sub = SubscriptionEntity.builder()
                .id("sub-1")
                .apiId(null)
                .referenceId(API_PRODUCT_ID)
                .referenceType(SubscriptionReferenceType.API_PRODUCT)
                .planId("plan-1")
                .applicationId("app-1")
                .status(SubscriptionEntity.Status.ACCEPTED)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
            when(getSubscriptionsUseCase.execute(any())).thenReturn(GetSubscriptionsUseCase.Output.multiple(List.of(sub)));

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.SubscriptionsResponse.class);
            assertThat(body.getData()).hasSize(1);
            assertThat(body.getData().get(0).getId()).isEqualTo("sub-1");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.READ, () ->
                rootTarget().request().get()
            );
        }
    }

    @Nested
    class CreateSubscriptionTest {

        @Test
        void should_create_subscription() {
            SubscriptionEntity created = SubscriptionEntity.builder()
                .id("new-sub-id")
                .apiId(null)
                .referenceId(API_PRODUCT_ID)
                .referenceType(SubscriptionReferenceType.API_PRODUCT)
                .planId("plan-1")
                .applicationId("app-1")
                .status(SubscriptionEntity.Status.PENDING)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
            when(createSubscriptionUseCase.execute(any())).thenReturn(new CreateSubscriptionUseCase.Output(created));

            var createPayload = new io.gravitee.rest.api.management.v2.rest.model.CreateSubscription();
            createPayload.setPlanId("plan-1");
            createPayload.setApplicationId("app-1");

            Response response = rootTarget().request().post(json(createPayload));

            MAPIAssertions.assertThat(response).hasStatus(CREATED_201);
            assertThat(response.getLocation().getPath()).contains("new-sub-id");

            var captor = ArgumentCaptor.forClass(CreateSubscriptionUseCase.Input.class);
            verify(createSubscriptionUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(captor.getValue().referenceId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(captor.getValue().referenceType()).isEqualTo(SubscriptionReferenceType.API_PRODUCT);
                soft.assertThat(captor.getValue().planId()).isEqualTo("plan-1");
                soft.assertThat(captor.getValue().applicationId()).isEqualTo("app-1");
            });
        }

        @Test
        void should_return_400_when_custom_api_key_not_allowed() {
            when(parameterService.findAsBoolean(any(), any(), any())).thenReturn(false);

            var createPayload = new io.gravitee.rest.api.management.v2.rest.model.CreateSubscription();
            createPayload.setPlanId("plan-1");
            createPayload.setApplicationId("app-1");
            createPayload.setCustomApiKey("custom-key");

            Response response = rootTarget().request().post(json(createPayload));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.CREATE, () ->
                rootTarget()
                    .request()
                    .post(
                        json(new io.gravitee.rest.api.management.v2.rest.model.CreateSubscription().planId("plan-1").applicationId("app-1"))
                    )
            );
        }
    }
}
