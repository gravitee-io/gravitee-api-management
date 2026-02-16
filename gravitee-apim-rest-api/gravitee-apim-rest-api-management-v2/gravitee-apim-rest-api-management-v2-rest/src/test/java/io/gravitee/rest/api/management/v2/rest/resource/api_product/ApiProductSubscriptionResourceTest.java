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
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.ApplicationFixtures;
import fixtures.SubscriptionFixtures;
import fixtures.core.model.ApiKeyFixtures;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.use_case.AcceptSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.CloseSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.GetSubscriptionsUseCase;
import io.gravitee.apim.core.subscription.use_case.RejectSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.UpdateSubscriptionUseCase;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionApiKeysResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.TransferSubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory;

    @Inject
    private ApplicationCrudServiceInMemory applicationCrudServiceInMemory;

    @Inject
    private SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory;

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
            closeSubscriptionUseCase,
            subscriptionService,
            apiKeyService,
            applicationService
        );
    }

    private static io.gravitee.rest.api.model.SubscriptionEntity anApiProductSubscriptionEntity() {
        return SubscriptionFixtures.aSubscriptionEntity()
            .toBuilder()
            .id(SUBSCRIPTION_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType("API_PRODUCT")
            .application("app-1")
            .build();
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

    @Nested
    class PauseSubscriptionTest {

        @Test
        void should_return_404_if_not_found() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

            Response response = rootTarget().path("_pause").request().post(null);

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
            verify(subscriptionService, never()).pause(any(), any());
        }

        @Test
        void should_return_404_if_subscription_not_for_this_api_product() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(
                SubscriptionFixtures.aSubscriptionEntity()
                    .toBuilder()
                    .id(SUBSCRIPTION_ID)
                    .referenceId("other-product")
                    .referenceType("API_PRODUCT")
                    .build()
            );

            Response response = rootTarget().path("_pause").request().post(null);

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
            verify(subscriptionService, never()).pause(any(), any());
        }

        @Test
        void should_pause_subscription() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(anApiProductSubscriptionEntity());
            io.gravitee.rest.api.model.SubscriptionEntity paused = anApiProductSubscriptionEntity();
            paused.setStatus(SubscriptionStatus.PAUSED);
            when(subscriptionService.pause(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(paused);

            Response response = rootTarget().path("_pause").request().post(null);

            assertThat(response.getStatus()).isEqualTo(OK_200);
            verify(subscriptionService, times(1)).pause(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("_pause").request().post(null)
            );
        }
    }

    @Nested
    class ResumeSubscriptionTest {

        @Test
        void should_return_404_if_not_found() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

            Response response = rootTarget().path("_resume").request().post(null);

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
            verify(subscriptionService, never()).resume(any(), any());
        }

        @Test
        void should_resume_subscription() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(anApiProductSubscriptionEntity());
            when(subscriptionService.resume(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(
                anApiProductSubscriptionEntity()
            );

            Response response = rootTarget().path("_resume").request().post(null);

            assertThat(response.getStatus()).isEqualTo(OK_200);
            verify(subscriptionService, times(1)).resume(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("_resume").request().post(null)
            );
        }
    }

    @Nested
    class ResumeFailureSubscriptionTest {

        @Test
        void should_return_404_if_not_found() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

            Response response = rootTarget().path("_resumeFailure").request().post(null);

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
            verify(subscriptionService, never()).resumeFailed(any(), any());
        }

        @Test
        void should_resume_failed_subscription() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(anApiProductSubscriptionEntity());
            when(subscriptionService.resumeFailed(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(
                anApiProductSubscriptionEntity()
            );

            Response response = rootTarget().path("_resumeFailure").request().post(null);

            assertThat(response.getStatus()).isEqualTo(OK_200);
            verify(subscriptionService, times(1)).resumeFailed(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("_resumeFailure").request().post(null)
            );
        }
    }

    @Nested
    class TransferSubscriptionTest {

        @Test
        void should_return_404_if_not_found() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

            Response response = rootTarget().path("_transfer").request().post(json(SubscriptionFixtures.aTransferSubscription()));

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
            verify(subscriptionService, never()).transfer(any(), any(), any());
        }

        @Test
        void should_transfer_subscription() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(anApiProductSubscriptionEntity());
            when(
                subscriptionService.transfer(eq(GraviteeContext.getExecutionContext()), any(TransferSubscriptionEntity.class), any())
            ).thenReturn(anApiProductSubscriptionEntity());

            var transferPayload = SubscriptionFixtures.aTransferSubscription();
            Response response = rootTarget().path("_transfer").request().post(json(transferPayload));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            verify(subscriptionService, times(1)).transfer(
                eq(GraviteeContext.getExecutionContext()),
                Mockito.argThat(
                    (TransferSubscriptionEntity e) -> transferPayload.getPlanId().equals(e.getPlan()) && SUBSCRIPTION_ID.equals(e.getId())
                ),
                any()
            );
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("_transfer").request().post(json(SubscriptionFixtures.aTransferSubscription()))
            );
        }
    }

    @Nested
    class GetApiKeysTest {

        @Test
        void should_return_404_if_subscription_not_found() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

            Response response = rootTarget().path("api-keys").request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_empty_list_when_no_api_keys() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(anApiProductSubscriptionEntity());
            when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(List.of());

            Response response = rootTarget().path("api-keys").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            SubscriptionApiKeysResponse body = response.readEntity(SubscriptionApiKeysResponse.class);
            assertThat(body.getData()).isEmpty();
        }

        @Test
        void should_return_list_of_api_keys() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(anApiProductSubscriptionEntity());
            when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(
                List.of(
                    SubscriptionFixtures.anApiKeyEntity().toBuilder().id("key-1").key("custom1").build(),
                    SubscriptionFixtures.anApiKeyEntity().toBuilder().id("key-2").key("custom2").build()
                )
            );

            Response response = rootTarget().path("api-keys").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            SubscriptionApiKeysResponse body = response.readEntity(SubscriptionApiKeysResponse.class);
            assertThat(body.getData()).hasSize(2);
            assertThat(body.getData().get(0).getId()).isEqualTo("key-1");
            assertThat(body.getData().get(1).getId()).isEqualTo("key-2");
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.READ, () ->
                rootTarget().path("api-keys").request().get()
            );
        }
    }

    @Nested
    class RenewApiKeysTest {

        @BeforeEach
        void setUp() {
            when(
                parameterService.findAsBoolean(
                    GraviteeContext.getExecutionContext(),
                    io.gravitee.rest.api.model.parameters.Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED,
                    io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
                )
            ).thenReturn(true);
        }

        @Test
        void should_return_400_if_custom_api_key_not_allowed() {
            when(
                parameterService.findAsBoolean(
                    GraviteeContext.getExecutionContext(),
                    io.gravitee.rest.api.model.parameters.Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED,
                    io.gravitee.rest.api.model.parameters.ParameterReferenceType.ENVIRONMENT
                )
            ).thenReturn(false);

            Response response = rootTarget().path("api-keys/_renew").request().post(json(SubscriptionFixtures.aRenewApiKey()));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            Error error = response.readEntity(Error.class);
            assertThat(error.getMessage()).isEqualTo("You are not allowed to provide a custom API Key");
        }

        @Test
        void should_return_404_if_subscription_not_found() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

            Response response = rootTarget().path("api-keys/_renew").request().post(json(SubscriptionFixtures.aRenewApiKey()));

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_renew_api_key() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(anApiProductSubscriptionEntity());
            when(applicationService.findById(GraviteeContext.getExecutionContext(), "app-1")).thenReturn(
                ApplicationFixtures.anApplicationEntity().toBuilder().id("app-1").apiKeyMode(ApiKeyMode.EXCLUSIVE).build()
            );
            io.gravitee.rest.api.model.ApiKeyEntity renewed = SubscriptionFixtures.anApiKeyEntity().toBuilder().id("new-key").build();
            when(apiKeyService.renew(any(), any(), any())).thenReturn(renewed);

            Response response = rootTarget().path("api-keys/_renew").request().post(json(SubscriptionFixtures.aRenewApiKey()));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            verify(apiKeyService).renew(eq(GraviteeContext.getExecutionContext()), any(), eq("custom"));
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("api-keys/_renew").request().post(json(SubscriptionFixtures.aRenewApiKey()))
            );
        }
    }

    @Nested
    class UpdateApiKeyTest {

        private static final String API_KEY_ID = "api-key-1";

        @Test
        void should_return_404_if_subscription_not_found() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

            Response response = rootTarget().path("api-keys/" + API_KEY_ID).request().put(json(SubscriptionFixtures.anUpdateApiKey()));

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_404_if_api_key_not_for_subscription() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(anApiProductSubscriptionEntity());
            when(applicationService.findById(GraviteeContext.getExecutionContext(), "app-1")).thenReturn(
                ApplicationFixtures.anApplicationEntity().toBuilder().id("app-1").build()
            );
            io.gravitee.rest.api.model.ApiKeyEntity apiKey = SubscriptionFixtures.anApiKeyEntity()
                .toBuilder()
                .id(API_KEY_ID)
                .subscriptions(Set.of(SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("other-sub").build()))
                .build();
            when(apiKeyService.findById(GraviteeContext.getExecutionContext(), API_KEY_ID)).thenReturn(apiKey);

            Response response = rootTarget().path("api-keys/" + API_KEY_ID).request().put(json(SubscriptionFixtures.anUpdateApiKey()));

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
            Error error = response.readEntity(Error.class);
            assertEquals("No API Key can be found.", error.getMessage());
        }

        @Test
        void should_update_api_key() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(anApiProductSubscriptionEntity());
            when(applicationService.findById(GraviteeContext.getExecutionContext(), "app-1")).thenReturn(
                ApplicationFixtures.anApplicationEntity().toBuilder().id("app-1").build()
            );
            io.gravitee.rest.api.model.ApiKeyEntity apiKey = SubscriptionFixtures.anApiKeyEntity()
                .toBuilder()
                .id(API_KEY_ID)
                .subscriptions(Set.of(SubscriptionFixtures.aSubscriptionEntity().toBuilder().id(SUBSCRIPTION_ID).build()))
                .build();
            when(apiKeyService.findById(GraviteeContext.getExecutionContext(), API_KEY_ID)).thenReturn(apiKey);
            when(apiKeyService.update(any(), any())).thenReturn(apiKey);

            Response response = rootTarget().path("api-keys/" + API_KEY_ID).request().put(json(SubscriptionFixtures.anUpdateApiKey()));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            verify(apiKeyService).update(eq(GraviteeContext.getExecutionContext()), any());
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("api-keys/" + API_KEY_ID).request().put(json(SubscriptionFixtures.anUpdateApiKey()))
            );
        }
    }

    @Nested
    class RevokeApiKeyTest {

        private static final String API_KEY_ID = "api-key-1";

        @Test
        void should_return_404_when_use_case_throws_subscription_not_found() {
            apiKeyCrudServiceInMemory.initWith(
                List.of(
                    ApiKeyFixtures.anApiKey()
                        .toBuilder()
                        .id(API_KEY_ID)
                        .applicationId("app-1")
                        .subscriptions(List.of(SUBSCRIPTION_ID))
                        .build()
                )
            );
            applicationCrudServiceInMemory.initWith(
                List.of(BaseApplicationEntity.builder().id("app-1").apiKeyMode(ApiKeyMode.EXCLUSIVE).build())
            );
            // No subscription in memory -> use case throws SubscriptionNotFoundException

            Response response = rootTarget().path("api-keys/" + API_KEY_ID + "/_revoke").request().post(null);

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_revoke_api_key() {
            subscriptionCrudServiceInMemory.initWith(
                List.of(
                    fixtures.core.model.SubscriptionFixtures.aSubscription()
                        .toBuilder()
                        .id(SUBSCRIPTION_ID)
                        .referenceId(API_PRODUCT_ID)
                        .referenceType(SubscriptionReferenceType.API_PRODUCT)
                        .applicationId("app-1")
                        .planId("plan-1")
                        .build()
                )
            );
            applicationCrudServiceInMemory.initWith(
                List.of(BaseApplicationEntity.builder().id("app-1").apiKeyMode(ApiKeyMode.EXCLUSIVE).build())
            );
            apiKeyCrudServiceInMemory.initWith(
                List.of(
                    ApiKeyFixtures.anApiKey()
                        .toBuilder()
                        .id(API_KEY_ID)
                        .applicationId("app-1")
                        .subscriptions(List.of(SUBSCRIPTION_ID))
                        .build()
                )
            );

            Response response = rootTarget().path("api-keys/" + API_KEY_ID + "/_revoke").request().post(null);

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var apiKeyResponse = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.ApiKey.class);
            assertThat(apiKeyResponse.getId()).isEqualTo(API_KEY_ID);
            assertThat(apiKeyResponse.getRevoked()).isTrue();
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("api-keys/" + API_KEY_ID + "/_revoke").request().post(null)
            );
        }
    }

    @Nested
    class ReactivateApiKeyTest {

        private static final String API_KEY_ID = "api-key-1";

        @Test
        void should_return_404_if_subscription_not_found() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

            Response response = rootTarget().path("api-keys/" + API_KEY_ID + "/_reactivate").request().post(null);

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_404_if_api_key_not_for_subscription() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(anApiProductSubscriptionEntity());
            io.gravitee.rest.api.model.ApiKeyEntity apiKey = SubscriptionFixtures.anApiKeyEntity()
                .toBuilder()
                .id(API_KEY_ID)
                .subscriptions(Set.of(SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("other-sub").build()))
                .build();
            when(apiKeyService.findById(GraviteeContext.getExecutionContext(), API_KEY_ID)).thenReturn(apiKey);

            Response response = rootTarget().path("api-keys/" + API_KEY_ID + "/_reactivate").request().post(null);

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_reactivate_api_key() {
            when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(anApiProductSubscriptionEntity());
            io.gravitee.rest.api.model.ApiKeyEntity apiKey = SubscriptionFixtures.anApiKeyEntity()
                .toBuilder()
                .id(API_KEY_ID)
                .subscriptions(Set.of(SubscriptionFixtures.aSubscriptionEntity().toBuilder().id(SUBSCRIPTION_ID).build()))
                .build();
            when(applicationService.findById(GraviteeContext.getExecutionContext(), apiKey.getApplication().getId())).thenReturn(
                ApplicationFixtures.anApplicationEntity().toBuilder().id(apiKey.getApplication().getId()).build()
            );
            when(apiKeyService.findById(GraviteeContext.getExecutionContext(), API_KEY_ID)).thenReturn(apiKey);
            when(apiKeyService.reactivate(any(), any())).thenReturn(apiKey);

            Response response = rootTarget().path("api-keys/" + API_KEY_ID + "/_reactivate").request().post(null);

            assertThat(response.getStatus()).isEqualTo(OK_200);
            verify(apiKeyService).reactivate(eq(GraviteeContext.getExecutionContext()), eq(apiKey));
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().path("api-keys/" + API_KEY_ID + "/_reactivate").request().post(null)
            );
        }
    }
}
