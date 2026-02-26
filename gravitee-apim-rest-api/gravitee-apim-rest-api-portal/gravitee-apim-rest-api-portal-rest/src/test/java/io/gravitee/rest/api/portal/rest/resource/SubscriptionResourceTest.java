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
package io.gravitee.rest.api.portal.rest.resource;

import static jakarta.ws.rs.client.Entity.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.SubscriptionConsumerStatus;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionConfigurationInput;
import io.gravitee.rest.api.portal.rest.model.UpdateSubscriptionInput;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import io.gravitee.rest.api.service.v4.exception.SubscriptionMetadataInvalidException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
class SubscriptionResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "subscriptions/";
    }

    private static final String SUBSCRIPTION = "my-subscription";
    private static final String UNKNOWN_SUBSCRIPTION = "unknown-subscription";
    private static final String API = "my-api";
    private static final String APPLICATION = "my-application";
    private static final String PLAN = "my-plan";
    private SubscriptionEntity subscriptionEntity;

    @BeforeEach
    void init() {
        resetAllMocks();

        subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION);
        subscriptionEntity.setApi(API);
        subscriptionEntity.setApplication(APPLICATION);
        subscriptionEntity.setPlan(PLAN);
        subscriptionEntity.setStatus(SubscriptionStatus.ACCEPTED);

        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(subscriptionEntity);
        when(subscriptionService.findById(UNKNOWN_SUBSCRIPTION)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION));

        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION)).thenReturn(List.of(new ApiKeyEntity()));

        when(keyMapper.convert(any(ApiKeyEntity.class))).thenReturn(new Key());
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Nested
    class GetSubscription {

        @Test
        void shouldGetSubscription() {
            Response response = target(SUBSCRIPTION).request().get();
            assertEquals(HttpStatusCode.OK_200, response.getStatus());

            Subscription subscription = response.readEntity(Subscription.class);
            assertNotNull(subscription);
            assertNull(subscription.getKeys());
        }

        @Test
        void testPermissionsForGettingASubscription() {
            reset(permissionService);

            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_SUBSCRIPTION,
                    API,
                    RolePermissionAction.READ
                )
            ).thenReturn(true);
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.APPLICATION_SUBSCRIPTION,
                    APPLICATION,
                    RolePermissionAction.READ
                )
            ).thenReturn(true);
            assertEquals(HttpStatusCode.OK_200, target(SUBSCRIPTION).request().get().getStatus());

            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_SUBSCRIPTION,
                    API,
                    RolePermissionAction.READ
                )
            ).thenReturn(true);
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.APPLICATION_SUBSCRIPTION,
                    APPLICATION,
                    RolePermissionAction.READ
                )
            ).thenReturn(false);
            assertEquals(HttpStatusCode.OK_200, target(SUBSCRIPTION).request().get().getStatus());

            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_SUBSCRIPTION,
                    API,
                    RolePermissionAction.READ
                )
            ).thenReturn(false);
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.APPLICATION_SUBSCRIPTION,
                    APPLICATION,
                    RolePermissionAction.READ
                )
            ).thenReturn(true);
            assertEquals(HttpStatusCode.OK_200, target(SUBSCRIPTION).request().get().getStatus());

            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_SUBSCRIPTION,
                    API,
                    RolePermissionAction.READ
                )
            ).thenReturn(false);
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.APPLICATION_SUBSCRIPTION,
                    APPLICATION,
                    RolePermissionAction.READ
                )
            ).thenReturn(false);
            assertEquals(HttpStatusCode.FORBIDDEN_403, target(SUBSCRIPTION).request().get().getStatus());
        }

        @Test
        void shouldGetSubscriptionWithKeys() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_SUBSCRIPTION,
                    API,
                    RolePermissionAction.READ
                )
            ).thenReturn(true);
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.APPLICATION_SUBSCRIPTION,
                    APPLICATION,
                    RolePermissionAction.READ
                )
            ).thenReturn(true);

            final Response response = target(SUBSCRIPTION).queryParam("include", "keys").request().get();
            assertEquals(HttpStatusCode.OK_200, response.getStatus());

            Subscription subscription = response.readEntity(Subscription.class);
            assertNotNull(subscription);
            assertNotNull(subscription.getKeys());
            assertFalse(subscription.getKeys().isEmpty());
        }

        @Test
        void shouldGetSubscriptionWithConsumerStatus() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.API_SUBSCRIPTION,
                    API,
                    RolePermissionAction.READ
                )
            ).thenReturn(true);
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.APPLICATION_SUBSCRIPTION,
                    APPLICATION,
                    RolePermissionAction.READ
                )
            ).thenReturn(true);

            SubscriptionConfigurationEntity subscriptionConfigurationEntity = new SubscriptionConfigurationEntity();
            subscriptionConfigurationEntity.setEntrypointId("entrypointId");
            subscriptionConfigurationEntity.setChannel("channel");
            subscriptionConfigurationEntity.setEntrypointConfiguration(
                "{\"auth\":{\"type\":\"none\"},\"callbackUrl\":\"https://webhook.example/1234\",\"ssl\":{\"keyStore\":{\"type\":\"\"},\"hostnameVerifier\":false,\"trustStore\":{\"type\":\"\"},\"trustAll\":true},\"retry\":{\"retryOption\":\"No Retry\"}}"
            );
            subscriptionEntity.setConfiguration(subscriptionConfigurationEntity);

            final Response response = target(SUBSCRIPTION).queryParam("include", "consumerConfiguration").request().get();
            assertEquals(HttpStatusCode.OK_200, response.getStatus());

            Subscription subscription = response.readEntity(Subscription.class);
            assertNotNull(subscription);
            assertNotNull(subscription.getConsumerConfiguration());
        }

        @Test
        void shouldNotGetSubscription() {
            final Response response = target(UNKNOWN_SUBSCRIPTION).request().get();
            assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());

            ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
            List<Error> errors = errorResponse.getErrors();
            assertNotNull(errors);
        }
    }

    @Nested
    class CloseSubscription {

        @Test
        void testPermissionsForClosingASubscription() {
            reset(permissionService);

            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.APPLICATION_SUBSCRIPTION,
                    APPLICATION,
                    RolePermissionAction.DELETE
                )
            ).thenReturn(false);
            assertEquals(HttpStatusCode.FORBIDDEN_403, target(SUBSCRIPTION).path("_close").request().post(null).getStatus());
        }
    }

    @Nested
    class UpdateSubscription {

        @Test
        void shouldNotUpdateSubscriptionConfigurationCauseNotFound() {
            SubscriptionConfigurationInput subscriptionConfigurationInput = new SubscriptionConfigurationInput();
            Response response = target(UNKNOWN_SUBSCRIPTION).request().put(json(subscriptionConfigurationInput));

            assertEquals(404, response.getStatus());
            verify(subscriptionService).findById(UNKNOWN_SUBSCRIPTION);
            verifyNoMoreInteractions(subscriptionService);
        }

        @Test
        void shouldNotUpdateSubscriptionConfigurationCauseInsufficientPermissions() {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

            SubscriptionConfigurationInput subscriptionConfigurationInput = new SubscriptionConfigurationInput();
            Response response = target(SUBSCRIPTION).request().put(json(subscriptionConfigurationInput));

            assertEquals(403, response.getStatus());
            verify(subscriptionService).findById(SUBSCRIPTION);
            verifyNoMoreInteractions(subscriptionService);
        }

        @Test
        void shouldUpdateSubscriptionConfiguration() {
            UpdateSubscriptionInput updateSubscriptionInput = new UpdateSubscriptionInput();
            SubscriptionConfigurationInput subscriptionConfigurationInput = new SubscriptionConfigurationInput();
            subscriptionConfigurationInput.setEntrypointConfiguration("{\"url\":\"my-url\"}");
            updateSubscriptionInput.setConfiguration(subscriptionConfigurationInput);

            SubscriptionConfigurationEntity subscriptionConfigurationEntity = new SubscriptionConfigurationEntity();
            subscriptionConfigurationEntity.setEntrypointConfiguration("{\"url\":\"my-url\"}");
            subscriptionEntity.setConfiguration(subscriptionConfigurationEntity);
            when(
                subscriptionService.update(eq(GraviteeContext.getExecutionContext()), any(UpdateSubscriptionConfigurationEntity.class))
            ).thenReturn(subscriptionEntity);

            Response response = target(SUBSCRIPTION).request().put(json(updateSubscriptionInput));

            assertEquals(200, response.getStatus());

            SubscriptionEntity subscriptionEntityResponse = response.readEntity(SubscriptionEntity.class);
            assertEquals("{\"url\":\"my-url\"}", subscriptionEntityResponse.getConfiguration().getEntrypointConfiguration());

            ArgumentCaptor<UpdateSubscriptionConfigurationEntity> subscriptionCaptor = ArgumentCaptor.forClass(
                UpdateSubscriptionConfigurationEntity.class
            );
            verify(subscriptionService).update(eq(GraviteeContext.getExecutionContext()), subscriptionCaptor.capture());
            assertEquals(SUBSCRIPTION, subscriptionCaptor.getValue().getSubscriptionId());
        }

        @Test
        void shouldReturnBadRequestWhenMetadataKeyIsInvalid() {
            doThrow(new SubscriptionMetadataInvalidException("Invalid metadata key."))
                .when(subscriptionService)
                .update(
                    eq(GraviteeContext.getExecutionContext()),
                    argThat((UpdateSubscriptionConfigurationEntity e) -> e.getMetadata() != null && e.getMetadata().containsKey("bad key"))
                );

            UpdateSubscriptionInput updateSubscriptionInput = new UpdateSubscriptionInput();
            updateSubscriptionInput.setMetadata(Map.of("bad key", "value"));

            Response response = target(SUBSCRIPTION).request().put(json(updateSubscriptionInput));

            assertEquals(400, response.getStatus());
            verify(subscriptionService, times(1)).update(
                eq(GraviteeContext.getExecutionContext()),
                any(UpdateSubscriptionConfigurationEntity.class)
            );
        }
    }

    @Nested
    class ChangeSubscriptionConsumerStatus {

        @Test
        void shouldPauseSubscriptionByConsumer() {
            Response response = target(SUBSCRIPTION).path("_changeConsumerStatus").queryParam("status", "STOPPED").request().post(null);

            assertEquals(200, response.getStatus());
            verify(subscriptionService, times(1)).pauseConsumer(GraviteeContext.getExecutionContext(), subscriptionEntity.getId());
        }

        @Test
        void shouldResumeSubscriptionByConsumer() {
            Response response = target(SUBSCRIPTION).path("_changeConsumerStatus").queryParam("status", "STARTED").request().post(null);

            assertEquals(200, response.getStatus());
            verify(subscriptionService, times(1)).resumeConsumer(GraviteeContext.getExecutionContext(), subscriptionEntity.getId());
        }

        @Test
        void shouldHaveBadRequestIfTryingAWrongConsumerStatus() {
            Response response = target(SUBSCRIPTION).path("_changeConsumerStatus").queryParam("status", "INVALID").request().post(null);

            assertEquals(400, response.getStatus());
            verify(subscriptionService, times(0)).pauseConsumer(GraviteeContext.getExecutionContext(), subscriptionEntity.getId());
            verify(subscriptionService, times(0)).resumeConsumer(GraviteeContext.getExecutionContext(), subscriptionEntity.getId());
        }

        @Test
        void shouldBeForbiddenWhenUpdatingConsumerStatus() {
            reset(permissionService);

            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.APPLICATION_SUBSCRIPTION,
                    APPLICATION,
                    RolePermissionAction.UPDATE
                )
            ).thenReturn(false);
            Response response = target(SUBSCRIPTION).path("_changeConsumerStatus").queryParam("status", "STOPPED").request().post(null);

            assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        }
    }

    @Nested
    class ResumeFailedSubscription {

        @Test
        void shouldReturn403IfIncorrectPermission() {
            reset(permissionService);

            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.APPLICATION_SUBSCRIPTION,
                    APPLICATION,
                    RolePermissionAction.UPDATE
                )
            ).thenReturn(false);
            assertEquals(HttpStatusCode.FORBIDDEN_403, target(SUBSCRIPTION).path("_resumeFailure").request().post(null).getStatus());
        }

        @ParameterizedTest
        @EnumSource(value = SubscriptionConsumerStatus.class, names = { "STARTED", "STOPPED" })
        void shouldReturnBadRequestIfTryingAWrongConsumerStatus(SubscriptionConsumerStatus status) {
            subscriptionEntity.setConsumerStatus(status);
            Response response = target(SUBSCRIPTION).path("_resumeFailure").request().post(null);

            assertEquals(400, response.getStatus());
        }

        @Test
        void shouldResumeFailedSubscription() {
            subscriptionEntity.setConsumerStatus(SubscriptionConsumerStatus.FAILURE);
            Response response = target(SUBSCRIPTION).path("_resumeFailure").request().post(null);

            verify(subscriptionService, times(1)).resumeFailed(GraviteeContext.getExecutionContext(), subscriptionEntity.getId());
            assertEquals(200, response.getStatus());
        }

        @Test
        public void shouldUpdateSubscriptionConfiguration() {
            UpdateSubscriptionInput updateSubscriptionInput = new UpdateSubscriptionInput();
            SubscriptionConfigurationInput subscriptionConfigurationInput = new SubscriptionConfigurationInput();
            subscriptionConfigurationInput.setEntrypointConfiguration("{\"url\":\"my-url\"}");
            updateSubscriptionInput.setConfiguration(subscriptionConfigurationInput);

            SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
            SubscriptionConfigurationEntity subscriptionConfigurationEntity = new SubscriptionConfigurationEntity();
            subscriptionConfigurationEntity.setEntrypointConfiguration("{\"url\":\"my-url\"}");
            subscriptionEntity.setConfiguration(subscriptionConfigurationEntity);
            when(
                subscriptionService.update(eq(GraviteeContext.getExecutionContext()), any(UpdateSubscriptionConfigurationEntity.class))
            ).thenReturn(subscriptionEntity);

            Response response = target(SUBSCRIPTION).request().put(json(updateSubscriptionInput));

            assertEquals(200, response.getStatus());

            SubscriptionEntity subscriptionEntityResponse = response.readEntity(SubscriptionEntity.class);
            assertEquals("{\"url\":\"my-url\"}", subscriptionEntityResponse.getConfiguration().getEntrypointConfiguration());

            ArgumentCaptor<UpdateSubscriptionConfigurationEntity> subscriptionCaptor = ArgumentCaptor.forClass(
                UpdateSubscriptionConfigurationEntity.class
            );
            verify(subscriptionService).update(eq(GraviteeContext.getExecutionContext()), subscriptionCaptor.capture());
            assertEquals(SUBSCRIPTION, subscriptionCaptor.getValue().getSubscriptionId());
        }

        @Test
        public void shouldPauseSubscriptionByConsumer() {
            Response response = target(SUBSCRIPTION).path("_changeConsumerStatus").queryParam("status", "STOPPED").request().post(null);

            assertEquals(200, response.getStatus());
            verify(subscriptionService, times(1)).pauseConsumer(eq(GraviteeContext.getExecutionContext()), eq(subscriptionEntity.getId()));
        }

        @Test
        public void shouldResumeSubscriptionByConsumer() {
            Response response = target(SUBSCRIPTION).path("_changeConsumerStatus").queryParam("status", "STARTED").request().post(null);

            assertEquals(200, response.getStatus());
            verify(subscriptionService, times(1)).resumeConsumer(eq(GraviteeContext.getExecutionContext()), eq(subscriptionEntity.getId()));
        }

        @Test
        public void shouldHaveBadRequestIfTryingAWrongConsumerStatus() {
            Response response = target(SUBSCRIPTION).path("_changeConsumerStatus").queryParam("status", "INVALID").request().post(null);

            assertEquals(400, response.getStatus());
            verify(subscriptionService, times(0)).pauseConsumer(eq(GraviteeContext.getExecutionContext()), eq(subscriptionEntity.getId()));
            verify(subscriptionService, times(0)).resumeConsumer(eq(GraviteeContext.getExecutionContext()), eq(subscriptionEntity.getId()));
        }

        @Test
        public void shouldBeForbiddenWhenUpdatingConsumerStatus() {
            reset(permissionService);

            doReturn(false)
                .when(permissionService)
                .hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.APPLICATION_SUBSCRIPTION),
                    eq(APPLICATION),
                    eq(RolePermissionAction.UPDATE)
                );
            Response response = target(SUBSCRIPTION).path("_changeConsumerStatus").queryParam("status", "STOPPED").request().post(null);

            assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        }

        @Test
        public void shouldReturnEmptyWhenNoAppsFound() {
            when(applicationService.findByUser(any(), any())).thenReturn(Collections.emptySet());

            final Response response = target().queryParam("apiId", API).request().get();

            assertEquals(HttpStatusCode.OK_200, response.getStatus());
        }

        @Test
        public void shouldThrowForbiddenWhenNoPermission() {
            doReturn(false).when(permissionService).hasPermission(any(), any(), any(), any());

            final Response response = target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get();

            assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        }

        @Test
        public void shouldReturnEmptyWhenNoSubscriptions() {
            doReturn(true).when(permissionService).hasPermission(any(), any(), any(), any());
            doReturn(Collections.emptyList()).when(subscriptionService).search(any(), any());

            final Response response = target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get();

            assertEquals(HttpStatusCode.OK_200, response.getStatus());
        }

        @Test
        public void shouldReturnMappedSubscriptions() {
            doReturn(true).when(permissionService).hasPermission(any(), any(), any(), any());

            SubscriptionEntity subscription = new SubscriptionEntity();
            subscription.setId("sub-id");
            subscription.setStatus(SubscriptionStatus.ACCEPTED);
            doReturn(Collections.singletonList(subscription)).when(subscriptionService).search(any(), any());
            doReturn(new Metadata()).when(subscriptionService).getMetadata(any(), any());

            final Response response = target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get();

            assertEquals(HttpStatusCode.OK_200, response.getStatus());
        }

        @Test
        public void shouldReturnAllSubscriptionsWhenPaginationIsDisabled() {
            doReturn(true).when(permissionService).hasPermission(any(), any(), any(), any());
            SubscriptionEntity subscription = new SubscriptionEntity();
            subscription.setId("sub-id");
            subscription.setStatus(SubscriptionStatus.ACCEPTED);
            doReturn(Collections.singletonList(subscription)).when(subscriptionService).search(any(), any());
            doReturn(new Metadata()).when(subscriptionService).getMetadata(any(), any());

            final Response response = target()
                .queryParam("apiId", API)
                .queryParam("applicationId", APPLICATION)
                .queryParam("size", -1)
                .request()
                .get();

            assertEquals(HttpStatusCode.OK_200, response.getStatus());
        }

        @Getter
        @AllArgsConstructor
        private class SubscriptionConfiguration {

            private String url;
        }
    }
}
