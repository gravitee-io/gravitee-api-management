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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.use_case.CreateSubscriptionUseCase;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormValidationException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionMetadataQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.portal.rest.model.ApiKeyModeEnum;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionConfigurationInput;
import io.gravitee.rest.api.portal.rest.model.SubscriptionInput;
import io.gravitee.rest.api.portal.rest.model.SubscriptionsResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.exception.SubscriptionMetadataInvalidException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionsResourceTest extends AbstractResourceTest {

    private static final String SUBSCRIPTION = "my-subscription";
    private static final String ANOTHER_SUBSCRIPTION = "my-other-subscription";
    private static final String API = "my-api";
    private static final String API_PRODUCT = "my-api-product";
    private static final String ANOTHER_API_PRODUCT = "my-other-api-product";
    private static final String APPLICATION = "my-application";
    private static final String PLAN = "my-plan";

    @Autowired
    private ApiProductQueryServiceInMemory apiProductQueryService;

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

    @Override
    protected String contextPath() {
        return "subscriptions";
    }

    @BeforeEach
    public void init() {
        apiProductQueryService.reset();
        portalNavigationItemsQueryService.reset();
        resetAllMocks();

        io.gravitee.rest.api.model.SubscriptionEntity subscriptionEntity1 = new io.gravitee.rest.api.model.SubscriptionEntity();
        subscriptionEntity1.setId(SUBSCRIPTION);
        subscriptionEntity1.setStatus(SubscriptionStatus.ACCEPTED);
        io.gravitee.rest.api.model.SubscriptionEntity subscriptionEntity2 = new io.gravitee.rest.api.model.SubscriptionEntity();
        subscriptionEntity2.setId(ANOTHER_SUBSCRIPTION);
        subscriptionEntity2.setStatus(SubscriptionStatus.ACCEPTED);
        final Page<io.gravitee.rest.api.model.SubscriptionEntity> subscriptionPage = new Page<>(
            asList(subscriptionEntity1, subscriptionEntity2),
            0,
            1,
            2
        );
        doReturn(subscriptionPage.getContent()).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());
        doReturn(subscriptionPage).when(subscriptionService).search(any(), any(), any());

        // Core subscription entity returned by UseCase
        SubscriptionEntity coreSubscriptionEntity = SubscriptionEntity.builder().id(SUBSCRIPTION).build();
        doReturn(new CreateSubscriptionUseCase.Output(coreSubscriptionEntity))
            .when(createSubscriptionUseCase)
            .execute(any(CreateSubscriptionUseCase.Input.class));

        io.gravitee.rest.api.model.SubscriptionEntity subscriptionEntity = new io.gravitee.rest.api.model.SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION);
        subscriptionEntity.setStatus(SubscriptionStatus.ACCEPTED);
        subscriptionEntity.setReferenceId(API);
        subscriptionEntity.setReferenceType("API");
        subscriptionEntity.setApplication(APPLICATION);
        doReturn(subscriptionEntity).when(subscriptionService).findById(eq(SUBSCRIPTION));
        doReturn(true).when(permissionService).hasPermission(any(), any(), any(), any());

        PlanEntity planEntity = new PlanEntity();
        planEntity.setApi(API);
        planEntity.setReferenceId(API);
        planEntity.setReferenceType(GenericPlanEntity.ReferenceType.API);
        doReturn(planEntity).when(planSearchService).findById(GraviteeContext.getExecutionContext(), PLAN);
    }

    @AfterEach
    void resetApiProductData() {
        apiProductQueryService.reset();
        portalNavigationItemsQueryService.reset();
    }

    @Test
    public void shouldGetSubscriptionsForApi() {
        final ApplicationListItem application = new ApplicationListItem();
        application.setId(APPLICATION);

        doReturn(newSet(application)).when(applicationService).findByUser(eq(GraviteeContext.getExecutionContext()), any());

        Metadata metadata = new Metadata();
        metadata.put("api-id", "name", "My api");
        doReturn(metadata).when(subscriptionService).getMetadata(eq(GraviteeContext.getExecutionContext()), any());

        final Response response = target().queryParam("apiId", API).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        SubscriptionsResponse subscriptionResponse = response.readEntity(SubscriptionsResponse.class);
        assertEquals(2, subscriptionResponse.getData().size());
    }

    @Test
    public void shouldGetNoSubscription() {
        final Response response = target().queryParam("page", 10).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        SubscriptionsResponse subscriptionResponse = response.readEntity(SubscriptionsResponse.class);
        assertEquals(0, subscriptionResponse.getData().size());
    }

    @Test
    public void shouldGetNoPublishedApiAndNoLink() {
        final Page<io.gravitee.rest.api.model.SubscriptionEntity> subscriptionPage = new Page<>(emptyList(), 0, 1, 2);
        doReturn(subscriptionPage).when(subscriptionService).search(any(), any(), any());

        //Test with default limit
        final Response response = target().queryParam("apiId", API).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        SubscriptionsResponse subscriptionResponse = response.readEntity(SubscriptionsResponse.class);
        assertEquals(0, subscriptionResponse.getData().size());

        Links links = subscriptionResponse.getLinks();
        assertNull(links);

        //Test with small limit
        final Response anotherResponse = target().queryParam("apiId", API).queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());

        subscriptionResponse = anotherResponse.readEntity(SubscriptionsResponse.class);
        assertEquals(0, subscriptionResponse.getData().size());

        links = subscriptionResponse.getLinks();
        assertNull(links);
    }

    @Test
    public void shouldCreateSubscription() {
        SubscriptionConfigurationInput configuration = new SubscriptionConfigurationInput();
        configuration.setEntrypointConfiguration(new SubscriptionConfiguration("my-url"));
        SubscriptionInput subscriptionInput = new SubscriptionInput()
            .application(APPLICATION)
            .plan(PLAN)
            .metadata(Map.of("my-metadata", "my-value"))
            ._configuration(configuration)
            .request("request")
            .apiKeyMode(ApiKeyModeEnum.EXCLUSIVE);

        final ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        final Key key = new Key();
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION)).thenReturn(
            Collections.singletonList(apiKeyEntity)
        );
        when(keyMapper.convert(apiKeyEntity)).thenReturn(key);

        final Response response = target().request().post(Entity.json(subscriptionInput));
        assertEquals(OK_200, response.getStatus());

        ArgumentCaptor<CreateSubscriptionUseCase.Input> argument = ArgumentCaptor.forClass(CreateSubscriptionUseCase.Input.class);
        verify(createSubscriptionUseCase).execute(argument.capture());
        CreateSubscriptionUseCase.Input useCaseInput = argument.getValue();
        assertEquals(APPLICATION, useCaseInput.applicationId());
        assertEquals(PLAN, useCaseInput.planId());
        assertEquals("request", useCaseInput.requestMessage());
        assertEquals(Map.of("my-metadata", "my-value"), useCaseInput.metadata());
        assertEquals("{\"url\":\"my-url\"}", useCaseInput.configuration().getEntrypointConfiguration());
        assertEquals(ApiKeyMode.EXCLUSIVE, useCaseInput.apiKeyMode());
        assertEquals(API, useCaseInput.referenceId());
        assertEquals(io.gravitee.apim.core.subscription.model.SubscriptionReferenceType.API, useCaseInput.referenceType());
        assertTrue(Boolean.TRUE.equals(useCaseInput.subscriptionFormMetadataValidationRequired()));

        final Subscription subscriptionResponse = response.readEntity(Subscription.class);
        assertNotNull(subscriptionResponse);
        assertEquals(SUBSCRIPTION, subscriptionResponse.getId());
        assertNotNull(subscriptionResponse.getKeys());
        assertEquals(1, subscriptionResponse.getKeys().size());
        assertEquals(key, subscriptionResponse.getKeys().get(0));
    }

    @Test
    void shouldCreateApiProductSubscriptionWhenProductIsAccessible() {
        mockApiProductPlan();
        exposeApiProductInPortal();

        io.gravitee.rest.api.model.SubscriptionEntity subscriptionEntity = new io.gravitee.rest.api.model.SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION);
        subscriptionEntity.setStatus(SubscriptionStatus.ACCEPTED);
        subscriptionEntity.setReferenceId(API_PRODUCT);
        subscriptionEntity.setReferenceType("API_PRODUCT");
        subscriptionEntity.setApplication(APPLICATION);
        doReturn(subscriptionEntity).when(subscriptionService).findById(SUBSCRIPTION);

        SubscriptionInput subscriptionInput = new SubscriptionInput().application(APPLICATION).plan(PLAN).metadata(Map.of("key", "value"));
        subscriptionInput.setGeneralConditionsAccepted(true);

        Response response = target().request().post(Entity.json(subscriptionInput));

        assertEquals(OK_200, response.getStatus());

        ArgumentCaptor<CreateSubscriptionUseCase.Input> inputCaptor = ArgumentCaptor.forClass(CreateSubscriptionUseCase.Input.class);
        verify(createSubscriptionUseCase).execute(inputCaptor.capture());
        CreateSubscriptionUseCase.Input useCaseInput = inputCaptor.getValue();
        assertEquals(API_PRODUCT, useCaseInput.referenceId());
        assertEquals(io.gravitee.apim.core.subscription.model.SubscriptionReferenceType.API_PRODUCT, useCaseInput.referenceType());
        assertEquals(Map.of("key", "value"), useCaseInput.metadata());
        assertFalse(Boolean.TRUE.equals(useCaseInput.subscriptionFormMetadataValidationRequired()));
        assertNull(useCaseInput.generalConditionsAccepted());
        assertNull(useCaseInput.generalConditionsContentRevision());

        Subscription subscription = response.readEntity(Subscription.class);
        assertNull(subscription.getApi());
        assertEquals(API_PRODUCT, subscription.getReferenceId());
        assertEquals(Subscription.ReferenceTypeEnum.API_PRODUCT, subscription.getReferenceType());
    }

    @Test
    void shouldReturnNotFoundAndNotCreateSubscriptionWhenApiProductIsNotAccessible() {
        mockApiProductPlan();
        initApiProduct();

        Response response = target().request().post(Entity.json(new SubscriptionInput().application(APPLICATION).plan(PLAN)));

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        verify(createSubscriptionUseCase, never()).execute(any());
    }

    @Test
    void shouldFilterSubscriptionsByApiProductIdsAndIncludeApiProductMetadata() {
        mockCurrentUserApplication();
        Metadata metadata = new Metadata();
        metadata.put(API_PRODUCT, "name", "My API Product");
        doReturn(metadata).when(subscriptionService).getMetadata(eq(GraviteeContext.getExecutionContext()), any());

        Response response = target().queryParam("apiProductIds", API_PRODUCT, ANOTHER_API_PRODUCT).request().get();

        assertEquals(OK_200, response.getStatus());

        ArgumentCaptor<SubscriptionQuery> queryCaptor = ArgumentCaptor.forClass(SubscriptionQuery.class);
        verify(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), queryCaptor.capture(), any());
        assertEquals(List.of(API_PRODUCT, ANOTHER_API_PRODUCT), queryCaptor.getValue().getApiProducts());
        assertNull(queryCaptor.getValue().getApis());
        assertNull(queryCaptor.getValue().getReferenceType());

        ArgumentCaptor<SubscriptionMetadataQuery> metadataQueryCaptor = ArgumentCaptor.forClass(SubscriptionMetadataQuery.class);
        verify(subscriptionService).getMetadata(eq(GraviteeContext.getExecutionContext()), metadataQueryCaptor.capture());
        assertTrue(metadataQueryCaptor.getValue().ifApiProducts().isPresent());
    }

    @Test
    void shouldRejectCombinedApiAndApiProductFilters() {
        Response response = target().queryParam("apiIds", API).queryParam("apiProductIds", API_PRODUCT).request().get();

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    void shouldRejectCombinedDeprecatedApiAndApiProductFilters() {
        Response response = target().queryParam("apiId", API).queryParam("apiProductIds", API_PRODUCT).request().get();

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    void shouldReturnBadRequestWhenMetadataKeyIsInvalid() {
        doThrow(new SubscriptionMetadataInvalidException("Invalid metadata key."))
            .when(createSubscriptionUseCase)
            .execute(argThat(input -> input.metadata() != null && input.metadata().containsKey("bad key")));

        SubscriptionInput subscriptionInput = new SubscriptionInput()
            .application(APPLICATION)
            .plan(PLAN)
            .metadata(Map.of("bad key", "value"));

        final Response response = target().request().post(Entity.json(subscriptionInput));
        Assertions.assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        verify(createSubscriptionUseCase, times(1)).execute(any());
    }

    @Test
    void shouldReturnBadRequestWhenSubscriptionFormMetadataIsInvalid() {
        doThrow(new SubscriptionFormValidationException(List.of("Field 'email' is required")))
            .when(createSubscriptionUseCase)
            .execute(any());

        SubscriptionInput subscriptionInput = new SubscriptionInput().application(APPLICATION).plan(PLAN).metadata(Map.of());

        final Response response = target().request().post(Entity.json(subscriptionInput));
        Assertions.assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        verify(createSubscriptionUseCase, times(1)).execute(any());
    }

    @Test
    public void shouldHaveBadRequestWhileCreatingSubscription() {
        final Response response = target().request().post(Entity.json(null));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void testPermissionsForCreation() {
        reset(permissionService);

        SubscriptionInput subscriptionInput = new SubscriptionInput()
            .application(APPLICATION)
            .plan(PLAN)
            .request("request")
            .apiKeyMode(ApiKeyModeEnum.EXCLUSIVE);

        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.CREATE)
            );
        Response response = target().request().post(Entity.json(subscriptionInput));
        assertEquals(OK_200, response.getStatus());

        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.CREATE)
            );
        response = target().request().post(Entity.json(subscriptionInput));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void testPermissionsForListing() {
        reset(permissionService);

        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.READ)
            );
        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.READ)
            );

        Metadata metadata = new Metadata();
        metadata.put("api-id", "name", "My api");
        doReturn(metadata).when(subscriptionService).getMetadata(eq(GraviteeContext.getExecutionContext()), any());

        assertEquals(OK_200, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());

        //-----

        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.READ)
            );
        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.READ)
            );

        assertEquals(FORBIDDEN_403, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).request().get().getStatus());

        assertEquals(FORBIDDEN_403, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());

        //----

        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.READ)
            );
        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.READ)
            );

        assertEquals(OK_200, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());

        //----

        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.READ)
            );
        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.READ)
            );

        assertEquals(FORBIDDEN_403, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).request().get().getStatus());
        assertEquals(FORBIDDEN_403, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());
    }

    private void mockApiProductPlan() {
        PlanEntity planEntity = new PlanEntity();
        planEntity.setReferenceId(API_PRODUCT);
        planEntity.setReferenceType(GenericPlanEntity.ReferenceType.API_PRODUCT);
        doReturn(planEntity).when(planSearchService).findById(GraviteeContext.getExecutionContext(), PLAN);
    }

    private void exposeApiProductInPortal() {
        initApiProduct();
        portalNavigationItemsQueryService.initWith(
            List.of(
                PortalNavigationApiProduct.builder()
                    .id(PortalNavigationItemId.random())
                    .organizationId("organization-id")
                    .environmentId(GraviteeContext.getCurrentEnvironment())
                    .title("API Product")
                    .segment("api-product")
                    .area(PortalArea.TOP_NAVBAR)
                    .order(0)
                    .apiProductId(API_PRODUCT)
                    .published(true)
                    .visibility(PortalVisibility.PUBLIC)
                    .build()
            )
        );
    }

    private void initApiProduct() {
        apiProductQueryService.initWith(
            List.of(ApiProduct.builder().id(API_PRODUCT).environmentId(GraviteeContext.getCurrentEnvironment()).name("API Product").build())
        );
    }

    private void mockCurrentUserApplication() {
        ApplicationListItem application = new ApplicationListItem();
        application.setId(APPLICATION);
        doReturn(newSet(application)).when(applicationService).findByUser(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Getter
    @AllArgsConstructor
    private class SubscriptionConfiguration {

        private String url;
    }
}
