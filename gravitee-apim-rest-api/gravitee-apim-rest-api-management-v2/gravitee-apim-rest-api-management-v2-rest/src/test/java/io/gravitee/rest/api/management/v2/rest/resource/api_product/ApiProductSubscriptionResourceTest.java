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

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.use_case.AcceptSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.CloseSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.GetSubscriptionsUseCase;
import io.gravitee.apim.core.subscription.use_case.RejectSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.UpdateSubscriptionUseCase;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApiProductSubscriptionResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";
    private static final String SUBSCRIPTION_ID = "subscription-id";

    @Inject
    private GetSubscriptionsUseCase getSubscriptionsUseCase;

    @Inject
    private UpdateSubscriptionUseCase updateSubscriptionUseCase;

    @Inject
    private AcceptSubscriptionUseCase acceptSubscriptionUseCase;

    @Inject
    private RejectSubscriptionUseCase rejectSubscriptionUseCase;

    @Inject
    private CloseSubscriptionUseCase closeSubscriptionUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/api-products/" + API_PRODUCT_ID + "/subscriptions/" + SUBSCRIPTION_ID;
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
        reset(
            getSubscriptionsUseCase,
            updateSubscriptionUseCase,
            acceptSubscriptionUseCase,
            rejectSubscriptionUseCase,
            closeSubscriptionUseCase
        );
    }

    private static SubscriptionEntity aSubscription() {
        return SubscriptionEntity.builder()
            .id(SUBSCRIPTION_ID)
            .apiId(null)
            .referenceId(API_PRODUCT_ID)
            .referenceType(SubscriptionReferenceType.API_PRODUCT)
            .planId("plan-1")
            .applicationId("app-1")
            .status(SubscriptionEntity.Status.ACCEPTED)
            .createdAt(ZonedDateTime.now())
            .updatedAt(ZonedDateTime.now())
            .build();
    }

    @Nested
    class GetSubscriptionTest {

        @Test
        void should_return_subscription() {
            SubscriptionEntity sub = aSubscription();
            when(getSubscriptionsUseCase.execute(any())).thenReturn(GetSubscriptionsUseCase.Output.single(Optional.of(sub)));

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Subscription.class);
            assertThat(body.getId()).isEqualTo(SUBSCRIPTION_ID);

            var captor = ArgumentCaptor.forClass(GetSubscriptionsUseCase.Input.class);
            verify(getSubscriptionsUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(captor.getValue().referenceId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(captor.getValue().referenceType()).isEqualTo(SubscriptionReferenceType.API_PRODUCT);
                soft.assertThat(captor.getValue().subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
            });
        }

        @Test
        void should_return_404_when_subscription_not_found() {
            when(getSubscriptionsUseCase.execute(any())).thenReturn(GetSubscriptionsUseCase.Output.single(Optional.empty()));

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.READ, () ->
                rootTarget().request().get()
            );
        }
    }

    @Nested
    class UpdateSubscriptionTest {

        @Test
        void should_update_subscription() {
            SubscriptionEntity updated = aSubscription();
            when(updateSubscriptionUseCase.execute(any())).thenReturn(new UpdateSubscriptionUseCase.Output(updated));

            var updatePayload = new io.gravitee.rest.api.management.v2.rest.model.UpdateSubscription();
            updatePayload.setMetadata(java.util.Map.of("k", "v"));

            Response response = rootTarget().request().put(json(updatePayload));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var captor = ArgumentCaptor.forClass(UpdateSubscriptionUseCase.Input.class);
            verify(updateSubscriptionUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(captor.getValue().referenceId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(captor.getValue().referenceType()).isEqualTo(SubscriptionReferenceType.API_PRODUCT);
                soft.assertThat(captor.getValue().subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
            });
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().request().put(json(new io.gravitee.rest.api.management.v2.rest.model.UpdateSubscription()))
            );
        }
    }

    @Nested
    class AcceptSubscriptionTest {

        @Test
        void should_accept_subscription() {
            SubscriptionEntity accepted = aSubscription();
            doReturn(new AcceptSubscriptionUseCase.Output(accepted)).when(acceptSubscriptionUseCase).execute(any());

            var acceptPayload = new io.gravitee.rest.api.management.v2.rest.model.AcceptSubscription();

            Response response = rootTarget().path("_accept").request().post(json(acceptPayload));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var captor = ArgumentCaptor.forClass(AcceptSubscriptionUseCase.Input.class);
            verify(acceptSubscriptionUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(captor.getValue().referenceId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(captor.getValue().referenceType()).isEqualTo(SubscriptionReferenceType.API_PRODUCT);
                soft.assertThat(captor.getValue().subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
            });
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("_accept").request().post(json(new io.gravitee.rest.api.management.v2.rest.model.AcceptSubscription()))
            );
        }
    }

    @Nested
    class RejectSubscriptionTest {

        @Test
        void should_reject_subscription() {
            SubscriptionEntity rejected = aSubscription().toBuilder().status(SubscriptionEntity.Status.REJECTED).build();
            doReturn(new RejectSubscriptionUseCase.Output(rejected)).when(rejectSubscriptionUseCase).execute(any());

            var rejectPayload = new io.gravitee.rest.api.management.v2.rest.model.RejectSubscription();
            rejectPayload.setReason("Not allowed");

            Response response = rootTarget().path("_reject").request().post(json(rejectPayload));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var captor = ArgumentCaptor.forClass(RejectSubscriptionUseCase.Input.class);
            verify(rejectSubscriptionUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(captor.getValue().referenceId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(captor.getValue().referenceType()).isEqualTo(SubscriptionReferenceType.API_PRODUCT);
                soft.assertThat(captor.getValue().subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
                soft.assertThat(captor.getValue().reasonMessage()).isEqualTo("Not allowed");
            });
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("_reject").request().post(json(new io.gravitee.rest.api.management.v2.rest.model.RejectSubscription()))
            );
        }
    }

    @Nested
    class CloseSubscriptionTest {

        @Test
        void should_close_subscription() {
            SubscriptionEntity closed = aSubscription().toBuilder().status(SubscriptionEntity.Status.CLOSED).build();
            doReturn(new CloseSubscriptionUseCase.Output(closed)).when(closeSubscriptionUseCase).execute(any());

            Response response = rootTarget().path("_close").request().post(null);

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var captor = ArgumentCaptor.forClass(CloseSubscriptionUseCase.Input.class);
            verify(closeSubscriptionUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(captor.getValue().referenceId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(captor.getValue().referenceType()).isEqualTo(SubscriptionReferenceType.API_PRODUCT);
                soft.assertThat(captor.getValue().subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
            });
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("_close").request().post(null)
            );
        }
    }
}
