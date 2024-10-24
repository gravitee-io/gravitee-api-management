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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.SubscriptionFixtures;
import io.gravitee.apim.core.subscription.use_case.AcceptSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.RejectSubscriptionUseCase;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.ApiKeyMode;
import io.gravitee.rest.api.management.v2.rest.model.BaseApplication;
import io.gravitee.rest.api.management.v2.rest.model.BasePlan;
import io.gravitee.rest.api.management.v2.rest.model.CreateSubscription;
import io.gravitee.rest.api.management.v2.rest.model.Subscription;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.Objects;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiSubscriptionsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final String API = "my-api";
    private static final String PLAN = "my-plan";
    private static final String APPLICATION = "my-application";
    private static final String SUBSCRIPTION = "my-subscription";

    @Autowired
    protected SubscriptionService subscriptionService;

    @Autowired
    protected PlanSearchService planSearchService;

    @Autowired
    protected ApplicationService applicationService;

    @Autowired
    protected ApiKeyService apiKeyService;

    @Autowired
    protected UserService userService;

    @Autowired
    AcceptSubscriptionUseCase acceptSubscriptionUseCase;

    @Autowired
    RejectSubscriptionUseCase rejectSubscriptionUseCase;

    WebTarget target;

    @BeforeEach
    public void init() throws TechnicalException {
        target = rootTarget();

        when(apiRepository.findById(API)).thenReturn(Optional.of(Api.builder().id(API).environmentId(ENVIRONMENT).build()));

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Override
    @AfterEach
    public void tearDown() {
        Mockito.reset(subscriptionService, applicationService, planSearchService, parameterService, apiKeyService);
        GraviteeContext.cleanContext();

        super.tearDown();
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/subscriptions";
    }

    @Nested
    class Create {

        @Test
        public void should_create_subscription() {
            final CreateSubscription createSubscription = SubscriptionFixtures
                .aCreateSubscription()
                .toBuilder()
                .applicationId(APPLICATION)
                .planId(PLAN)
                .customApiKey(null)
                .apiKeyMode(ApiKeyMode.EXCLUSIVE)
                .build();

            when(subscriptionService.create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), any()))
                .thenReturn(
                    SubscriptionFixtures.aSubscriptionEntity().toBuilder().id(SUBSCRIPTION).application(APPLICATION).plan(PLAN).build()
                );

            final Response response = target.request().post(Entity.json(createSubscription));
            assertThat(response)
                .hasStatus(CREATED_201)
                .asEntity(Subscription.class)
                .satisfies(subscription -> {
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(subscription.getId()).isEqualTo(SUBSCRIPTION);
                        soft.assertThat(subscription.getPlan()).extracting(BasePlan::getId).isEqualTo(PLAN);
                        soft.assertThat(subscription.getApplication()).extracting(BaseApplication::getId).isEqualTo(APPLICATION);
                    });
                });

            verify(subscriptionService)
                .create(
                    eq(GraviteeContext.getExecutionContext()),
                    argThat(newSubscriptionEntity -> {
                        assertThat(newSubscriptionEntity.getPlan()).isEqualTo(PLAN);
                        assertThat(newSubscriptionEntity.getApplication()).isEqualTo(APPLICATION);
                        return true;
                    }),
                    isNull()
                );
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_SUBSCRIPTION),
                    eq(API),
                    eq(RolePermissionAction.CREATE)
                )
            )
                .thenReturn(false);

            final Response response = target.request().post(Entity.json(SubscriptionFixtures.aCreateSubscription()));

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_400_if_custom_api_key_not_enabled() {
            final CreateSubscription createSubscription = SubscriptionFixtures
                .aCreateSubscription()
                .toBuilder()
                .applicationId(APPLICATION)
                .planId(PLAN)
                .customApiKey("custom")
                .build();

            when(
                parameterService.findAsBoolean(
                    GraviteeContext.getExecutionContext(),
                    Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED,
                    ParameterReferenceType.ENVIRONMENT
                )
            )
                .thenReturn(false);

            final Response response = target.request().post(Entity.json(createSubscription));

            assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("You are not allowed to provide a custom API Key");
        }

        @Test
        public void should_create_subscription_with_custom_api_key() {
            final CreateSubscription createSubscription = SubscriptionFixtures
                .aCreateSubscription()
                .toBuilder()
                .applicationId(APPLICATION)
                .planId(PLAN)
                .customApiKey("custom")
                .build();

            when(
                parameterService.findAsBoolean(
                    GraviteeContext.getExecutionContext(),
                    Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED,
                    ParameterReferenceType.ENVIRONMENT
                )
            )
                .thenReturn(true);
            when(subscriptionService.create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), any()))
                .thenReturn(
                    SubscriptionFixtures
                        .aSubscriptionEntity()
                        .toBuilder()
                        .id(SUBSCRIPTION)
                        .application(APPLICATION)
                        .plan(PLAN)
                        .status(SubscriptionStatus.ACCEPTED)
                        .build()
                );

            final Response response = target.request().post(Entity.json(createSubscription));

            assertThat(response).hasStatus(CREATED_201);

            verify(subscriptionService).create(eq(GraviteeContext.getExecutionContext()), any(), eq("custom"));
        }

        @Test
        public void should_create_subscription_with_custom_api_key_and_auto_process_it_if_pending() {
            final CreateSubscription createSubscription = SubscriptionFixtures
                .aCreateSubscription()
                .toBuilder()
                .applicationId(APPLICATION)
                .planId(PLAN)
                .customApiKey(null)
                .build();

            doReturn(
                SubscriptionFixtures
                    .aSubscriptionEntity()
                    .toBuilder()
                    .id(SUBSCRIPTION)
                    .application(APPLICATION)
                    .plan(PLAN)
                    .status(SubscriptionStatus.PENDING)
                    .build()
            )
                .when(subscriptionService)
                .create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), any());
            when(acceptSubscriptionUseCase.execute(any()))
                .thenReturn(
                    new AcceptSubscriptionUseCase.Output(
                        fixtures.core.model.SubscriptionFixtures
                            .aSubscription()
                            .toBuilder()
                            .id(SUBSCRIPTION)
                            .planId(PLAN)
                            .applicationId(APPLICATION)
                            .status(io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status.ACCEPTED)
                            .build()
                    )
                );

            final Response response = target.request().post(Entity.json(createSubscription));
            assertThat(response)
                .hasStatus(CREATED_201)
                .asEntity(Subscription.class)
                .satisfies(subscription -> {
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(subscription.getId()).isEqualTo(SUBSCRIPTION);
                        soft.assertThat(subscription.getPlan()).extracting(BasePlan::getId).isEqualTo(PLAN);
                        soft.assertThat(subscription.getApplication()).extracting(BaseApplication::getId).isEqualTo(APPLICATION);
                    });
                });
        }
    }

    @Nested
    class Accept {

        @BeforeEach
        void setUp() {
            target = target.path(SUBSCRIPTION).path("_accept");
        }

        @Test
        public void should_accept_subscription() {
            final var acceptSubscription = SubscriptionFixtures.anAcceptSubscription();

            doReturn(
                new AcceptSubscriptionUseCase.Output(
                    fixtures.core.model.SubscriptionFixtures
                        .aSubscription()
                        .toBuilder()
                        .id(SUBSCRIPTION)
                        .planId(PLAN)
                        .applicationId(APPLICATION)
                        .build()
                )
            )
                .when(acceptSubscriptionUseCase)
                .execute(any());

            final Response response = target.request().post(Entity.json(acceptSubscription));
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(Subscription.class)
                .satisfies(subscription -> {
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(subscription.getId()).isEqualTo(SUBSCRIPTION);
                        soft.assertThat(subscription.getPlan()).extracting(BasePlan::getId).isEqualTo(PLAN);
                        soft.assertThat(subscription.getApplication()).extracting(BaseApplication::getId).isEqualTo(APPLICATION);
                    });
                });

            var captor = ArgumentCaptor.forClass(AcceptSubscriptionUseCase.Input.class);
            verify(acceptSubscriptionUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                var input = captor.getValue();
                soft.assertThat(input.subscriptionId()).isEqualTo(SUBSCRIPTION);
                soft.assertThat(input.apiId()).isEqualTo(API);
                soft.assertThat(input.startingAt()).isEqualTo(Objects.requireNonNull(acceptSubscription.getStartingAt()).toZonedDateTime());
                soft.assertThat(input.endingAt()).isEqualTo(Objects.requireNonNull(acceptSubscription.getEndingAt()).toZonedDateTime());
                soft.assertThat(input.reasonMessage()).isEqualTo(acceptSubscription.getReason());
                soft.assertThat(input.customKey()).isEqualTo(acceptSubscription.getCustomApiKey());
            });
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_SUBSCRIPTION),
                    eq(API),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);

            final Response response = target.request().post(Entity.json(SubscriptionFixtures.anAcceptSubscription()));
            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }
    }

    @Nested
    class Reject {

        @BeforeEach
        void setUp() {
            target = target.path(SUBSCRIPTION).path("_reject");
        }

        @Test
        public void should_reject_subscription() {
            final var rejectPayload = SubscriptionFixtures.aRejectSubscription();

            doReturn(
                new RejectSubscriptionUseCase.Output(
                    fixtures.core.model.SubscriptionFixtures
                        .aSubscription()
                        .toBuilder()
                        .id(SUBSCRIPTION)
                        .planId(PLAN)
                        .applicationId(APPLICATION)
                        .status(io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status.REJECTED)
                        .build()
                )
            )
                .when(rejectSubscriptionUseCase)
                .execute(any());

            final Response response = target.request().post(Entity.json(rejectPayload));
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(Subscription.class)
                .satisfies(subscription -> {
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(subscription.getId()).isEqualTo(SUBSCRIPTION);
                        soft
                            .assertThat(subscription.getStatus())
                            .isEqualTo(io.gravitee.rest.api.management.v2.rest.model.SubscriptionStatus.REJECTED);
                    });
                });

            var captor = ArgumentCaptor.forClass(RejectSubscriptionUseCase.Input.class);
            verify(rejectSubscriptionUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                var input = captor.getValue();
                soft.assertThat(input.subscriptionId()).isEqualTo(SUBSCRIPTION);
                soft.assertThat(input.apiId()).isEqualTo(API);
                soft.assertThat(input.reasonMessage()).isEqualTo(rejectPayload.getReason());
            });
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_SUBSCRIPTION),
                    eq(API),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);

            final Response response = target.request().post(Entity.json(SubscriptionFixtures.aRejectSubscription()));
            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }
    }
}
