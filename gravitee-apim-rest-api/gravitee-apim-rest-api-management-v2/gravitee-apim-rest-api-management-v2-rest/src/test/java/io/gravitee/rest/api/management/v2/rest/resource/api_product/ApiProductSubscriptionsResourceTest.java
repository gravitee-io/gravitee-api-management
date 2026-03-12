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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.PlanFixtures;
import fixtures.SubscriptionFixtures;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.use_case.AcceptSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.CreateSubscriptionUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.v2.rest.model.ApiKeyMode;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.VerifySubscription;
import io.gravitee.rest.api.management.v2.rest.model.VerifySubscriptionResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

class ApiProductSubscriptionsResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";

    @Inject
    private CreateSubscriptionUseCase createSubscriptionUseCase;

    @Inject
    private AcceptSubscriptionUseCase acceptSubscriptionUseCase;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private PlanSearchService planSearchService;

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
        reset(createSubscriptionUseCase, acceptSubscriptionUseCase, subscriptionService, apiKeyService, planSearchService);
    }

    @Nested
    class VerifyCreateApiProductSubscriptionTest {

        private static final String APPLICATION_ID = "my-application";

        @BeforeEach
        void before() {
            reset(apiKeyService);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.CREATE, () ->
                rootTarget().path("_verify").request().post(json(SubscriptionFixtures.aVerifySubscription()))
            );
        }

        @Test
        void should_return_400_when_invalid_api_key_pattern() {
            Response response = rootTarget().path("_verify").request().post(json(SubscriptionFixtures.aVerifySubscription().apiKey("###")));
            assertEquals(BAD_REQUEST_400, response.getStatus());

            Error error = response.readEntity(Error.class);
            assertEquals(BAD_REQUEST_400, (int) error.getHttpStatus());
            assertEquals("Validation error", error.getMessage());
        }

        @Test
        void should_return_400_when_missing_api_key() {
            Response response = rootTarget().path("_verify").request().post(json(SubscriptionFixtures.aVerifySubscription().apiKey(null)));
            assertEquals(BAD_REQUEST_400, response.getStatus());

            Error error = response.readEntity(Error.class);
            assertEquals(BAD_REQUEST_400, (int) error.getHttpStatus());
            assertEquals("Validation error", error.getMessage());
        }

        @Test
        void should_return_400_when_missing_application() {
            Response response = rootTarget()
                .path("_verify")
                .request()
                .post(json(SubscriptionFixtures.aVerifySubscription().applicationId(null)));
            assertEquals(BAD_REQUEST_400, response.getStatus());

            Error error = response.readEntity(Error.class);
            assertEquals(BAD_REQUEST_400, (int) error.getHttpStatus());
            assertEquals("Validation error", error.getMessage());
        }

        @Test
        void should_verify_subscription_ok_true() {
            when(
                apiKeyService.canCreate(
                    eq(GraviteeContext.getExecutionContext()),
                    eq("apiKey"),
                    eq(API_PRODUCT_ID),
                    eq(SubscriptionReferenceType.API_PRODUCT.name()),
                    eq(APPLICATION_ID)
                )
            ).thenReturn(true);

            VerifySubscription verifySubscription = SubscriptionFixtures.aVerifySubscription()
                .applicationId(APPLICATION_ID)
                .apiKey("apiKey");
            Response response = rootTarget().path("_verify").request().post(json(verifySubscription));
            assertEquals(OK_200, response.getStatus());

            VerifySubscriptionResponse verifyResponse = response.readEntity(VerifySubscriptionResponse.class);
            assertTrue(verifyResponse.getOk());
        }

        @Test
        void should_verify_subscription_ok_false() {
            when(
                apiKeyService.canCreate(
                    eq(GraviteeContext.getExecutionContext()),
                    eq("apiKey"),
                    eq(API_PRODUCT_ID),
                    eq(SubscriptionReferenceType.API_PRODUCT.name()),
                    eq(APPLICATION_ID)
                )
            ).thenReturn(false);

            VerifySubscription verifySubscription = SubscriptionFixtures.aVerifySubscription()
                .applicationId(APPLICATION_ID)
                .apiKey("apiKey");
            Response response = rootTarget().path("_verify").request().post(json(verifySubscription));
            assertEquals(OK_200, response.getStatus());

            VerifySubscriptionResponse verifyResponse = response.readEntity(VerifySubscriptionResponse.class);
            assertFalse(verifyResponse.getOk());
        }
    }

    @Nested
    class CanCreateApiProductSubscriptionTest {

        private static final String APPLICATION_ID = "my-application";
        private static final String API_KEY = "valid-api-key";

        @BeforeEach
        void before() {
            reset(apiKeyService);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_SUBSCRIPTION, API_PRODUCT_ID, RolePermissionAction.READ, () ->
                rootTarget().path("_canCreate").queryParam("key", API_KEY).queryParam("application", APPLICATION_ID).request().get()
            );
        }

        @Test
        void should_return_400_when_key_query_param_omitted() {
            Response response = rootTarget().path("_canCreate").queryParam("application", APPLICATION_ID).request().get();
            assertEquals(BAD_REQUEST_400, response.getStatus());
            verifyNoInteractions(apiKeyService);
        }

        @Test
        void should_return_400_when_key_query_param_invalid_format() {
            Response response = rootTarget()
                .path("_canCreate")
                .queryParam("key", "short")
                .queryParam("application", APPLICATION_ID)
                .request()
                .get();
            assertEquals(BAD_REQUEST_400, response.getStatus());
            verifyNoInteractions(apiKeyService);
        }

        @Test
        void should_return_400_when_application_query_param_omitted() {
            Response response = rootTarget().path("_canCreate").queryParam("key", API_KEY).request().get();
            assertEquals(BAD_REQUEST_400, response.getStatus());
            verifyNoInteractions(apiKeyService);
        }

        @Test
        void should_return_200_true_when_canCreate_returns_true() {
            when(
                apiKeyService.canCreate(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(API_KEY),
                    eq(API_PRODUCT_ID),
                    eq(SubscriptionReferenceType.API_PRODUCT.name()),
                    eq(APPLICATION_ID)
                )
            ).thenReturn(true);

            Response response = rootTarget()
                .path("_canCreate")
                .queryParam("key", API_KEY)
                .queryParam("application", APPLICATION_ID)
                .request()
                .get();
            assertEquals(OK_200, response.getStatus());
            assertTrue(response.readEntity(Boolean.class));
        }

        @Test
        void should_return_200_false_when_canCreate_returns_false() {
            when(
                apiKeyService.canCreate(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(API_KEY),
                    eq(API_PRODUCT_ID),
                    eq(SubscriptionReferenceType.API_PRODUCT.name()),
                    eq(APPLICATION_ID)
                )
            ).thenReturn(false);

            Response response = rootTarget()
                .path("_canCreate")
                .queryParam("key", API_KEY)
                .queryParam("application", APPLICATION_ID)
                .request()
                .get();
            assertEquals(OK_200, response.getStatus());
            assertFalse(response.readEntity(Boolean.class));
        }
    }

    @Nested
    class ListSubscriptionsTest {

        @Test
        void should_return_empty_list_when_no_subscriptions() {
            when(
                subscriptionService.search(
                    eq(GraviteeContext.getExecutionContext()),
                    argThat(
                        (SubscriptionQuery q) ->
                            API_PRODUCT_ID.equals(q.getReferenceId()) && q.getReferenceType() == GenericPlanEntity.ReferenceType.API_PRODUCT
                    ),
                    any(),
                    eq(false),
                    eq(false)
                )
            ).thenReturn(new Page<>(List.of(), 1, 10, 0));

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.SubscriptionsResponse.class);
            assertThat(body.getData()).isEmpty();
        }

        @Test
        void should_return_subscriptions_list() {
            SubscriptionEntity sub = SubscriptionFixtures.aSubscriptionEntity()
                .toBuilder()
                .id("sub-1")
                .referenceId(API_PRODUCT_ID)
                .referenceType("API_PRODUCT")
                .plan("plan-1")
                .application("app-1")
                .build();
            when(
                subscriptionService.search(
                    eq(GraviteeContext.getExecutionContext()),
                    argThat(
                        (SubscriptionQuery q) ->
                            API_PRODUCT_ID.equals(q.getReferenceId()) && q.getReferenceType() == GenericPlanEntity.ReferenceType.API_PRODUCT
                    ),
                    any(),
                    eq(false),
                    eq(false)
                )
            ).thenReturn(new Page<>(List.of(sub), 1, 10, 1));

            Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.SubscriptionsResponse.class);
            assertThat(body.getData()).hasSize(1);
            var subscription = body.getData().get(0);
            assertThat(subscription.getId()).isEqualTo("sub-1");
            assertThat(subscription.getApiProduct()).isNotNull();
            assertThat(subscription.getApiProduct().getId()).isEqualTo(API_PRODUCT_ID);
            assertThat(subscription.getApi()).isNull();
        }

        @Test
        void should_return_plan_with_apiProduct_and_no_apiId_when_expand_plan() {
            String planId = "plan-1";
            SubscriptionEntity sub = SubscriptionFixtures.aSubscriptionEntity()
                .toBuilder()
                .id("sub-1")
                .referenceId(API_PRODUCT_ID)
                .referenceType("API_PRODUCT")
                .plan(planId)
                .application("app-1")
                .build();
            when(
                subscriptionService.search(
                    eq(GraviteeContext.getExecutionContext()),
                    argThat(
                        (SubscriptionQuery q) ->
                            API_PRODUCT_ID.equals(q.getReferenceId()) && q.getReferenceType() == GenericPlanEntity.ReferenceType.API_PRODUCT
                    ),
                    any(),
                    eq(false),
                    eq(false)
                )
            ).thenReturn(new Page<>(List.of(sub), 1, 10, 1));

            var planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(planId).referenceId(API_PRODUCT_ID).build();
            when(planSearchService.findByIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(planId)))).thenReturn(
                Set.of(planEntity)
            );

            Response response = rootTarget().queryParam("expands", "plan").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.SubscriptionsResponse.class);
            assertThat(body.getData()).hasSize(1);
            var subscription = body.getData().get(0);
            assertThat(subscription.getPlan()).isNotNull();
            assertThat(subscription.getPlan().getId()).isEqualTo(planId);
            assertThat(subscription.getPlan().getApiId()).isNull();
            assertThat(subscription.getPlan().getApiProductId()).isEqualTo(API_PRODUCT_ID);
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
            io.gravitee.apim.core.subscription.model.SubscriptionEntity created =
                io.gravitee.apim.core.subscription.model.SubscriptionEntity.builder()
                    .id("new-sub-id")
                    .apiId(null)
                    .referenceId(API_PRODUCT_ID)
                    .referenceType(SubscriptionReferenceType.API_PRODUCT)
                    .planId("plan-1")
                    .applicationId("app-1")
                    .status(io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status.PENDING)
                    .createdAt(ZonedDateTime.now())
                    .updatedAt(ZonedDateTime.now())
                    .build();
            when(createSubscriptionUseCase.execute(any())).thenReturn(new CreateSubscriptionUseCase.Output(created));
            doReturn(
                new AcceptSubscriptionUseCase.Output(
                    fixtures.core.model.SubscriptionFixtures.aSubscription()
                        .toBuilder()
                        .id("new-sub-id")
                        .planId("plan-1")
                        .applicationId("app-1")
                        .status(io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status.ACCEPTED)
                        .build()
                )
            )
                .when(acceptSubscriptionUseCase)
                .execute(any());

            var createPayload = new io.gravitee.rest.api.management.v2.rest.model.CreateSubscription();
            createPayload.setPlanId("plan-1");
            createPayload.setApplicationId("app-1");

            Response response = rootTarget().request().post(json(createPayload));

            MAPIAssertions.assertThat(response).hasStatus(CREATED_201);
            assertThat(response.getLocation().getPath()).contains("new-sub-id");

            var subscription = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Subscription.class);
            assertThat(subscription.getApi()).isNull();
            assertThat(subscription.getApiProduct()).isNotNull();
            assertThat(subscription.getApiProduct().getId()).isEqualTo(API_PRODUCT_ID);

            var captor = ArgumentCaptor.forClass(CreateSubscriptionUseCase.Input.class);
            verify(createSubscriptionUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(captor.getValue().referenceId()).isEqualTo(API_PRODUCT_ID);
                soft.assertThat(captor.getValue().referenceType()).isEqualTo(SubscriptionReferenceType.API_PRODUCT);
                soft.assertThat(captor.getValue().planId()).isEqualTo("plan-1");
                soft.assertThat(captor.getValue().applicationId()).isEqualTo("app-1");
                soft.assertThat(captor.getValue().apiKeyMode()).isNull();
            });
            verify(acceptSubscriptionUseCase).execute(any());
        }

        @ParameterizedTest
        @EnumSource(value = ApiKeyMode.class, names = { "SHARED", "EXCLUSIVE" })
        void should_create_subscription_with_api_key_mode(ApiKeyMode apiKeyMode) {
            io.gravitee.apim.core.subscription.model.SubscriptionEntity created =
                io.gravitee.apim.core.subscription.model.SubscriptionEntity.builder()
                    .id("new-sub-id")
                    .apiId(null)
                    .referenceId(API_PRODUCT_ID)
                    .referenceType(SubscriptionReferenceType.API_PRODUCT)
                    .planId("plan-1")
                    .applicationId("app-1")
                    .status(io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status.ACCEPTED)
                    .createdAt(ZonedDateTime.now())
                    .updatedAt(ZonedDateTime.now())
                    .build();
            when(createSubscriptionUseCase.execute(any())).thenReturn(new CreateSubscriptionUseCase.Output(created));

            var createPayload = new io.gravitee.rest.api.management.v2.rest.model.CreateSubscription();
            createPayload.setPlanId("plan-1");
            createPayload.setApplicationId("app-1");
            createPayload.setApiKeyMode(apiKeyMode);

            Response response = rootTarget().request().post(json(createPayload));

            MAPIAssertions.assertThat(response).hasStatus(CREATED_201);

            var captor = ArgumentCaptor.forClass(CreateSubscriptionUseCase.Input.class);
            verify(createSubscriptionUseCase).execute(captor.capture());
            assertThat(captor.getValue().apiKeyMode()).isEqualTo(io.gravitee.rest.api.model.ApiKeyMode.valueOf(apiKeyMode.name()));
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

    @Nested
    class ExportTest {

        private static final String CSV_HEADERS = "Plan;Application;Creation date;Process date;Start date;End date date;Status";
        private static final String CSV_CONTENT =
            "Plan value;Application value;Creation date value;Process date value;Start date value;End date date value;Status value";
        private static final String CSV = CSV_HEADERS + "\n" + CSV_CONTENT;

        @Test
        void should_return_empty_export_if_no_subscriptions() {
            when(
                subscriptionService.search(
                    eq(GraviteeContext.getExecutionContext()),
                    argThat(
                        q ->
                            API_PRODUCT_ID.equals(q.getReferenceId()) && q.getReferenceType() == GenericPlanEntity.ReferenceType.API_PRODUCT
                    ),
                    any(),
                    eq(false),
                    eq(false)
                )
            ).thenReturn(new Page<>(List.of(), 0, 0, 0));

            when(subscriptionService.exportAsCsv(any(), any())).thenReturn(CSV_HEADERS);

            final Response response = rootTarget().path("_export").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.getHeaderString("content-disposition")).matches(
                "attachment;filename=subscriptions-" + API_PRODUCT_ID + "-\\d+.csv"
            );

            var csv = response.readEntity(String.class);
            assertThat(csv).isEqualTo(CSV_HEADERS);

            verify(subscriptionService).exportAsCsv(List.of(), Map.of());
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_PRODUCT_SUBSCRIPTION),
                    eq(API_PRODUCT_ID),
                    eq(RolePermissionAction.READ)
                )
            ).thenReturn(false);

            final Response response = rootTarget().path("_export").request().get();
            assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);

            var error = response.readEntity(Error.class);
            assertThat(error.getHttpStatus()).isEqualTo(FORBIDDEN_403);
            assertThat(error.getMessage()).isEqualTo("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_export_subscriptions_as_csv() {
            final List<SubscriptionEntity> subscriptionEntities = List.of(
                SubscriptionFixtures.aSubscriptionEntity()
                    .toBuilder()
                    .id("subscription-1")
                    .referenceId(API_PRODUCT_ID)
                    .referenceType("API_PRODUCT")
                    .build(),
                SubscriptionFixtures.aSubscriptionEntity()
                    .toBuilder()
                    .id("subscription-2")
                    .referenceId(API_PRODUCT_ID)
                    .referenceType("API_PRODUCT")
                    .build()
            );
            when(
                subscriptionService.search(
                    eq(GraviteeContext.getExecutionContext()),
                    argThat(
                        q ->
                            API_PRODUCT_ID.equals(q.getReferenceId()) && q.getReferenceType() == GenericPlanEntity.ReferenceType.API_PRODUCT
                    ),
                    any(),
                    eq(false),
                    eq(false)
                )
            ).thenReturn(new Page<>(subscriptionEntities, 1, 10, 2));

            final Metadata metadata = new Metadata();
            metadata.put(API_PRODUCT_ID, "name", "my-api-product-name");
            metadata.put("plan-1", "name", "my-plan-name");
            metadata.put("app-1", "name", "my-application-name");
            when(subscriptionService.getMetadata(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(metadata);
            when(subscriptionService.exportAsCsv(any(), any())).thenReturn(CSV);

            final Response response = rootTarget().path("_export").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.getHeaderString("content-disposition")).matches(
                "attachment;filename=subscriptions-" + API_PRODUCT_ID + "-\\d+.csv"
            );

            var csv = response.readEntity(String.class);
            assertThat(csv).isEqualTo(CSV);

            verify(subscriptionService).getMetadata(
                eq(GraviteeContext.getExecutionContext()),
                argThat(metadataQuery -> {
                    assertThat(metadataQuery.getSubscriptions()).isEqualTo(subscriptionEntities);
                    assertThat(metadataQuery.getOrganization()).isEqualTo(ORGANIZATION);
                    assertThat(metadataQuery.getEnvironment()).isEqualTo(ENV_ID);
                    assertThat(metadataQuery.ifApiProducts().orElse(false)).isTrue();
                    assertThat(metadataQuery.ifApplications().orElse(false)).isTrue();
                    assertThat(metadataQuery.ifPlans().orElse(false)).isTrue();
                    return true;
                })
            );
            verify(subscriptionService).exportAsCsv(subscriptionEntities, metadata.toMap());
        }

        @Test
        void should_export_with_multi_criteria() {
            final List<SubscriptionEntity> subscriptionEntities = List.of(
                SubscriptionFixtures.aSubscriptionEntity()
                    .toBuilder()
                    .id("subscription-1")
                    .referenceId(API_PRODUCT_ID)
                    .referenceType("API_PRODUCT")
                    .build()
            );
            when(
                subscriptionService.search(
                    eq(GraviteeContext.getExecutionContext()),
                    argThat(
                        (SubscriptionQuery q) ->
                            API_PRODUCT_ID.equals(q.getReferenceId()) && q.getReferenceType() == GenericPlanEntity.ReferenceType.API_PRODUCT
                    ),
                    any(),
                    eq(false),
                    eq(false)
                )
            ).thenReturn(new Page<>(subscriptionEntities, 1, 10, 1));

            final Metadata metadata = new Metadata();
            when(subscriptionService.getMetadata(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(metadata);
            when(subscriptionService.exportAsCsv(any(), any())).thenReturn(CSV);

            final Response response = rootTarget()
                .path("_export")
                .queryParam("applicationIds", "application-1", "application-2")
                .queryParam("planIds", "plan-1", "plan-2")
                .queryParam("statuses", "ACCEPTED", "CLOSED")
                .queryParam("apiKey", "apiKey")
                .queryParam("page", 2)
                .queryParam("perPage", 20)
                .request()
                .get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.getHeaderString("content-disposition")).matches(
                "attachment;filename=subscriptions-" + API_PRODUCT_ID + "-\\d+.csv"
            );

            var csv = response.readEntity(String.class);
            assertThat(csv).isEqualTo(CSV);

            verify(subscriptionService).search(
                eq(GraviteeContext.getExecutionContext()),
                argThat(
                    (SubscriptionQuery q) ->
                        API_PRODUCT_ID.equals(q.getReferenceId()) && q.getReferenceType() == GenericPlanEntity.ReferenceType.API_PRODUCT
                ),
                any(),
                eq(false),
                eq(false)
            );
            verify(subscriptionService).getMetadata(
                eq(GraviteeContext.getExecutionContext()),
                argThat(metadataQuery -> {
                    assertThat(metadataQuery.getSubscriptions()).isEqualTo(subscriptionEntities);
                    assertThat(metadataQuery.getOrganization()).isEqualTo(ORGANIZATION);
                    assertThat(metadataQuery.getEnvironment()).isEqualTo(ENV_ID);
                    assertThat(metadataQuery.ifApiProducts().orElse(false)).isTrue();
                    assertThat(metadataQuery.ifApplications().orElse(false)).isTrue();
                    assertThat(metadataQuery.ifPlans().orElse(false)).isTrue();
                    return true;
                })
            );
            verify(subscriptionService).exportAsCsv(subscriptionEntities, metadata.toMap());
        }
    }
}
